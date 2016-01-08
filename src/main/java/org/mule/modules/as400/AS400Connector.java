/**
 * (c) 2003-2015 MuleSoft, Inc. The software in this package is published under the terms of the CPAL v1.0 license,
 * a copy of which has been included with this distribution in the LICENSE.md file.
 */

package org.mule.modules.as400;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.mule.api.annotations.ConnectionStrategy;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.ReconnectOn;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.SourceStrategy;
import org.mule.api.annotations.SourceThreadingModel;
import org.mule.api.annotations.display.FriendlyName;
import org.mule.api.annotations.display.Placement;
import org.mule.api.annotations.display.Summary;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.annotations.param.OutboundHeaders;
import org.mule.api.callback.SourceCallback;
import org.mule.modules.as400.exception.AS400CommandCallException;
import org.mule.modules.as400.exception.AS400DataQueueReadException;
import org.mule.modules.as400.exception.AS400DataQueueWriteException;
import org.mule.modules.as400.exception.AS400TransportException;
import org.mule.modules.as400.strategy.AS400ConnectionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.ConnectionDroppedException;
import com.ibm.as400.access.DataQueue;
import com.ibm.as400.access.DataQueueEntry;
import com.ibm.as400.access.KeyedDataQueue;
import com.ibm.as400.access.KeyedDataQueueEntry;

/**
 * AS400 Connector
 *
 * @author ravenugo, Dmitriy Kuznetsov
 */
@Connector(name = "as400", friendlyName = "AS400")
public class AS400Connector {
	
	private static final Logger logger = LoggerFactory.getLogger(AS400Connector.class);
    private static String LIBRARY_BASE_PATH = "/QSYS.LIB/"; 
    private static int MAX_DQ_WAIT_TIME = 30; // seconds!
    private static long RECONNECT_WAIT_TIME = 10000;
    private static int MAX_PROCESSOR_RETRIES = 3;
    private static int MAX_SOURCE_RETRIES = -1;

	
	@ConnectionStrategy
	AS400ConnectionStrategy connectionStrategy;
	
	@Source(sourceStrategy = SourceStrategy.POLLING, pollingPeriod = 1)
	public void readDataQueue(final SourceCallback callback, @Placement (order=1) @FriendlyName("Data Queue") String dtaq,@Placement (order=2) String library, @Placement (order=3)@FriendlyName("Key")@Summary("Only for keyed data queues. If not specified, the data queue is not keyed.") @Optional String dtaqKey, @Placement (order=4)@FriendlyName("Key Search Type")@Summary("GE (Greater or Equal), GT (Greater Than), LE (Lesser or equal), LT, EQ (Equal), NE (Not Equal) Required if key is specified") @Optional SearchType dtaqKeySearchType,  @Placement (order=5) @FriendlyName("Keep messages in Queue") @Default("false") Boolean peek) throws AS400DataQueueReadException, AS400TransportException {	

		Map<String,Object> result = connectAndRead(dtaq, library, dtaqKey, dtaqKeySearchType, MAX_DQ_WAIT_TIME, peek, MAX_SOURCE_RETRIES);
		try{
			if (result.containsKey("properties")) {
				callback.process(result.get("readMessage"), (Map<String,Object>) result.get("properties"));
			}else if (result.containsKey("readMessage")){
				callback.process(result.get("readMessage"));
			}
		}catch (Exception e){
			if (e instanceof AS400DataQueueReadException){
				throw (AS400DataQueueReadException) e;
			}else {
				throw new AS400DataQueueReadException("DataQueueRead error: ", e);
			}
		}
		
	}
	
