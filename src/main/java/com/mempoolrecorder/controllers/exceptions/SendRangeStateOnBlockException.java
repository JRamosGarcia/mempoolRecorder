package com.mempoolrecorder.controllers.exceptions;

public class SendRangeStateOnBlockException extends Exception {

	private static final long serialVersionUID = 1L;

	public SendRangeStateOnBlockException() {
		super();
	}

	public SendRangeStateOnBlockException(String message) {
		super(message);
	}

	public SendRangeStateOnBlockException(String message, Throwable cause) {
		super(message, cause);
	}
}