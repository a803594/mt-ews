/*
DIT
 */
package ru.mos.mostech.ews.exception;

import org.apache.http.client.HttpResponseException;
import lombok.extern.slf4j.Slf4j;

/**
 * HttpResponseException with 440 login timeout status.
 */

@Slf4j
public class LoginTimeoutException extends HttpResponseException {
    /**
     * HttpResponseException with 550 login timeout status.
     *
     * @param message exception message
     */
    public LoginTimeoutException(String message) {
        super(440, message);
    }
}
