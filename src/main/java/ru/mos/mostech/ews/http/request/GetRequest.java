/*
DIT
 */

package ru.mos.mostech.ews.http.request;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import ru.mos.mostech.ews.http.HttpClientAdapter;

import java.io.IOException;
import java.net.URI;

/**
 * Http get request with a string response handler.
 */

@Slf4j
public class GetRequest extends HttpGet implements ResponseHandler<String>, ResponseWrapper {
    private HttpResponse response;
    private String responseBodyAsString;

    public GetRequest(final URI uri) {
        super(uri);
    }

    /**
     * @throws IllegalArgumentException if the uri is invalid.
     */
    public GetRequest(final String uri) {
        super(uri);
    }


    /**
     * Handle request response and return response as string.
     * response body is null on redirect
     *
     * @param response response object
     * @return response body as string
     * @throws IOException on error
     */
    @Override
    public String handleResponse(HttpResponse response) throws IOException {
        this.response = response;
        if (HttpClientAdapter.isRedirect(response)) {
            return null;
        } else {
            responseBodyAsString = new BasicResponseHandler().handleResponse(response);
            return responseBodyAsString;
        }
    }

    public String getResponseBodyAsString() throws IOException {
        checkResponse();
        if (responseBodyAsString == null) {
            throw new IOException("No response body available");
        }
        return responseBodyAsString;
    }


    public Header getResponseHeader(String name) {
        checkResponse();
        return response.getFirstHeader(name);
    }

    /**
     * Get status code from response.
     * @return Http status code
     */
    public int getStatusCode() {
        checkResponse();
        return response.getStatusLine().getStatusCode();
    }

    /**
     * Get reason phrase from response.
     * @return reason phrase
     */
    public String getReasonPhrase() {
        checkResponse();
        return response.getStatusLine().getReasonPhrase();
    }

    public URI getRedirectLocation() {
        checkResponse();
        return HttpClientAdapter.getRedirectLocation(response);
    }

    public HttpResponse getHttpResponse() {
        return response;
    }

    /**
     * Check if response is available.
     */
    private void checkResponse() {
        if (response == null) {
            throw new IllegalStateException("Should execute request first");
        }
    }

}
