/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Writer;

/**
 * Generic attribute option.
 */

@Slf4j
public class AttributeOption extends Option {

    protected AttributeOption(String name, String value) {
        super(name, value);
    }

    /**
     * @inheritDoc
     */
    public void appendTo(StringBuilder buffer) {
        buffer.append(' ').append(name).append("=\"").append(value).append('"');
    }

    /**
     * @inheritDoc
     */
    @Override
    public void write(Writer writer) throws IOException {
        writer.write(" ");
        writer.write(name);
        writer.write("=\"");
        writer.write(value);
        writer.write("\"");
    }
}