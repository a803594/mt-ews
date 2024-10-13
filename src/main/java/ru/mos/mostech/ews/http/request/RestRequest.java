/*
DIT
 */

package ru.mos.mostech.ews.http.request;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import ru.mos.mostech.ews.http.HttpClientAdapter;
import ru.mos.mostech.ews.util.IOUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

/**
 * Generic Rest request.
 */
public class RestRequest extends HttpPost implements ResponseHandler<JSONObject> {
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final Logger LOGGER = Logger.getLogger(RestRequest.class);

    private HttpResponse response;
    private JSONObject jsonBody;

    public RestRequest(String uri) {
        super(uri);

        AbstractHttpEntity httpEntity = new AbstractHttpEntity() {
            byte[] content;

            @Override
            public boolean isRepeatable() {
                return true;
            }

            @Override
            public long getContentLength() {
                if (content == null) {
                    content = getJsonContent();
                }
                return content.length;
            }

            @Override
            public InputStream getContent() throws UnsupportedOperationException {
                if (content == null) {
                    content = getJsonContent();
                }
                return new ByteArrayInputStream(content);
            }

            @Override
            public void writeTo(OutputStream outputStream) throws IOException {
                if (content == null) {
                    content = getJsonContent();
                }
                outputStream.write(content);
            }

            @Override
            public boolean isStreaming() {
                return false;
            }
        };
        httpEntity.setContentType(JSON_CONTENT_TYPE);
        setEntity(httpEntity);
    }

    public RestRequest(String uri, HttpEntity entity) {
        super(uri);
        setEntity(entity);
    }

    protected byte[] getJsonContent() {
        return jsonBody.toString().getBytes(Consts.UTF_8);
    }

    public void setJsonBody(JSONObject jsonBody) {
        this.jsonBody = jsonBody;
    }

    public void setRequestHeader(String name, String value) {
        setHeader(name, value);
    }

    @Override
    public JSONObject handleResponse(HttpResponse response) throws IOException {
        this.response = response;
        JSONObject jsonResponse;
        Header contentTypeHeader = response.getFirstHeader("Content-Type");
        if (contentTypeHeader != null && JSON_CONTENT_TYPE.equals(contentTypeHeader.getValue())) {
            try (InputStream inputStream = response.getEntity().getContent()){
                if (HttpClientAdapter.isGzipEncoded(response)) {
                    jsonResponse = processResponseStream(new GZIPInputStream(inputStream));
                } else {
                    jsonResponse = processResponseStream(inputStream);
                }
            } catch (JSONException e) {
                LOGGER.error("Error while parsing json response: " + e, e);
                throw new IOException(e.getMessage(), e);
            }
        } else {
            throw new IOException("Invalid response content");
        }
        return jsonResponse;
    }

    private JSONObject processResponseStream(InputStream responseBodyAsStream) throws IOException, JSONException {
        // quick non streaming implementation
        return new JSONObject(new String(IOUtil.readFully(responseBodyAsStream), StandardCharsets.UTF_8));
    }
}
