/*
DIT
 */

package ru.mos.mostech.ews.http.request;

import org.apache.http.HttpResponse;

import java.io.IOException;
import java.net.URI;

/**
 * Http response wrapper.
 */
public interface ResponseWrapper {
    URI getURI();

    int getStatusCode();
    HttpResponse getHttpResponse();
    String getResponseBodyAsString() throws IOException;
}
