/*
DIT
 */
package ru.mos.mostech.ews.exception;


import org.apache.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpResponseException;
import lombok.extern.slf4j.Slf4j;

/**
 * HttpResponseException with 507 Insufficient Storage status.
 */

@Slf4j
public class InsufficientStorageException extends HttpResponseException {
    /**
     * HttpResponseException with 507 Insufficient Storage status.
     *
     * @param message exception message
     */
    public InsufficientStorageException(String message) {
        super(HttpStatus.SC_INSUFFICIENT_STORAGE, message);
    }
}