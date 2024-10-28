/*
DIT
 */

package ru.mos.mostech.ews.exchange.auth;

import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.http.HttpClientAdapter;

import java.io.IOException;
import java.net.URI;

@Slf4j
public class O365Authenticator implements ExchangeAuthenticator {

	public static String buildAuthorizeUrl(String tenantId, String clientId, String redirectUri, String username)
			throws IOException {
		throw new UnsupportedOperationException();
	}

	public void setUsername(String username) {
	}

	public void setPassword(String password) {

	}

	public O365Token getToken() {
		throw new UnsupportedOperationException();
	}

	public URI getExchangeUri() {
		return URI.create(Settings.O365_URL);
	}

	/**
	 * Вернуть экземпляр HttpClientAdapter с включенным пулом для доступа к O365
	 * @return экземпляр HttpClientAdapter
	 */
	@Override
	public HttpClientAdapter getHttpClientAdapter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void authenticate() throws IOException {

	}

}
