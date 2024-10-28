/*
DIT
 */
package ru.mos.mostech.ews.exception;

import org.apache.http.client.HttpResponseException;

/**
 * HttpResponseException с кодом состояния 440 времени ожидания входа.
 */
public class LoginTimeoutException extends HttpResponseException {

	/**
	 * HttpResponseException с статусом 550 таймаута входа.
	 * @param message сообщение об ошибке
	 */
	public LoginTimeoutException(String message) {
		super(440, message);
	}

}
