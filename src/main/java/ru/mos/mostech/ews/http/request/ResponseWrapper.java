/*
DIT
 */

package ru.mos.mostech.ews.http.request;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.net.URI;

/**
 * Http response wrapper.
 */

@Slf4j
public interface ResponseWrapper {
    URI getURI();

    int getStatusCode();
    HttpResponse getHttpResponse();
    String getResponseBodyAsString() throws IOException;
}
