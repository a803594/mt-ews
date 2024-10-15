/*
DIT
 */

package ru.mos.mostech.ews.http.request;

import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.webdav.header.DepthHeader;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameIterator;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom Exchange PROPFIND method.
 * Does not load full DOM in memory.
 */

@Slf4j
public class ExchangePropFindRequest extends ExchangeDavRequest {
    protected static final Logger LOGGER = Logger.getLogger(ExchangePropFindRequest.class);

    protected final DavPropertyNameSet propertyNameSet;

    public ExchangePropFindRequest(String uri) {
        this(uri, null, DepthHeader.DEPTH_INFINITY);
    }

    public ExchangePropFindRequest(String uri, DavPropertyNameSet propertyNameSet, int depth) {
        super(uri);
        this.propertyNameSet = propertyNameSet;
        DepthHeader dh = new DepthHeader(depth);
        setHeader(dh.getHeaderName(), dh.getHeaderValue());
    }

    protected byte[] generateRequestContent() {
        try {
            // build namespace map
            int currentChar = 'e';
            final Map<String, Integer> nameSpaceMap = new HashMap<>();
            nameSpaceMap.put("DAV:", (int) 'D');
            if (propertyNameSet != null) {
                DavPropertyNameIterator propertyNameIterator = propertyNameSet.iterator();
                while (propertyNameIterator.hasNext()) {
                    DavPropertyName davPropertyName = propertyNameIterator.nextPropertyName();

                    davPropertyName.getName();
                    // property namespace
                    String namespaceUri = davPropertyName.getNamespace().getURI();
                    if (!nameSpaceMap.containsKey(namespaceUri)) {
                        nameSpaceMap.put(namespaceUri, currentChar++);
                    }
                }
            }
            // <D:propfind xmlns:D="DAV:"><D:prop><D:displayname/></D:prop></D:propfind>
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
                writer.write("<D:propfind ");
                for (Map.Entry<String, Integer> mapEntry : nameSpaceMap.entrySet()) {
                    writer.write(" xmlns:");
                    writer.write((char) mapEntry.getValue().intValue());
                    writer.write("=\"");
                    writer.write(mapEntry.getKey());
                    writer.write("\"");
                }
                writer.write(">");
                if (propertyNameSet == null || propertyNameSet.isEmpty()) {
                    writer.write("<D:allprop/>");
                } else {
                    writer.write("<D:prop>");
                    DavPropertyNameIterator propertyNameIterator = propertyNameSet.iterator();
                    while (propertyNameIterator.hasNext()) {
                        DavPropertyName davPropertyName = propertyNameIterator.nextPropertyName();
                        char nameSpaceChar = (char) nameSpaceMap.get(davPropertyName.getNamespace().getURI()).intValue();
                        writer.write('<');
                        writer.write(nameSpaceChar);
                        writer.write(':');
                        writer.write(davPropertyName.getName());
                        writer.write("/>");
                    }
                    writer.write("</D:prop>");
                }
                writer.write("</D:propfind>");
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public String getMethod() {
        return "PROPFIND";
    }

}
