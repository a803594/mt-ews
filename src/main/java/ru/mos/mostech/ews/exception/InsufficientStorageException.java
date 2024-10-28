/*
DIT
 */
package ru.mos.mostech.ews.exception;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;

/**
 * Исключение HttpResponse с кодом состояния 507 Нехватка памяти.
 */
public class InsufficientStorageException extends HttpResponseException {

	/**
	 * HttpResponseException с статусом 507 Недостаточно места для хранения.
	 * @param message сообщение исключения
	 */
	public InsufficientStorageException(String message) {
		super(HttpStatus.SC_INSUFFICIENT_STORAGE, message);
	}

}