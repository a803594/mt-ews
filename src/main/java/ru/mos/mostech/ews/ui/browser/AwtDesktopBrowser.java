/*
DIT
 */
package ru.mos.mostech.ews.ui.browser;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

/**
 * Обертка класса для вызова класса Desktop Java6 для запуска браузера по умолчанию.
 */
public final class AwtDesktopBrowser {

	private AwtDesktopBrowser() {
	}

	/**
	 * Открыть браузер по умолчанию по адресу URI. Используйте класс Desktop Java 6
	 * @param location адрес URI
	 * @throws IOException при ошибке
	 */
	public static void browse(URI location) throws IOException {
		Desktop desktop = Desktop.getDesktop();
		desktop.browse(location);
	}

}
