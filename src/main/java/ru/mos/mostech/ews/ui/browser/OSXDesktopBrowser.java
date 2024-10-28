/*
DIT
 */
package ru.mos.mostech.ews.ui.browser;

import java.io.IOException;
import java.net.URI;

/**
 * Резервирование: Runtime.exec открывает URL
 */
public final class OSXDesktopBrowser {

	private OSXDesktopBrowser() {
	}

	/**
	 * Открыть браузер по умолчанию по адресу URI. Команда open для OSX
	 * @param location адрес URI
	 * @throws IOException в случае ошибки
	 */
	public static void browse(URI location) throws IOException {
		Runtime.getRuntime().exec(new String[] { "open", location.toString() });
	}

}
