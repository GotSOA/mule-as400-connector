package org.mule.modules.as400.exception;

public class AS400ProgramCallException extends AS400ConnectorException {

	private static final long serialVersionUID = 1L;

	public AS400ProgramCallException(String message, Throwable cause) {
		super(message, cause);
	}

	public AS400ProgramCallException(String message) {
		super(message);
	}

}

