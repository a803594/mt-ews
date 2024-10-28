/*
DIT
 */

package ru.mos.mostech.ews.exchange.ews;

/**
 * Ошибка ограничения обмена.
 */
public class EWSThrottlingException extends EWSException {

	/**
	 * Создать исключение ограничения EWS с подробным сообщением об ошибке
	 * @param message сообщение об ошибке
	 */
	public EWSThrottlingException(String message) {
		super(message);
	}

}