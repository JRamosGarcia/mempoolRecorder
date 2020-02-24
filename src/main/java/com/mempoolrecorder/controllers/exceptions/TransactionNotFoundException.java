package com.mempoolrecorder.controllers.exceptions;

public class TransactionNotFoundException extends Exception {

	private static final long serialVersionUID = 3372623966394648556L;

	public TransactionNotFoundException() {
		super();
	}

	public TransactionNotFoundException(String message) {
		super(message);
	}

	public TransactionNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}
