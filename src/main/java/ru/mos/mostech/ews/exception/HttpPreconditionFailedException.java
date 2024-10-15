/*
DIT
 */
package ru.mos.mostech.ews.exception;

import org.apache.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpResponseException;
import lombok.extern.slf4j.Slf4j;

/**
 * HttpResponseException with 412 precondition failed status.
 */

@Slf4j
public class HttpPreconditionFailedException extends HttpResponseException {
    /**
     * HttpResponseException with 412 precondition failed status.
     *
     * @param message exception message
     */
    public HttpPreconditionFailedException(String message) {
        super(HttpStatus.SC_PRECONDITION_FAILED, message);
    }
}
