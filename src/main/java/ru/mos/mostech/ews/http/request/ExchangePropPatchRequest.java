/*
DIT
 */

package ru.mos.mostech.ews.http.request;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.log4j.Logger;
import ru.mos.mostech.ews.exchange.dav.PropertyValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Custom Exchange PROPPATCH method.
 * Supports extended property update with type.
 */

public class ExchangePropPatchRequest extends ExchangeDavRequest {
    protected static final Logger LOGGER = Logger.getLogger(ExchangePropPatchRequest.class);
    static final String TYPE_NAMESPACE = "urn:schemas-microsoft-com:datatypes";
    final Set<PropertyValue> propertyValues;
    private StatusLine statusLine;

    /**
     * Create PROPPATCH method.
     *
     * @param path           path
     * @param propertyValues property values
     */
    public ExchangePropPatchRequest(String path, Set<PropertyValue> propertyValues) {
        super(path);
        this.propertyValues = propertyValues;
    }

    @Override
    protected byte[] generateRequestContent() {
        try {
            // build namespace map
            int currentChar = 'e';
            final Map<String, Integer> nameSpaceMap = new HashMap<>();
            final Set<PropertyValue> setPropertyValues = new HashSet<>();
            final Set<PropertyValue> deletePropertyValues = new HashSet<>();
            for (PropertyValue propertyValue : propertyValues) {
                // data type namespace
                if (!nameSpaceMap.containsKey(TYPE_NAMESPACE) && propertyValue.getTypeString() != null) {
                    nameSpaceMap.put(TYPE_NAMESPACE, currentChar++);
                }
                // property namespace
                String namespaceUri = propertyValue.getNamespaceUri();
                if (!nameSpaceMap.containsKey(namespaceUri)) {
                    nameSpaceMap.put(namespaceUri, currentChar++);
                }
                if (propertyValue.getXmlEncodedValue() == null) {
                    deletePropertyValues.add(propertyValue);
                } else {
                    setPropertyValues.add(propertyValue);
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (
                    OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)
            ) {
                writer.write("<D:propertyupdate xmlns:D=\"DAV:\"");
                for (Map.Entry<String, Integer> mapEntry : nameSpaceMap.entrySet()) {
                    writer.write(" xmlns:");
                    writer.write((char) mapEntry.getValue().intValue());
                    writer.write("=\"");
                    writer.write(mapEntry.getKey());
                    writer.write("\"");
                }
                writer.write(">");
                if (!setPropertyValues.isEmpty()) {
                    writer.write("<D:set><D:prop>");
                    for (PropertyValue propertyValue : setPropertyValues) {
                        String typeString = propertyValue.getTypeString();
                        char nameSpaceChar = (char) nameSpaceMap.get(propertyValue.getNamespaceUri()).intValue();
                        writer.write('<');
                        writer.write(nameSpaceChar);
                        writer.write(':');
                        writer.write(propertyValue.getName());
                        if (typeString != null) {
                            writer.write(' ');
                            writer.write(nameSpaceMap.get(TYPE_NAMESPACE));
                            writer.write(":dt=\"");
                            writer.write(typeString);
                            writer.write("\"");
                        }
                        writer.write('>');
                        writer.write(propertyValue.getXmlEncodedValue());
                        writer.write("</");
                        writer.write(nameSpaceChar);
                        writer.write(':');
                        writer.write(propertyValue.getName());
                        writer.write('>');
                    }
                    writer.write("</D:prop></D:set>");
                }
                if (!deletePropertyValues.isEmpty()) {
                    writer.write("<D:remove><D:prop>");
                    for (PropertyValue propertyValue : deletePropertyValues) {
                        char nameSpaceChar = (char) nameSpaceMap.get(propertyValue.getNamespaceUri()).intValue();
                        writer.write('<');
                        writer.write(nameSpaceChar);
                        writer.write(':');
                        writer.write(propertyValue.getName());
                        writer.write("/>");
                    }
                    writer.write("</D:prop></D:remove>");
                }
                writer.write("</D:propertyupdate>");
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getMethod() {
        return "PROPPATCH";
    }

    @Override
    public List<MultiStatusResponse> handleResponse(HttpResponse response) {
        this.statusLine = response.getStatusLine();
        return super.handleResponse(response);
    }


    public StatusLine getStatusLine() {
        return statusLine;
    }
}
