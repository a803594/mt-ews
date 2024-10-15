/*
DIT
 */
package ru.mos.mostech.ews.exception;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;

/**
 * HttpResponseException with 403 forbidden status.
 */
public class HttpForbiddenException extends HttpResponseException {
    /**
     * HttpResponseException with 403 forbidden status.
     *
     * @param message exception message
     */
    public HttpForbiddenException(String message) {
        super(HttpStatus.SC_FORBIDDEN, message);
    }
}
