/*
DIT
 */
package ru.mos.mostech.ews.exception;

/**
 * Exchange 2007 with Webdav disabled will trigger this exception.
 */
public class WebdavNotAvailableException extends MosTechEwsException {

	/**
	 * Create a MT-EWS exception with the given BundleMessage key and arguments.
	 * @param key message key
	 * @param arguments message values
	 */
	public WebdavNotAvailableException(String key, Object... arguments) {
		super(key, arguments);
	}

}
