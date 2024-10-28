/*
DIT
 */
package ru.mos.mostech.ews.ui.browser;

import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.ui.AboutFrame;
import ru.mos.mostech.ews.ui.tray.MosTechEwsTray;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Открыть браузер по умолчанию.
 */
public final class DesktopBrowser {

	private DesktopBrowser() {
	}

	/**
	 * Открывает браузер по умолчанию по указанному URI. Использует класс Desktop из Java
	 * 6, команду open в OSX или запуск программы SWT
	 * @param location URI для места назначения
	 */
	public static void browse(URI location) {
		try {
			// trigger ClassNotFoundException
			ClassLoader classloader = AboutFrame.class.getClassLoader();
			classloader.loadClass("java.awt.Desktop");

			// Open link in default browser
			AwtDesktopBrowser.browse(location);
		}
		catch (ClassNotFoundException e) {
			MosTechEwsTray.debug(new BundleMessage("LOG_JAVA6_DESKTOP_UNAVAILABLE"));
			// failover for MacOSX
			if (System.getProperty("os.name").toLowerCase().startsWith("mac os x")) {
				try {
					OSXDesktopBrowser.browse(location);
				}
				catch (Exception e2) {
					MosTechEwsTray.error(new BundleMessage("LOG_UNABLE_TO_OPEN_LINK"), e2);
				}
			}
		}
		catch (java.lang.UnsupportedOperationException e) {
			if (Settings.isUnix()) {
				try {
					XdgDesktopBrowser.browse(location);
				}
				catch (Exception e2) {
					MosTechEwsTray.error(new BundleMessage("LOG_UNABLE_TO_OPEN_LINK"), e2);
				}
			}
			else {
				MosTechEwsTray.error(new BundleMessage("LOG_UNABLE_TO_OPEN_LINK"), e);
			}
		}
		catch (Exception e) {
			MosTechEwsTray.error(new BundleMessage("LOG_UNABLE_TO_OPEN_LINK"), e);
		}
	}

	/**
	 * Открыть браузер по умолчанию по указанному пути. Используйте класс Desktop Java 6,
	 * команду open для OSX или запуск программы SWT
	 * @param location целевое местоположение
	 */
	public static void browse(String location) {
		try {
			DesktopBrowser.browse(new URI(location));
		}
		catch (URISyntaxException e) {
			MosTechEwsTray.error(new BundleMessage("LOG_UNABLE_TO_OPEN_LINK"), e);
		}
	}

}
