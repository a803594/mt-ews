package ru.mos.mostech.ews.server.handler.extension;

import lombok.experimental.UtilityClass;

import java.io.InputStream;

@UtilityClass
public class EwsExtensionHelper {

	private static final String UPDATE_JSON_PATH = "extension/updates.json";

	private static final String EXTENSION_FILE_NAME = "mt-ews-extension@mostech.mos.ru.xpi";

	private static final String EXTENSION_FILE_PATH = "extension/" + EXTENSION_FILE_NAME;

	public static InputStream getUpdateJson() {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		return classLoader.getResourceAsStream(UPDATE_JSON_PATH);
	}

	public static InputStream getExtensionFile() {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		return classLoader.getResourceAsStream(EXTENSION_FILE_PATH);
	}

	public static String getExtensionFileName() {
		return EXTENSION_FILE_NAME;
	}

}
