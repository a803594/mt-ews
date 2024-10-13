/*
DIT
 */

package ru.mos.mostech.ews.exchange.graph;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.exchange.ews.ExtendedFieldURI;
import ru.mos.mostech.ews.exchange.ews.FieldURI;
import ru.mos.mostech.ews.exchange.ews.IndexedFieldURI;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Build Microsoft graph request
 */
public class GraphRequestBuilder {

    String method = "POST";
    String baseUrl = Settings.GRAPH_URL;
    String version = "beta";
    String mailbox;
    String objectType;

    String childType;

    String filter;

    Set<FieldURI> expandFields;

    String accessToken;

    JSONObject jsonBody = null;
    private String objectId;

    /**
     * Set property in Json body.
     * @param name property name
     * @param value property value
     * @throws JSONException on error
     */
    public GraphRequestBuilder setProperty(String name, String value) throws JSONException {
        if (jsonBody == null) {
            jsonBody = new JSONObject();
        }
        jsonBody.put(name, value);
        return this;
    }

    /**
     * Set epxand fields (returning attributes).
     * @param expandFields set of fields to return
     * @return this
     */
    public GraphRequestBuilder setExpandFields(Set<FieldURI> expandFields) {
        this.expandFields = expandFields;
        return this;
    }

    public GraphRequestBuilder setObjectType(String objectType) {
        this.objectType = objectType;
        return this;
    }

    public GraphRequestBuilder setChildType(String childType) {
        this.childType = childType;
        return this;
    }

    public GraphRequestBuilder setFilter(String filter) {
        this.filter = filter;
        return this;
    }

    public GraphRequestBuilder setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public GraphRequestBuilder setMethod(String method) {
        this.method = method;
        return this;
    }

    public GraphRequestBuilder setMailbox(String mailbox) {
        this.mailbox = mailbox;
        return this;
    }

    public GraphRequestBuilder setObjectId(String objectId) {
        this.objectId = objectId;
        return this;
    }

    /**
     * Build request path based on version, username, object type and object id.
     * @return request path
     */
    protected String buildPath() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("/").append(version);
        if (mailbox != null) {
            buffer.append("/users/").append(mailbox);
        } else {
            buffer.append("/me");
        }
        if (objectType != null) {
            buffer.append("/").append(objectType);
        }
        if (objectId != null) {
            buffer.append("/").append(objectId);
        }
        if (childType != null) {
            buffer.append("/").append(childType);
        }

        return buffer.toString();
    }

    /**
     * Compute expand parameters from properties.
     * @return $expand value
     */
    private String buildExpand() {
        ArrayList<String> singleValueProperties = new ArrayList<>();
        ArrayList<String> multiValueProperties = new ArrayList<>();
        for (FieldURI fieldURI : expandFields) {
            if (fieldURI instanceof ExtendedFieldURI) {
                singleValueProperties.add(fieldURI.getGraphId());
            } else if (fieldURI instanceof IndexedFieldURI) {
                multiValueProperties.add(fieldURI.getGraphId());
            }
        }
        StringBuilder expand = new StringBuilder();
        if (!singleValueProperties.isEmpty()) {
            expand.append("singleValueExtendedProperties($filter=");
            appendExpandProperties(expand, singleValueProperties);
            expand.append(")");
        }
        if (!multiValueProperties.isEmpty()) {
            if (!singleValueProperties.isEmpty()) {
                expand.append(",");
            }
            expand.append("multiValueExtendedProperties($filter=");
            appendExpandProperties(expand, multiValueProperties);
            expand.append(")");
        }
        return expand.toString();
    }

    protected void appendExpandProperties(StringBuilder buffer, List<String> properties) {
        boolean first = true;
        for (String id : properties) {
            if (first) {
                first = false;
            } else {
                buffer.append(" or ");
            }
            buffer.append("id eq '").append(id).append("'");
        }
    }


    /**
     * Build http request.
     * @return Http request
     * @throws IOException on error
     */
    public HttpRequestBase build() throws IOException {
        try {
            URIBuilder uriBuilder = new URIBuilder(baseUrl).setPath(buildPath());
            if (expandFields != null) {
                uriBuilder.addParameter("$expand", buildExpand());
            }

            if (filter != null) {
                uriBuilder.addParameter("$filter", filter);
            }

            HttpRequestBase httpRequest;
            if ("POST".equals(method)) {
                httpRequest = new HttpPost(uriBuilder.build());
                if (jsonBody != null) {
                    ((HttpPost) httpRequest).setEntity(new ByteArrayEntity(jsonBody.toString().getBytes(StandardCharsets.UTF_8)));
                }
            } else {
                // default to GET request
                httpRequest = new HttpGet(uriBuilder.build());
            }
            httpRequest.setHeader("Content-Type", "application/json");
            httpRequest.setHeader("Authorization", "Bearer " + accessToken);

            return httpRequest;
        } catch (URISyntaxException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

}
