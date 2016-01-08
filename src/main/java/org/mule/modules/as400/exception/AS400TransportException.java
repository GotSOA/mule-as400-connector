package org.mule.modules.as400.exception;

public class AS400TransportException extends AS400ConnectorException {

	private static final long serialVersionUID = 1L;

	public AS400TransportException(String message, Throwable cause) {
		super(message, cause);
	}

	public AS400TransportException(String message) {
		super(message);
	}

}

