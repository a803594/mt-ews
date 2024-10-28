/*
DIT
 */
package ru.mos.mostech.ews.exception;

import ru.mos.mostech.ews.BundleMessage;

import java.io.IOException;
import java.util.Locale;

/**
 * Подкласс IOException для интернационализации.
 */
public class MosTechEwsException extends IOException {

	private final BundleMessage message;

	/**
	 * Создать исключение MT-EWS с заданным ключом BundleMessage и аргументами.
	 * @param key ключ сообщения
	 * @param arguments значения сообщения
	 */
	public MosTechEwsException(String key, Object... arguments) {
		this.message = new BundleMessage(key, arguments);
	}

	/**
	 * Получить отформатированное сообщение
	 * @return отформатированное сообщение на английском
	 */
	@Override
	public String getMessage() {
		return message.formatLog();
	}

	/**
	 * Получить отформатированное сообщение с учетом локали.
	 * @param locale локаль
	 * @return локализованное отформатированное сообщение
	 */
	public String getMessage(Locale locale) {
		return message.format(locale);
	}

	/**
	 * Получить внутреннее исключение BundleMessage.
	 * @return неформатированное сообщение
	 */
	public BundleMessage getBundleMessage() {
		return message;
	}

}
