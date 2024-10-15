/*
DIT
 */

package ru.mos.mostech.ews.exchange.ews;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

/**

@Slf4j
 * Specific field update class to handle multiple attendee values
 */

@Slf4j
public class MultiValuedFieldUpdate extends FieldUpdate {
    ArrayList<String> values = new ArrayList<>();

    /**
     * Create field update with value.
     *
     * @param fieldURI target field
     */
    public MultiValuedFieldUpdate(FieldURI fieldURI) {
        this.fieldURI = fieldURI;
    }

    /**
     * Add single value
     *
     * @param value value
     */
    public void addValue(String value) {
        values.add(value);
    }

    /**
     * Write field to request writer.
     *
     * @param itemType item type
     * @param writer   request writer
     * @throws IOException on error
     */
    @Override
    public void write(String itemType, Writer writer) throws IOException {
        String action;
        //noinspection VariableNotUsedInsideIf
        if (values.isEmpty()) {
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
        if (itemType != null || (!values.isEmpty())) {
            StringBuilder buffer = new StringBuilder();
            ((UnindexedFieldURI)fieldURI).appendValues(buffer, itemType, values);
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
