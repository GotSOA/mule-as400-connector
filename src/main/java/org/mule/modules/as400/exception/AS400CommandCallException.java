package org.mule.modules.as400.exception;

public class AS400CommandCallException extends AS400ConnectorException {

	private static final long serialVersionUID = 1L;

	public AS400CommandCallException(String message, Throwable cause) {
		super(message, cause);
	}

	public AS400CommandCallException(String message) {
		super(message);
	}

}
