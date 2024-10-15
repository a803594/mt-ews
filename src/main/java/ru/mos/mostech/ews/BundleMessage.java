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
 * Internationalization message.
 */
@Slf4j
public class BundleMessage implements Serializable {
    /**
     * Root locale to get english messages for logging.
     */
    public static final Locale ROOT_LOCALE = Locale.forLanguageTag("ru");
    protected static final String MESSAGE_BUNDLE_NAME = "mt-ews-messages";
    protected final String key;
    private final Object[] arguments;

    /**
     * Internationalization message.
     *
     * @param key       message key in resource bundle
     * @param arguments message values
     */
    public BundleMessage(String key, Object... arguments) {
        this.key = key;
        this.arguments = arguments;
    }


    /**
     * Format message with the default locale.
     *
     * @return formatted message
     */
    public String format() {
        return format(null);
    }

    /**
     * Format message with the given locale.
     *
     * @param locale resource bundle locale
     * @return formatted message
     */
    public String format(Locale locale) {
        return BundleMessage.format(locale, key, arguments);
    }

    /**
     * Format message for logging (with the root locale).
     * Log file should remain in english
     *
     * @return log formatted message
     */
    public String formatLog() {
        return format(ROOT_LOCALE);
    }

    /**
     * Format message for logging (with the root locale).
     * Log file should remain in english
     *
     * @return log formatted message
     */
    @Override
    public String toString() {
        return formatLog();
    }

    /**
     * Get bundle for the given locale.
     * Load the properties file for the given locale in a resource bundle
     *
     * @param locale resource bundle locale
     * @return resource bundle
     */
    protected static ResourceBundle getBundle(Locale locale) {
        if (locale == null) {
            return ResourceBundle.getBundle(MESSAGE_BUNDLE_NAME);
        } else {
            return ResourceBundle.getBundle(MESSAGE_BUNDLE_NAME, locale);
        }
    }

    /**
     * Get formatted message for message key and values with the default locale.
     *
     * @param key       message key in resource bundle
     * @param arguments message values
     * @return formatted message
     */
    public static String format(String key, Object... arguments) {
        return format(ROOT_LOCALE, key, arguments);
    }

    /**
     * Get formatted message for message key and values with the given locale.
     *
     * @param locale    resource bundle locale
     * @param key       message key in resource bundle
     * @param arguments message values
     * @return formatted message
     */
    public static String format(Locale locale, String key, Object... arguments) {
        Object[] formattedArguments = null;
        if (arguments != null) {
            formattedArguments = new Object[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                if (arguments[i] instanceof BundleMessage) {
                    formattedArguments[i] = ((BundleMessage) arguments[i]).format(locale);
                } else if (arguments[i] instanceof BundleMessageList) {
                    StringBuilder buffer = new StringBuilder();
                    for (BundleMessage bundleMessage : (BundleMessageList) arguments[i]) {
                        buffer.append(bundleMessage.format(locale));
                    }
                    formattedArguments[i] = buffer.toString();
                } else if (arguments[i] instanceof MosTechEwsException) {
                    formattedArguments[i] = ((MosTechEwsException) arguments[i]).getMessage(locale);
                } else if (arguments[i] instanceof Throwable) {
                    formattedArguments[i] = ((Throwable) arguments[i]).getMessage();
                    if (formattedArguments[i] == null) {
                        formattedArguments[i] = arguments[i].toString();
                    }
                } else {
                    formattedArguments[i] = arguments[i];
                }
            }
        }
        return MessageFormat.format(getBundle(locale).getString(key), formattedArguments);
    }

    /**
     * Get formatted log message for message key and values.
     * Use the root locale
     *
     * @param key       message key in resource bundle
     * @param arguments message values
     * @return formatted message
     */
    public static String formatLog(String key, Object... arguments) {
        return format(ROOT_LOCALE, key, arguments);
    }

    /**
     * Get formatted error message for bundle message and exception for logging.
     * Use the root locale
     *
     * @param message bundle message
     * @param e       exception
     * @return formatted message
     */
    public static String getExceptionLogMessage(BundleMessage message, Exception e) {
        return getExceptionMessage(message, e, ROOT_LOCALE);
    }

    /**
     * Get formatted error message for bundle message and exception with default locale.
     *
     * @param message bundle message
     * @param e       exception
     * @return formatted message
     */
    public static String getExceptionMessage(BundleMessage message, Exception e) {
        return getExceptionMessage(message, e, null);
    }

    /**
     * Get formatted error message for bundle message and exception with given locale.
     *
     * @param message bundle message
     * @param e       exception
     * @param locale  bundle locale
     * @return formatted message
     */
    public static String getExceptionMessage(BundleMessage message, Exception e, Locale locale) {
        StringBuilder buffer = new StringBuilder();
        if (message != null) {
            buffer.append(message.format(locale)).append(' ');
        }
        if (e instanceof MosTechEwsException) {
            buffer.append(((MosTechEwsException) e).getMessage(locale));
        } else if (e.getMessage() != null) {
            buffer.append(e.getMessage());
        } else {
            buffer.append(e);
        }
        return buffer.toString();
    }

    /**
     * Typed bundle message collection
     */
    public static class BundleMessageList extends ArrayList<BundleMessage> {
    }
}
