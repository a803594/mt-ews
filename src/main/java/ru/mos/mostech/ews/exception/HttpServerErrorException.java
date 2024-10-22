/*
DIT
 */
package ru.mos.mostech.ews.exception;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;

/**
 * HttpResponseException with 500 internal server error status.
 */
public class HttpServerErrorException extends HttpResponseException {

	/**
	 * HttpResponseException with 500 internal server error status.
	 * @param message exception message
	 */
	public HttpServerErrorException(String message) {
		super(HttpStatus.SC_INTERNAL_SERVER_ERROR, message);
	}

}
