/*
DIT
 */
package ru.mos.mostech.ews.exception;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;

/**
 * Исключение HttpResponse с статусом 403 запрещено.
 */
public class HttpForbiddenException extends HttpResponseException {

	/**
	 * HttpResponseException с статусом 403 запрещено.
	 * @param message сообщение об исключении
	 */
	public HttpForbiddenException(String message) {
		super(HttpStatus.SC_FORBIDDEN, message);
	}

}
