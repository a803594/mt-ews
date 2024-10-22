/*
DIT
 */
package ru.mos.mostech.ews.exchange.auth;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.exception.MosTechEwsAuthenticationException;
import ru.mos.mostech.ews.exception.MosTechEwsException;
import ru.mos.mostech.ews.exchange.ews.BaseShape;
import ru.mos.mostech.ews.exchange.ews.DistinguishedFolderId;
import ru.mos.mostech.ews.exchange.ews.GetFolderMethod;
import ru.mos.mostech.ews.exchange.ews.GetUserConfigurationMethod;
import ru.mos.mostech.ews.http.HttpClientAdapter;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.security.Security;

@Slf4j
public class O365InteractiveAuthenticator implements ExchangeAuthenticator {

	private static final int MAX_COUNT = 300;

	static {
		// disable HTTP/2 loader on Java 14 and later to enable custom socket factory
		System.setProperty("com.sun.webkit.useHTTP2Loader", "false");
	}

	boolean isAuthenticated = false;

	String errorCode = null;

	String code = null;

	URI ewsUrl = URI.create(Settings.getO365Url());

	private O365InteractiveAuthenticatorFrame o365InteractiveAuthenticatorFrame;

	private O365ManualAuthenticatorDialog o365ManualAuthenticatorDialog;

	private String username;

	private String password;

	private O365Token token;

	public O365Token getToken() {
		return token;
	}

