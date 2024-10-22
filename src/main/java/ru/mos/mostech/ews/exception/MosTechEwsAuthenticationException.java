/*
DIT
 */
package ru.mos.mostech.ews.exception;

/**
 * I18 AuthenticationException subclass.
 */
public class MosTechEwsAuthenticationException extends MosTechEwsException {

	/**
	 * Create a MT-EWS authentication exception with the given BundleMessage key.
	 * @param key message key
	 */
	public MosTechEwsAuthenticationException(String key) {
		super(key);
	}

	/**
	 * Create a MT-EWS authentication exception with the given BundleMessage key and
	 * arguments.
	 * @param key message key
	 * @param arguments message values
	 */
	public MosTechEwsAuthenticationException(String key, Object... arguments) {
		super(key, arguments);
	}

}