	private Map<String, Object> connectAndRead(String dtaq, String library,
			String dtaqKey, SearchType dtaqKeySearchType, int dtaqwaittime,
			Boolean peek, int maxRetries) throws AS400DataQueueReadException, AS400TransportException {
		Map<String,Object> result = null;
		long starttime = System.currentTimeMillis();
		// Read for the period of up to MAX_WAIT_TIME seconds, up to max wait time or forever. Catch / reconnect automatically if connection is dropped, don't rely on Mule reconnection strategy
		do { 
			try {
				connectionStrategy.validateOrReconnect(maxRetries, RECONNECT_WAIT_TIME); // Don't give up till reconnected, reconnect every 10 seconds
				result = readDataQueue(dtaq, library, dtaqKey, dtaqKeySearchType, Math.min(dtaqwaittime, MAX_DQ_WAIT_TIME), peek);
			} catch (AS400TransportException e) {
				logger.warn("DataQueue connection issue ", e);
				connectionStrategy.validateOrReconnect(maxRetries, RECONNECT_WAIT_TIME); // Don't give up till reconnected, reconnect every 10 seconds
			}
			} while ((result == null || !result.containsKey("readMessage")) 
					&& (dtaqwaittime == -1 || System.currentTimeMillis() - starttime <= dtaqwaittime * 1000 ));
		return result;
	}

	@Processor(friendlyName="Read Data Queue")
	public String readDataQueueProcessor(@OutboundHeaders
			Map<String, Object> outboundHeaders, @Placement (order=1) @FriendlyName("Data Queue") String dtaq,@Placement (order=2) String library, @Placement (order=3)@FriendlyName("Key")@Summary("Only for keyed data queues. If not specified, the data queue is not keyed.") @Optional String dtaqKey, @Placement (order=4)@FriendlyName("Key Search Type")@Summary("GE (Greater or Equal), GT (Greater Than), LE (Lesser or equal), LT, EQ (Equal), NE (Not Equal) Required if key is specified") @Optional SearchType dtaqKeySearchType, @Placement (order=5) @FriendlyName("Max Wait Time")@Summary("Defaults to -1 (wait till entry is retrieved).") @Default("-1") int dtaqwaittime, @Placement (order=6) @FriendlyName("Keep messages in Queue") @Default("false") Boolean peek) throws AS400DataQueueReadException, AS400TransportException {

		Map<String,Object> result = connectAndRead(dtaq, library, dtaqKey, dtaqKeySearchType, dtaqwaittime, peek, MAX_PROCESSOR_RETRIES);
 
		try{
			if (result.containsKey("properties")){
				Map <String, Object> outboundProperties = (Map<String,Object>) result.get("properties");
				for (Map.Entry<String, Object> entry : outboundProperties.entrySet()) {
					outboundHeaders.put(entry.getKey(), entry.getValue());
				}
				return (String) result.get("readMessage");
			}else{
				return (String) result.get("readMessage");
			}
		}catch (Exception e){
			if (e instanceof AS400DataQueueReadException){
				throw (AS400DataQueueReadException) e;
			}else {
				throw new AS400DataQueueReadException("DataQueueRead error: ", e);
			}
		}		
	}

	
	private Map<String,Object> readDataQueue(String dtaq, String library, String dtaqKey, SearchType dtaqKeySearchType, int dtaqwaittime, Boolean peek) throws AS400DataQueueReadException, AS400TransportException {
		
		AS400 system = connectionStrategy.getSystem();
		// Verify connection
		if (! system.isConnected()) {
			logger.warn("System connection check failed when reading data queue!");
			throw new AS400TransportException("DataQueueRead connection dropped error: ");
		}
		Map<String, Object> result = new HashMap<String, Object>();
		
		try {
			String dataQueue = constructDataQueue(dtaq, library);
		
			if (dtaqKey != null) {
				KeyedDataQueue kd = new KeyedDataQueue(system, dataQueue);
				KeyedDataQueueEntry dqentry = null;
				// Pad the key value to the full length of the key with blanks 
				int keyLength = kd.getKeyLength();
				dtaqKey = padString(dtaqKey, keyLength);
				if(peek){
					dqentry = kd.peek(dtaqKey, dtaqwaittime,
							dtaqKeySearchType.getSearchType());
				}else {
					dqentry = kd.read(dtaqKey, dtaqwaittime,
							dtaqKeySearchType.getSearchType());
				}
				if (dqentry != null) {
					result.put("readMessage", dqentry.getString().trim());
					result.put("properties", createProperties(dqentry.getKeyString().trim()));
				}

			} else {
				DataQueue dq = new DataQueue(system, dataQueue);
				DataQueueEntry dqentry = null; 
				if(peek) {
					dqentry = dq.peek(dtaqwaittime);
				}else{
					dqentry = dq.read(dtaqwaittime);
				}
				if (dqentry != null) {
					result.put("readMessage", dqentry.getString().trim());
				}
			}
			return result;
		} catch (java.io.IOException e) {
			throw new AS400TransportException("DataQueueRead connection dropped error: ", e);
		}
		catch (Exception e) {
			throw new AS400DataQueueReadException("DataQueueRead error: ", e);
		}		
	}
	
