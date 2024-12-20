/*
DIT
 */

package ru.mos.mostech.ews.exchange.auth;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.exception.MosTechEwsAuthenticationException;
import ru.mos.mostech.ews.exception.MosTechEwsException;
import ru.mos.mostech.ews.exchange.ews.BaseShape;
import ru.mos.mostech.ews.exchange.ews.DistinguishedFolderId;
import ru.mos.mostech.ews.exchange.ews.GetFolderMethod;
import ru.mos.mostech.ews.exchange.ews.GetUserConfigurationMethod;
import ru.mos.mostech.ews.http.HttpClientAdapter;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;

@Slf4j
public class O365ManualAuthenticator implements ExchangeAuthenticator {

	String errorCode = null;

	String code = null;

	URI ewsUrl = URI.create(Settings.getO365Url());

	private O365ManualAuthenticatorDialog o365ManualAuthenticatorDialog;

	@Getter
	@Setter
	private String username;

	@Setter
	private String password;

	@Getter
	private O365Token token;

	@Override
	public URI getExchangeUri() {
		return ewsUrl;
	}

	/**
	 * Вернуть экземпляр HttpClientAdapter с включенным пулом для доступа к O365
	 * @return HttpClientAdapter экземпляр
	 */
	@Override
	public HttpClientAdapter getHttpClientAdapter() {
		return new HttpClientAdapter(getExchangeUri(), username, password, true);
	}

	public void authenticate() throws IOException {
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
			return;
		}

		final String initUrl = O365Authenticator.buildAuthorizeUrl(tenantId, clientId, redirectUri, username);

		if (Settings.getBooleanProperty("mt.ews.server") || GraphicsEnvironment.isHeadless()) {
			// command line mode
			code = getCodeFromConsole(initUrl);
		}
		else {
			try {
				SwingUtilities
					.invokeAndWait(() -> o365ManualAuthenticatorDialog = new O365ManualAuthenticatorDialog(initUrl));
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			catch (InvocationTargetException e) {
				throw new IOException(e);
			}
			code = o365ManualAuthenticatorDialog.getCode();
		}

		if (code == null) {
			log.error("Authentication failed, code not available");
			throw new MosTechEwsException("EXCEPTION_AUTHENTICATION_FAILED_REASON", errorCode);
		}

		token = O365Token.build(tenantId, clientId, redirectUri, code, password);

		log.debug("Authenticated username: " + token.getUsername());
		if (username != null && !username.isEmpty() && !username.equalsIgnoreCase(token.getUsername())) {
			throw new MosTechEwsAuthenticationException(
					"Authenticated username " + token.getUsername() + " does not match " + username);
		}

	}

	private String getCodeFromConsole(String initUrl) {
		BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
		StringBuilder buffer = new StringBuilder();
		buffer.append(BundleMessage.format("UI_0365_AUTHENTICATION_PROMPT_CONSOLE", initUrl))
			.append("\n")
			.append(BundleMessage.format("UI_0365_AUTHENTICATION_CODE"));
		try {
			log.info(buffer.toString());
			code = inReader.readLine();
			if (code != null && code.contains("code=") && code.contains("&session_state=")) {
				code = code.substring(code.indexOf("code=") + 5, code.indexOf("&session_state="));
			}
		}
		catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		return code;
	}

	public static void main(String[] argv) {
		try {
			Settings.setDefaultSettings();
			Settings.setProperty("mt.ews.server", "false");

			O365ManualAuthenticator authenticator = new O365ManualAuthenticator();
			authenticator.setUsername("");
			authenticator.authenticate();

			// switch to EWS url
			HttpClientAdapter httpClientAdapter = new HttpClientAdapter(authenticator.getExchangeUri(), true);

			GetFolderMethod checkMethod = new GetFolderMethod(BaseShape.ID_ONLY,
					DistinguishedFolderId.getInstance(null, DistinguishedFolderId.Name.root), null);
			checkMethod.setHeader("Authorization", "Bearer " + authenticator.getToken().getAccessToken());
			try (CloseableHttpResponse response = httpClientAdapter.execute(checkMethod)) {
				checkMethod.handleResponse(response);
				checkMethod.checkSuccess();
			}
			log.info("Retrieved folder id {}", checkMethod.getResponseItem().get("FolderId"));

			// loop to check expiration
			int i = 0;
			while (i++ < 12 * 60 * 2) {
				GetUserConfigurationMethod getUserConfigurationMethod = new GetUserConfigurationMethod();
				getUserConfigurationMethod.setHeader("Authorization",
						"Bearer " + authenticator.getToken().getAccessToken());
				try (CloseableHttpResponse response = httpClientAdapter.execute(checkMethod)) {
					checkMethod.handleResponse(response);

					checkMethod.checkSuccess();
				}
				log.info(getUserConfigurationMethod.getResponseItem().toString());

				Thread.sleep(5000);
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
