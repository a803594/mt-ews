/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import ru.mos.mostech.ews.exchange.XMLStreamUtil;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.Writer;

/**
 * Get User Configuration method.
 */
public class GetUserConfigurationMethod extends EWSMethod {

    /**
     * Get User Configuration method.
     */
    public GetUserConfigurationMethod() {
        super("UserConfiguration", "GetUserConfiguration");
        folderId = DistinguishedFolderId.getInstance(null, DistinguishedFolderId.Name.root);
    }

    @Override
    protected void writeSoapBody(Writer writer) throws IOException {
        writer.write("<m:UserConfigurationName Name=\"OWA.UserOptions\">");
        folderId.write(writer);
        writer.write("</m:UserConfigurationName>");
        writer.write("<m:UserConfigurationProperties>All</m:UserConfigurationProperties>");
    }

    @Override
    protected void handleCustom(XMLStreamReader reader) throws XMLStreamException {
        if (XMLStreamUtil.isStartTag(reader, "UserConfiguration")) {
            responseItems.add(handleUserConfiguration(reader));
        }
    }

    private Item handleUserConfiguration(XMLStreamReader reader) throws XMLStreamException {
        Item responseItem = new Item();
        while (reader.hasNext() && !(XMLStreamUtil.isEndTag(reader, "UserConfiguration"))) {
            reader.next();
            if (XMLStreamUtil.isStartTag(reader)) {
                String tagLocalName = reader.getLocalName();
                if ("DictionaryEntry".equals(tagLocalName)) {
                    handleDictionaryEntry(reader, responseItem);
                }
            }
        }
        return responseItem;
    }

    private void handleDictionaryEntry(XMLStreamReader reader, Item responseItem) throws XMLStreamException {
        String key = null;
        while (reader.hasNext() && !(XMLStreamUtil.isEndTag(reader, "DictionaryEntry"))) {
            reader.next();
            if (XMLStreamUtil.isStartTag(reader)) {
                String tagLocalName = reader.getLocalName();
                if ("Value".equals(tagLocalName)) {
                    if (key == null) {
                        key = reader.getElementText();
                    } else {
                        responseItem.put(key, XMLStreamUtil.getElementText(reader));
                    }
                }
            }
        }
    }

}
