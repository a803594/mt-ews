/*
DIT
 */
package ru.mos.mostech.ews.exception;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;

/**
 * Исключение HttpResponse с статусом 500 внутренней ошибки сервера.
 */
public class HttpServerErrorException extends HttpResponseException {

	/**
	 * Исключение HttpResponseException с кодом состояния 500 - внутренняя ошибка сервера.
	 * @param message сообщение исключения
	 */
	public HttpServerErrorException(String message) {
		super(HttpStatus.SC_INTERNAL_SERVER_ERROR, message);
	}

}
