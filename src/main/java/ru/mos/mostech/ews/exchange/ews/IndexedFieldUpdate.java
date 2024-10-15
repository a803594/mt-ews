/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

/**
 * Field update with multiple values.
 */

@Slf4j
public class IndexedFieldUpdate extends FieldUpdate {
    final Set<FieldUpdate> updates = new HashSet<>();
    protected final String collectionName;

    /**
     * Create indexed field update object.
     *
     * @param collectionName values collection name e.g. EmailAddresses
     */
    public IndexedFieldUpdate(String collectionName) {
        this.collectionName = collectionName;
    }

    /**
     * Add indexed field value.
     *
     * @param fieldUpdate field update object
     */
    public void addFieldValue(FieldUpdate fieldUpdate) {
        updates.add(fieldUpdate);
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
        if (itemType == null) {
            // check if at least one non null value
            boolean hasValue = false;
            for (FieldUpdate fieldUpdate : updates) {
                if (fieldUpdate.value != null) {
                    hasValue = true;
                    break;
                }
            }
            if (hasValue) {
                // use collection name on create
                writer.write("<t:");
                writer.write(collectionName);
                writer.write(">");

                StringBuilder buffer = new StringBuilder();
                for (FieldUpdate fieldUpdate : updates) {
                    fieldUpdate.fieldURI.appendValue(buffer, null, fieldUpdate.value);
                }
                writer.write(buffer.toString());

                writer.write("</t:");
                writer.write(collectionName);
                writer.write(">");
            }
        } else {
            // on update, write each fieldupdate
            for (FieldUpdate fieldUpdate : updates) {
                fieldUpdate.write(itemType, writer);
            }
        }
    }

}
