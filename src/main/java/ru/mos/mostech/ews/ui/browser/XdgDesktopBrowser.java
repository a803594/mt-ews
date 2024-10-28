/*
DIT
 */
package ru.mos.mostech.ews.ui.browser;

import java.io.IOException;
import java.net.URI;

/**
 * Резервное копирование: Runtime.exec открыть URL
 */
public final class XdgDesktopBrowser {

	private XdgDesktopBrowser() {
	}

	/**
	 * Открыть браузер по умолчанию по адресу URI. Используйте xdg-open для открытия URL
	 * браузера
	 * @param location адрес URI
	 * @throws IOException при ошибке
	 */
	public static void browse(URI location) throws IOException {
		Runtime.getRuntime().exec(new String[] { "xdg-open", location.toString() });
	}

}
