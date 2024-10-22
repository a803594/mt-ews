/*
DIT
 */
package ru.mos.mostech.ews.exchange;

import ru.mos.mostech.ews.exception.MosTechEwsException;

/**
 * Custom exception to mark network down case.
 */
public class NetworkDownException extends MosTechEwsException {

	/**
	 * Build a network down exception with the provided BundleMessage key.
	 * @param key message key
	 */
	public NetworkDownException(String key) {
		super(key);
	}

	/**
	 * Build a network down exception with the provided BundleMessage key.
	 * @param key message key
	 * @param message detailed message
	 */
	public NetworkDownException(String key, Object message) {
		super(key, message);
	}

}