	@Override
	public URI getExchangeUri() {
		return ewsUrl;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

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

	public void authenticate() throws IOException {

		// allow cross domain requests for Okta form support
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
		// enable NTLM for ADFS support
		System.setProperty("jdk.http.ntlm.transparentAuth", "allHosts");

		// common MT-EWS client id
		final String clientId = Settings.getProperty("mt.ews.oauth.clientId", "facd6cff-a294-4415-b59f-c5b01937d7bd");
		// standard native app redirectUri
		final String redirectUri = Settings.getProperty("mt.ews.oauth.redirectUri",
				Settings.O365_LOGIN_URL + "common/oauth2/nativeclient");
		// company tenantId or common
		String tenantId = Settings.getProperty("mt.ews.oauth.tenantId", "common");

		// first try to load stored token
		token = O365Token.load(tenantId, clientId, redirectUri, username, password);
		if (token != null) {
			isAuthenticated = true;
			return;
		}

		final String initUrl = O365Authenticator.buildAuthorizeUrl(tenantId, clientId, redirectUri, username);

		// set default authenticator
		Authenticator.setDefault(new Authenticator() {
			@Override
			public PasswordAuthentication getPasswordAuthentication() {
				if (getRequestorType() == RequestorType.PROXY) {
					String proxyUser = Settings.getProperty("mt.ews.proxyUser");
					String proxyPassword = Settings.getProperty("mt.ews.proxyPassword");
					if (proxyUser != null && proxyPassword != null) {
						log.debug("Proxy authentication with user " + proxyUser);
						return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
					}
					else {
						log.debug("Missing proxy credentials ");
						return null;
					}
				}
				else {
					log.debug("Password authentication with user " + username);
					return new PasswordAuthentication(username, password.toCharArray());
				}
			}
		});

		boolean isJFXAvailable = true;
		try {
			Class.forName("javafx.application.Platform");
		}
		catch (ClassNotFoundException e) {
			log.warn("Unable to load JavaFX (OpenJFX), switch to manual mode");
			isJFXAvailable = false;
		}

		if (isJFXAvailable) {
			SwingUtilities.invokeLater(() -> {
				try {
					o365InteractiveAuthenticatorFrame = new O365InteractiveAuthenticatorFrame();
					o365InteractiveAuthenticatorFrame
						.setO365InteractiveAuthenticator(O365InteractiveAuthenticator.this);
					o365InteractiveAuthenticatorFrame.authenticate(initUrl, redirectUri);
				}
				catch (NoClassDefFoundError e) {
					log.warn("Unable to load JavaFX (OpenJFX)");
				}
				catch (IllegalAccessError e) {
					log.warn(
							"Unable to load JavaFX (OpenJFX), append --add-exports java.base/sun.net.www.protocol.https=ALL-UNNAMED to java options");
				}

			});
		}
		else {
			if (o365InteractiveAuthenticatorFrame == null) {
				try {
					SwingUtilities.invokeAndWait(
							() -> o365ManualAuthenticatorDialog = new O365ManualAuthenticatorDialog(initUrl));
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				catch (InvocationTargetException e) {
					throw new IOException(e);
				}
				code = o365ManualAuthenticatorDialog.getCode();
				isAuthenticated = code != null;
				if (!isAuthenticated) {
					errorCode = "User did not provide authentication code";
				}
			}
		}

		int count = 0;

		while (!isAuthenticated && errorCode == null && count++ < MAX_COUNT) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		if (count > MAX_COUNT) {
			errorCode = "Timed out waiting for interactive authentication";
		}

		if (o365InteractiveAuthenticatorFrame != null && o365InteractiveAuthenticatorFrame.isVisible()) {
			o365InteractiveAuthenticatorFrame.close();
		}

		if (isAuthenticated) {
			token = O365Token.build(tenantId, clientId, redirectUri, code, password);

			log.debug("Authenticated username: " + token.getUsername());
			if (username != null && !username.isEmpty() && !username.equalsIgnoreCase(token.getUsername())) {
				throw new MosTechEwsAuthenticationException(
						"Authenticated username " + token.getUsername() + " does not match " + username);
			}

		}
		else {
			log.error("Authentication failed " + errorCode);
			throw new MosTechEwsException("EXCEPTION_AUTHENTICATION_FAILED_REASON", errorCode);
		}
	}

	public static void main(String[] argv) {

		try {
			// set custom factory before loading OpenJFX
			Security.setProperty("ssl.SocketFactory.provider", "mt.ews.http.DavGatewaySSLSocketFactory");

			Settings.setDefaultSettings();
			Settings.setConfigFilePath("mt-ews-interactive.properties");
			Settings.load();

			O365InteractiveAuthenticator authenticator = new O365InteractiveAuthenticator();
			authenticator.setUsername("");
			authenticator.authenticate();

			try (HttpClientAdapter httpClientAdapter = new HttpClientAdapter(authenticator.getExchangeUri(), true)) {

				// switch to EWS url
				GetFolderMethod checkMethod = new GetFolderMethod(BaseShape.ID_ONLY,
						DistinguishedFolderId.getInstance(null, DistinguishedFolderId.Name.root), null);
				checkMethod.setHeader("Authorization", "Bearer " + authenticator.getToken().getAccessToken());
				try (CloseableHttpResponse response = httpClientAdapter.execute(checkMethod)) {
					checkMethod.handleResponse(response);
					checkMethod.checkSuccess();
				}
				log.info("Retrieved folder id " + checkMethod.getResponseItem().get("FolderId"));

				// loop to check expiration
				int i = 0;
				while (i++ < 12 * 60 * 2) {
					GetUserConfigurationMethod getUserConfigurationMethod = new GetUserConfigurationMethod();
					getUserConfigurationMethod.setHeader("Authorization",
							"Bearer " + authenticator.getToken().getAccessToken());
					try (CloseableHttpResponse response = httpClientAdapter.execute(getUserConfigurationMethod)) {
						getUserConfigurationMethod.handleResponse(response);
						getUserConfigurationMethod.checkSuccess();
					}
					log.info("{}", getUserConfigurationMethod.getResponseItem());

					Thread.sleep(5000);
				}
			}
		}
		catch (InterruptedException e) {
			log.warn("Thread interrupted", e);
			Thread.currentThread().interrupt();
		}
		catch (Exception e) {
			log.error(e + " " + e.getMessage(), e);
		}
		System.exit(0);
	}

}
