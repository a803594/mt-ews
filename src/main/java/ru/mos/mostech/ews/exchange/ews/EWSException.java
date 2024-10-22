/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import java.io.IOException;

/**
 * EWS Exception
 */
public class EWSException extends IOException {

	/**
	 * Create EWS Exception with detailed error message
	 * @param message error message
	 */
	public EWSException(String message) {
		super(message);
	}

}
