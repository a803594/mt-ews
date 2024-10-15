/*
DIT
 */

package ru.mos.mostech.ews.http.request;

import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.util.StringUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Custom Exchange SEARCH method.
 * Does not load full DOM in memory.
 */

@Slf4j
public class ExchangeSearchRequest extends ExchangeDavRequest {
    protected static final Logger LOGGER = Logger.getLogger(ExchangeSearchRequest.class);

    protected final String searchRequest;

    /**
     * Create search method.
     *
     * @param uri           method uri
     * @param searchRequest Exchange search request
     */
    public ExchangeSearchRequest(String uri, String searchRequest) {
        super(uri);
        this.searchRequest = searchRequest;
    }

    protected byte[] generateRequestContent() {
        try {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
                writer.write("<?xml version=\"1.0\"?>\n");
                writer.write("<d:searchrequest xmlns:d=\"DAV:\">\n");
                writer.write("        <d:sql>");
                writer.write(StringUtil.xmlEncode(searchRequest));
                writer.write("</d:sql>\n");
                writer.write("</d:searchrequest>");
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public String getMethod() {
        return "SEARCH";
    }

}