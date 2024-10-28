/*
DIT
 */
package ru.mos.mostech.ews.exception;

/**
 * Exchange 2007 с отключенным Webdav вызовет это исключение.
 */
public class WebdavNotAvailableException extends MosTechEwsException {

	/**
	 * Создайте исключение MT-EWS с заданным ключом BundleMessage и аргументами.
	 * @param key ключ сообщения
	 * @param arguments значения сообщения
	 */
	public WebdavNotAvailableException(String key, Object... arguments) {
		super(key, arguments);
	}

}
