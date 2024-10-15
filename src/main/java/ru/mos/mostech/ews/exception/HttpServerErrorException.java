/*
DIT
 */
package ru.mos.mostech.ews.exception;

import org.apache.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpResponseException;
import lombok.extern.slf4j.Slf4j;

/**
 * HttpResponseException with 500 internal server error status.
 */

@Slf4j
public class HttpServerErrorException extends HttpResponseException {
    /**
     * HttpResponseException with 500 internal server error status.
     *
     * @param message exception message
     */
    public HttpServerErrorException(String message) {
        super(HttpStatus.SC_INTERNAL_SERVER_ERROR, message);
    }
}
