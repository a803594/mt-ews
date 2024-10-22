/*
DIT
 */
package ru.mos.mostech.ews.exception;

import org.apache.http.client.HttpResponseException;

/**
 * HttpResponseException with 440 login timeout status.
 */
public class LoginTimeoutException extends HttpResponseException {

	/**
	 * HttpResponseException with 550 login timeout status.
	 * @param message exception message
	 */
	public LoginTimeoutException(String message) {
		super(440, message);
	}

}
