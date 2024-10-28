/*
DIT
 */
package ru.mos.mostech.ews.exchange;

import ru.mos.mostech.ews.exception.MosTechEwsException;

/**
 * Пользовательское исключение для обозначения случая неисправности сети.
 */
public class NetworkDownException extends MosTechEwsException {

	/**
	 * Создайте исключение сети с указанным ключом BundleMessage.
	 * @param key ключ сообщения
	 */
	public NetworkDownException(String key) {
		super(key);
	}

	/**
	 * Создает исключение сети с предоставленным ключом BundleMessage.
	 * @param key ключ сообщения
	 * @param message подробное сообщение
	 */
	public NetworkDownException(String key, Object message) {
		super(key, message);
	}

}
