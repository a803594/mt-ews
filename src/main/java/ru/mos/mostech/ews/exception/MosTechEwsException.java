/*
DIT
 */
package ru.mos.mostech.ews.exception;

import ru.mos.mostech.ews.BundleMessage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;

/**
 * I18 IOException subclass.
 */

@Slf4j
public class MosTechEwsException extends IOException {
    private final BundleMessage message;

    /**
     * Create a MT-EWS exception with the given BundleMessage key and arguments.
     *
     * @param key       message key
     * @param arguments message values
     */
    public MosTechEwsException(String key, Object... arguments) {
        this.message = new BundleMessage(key, arguments);
    }

    /**
     * Get formatted message
     *
     * @return english formatted message
     */
    @Override
    public String getMessage() {
        return message.formatLog();
    }

    /**
     * Get formatted message using locale.
     *
     * @param locale locale
     * @return localized formatted message
     */
    public String getMessage(Locale locale) {
        return message.format(locale);
    }

    /**
     * Get internal exception BundleMessage.
     *
     * @return unformatted message
     */
    public BundleMessage getBundleMessage() {
        return message;
    }
}
