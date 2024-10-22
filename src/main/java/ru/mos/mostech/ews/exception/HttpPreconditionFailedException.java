/*
DIT
 */
package ru.mos.mostech.ews.exception;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;

/**
 * HttpResponseException with 412 precondition failed status.
 */
public class HttpPreconditionFailedException extends HttpResponseException {

	/**
	 * HttpResponseException with 412 precondition failed status.
	 * @param message exception message
	 */
	public HttpPreconditionFailedException(String message) {
		super(HttpStatus.SC_PRECONDITION_FAILED, message);
	}

}
