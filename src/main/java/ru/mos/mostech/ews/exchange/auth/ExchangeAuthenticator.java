/*
DIT
 */

package ru.mos.mostech.ews.exchange.auth;

import ru.mos.mostech.ews.http.HttpClientAdapter;

import java.io.IOException;
import java.net.URI;

/**
 * Common interface for all Exchange and O365 authenticators. Implement this interface to
 * build custom authenticators for unsupported Exchange architecture
 */
public interface ExchangeAuthenticator {

	void setUsername(String username);

	void setPassword(String password);

	/**
	 * Authenticate against Exchange or O365
	 * @throws IOException on error
	 */
	void authenticate() throws IOException;

	O365Token getToken() throws IOException;

	/**
	 * Return default or computed Exchange or O365 url
	 * @return target url
	 */
	URI getExchangeUri();

	/**
	 * Return a new HttpClientAdapter instance with pooling enabled for ExchangeSession
	 * @return HttpClientAdapter instance
	 */
	HttpClientAdapter getHttpClientAdapter();

}
