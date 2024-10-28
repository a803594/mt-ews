/*
DIT
 */
package ru.mos.mostech.ews;

import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.exception.MosTechEwsException;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Сообщение для интернационализации.
 */
@Slf4j
public class BundleMessage implements Serializable {

	/**
	 * Корневая локализация для получения английских сообщений для логирования.
	 */
	public static final Locale ROOT_LOCALE = Locale.forLanguageTag("ru");

	protected static final String MESSAGE_BUNDLE_NAME = "mt-ews-messages";

	protected final String key;

	private final Object[] arguments;

	/**
	 * Сообщение интернационализации.
	 * @param ключ ключ сообщения в ресурсном пакете
	 * @param аргументы значения сообщения
	 */
	public BundleMessage(String key, Object... arguments) {
		this.key = key;
		this.arguments = arguments;
	}

	/**
	 * Отформатировать сообщение в соответствии с локалью по умолчанию.
	 * @return отформатированное сообщение
	 */
	public String format() {
		return format(null);
	}

	/**
	 * Форматирует сообщение с учетом заданной локали.
	 * @param locale локаль ресурсного пакета
	 * @return форматированное сообщение
	 */
	public String format(Locale locale) {
		return BundleMessage.format(locale, key, arguments);
	}

	/**
	 * Форматировать сообщение для ведения лога (с использованием корневой локали). Файл
	 * лога должен оставаться на английском языке.
	 * @return отформатированное сообщение для лога
	 */
	public String formatLog() {
		return format(ROOT_LOCALE);
	}

	/**
	 * Форматировать сообщение для ведения лога (с использованием корневой локали). Файл
	 * лога должен оставаться на английском языке.
	 * @return отформатированное сообщение для лога
	 */
	@Override
	public String toString() {
		return formatLog();
	}

	/**
	 * Получить пакет ресурсов для заданной локали. Загрузить файл свойств для заданной
	 * локали в пакет ресурсов
	 * @param locale локаль пакета ресурсов
	 * @return пакет ресурсов
	 */
	protected static ResourceBundle getBundle(Locale locale) {
		if (locale == null) {
			return ResourceBundle.getBundle(MESSAGE_BUNDLE_NAME);
		}
		else {
			return ResourceBundle.getBundle(MESSAGE_BUNDLE_NAME, locale);
		}
	}

	/**
	 * Получить отформатированное сообщение для ключа сообщения и значений с
	 * использованием локали по умолчанию.
	 * @param key ключ сообщения в ресурсном пакете
	 * @param arguments значения сообщения
	 * @return отформатированное сообщение
	 */
	public static String format(String key, Object... arguments) {
		return format(ROOT_LOCALE, key, arguments);
	}

	/**
	 * Получить отформатированное сообщение для ключа сообщения и значений с указанной
	 * локализацией.
	 * @param locale локализация ресурсного пакета
	 * @param key ключ сообщения в ресурсном пакете
	 * @param arguments значения сообщения
	 * @return отформатированное сообщение
	 */
	public static String format(Locale locale, String key, Object... arguments) {
		Object[] formattedArguments = null;
		if (arguments != null) {
			formattedArguments = new Object[arguments.length];
			for (int i = 0; i < arguments.length; i++) {
				if (arguments[i] instanceof BundleMessage) {
					formattedArguments[i] = ((BundleMessage) arguments[i]).format(locale);
				}
				else if (arguments[i] instanceof BundleMessageList) {
					StringBuilder buffer = new StringBuilder();
					for (BundleMessage bundleMessage : (BundleMessageList) arguments[i]) {
						buffer.append(bundleMessage.format(locale));
					}
					formattedArguments[i] = buffer.toString();
				}
				else if (arguments[i] instanceof MosTechEwsException) {
					formattedArguments[i] = ((MosTechEwsException) arguments[i]).getMessage(locale);
				}
				else if (arguments[i] instanceof Throwable) {
					formattedArguments[i] = ((Throwable) arguments[i]).getMessage();
					if (formattedArguments[i] == null) {
						formattedArguments[i] = arguments[i].toString();
					}
				}
				else {
					formattedArguments[i] = arguments[i];
				}
			}
		}
		return MessageFormat.format(getBundle(locale).getString(key), formattedArguments);
	}

	/**
	 * Получить форматированное сообщение лога для ключа сообщения и значений. Используйте
	 * корневую локаль
	 * @param key ключ сообщения в ресурсном пакете
	 * @param arguments значения сообщения
	 * @return форматированное сообщение
	 */
	public static String formatLog(String key, Object... arguments) {
		return format(ROOT_LOCALE, key, arguments);
	}

	/**
	 * Получить отформатированное сообщение об ошибке для сообщения из бандла и исключения
	 * для логирования. Используйте корневую локализацию
	 * @param message сообщение из бандла
	 * @param e исключение
	 * @return отформатированное сообщение
	 */
	public static String getExceptionLogMessage(BundleMessage message, Exception e) {
		return getExceptionMessage(message, e, ROOT_LOCALE);
	}

	/**
	 * Получить отформатированное сообщение об ошибке для сообщения пакета и исключения с
	 * локалью по умолчанию.
	 * @param message сообщение пакета
	 * @param e исключение
	 * @return отформатированное сообщение
	 */
	public static String getExceptionMessage(BundleMessage message, Exception e) {
		return getExceptionMessage(message, e, null);
	}

	/**
	 * Получить форматированное сообщение об ошибке для сообщения пакета и исключения с
	 * заданной локалью.
	 * @param message сообщение пакета
	 * @param e исключение
	 * @param locale локаль пакета
	 * @return форматированное сообщение
	 */
	public static String getExceptionMessage(BundleMessage message, Exception e, Locale locale) {
		StringBuilder buffer = new StringBuilder();
		if (message != null) {
			buffer.append(message.format(locale)).append(' ');
		}
		if (e instanceof MosTechEwsException) {
			buffer.append(((MosTechEwsException) e).getMessage(locale));
		}
		else if (e.getMessage() != null) {
			buffer.append(e.getMessage());
		}
		else {
			buffer.append(e);
		}
		return buffer.toString();
	}

	/**
	 * Коллекция сообщений типизированного пакета
	 */
	public static class BundleMessageList extends ArrayList<BundleMessage> {

	}

}
