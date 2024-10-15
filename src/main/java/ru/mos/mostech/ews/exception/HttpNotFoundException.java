/*
DIT
 */
package ru.mos.mostech.ews.exception;

import org.apache.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpResponseException;
import lombok.extern.slf4j.Slf4j;

/**
 * HttpResponseException with 404 not found status.
 */

@Slf4j
public class HttpNotFoundException extends HttpResponseException {
    /**
     * HttpResponseException with 404 not found status.
     *
     * @param message exception message
     */
    public HttpNotFoundException(String message) {
        super(HttpStatus.SC_NOT_FOUND, message);
    }
}
