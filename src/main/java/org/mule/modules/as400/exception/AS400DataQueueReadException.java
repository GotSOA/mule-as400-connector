package org.mule.modules.as400.exception;

public class AS400DataQueueReadException extends AS400ConnectorException {

	private static final long serialVersionUID = 1L;

	public AS400DataQueueReadException(String message, Throwable cause) {
		super(message, cause);
	}

	public AS400DataQueueReadException(String message) {
		super(message);
	}

}
