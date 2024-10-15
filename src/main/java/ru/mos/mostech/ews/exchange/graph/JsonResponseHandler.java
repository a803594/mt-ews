/*
DIT
 */

package ru.mos.mostech.ews.exchange.graph;

import org.apache.http.Header;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.ResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONException;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.http.HttpClientAdapter;
import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.util.IOUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import java.util.zip.GZIPInputStream;
import lombok.extern.slf4j.Slf4j;

/**
 * Generic Json response handler for graph API calls
 */

@Slf4j
public class JsonResponseHandler implements ResponseHandler<JSONObject> {
    @Override
    public JSONObject handleResponse(HttpResponse response) throws IOException {
        JSONObject jsonResponse = null;
        Header contentTypeHeader = response.getFirstHeader("Content-Type");
        if (contentTypeHeader != null && contentTypeHeader.getValue().startsWith("application/json")) {
            try {
                jsonResponse = new JSONObject(new String(readResponse(response), StandardCharsets.UTF_8));
            } catch (JSONException e) {
                throw new IOException(e.getMessage(), e);
            }
        } else {
            HttpEntity httpEntity = response.getEntity();
            if (httpEntity != null) {
                try {
                    return new JSONObject().put("response", new String(readResponse(response), StandardCharsets.UTF_8));
                } catch (JSONException e) {
                    throw new IOException("Invalid response content");
                }
            }
        }
        // check http error code
        if (response.getStatusLine().getStatusCode() >= 400) {
            String errorMessage = null;
            if (jsonResponse != null && jsonResponse.optJSONObject("error") != null) {
                try {
                    JSONObject jsonError = jsonResponse.getJSONObject("error");
                    errorMessage = jsonError.optString("code") + " " + jsonError.optString("message");
                } catch (JSONException e) {
                    // ignore
                }
            }
            if (errorMessage == null) {
                errorMessage = response.getStatusLine().getReasonPhrase();
            }
            throw new IOException(errorMessage);
        }
        return jsonResponse;
    }

    protected byte[] readResponse(HttpResponse response) throws IOException {
        byte[] content;
        try (InputStream inputStream = response.getEntity().getContent()) {
            if (HttpClientAdapter.isGzipEncoded(response)) {
                content = IOUtil.readFully(new GZIPInputStream(inputStream));
            } else {
                content = IOUtil.readFully(inputStream);
            }
        }
        return content;
    }
}