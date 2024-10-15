/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Writer;

/**
 * Folder Id.
 */

@Slf4j
public class FolderId extends Option {
    protected String changeKey;
    protected String mailbox;

    /**
     * Create FolderId with specified tag name.
     *
     * @param name      field tag name
     * @param value     id value
     * @param changeKey folder change key
     * @param mailbox   shared mailbox name
     */
    protected FolderId(String name, String value, String changeKey, String mailbox) {
        this(name, value, changeKey);
        this.mailbox = mailbox;
    }

    /**
     * Create FolderId with specified tag name.
     *
     * @param name      field tag name
     * @param value     id value
     * @param changeKey folder change key
     */
    protected FolderId(String name, String value, String changeKey) {
        super(name, value);
        this.changeKey = changeKey;
    }

    /**
     * Build Folder id from response item.
     *
     * @param item    response item
     */
    public FolderId(EWSMethod.Item item) {
        this("t:FolderId", item.get("FolderId"), item.get("ChangeKey"));
    }


    /**
     * @inheritDoc
     */
    @Override
    public void write(Writer writer) throws IOException {
        writer.write('<');
        writer.write(name);
        writer.write(" Id=\"");
        writer.write(value);
        if (changeKey != null) {
            writer.write("\" ChangeKey=\"");
            writer.write(changeKey);
        }
        if (mailbox == null) {
            writer.write("\"/>");
        } else {
            writer.write("\"><t:Mailbox><t:EmailAddress>");
            writer.write(mailbox);
            writer.write("</t:EmailAddress></t:Mailbox></");
            writer.write(name);
            writer.write('>');
        }
    }

}
