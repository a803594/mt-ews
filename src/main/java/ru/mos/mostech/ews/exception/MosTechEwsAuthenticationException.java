/*
DIT
 */
package ru.mos.mostech.ews.exception;

/**
 * Подкласс AuthenticationException для I18.
 */
public class MosTechEwsAuthenticationException extends MosTechEwsException {

	/**
	 * Создать исключение аутентификации MT-EWS с заданным ключом BundleMessage.
	 * @param ключ ключ сообщения
	 */
	public MosTechEwsAuthenticationException(String key) {
		super(key);
	}

	/**
	 * Создать исключение аутентификации MT-EWS с заданным ключом BundleMessage и
	 * аргументами.
	 * @param ключ ключ сообщения
	 * @param аргументы значения сообщения
	 */
	public MosTechEwsAuthenticationException(String key, Object... arguments) {
		super(key, arguments);
	}

}
