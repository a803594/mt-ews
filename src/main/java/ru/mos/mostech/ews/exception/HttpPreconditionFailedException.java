/*
DIT
 */
package ru.mos.mostech.ews.exception;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;

/**
 * HttpResponseException с статусом 412 ошибка условий.
 */
public class HttpPreconditionFailedException extends HttpResponseException {

	/**
	 * HttpResponseException с кодом состояния 412 - предусловие не выполнено.
	 * @param message сообщение об исключении
	 */
	public HttpPreconditionFailedException(String message) {
		super(HttpStatus.SC_PRECONDITION_FAILED, message);
	}

}
