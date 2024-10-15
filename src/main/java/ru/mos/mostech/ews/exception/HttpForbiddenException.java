/*
DIT
 */
package ru.mos.mostech.ews.exception;

import org.apache.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpResponseException;
import lombok.extern.slf4j.Slf4j;

/**
 * HttpResponseException with 403 forbidden status.
 */

@Slf4j
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
