package org.mule.modules.as400.exception;

public class AS400DataQueueWriteException extends AS400ConnectorException {

	private static final long serialVersionUID = 1L;

	public AS400DataQueueWriteException(String message, Throwable cause) {
		super(message, cause);
	}

	public AS400DataQueueWriteException(String message) {
		super(message);
	}

}
