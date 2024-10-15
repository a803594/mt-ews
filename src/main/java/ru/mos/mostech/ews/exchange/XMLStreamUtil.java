/*
DIT
 */
package ru.mos.mostech.ews.exchange;

import org.apache.log4j.Logger;
import lombok.extern.slf4j.Slf4j;

import javax.xml.stream.XMLInputFactory;
import lombok.extern.slf4j.Slf4j;
import javax.xml.stream.XMLStreamConstants;
import lombok.extern.slf4j.Slf4j;
import javax.xml.stream.XMLStreamException;
import lombok.extern.slf4j.Slf4j;
import javax.xml.stream.XMLStreamReader;
import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayInputStream;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import java.io.StringReader;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * XmlStreamReader utility methods
 */

@Slf4j
public final class XMLStreamUtil {
    private static final Logger LOGGER = Logger.getLogger(XMLStreamUtil.class);

    private XMLStreamUtil() {
    }

    /**
     * Build a new XMLInputFactory.
     *
     * @return XML input factory
     */
    public static XMLInputFactory getXmlInputFactory() {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        inputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        // Woodstox 5.2.0 or later
        if (inputFactory.isPropertySupported("com.ctc.wstx.allowXml11EscapedCharsInXml10")) {
            inputFactory.setProperty("com.ctc.wstx.allowXml11EscapedCharsInXml10", Boolean.TRUE);
        }
        return inputFactory;
    }

    /**
     * Convert the XML stream to a map of entries.
     * An entry is also a key/value map
     *
     * @param inputStream xml input stream
     * @param rowName     xml tag name of entries
     * @param idName      xml tag name of entry attribute used as key in the main map
     * @return map of entries
     * @throws IOException on error
     */
    public static Map<String, Map<String, String>> getElementContentsAsMap(InputStream inputStream, String rowName, String idName) throws IOException {
        Map<String, Map<String, String>> results = new HashMap<>();
        Map<String, String> item = null;
        String currentElement = null;
        XMLStreamReader reader = null;
        try {
            XMLInputFactory inputFactory = getXmlInputFactory();
            reader = inputFactory.createXMLStreamReader(inputStream);
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT && rowName.equals(reader.getLocalName())) {
                    item = new HashMap<>();
                } else if (event == XMLStreamConstants.END_ELEMENT && rowName.equals(reader.getLocalName())) {
                    if (item != null && item.containsKey(idName)) {
                        results.put(item.get(idName).toLowerCase(), item);
                    }
                    item = null;
                } else if (event == XMLStreamConstants.START_ELEMENT && item != null) {
                    currentElement = reader.getLocalName();
                } else if (event == XMLStreamConstants.CHARACTERS && currentElement != null) {
                    String text = reader.getText();
                    if (item != null) {
                        item.put(currentElement, text);
                    }
                    currentElement = null;
                }
            }
        } catch (XMLStreamException e) {
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (XMLStreamException e) {
                ExchangeSession.LOGGER.error(e);
            }
        }
        return results;
    }

    /**
     * Test if reader is on a start tag named tagLocalName.
     *
     * @param reader       xml stream reader
     * @param tagLocalName tag local name
     * @return true if reader is on a start tag named tagLocalName
     */
    public static boolean isStartTag(XMLStreamReader reader, String tagLocalName) {
        return (reader.getEventType() == XMLStreamConstants.START_ELEMENT) && (reader.getLocalName().equals(tagLocalName));
    }

    /**
     * Test if reader is on a start tag.
     *
     * @param reader xml stream reader
     * @return true if reader is on a start tag
     */
    public static boolean isStartTag(XMLStreamReader reader) {
        return (reader.getEventType() == XMLStreamConstants.START_ELEMENT);
    }

    /**
     * Test if reader is on an end tag named tagLocalName.
     *
     * @param reader       xml stream reader
     * @param tagLocalName tag local name
     * @return true if reader is on an end tag named tagLocalName
     */
    public static boolean isEndTag(XMLStreamReader reader, String tagLocalName) {
        return (reader.getEventType() == XMLStreamConstants.END_ELEMENT) && (reader.getLocalName().equals(tagLocalName));
    }

    /**
     * Create XML stream reader for byte array
     *
     * @param xmlContent xml content as byte array
     * @return XML stream reader
     * @throws XMLStreamException on error
     */
    public static XMLStreamReader createXMLStreamReader(byte[] xmlContent) throws XMLStreamException {
        return createXMLStreamReader(new ByteArrayInputStream(xmlContent));
    }

    /**
     * Create XML stream reader for string
     *
     * @param xmlContent xml content as string
     * @return XML stream reader
     * @throws XMLStreamException on error
     */
    public static XMLStreamReader createXMLStreamReader(String xmlContent) throws XMLStreamException {
        XMLInputFactory xmlInputFactory = XMLStreamUtil.getXmlInputFactory();
        return xmlInputFactory.createXMLStreamReader(new StringReader(xmlContent));
    }

    /**
     * Create XML stream reader for inputStream
     *
     * @param inputStream xml content inputStream
     * @return XML stream reader
     * @throws XMLStreamException on error
     */
    public static XMLStreamReader createXMLStreamReader(InputStream inputStream) throws XMLStreamException {
        XMLInputFactory xmlInputFactory = XMLStreamUtil.getXmlInputFactory();
        return xmlInputFactory.createXMLStreamReader(inputStream);
    }

    /**
     * Get element text.
     *
     * @param reader stream reader
     * @return element text
     */
    public static String getElementText(XMLStreamReader reader) {
        String value = null;
        try {
            value = reader.getElementText();
        } catch (XMLStreamException | RuntimeException e) {
            // RuntimeException: probably com.ctc.wstx.exc.WstxLazyException on invalid character sequence
            LOGGER.warn(e.getMessage());
        }

        return value;
    }

}
