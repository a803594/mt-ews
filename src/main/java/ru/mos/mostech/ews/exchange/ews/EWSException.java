/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * EWS Exception
 */

@Slf4j
public class EWSException extends IOException {
    /**
     * Create EWS Exception with detailed error message
     *
     * @param message error message
     */
    public EWSException(String message) {
        super(message);
    }
}
