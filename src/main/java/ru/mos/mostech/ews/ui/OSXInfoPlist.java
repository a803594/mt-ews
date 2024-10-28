/*
DIT
 */
package ru.mos.mostech.ews.ui;

import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Обработка доступа к файлу Info.plist на OSX
 */
@Slf4j
public class OSXInfoPlist {

	protected static final String INFO_PLIST_PATH = "../Info.plist";

	private OSXInfoPlist() {
	}

	protected static boolean isOSX() {
		return System.getProperty("os.name").toLowerCase().startsWith("mac os x");
	}

	protected static String getInfoPlistContent() throws IOException {
		try (FileInputStream fileInputStream = new FileInputStream(getInfoPlistPath())) {
			return new String(IOUtil.readFully(fileInputStream), StandardCharsets.UTF_8);
		}
	}

	/**
	 * Тестирует текущее значение LSUIElement (скрыть из дока)
	 * @return true, если приложение скрыто из дока
	 */
	public static boolean isHideFromDock() {
		boolean result = false;
		try {
			result = isOSX() && getInfoPlistContent().contains("<key>LSUIElement</key><string>1</string>");
		}
		catch (IOException e) {
			log.warn("Unable to update Info.plist", e);
		}
		return result;
	}

	/**
	 * Обновить значение LSUIElement (скрыть из дока)
	 * @param hideFromDock новое значение скрытия из дока
	 */
	public static void setOSXHideFromDock(boolean hideFromDock) {
		try {
			if (isOSX()) {
				boolean currentHideFromDock = isHideFromDock();
				if (currentHideFromDock != hideFromDock) {
					String content = getInfoPlistContent();
					try (FileOutputStream fileOutputStream = new FileOutputStream(getInfoPlistPath())) {
						fileOutputStream
							.write(content
								.replaceFirst(
										"<key>LSUIElement</key><string>" + (currentHideFromDock ? "1" : "0")
												+ "</string>",
										"<key>LSUIElement</key><string>" + (hideFromDock ? "1" : "0") + "</string>")
								.getBytes(StandardCharsets.UTF_8));
					}
				}
			}
		}
		catch (IOException e) {
			log.warn("Unable to update Info.plist", e);
		}
	}

	private static String getInfoPlistPath() throws IOException {
		File file = new File(INFO_PLIST_PATH);
		if (file.exists()) {
			return INFO_PLIST_PATH;
		}
		throw new IOException("Info.plist file not found");
	}

}
