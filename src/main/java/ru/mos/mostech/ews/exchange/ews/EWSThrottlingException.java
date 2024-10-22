/*
DIT
 */

package ru.mos.mostech.ews.exchange.ews;

/**
 * Exchange throttling error.
 */
public class EWSThrottlingException extends EWSException {

	/**
	 * Create EWS throttling Exception with detailed error message
	 * @param message error message
	 */
	public EWSThrottlingException(String message) {
		super(message);
	}

}