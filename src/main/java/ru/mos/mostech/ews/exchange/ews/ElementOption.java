/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import ru.mos.mostech.ews.util.StringUtil;

import java.io.IOException;
import java.io.Writer;

/**
 * Generic element option.
 */
public class ElementOption extends Option {
    ElementOption option;
    /**
     * Create element option.
     *
     * @param name  element tag name
     * @param value element value
     */
    protected ElementOption(String name, String value) {
        super(name, value);
    }

    protected ElementOption(String name, ElementOption option) {
        super(name, null);
        this.option = option;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void write(Writer writer) throws IOException {
        writer.write('<');
        writer.write(name);
        writer.write('>');
        if (option != null) {
            option.write(writer);
        } else {
            writer.write(StringUtil.xmlEncode(value));
        }
        writer.write("</");
        writer.write(name);
        writer.write('>');
    }
}
