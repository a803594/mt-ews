/*
DIT
 */
package ru.mos.mostech.ews.exception;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;

/**
 * Исключение HttpResponse с статусом 404 не найдено.
 */
public class HttpNotFoundException extends HttpResponseException {

	/**
	 * HttpResponseException с статусом 404 не найдено.
	 * @param message сообщение исключения
	 */
	public HttpNotFoundException(String message) {
		super(HttpStatus.SC_NOT_FOUND, message);
	}

}
