/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Writer;

/**
 * Generic option.
 */

@Slf4j
public abstract class Option {
    protected final String name;
    protected final String value;

    protected Option(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Write XML content to writer.
     *
     * @param writer writer
     * @throws IOException on error
     */
    public abstract void write(Writer writer) throws IOException;

}
