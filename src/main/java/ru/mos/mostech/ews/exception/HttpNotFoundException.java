/*
DIT
 */
package ru.mos.mostech.ews.exception;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;

/**
 * HttpResponseException with 404 not found status.
 */
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
