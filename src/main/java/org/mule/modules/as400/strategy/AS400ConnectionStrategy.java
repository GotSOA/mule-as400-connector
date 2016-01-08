/**
 * (c) 2003-2015 MuleSoft, Inc. The software in this package is published under the terms of the CPAL v1.0 license,
 * a copy of which has been included with this distribution in the LICENSE.md file.
 */

package org.mule.modules.as400.strategy;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import org.mule.api.ConnectionException;
import org.mule.api.ConnectionExceptionCode;
import org.mule.api.annotations.Connect;
import org.mule.api.annotations.ConnectionIdentifier;
import org.mule.api.annotations.Disconnect;
import org.mule.api.annotations.TestConnectivity;
import org.mule.api.annotations.ValidateConnection;
import org.mule.api.annotations.components.ConnectionManagement;
import org.mule.api.annotations.display.FriendlyName;
import org.mule.api.annotations.display.Password;
import org.mule.api.annotations.display.Summary;
import org.mule.api.annotations.param.ConnectionKey;
import org.mule.api.annotations.param.Optional;
import org.mule.modules.as400.exception.AS400CommandCallException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.SocketProperties;

/**
 * AS400 Connection Management
 *
 * @author ravenugo, Dmitriy Kuznetsov
 */
@ConnectionManagement(friendlyName = "Configuration type strategy")
public class AS400ConnectionStrategy {

	private static final Logger logger = LoggerFactory.getLogger(AS400ConnectionStrategy.class);
	
	private AS400 system;
	private CommandCall command;
	private String endpoint, userid, password, libraryList;
	private long lastConnectionCheck;
	private static long KEEPALIVE_PERIOD = 60000;
	
	@Connect
	@TestConnectivity
	public void connect(@FriendlyName("URL")@Summary("AS400 system endpoint (IP or name)")String endpoint, @FriendlyName("UserID")@ConnectionKey String userid,
			@FriendlyName("Password")@Password String password, @Summary("Comma separated list of libraries to add to the connection user's default library list")@FriendlyName("LibraryList")@Optional String libraryList)
			throws ConnectionException {
		try {
			setEndpoint(endpoint);
			setUserID(userid);
			setPwd(password);
			setLibraryList(libraryList);
			connect();
		} catch (UnknownHostException e) {
			throw new ConnectionException(ConnectionExceptionCode.UNKNOWN_HOST,
					e.getLocalizedMessage(), e.getMessage(), e.getCause());
		} catch (IOException e) {
			throw new ConnectionException(ConnectionExceptionCode.CANNOT_REACH,
					e.getLocalizedMessage(), e.getMessage(), e.getCause());
		} catch (AS400SecurityException e) {
			throw new ConnectionException(ConnectionExceptionCode.INCORRECT_CREDENTIALS,
					e.getLocalizedMessage(), e.getMessage(), e.getCause());
		} catch (AS400CommandCallException e){
			throw new ConnectionException(ConnectionExceptionCode.UNKNOWN,
					e.getLocalizedMessage(), e.getMessage(), e.getCause());
		} catch (PropertyVetoException e) {
			e.printStackTrace();
		} catch (Exception e){
			throw new ConnectionException(ConnectionExceptionCode.UNKNOWN,
					e.getLocalizedMessage(), e.getMessage(), e.getCause());
		} 
		
	}

	public void connect() throws Exception {
		setLastConnectionCheck(System.currentTimeMillis());
		system = new AS400(endpoint, userid, password);
		system.setGuiAvailable(false);
		SocketProperties sp = new SocketProperties();
		sp.setKeepAlive(true);
		system.setSocketProperties(sp);
		system.validateSignon();
		command = new CommandCall(system);
		// Set library list
		try{
			if (libraryList != null) {
				List<String> libs = Arrays.asList(libraryList.split("\\s*,\\s*"));
				for (String lib : libs)
				command.run("ADDLIBLE LIB(" + lib + ")");
			}
		}catch (Exception e){
			throw new AS400CommandCallException("CommandCall error: ", e); 
		}

	}
	public CommandCall getCommand() {
		return command;
	}

	public void setCommand(CommandCall command) {
		this.command = command;
	}

	@Disconnect
	public void disconnect() {
		if(system !=null){
			system.disconnectAllServices();
			system = null;
		}
	}

	@ValidateConnection
	public boolean isConnected() {
		boolean connected = (system != null); 
		if (connected) {
			// Connected longer than keepalive period? run a remote command to "ping" the server
			long currentTime = System.currentTimeMillis();
			if (currentTime - getLastConnectionCheck() > KEEPALIVE_PERIOD) {
				try {
					// OS 7.1 or higher - use isConnectionAlive
					if (system.getVersion() > 6) {
						connected = system.isConnectionAlive();
					// OS 6.1 and earlier - use a command / keepalive period
					} else {
						getCommand().run("CHGJOB");
						connected = true;
					}
				} catch (Exception e) {
					connected = false;
					logger.warn("AS400 connection check: Can't reconnect to " + endpoint, e);
				}
				setLastConnectionCheck(System.currentTimeMillis());
			}
		}	
		return connected;
	}

	// Validate and reconnect. Retry for specified number of times or forever if retries = -1
	public void validateOrReconnect(int numberRetry, long waitTime) {
		int retries = 0;
		while (!isConnected() && (numberRetry < 0 || retries++ < numberRetry)) {
			try {
				disconnect();
				connect();
			} catch (Exception e) {
				logger.warn("AS400 connector: Can't reconnect to " + endpoint, e);
				if (waitTime > 0)
					try {
						wait(waitTime);
					} catch (InterruptedException ie) {}
			}
		}

	}
	
	@ConnectionIdentifier
	public String connectionId() {
		return system.getUserId();
	}
	
	/**
	 * @return the system
	 */
	public AS400 getSystem() {
		return system;
	}

	/**
	 * @param system the system to set
	 */
	public void setSystem(AS400 system) {
		this.system = system;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getUserID() {
		return userid;
	}

	public void setUserID(String userID) {
		this.userid = userID;
	}

	public String getPwd() {
		return password;
	}

	public void setPwd(String pwd) {
		this.password = pwd;
	}

	public String getLibraryList() {
		return libraryList;
	}

	public void setLibraryList(String libraryList) {
		this.libraryList = libraryList;
	}

	public long getLastConnectionCheck() {
		return lastConnectionCheck;
	}

	public void setLastConnectionCheck(long lastConnectionCheck) {
		this.lastConnectionCheck = lastConnectionCheck;
	}
	
}