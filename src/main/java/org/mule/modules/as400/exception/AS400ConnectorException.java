package org.mule.modules.as400.exception;

public class AS400ConnectorException extends Exception {

	private static final long serialVersionUID = 1L;

	public AS400ConnectorException() {
		super();
	}

	public AS400ConnectorException(String message, Throwable cause) {
		super(message, cause);
	}

	public AS400ConnectorException(String message) {
		super(message);
	}

	public AS400ConnectorException(Throwable cause) {
		super(cause);
	}

}
