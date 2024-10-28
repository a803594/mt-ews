/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import java.io.IOException;

/**
 * Исключение EWS
 */
public class EWSException extends IOException {

	/**
	 * Создать исключение EWS с детальным сообщением об ошибке
	 * @param message сообщение об ошибке
	 */
	public EWSException(String message) {
		super(message);
	}

}
