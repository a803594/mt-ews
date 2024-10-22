/*
DIT
 */
package ru.mos.mostech.ews.exception;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;

/**
 * HttpResponseException with 507 Insufficient Storage status.
 */
public class InsufficientStorageException extends HttpResponseException {

	/**
	 * HttpResponseException with 507 Insufficient Storage status.
	 * @param message exception message
	 */
	public InsufficientStorageException(String message) {
		super(HttpStatus.SC_INSUFFICIENT_STORAGE, message);
	}

}