/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import java.io.IOException;
import java.io.Writer;

/**
 * Field update
 */
public class FieldUpdate {
    FieldURI fieldURI;
    String value;

    /**
     * Create field update with value.
     *
     * @param fieldURI target field
     * @param value    field value
     */
    public FieldUpdate(FieldURI fieldURI, String value) {
        this.fieldURI = fieldURI;
        this.value = value;
    }

    protected FieldUpdate() {
        // empty constructor for subclass
    }

    /**
     * Write field to request writer.
     *
     * @param itemType item type
     * @param writer   request writer
     * @throws IOException on error
     */
    public void write(String itemType, Writer writer) throws IOException {
        String action;
        //noinspection VariableNotUsedInsideIf
        if (value == null || value.length() == 0) {
            action = "Delete";
        } else {
            action = "Set";
        }
        if (itemType != null) {
            writer.write("<t:");
            writer.write(action);
            writer.write(itemType);
            writer.write("Field>");
        }

        // do not try to set empty value on create
        if (itemType != null || (value != null && value.length() > 0)) {
            StringBuilder buffer = new StringBuilder();
            if (value == null || value.length() == 0) {
                fieldURI.appendTo(buffer);
            } else {
                fieldURI.appendValue(buffer, itemType, value);
            }
            writer.write(buffer.toString());
        }

        if (itemType != null) {
            writer.write("</t:");
            writer.write(action);
            writer.write(itemType);
            writer.write("Field>");
        }
    }
}
