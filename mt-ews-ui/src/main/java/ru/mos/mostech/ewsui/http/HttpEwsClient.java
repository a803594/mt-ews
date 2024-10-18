package ru.mos.mostech.ewsui.http;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class HttpEwsClient {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final URI EWS_SETTINGS_URI;
    private static final String EWS_HTTP_ADDRESS = "http://localhost";
    private static final int EWS_HTTP_PORT = 51081;

    static {
        try {
            String settingsUri = String.format("%s:%s/ews-settings", EWS_HTTP_ADDRESS, EWS_HTTP_PORT);
            EWS_SETTINGS_URI = new URI(settingsUri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> getEwsSettings() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(EWS_SETTINGS_URI)
                .GET()
                .build();

        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return new JSONObject(response.body()).toMap();
        } catch (InterruptedException | IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
