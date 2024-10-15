/*
DIT
 */

package ru.mos.mostech.ews.exchange.auth;

import org.apache.log4j.Logger;
import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.Settings;
import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.http.HttpClientAdapter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;

/**
 * Experimental: load Oauth2 token from settings
 */
@SuppressWarnings("unused")

@Slf4j
public class O365StoredTokenAuthenticator implements ExchangeAuthenticator {
    private static final Logger LOGGER = Logger.getLogger(O365StoredTokenAuthenticator.class);

    URI ewsUrl = URI.create(Settings.getO365Url());

    private String username;
    private String password;
    private O365Token token;

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Return a pool enabled HttpClientAdapter instance to access O365
     * @return HttpClientAdapter instance
     */
    @Override
    public HttpClientAdapter getHttpClientAdapter() {
        return new HttpClientAdapter(getExchangeUri(), username, password, true);
    }

    @Override
    public void authenticate() throws IOException {
        // common MT-EWS client id
        final String clientId = Settings.getProperty("mt.ews.oauth.clientId", "facd6cff-a294-4415-b59f-c5b01937d7bd");
        // standard native app redirectUri
        final String redirectUri = Settings.getProperty("mt.ews.oauth.redirectUri", Settings.O365_LOGIN_URL+"common/oauth2/nativeclient");
        // company tenantId or common
        String tenantId = Settings.getProperty("mt.ews.oauth.tenantId", "common");

        String refreshToken = Settings.getProperty("mt.ews.oauth."+username.toLowerCase()+".refreshToken");
        if (refreshToken == null) {
            // single user mode
            refreshToken = Settings.getProperty("mt.ews.oauth.refreshToken");
        }
        String accessToken = Settings.getProperty("mt.ews.oauth.accessToken");
        if (refreshToken == null && accessToken == null) {
            LOGGER.warn("No stored Oauth refresh token found for "+username);
            throw new IOException("No stored Oauth refresh token found for "+username);
        }

        token = new O365Token(tenantId, clientId, redirectUri, password);
        if (accessToken != null) {
            // for tests only: load access token, will expire in at most one hour
            token.setAccessToken(accessToken);
        } else {
            token.setRefreshToken(refreshToken);
            token.refreshToken();
        }
    }

    @Override
    public O365Token getToken() {
        return token;
    }

    @Override
    public URI getExchangeUri() {
        return ewsUrl;
    }
}