	// Pad string to the right with blanks up to given string length
	private String padString(String inputString, int stringLength) {
		String result = String.format("%1$-" + stringLength + "s", inputString);
		if (result.length() > stringLength)
			result = result.substring(0, stringLength);
		return result;
	}

	private Map<String, Object> createProperties(@NotNull final String key) {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("as400.dataqueue.key", key);
        return properties;
    }
	
	@Processor
	public void writeDataQueue(@FriendlyName("Data Queue")@Placement (order=1)String dtaq,@Placement (order=2) String library,@FriendlyName("Data Entry")@Placement (order=3) String dqEntry,@FriendlyName("Key")@Placement (order=4) @Optional String dtaqKey) throws AS400DataQueueWriteException, AS400TransportException {

		
		connectionStrategy.validateOrReconnect(MAX_PROCESSOR_RETRIES, RECONNECT_WAIT_TIME);
		AS400 system = connectionStrategy.getSystem();
		try {
			String dataQueue = constructDataQueue(dtaq, library);
				if (dtaqKey != null) {
					KeyedDataQueue kd = new KeyedDataQueue(system, dataQueue);
					// Pad key value with blanks
					int keyLength = kd.getKeyLength();
					kd.write(padString(dtaqKey, keyLength), dqEntry);
				} else {
					DataQueue dq = new DataQueue(system, dataQueue);
					dq.write(dqEntry);
			}
		} catch (ConnectionDroppedException e) {
			throw new AS400TransportException("DataQueueWrite connection dropped error", e);
		} catch (java.io.IOException e) {
			throw new AS400TransportException("DataQueueWrite ioexception error", e);
		}

		catch (Exception e) {
			throw new AS400DataQueueWriteException("DataQueueWrite error", e);
		}
	}
	

	@Processor(friendlyName="Command Call")
	public void commandCall(@FriendlyName("Command") String cmd) throws AS400CommandCallException, AS400TransportException {
		
		connectionStrategy.validateOrReconnect(MAX_PROCESSOR_RETRIES, RECONNECT_WAIT_TIME);
		CommandCall command = connectionStrategy.getCommand();
		try {
			// Change job's inquiry message reply settings to auto-reply
			command.run("CHGJOB INQMSGRPY(*DFT)");

			// Now go ahead and run our command
			if (command.run(cmd) != true) {
				StringBuilder errorMessage =new StringBuilder();
				errorMessage.append("Command call ended in error! Below are error message details:").append("\n");
				// Get error messages
				for (AS400Message em: command.getMessageList()){
					errorMessage.append("ERROR text: " + em.getText()).append("\n");
					em.load();
					errorMessage.append("ERROR additional details: " + em.getHelp()).append("\n");
				}
				logger.error(errorMessage.toString());
				throw new AS400CommandCallException(errorMessage.toString());
			}		
		} catch (java.io.IOException e) {
			throw new AS400TransportException("CommandCall connection error: ", e);
		}
		catch(Exception e) {
			throw new AS400CommandCallException("CommandCall error: ", e);
		}
	}
	
	private String constructDataQueue(String dtaq, String library){
		String dataQueue = new StringBuilder(LIBRARY_BASE_PATH)
				.append(library).append(".LIB/").append(dtaq)
				.append(".DTAQ").toString();
		return dataQueue;
	}
	
	/**
	 * @return the connectionStrategy
	 */
	public AS400ConnectionStrategy getConnectionStrategy() {
		return connectionStrategy;
	}

	/**
	 * @param connectionStrategy the connectionStrategy to set
	 */
	public void setConnectionStrategy(AS400ConnectionStrategy connectionStrategy) {
		this.connectionStrategy = connectionStrategy;
	}

}