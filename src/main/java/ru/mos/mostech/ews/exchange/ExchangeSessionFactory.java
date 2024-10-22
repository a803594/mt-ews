/*
DIT
 */
package ru.mos.mostech.ews.exchange;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.exception.MosTechEwsAuthenticationException;
import ru.mos.mostech.ews.exception.MosTechEwsException;
import ru.mos.mostech.ews.exception.WebdavNotAvailableException;
import ru.mos.mostech.ews.exchange.auth.ExchangeAuthenticator;
import ru.mos.mostech.ews.exchange.auth.ExchangeFormAuthenticator;
import ru.mos.mostech.ews.exchange.dav.MosTechEwsExchangeSession;
import ru.mos.mostech.ews.exchange.ews.EwsExchangeSession;
import ru.mos.mostech.ews.http.HttpClientAdapter;
import ru.mos.mostech.ews.http.request.GetRequest;

import java.awt.*;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Create ExchangeSession instances.
 */
@Slf4j
public final class ExchangeSessionFactory {

	private static final Object LOCK = new Object();

	private static final Map<PoolKey, ExchangeSession> POOL_MAP = new HashMap<>();

	private static boolean configChecked;

	private static boolean errorSent;

	static class PoolKey {

		final String url;

		final String userName;

		final String password;

		PoolKey(String url, String userName, String password) {
			this.url = url;
			this.userName = convertUserName(userName);
			this.password = password;
		}

		@Override
		public boolean equals(Object object) {
			return object == this || object instanceof PoolKey && ((PoolKey) object).url.equals(this.url)
					&& ((PoolKey) object).userName.equals(this.userName)
					&& ((PoolKey) object).password.equals(this.password);
		}

		@Override
		public int hashCode() {
			return url.hashCode() + userName.hashCode() + password.hashCode();
		}

	}

	private ExchangeSessionFactory() {
	}

	/**
	 * Create authenticated Exchange session
	 * @param userName user login
	 * @param password user password
	 * @return authenticated session
	 * @throws IOException on error
	 */
	public static ExchangeSession getInstance(String userName, String password) throws IOException {
		String baseUrl = Settings.getProperty("mt.ews.url");
		if (Settings.getBooleanProperty("mt.ews.server")) {
			return getInstance(baseUrl, userName, password);
		}
		else {
			// serialize session creation in workstation mode to avoid multiple OTP
			// requests
			synchronized (LOCK) {
				return getInstance(baseUrl, userName, password);
			}
		}
	}

	private static String convertUserName(String userName) {
		String result = userName;
		// prepend default windows domain prefix
		String defaultDomain = Settings.getProperty("mt.ews.defaultDomain");
		if (defaultDomain != null && userName.indexOf('\\') < 0 && userName.indexOf('@') < 0) {
			result = defaultDomain + '\\' + userName;
		}
		return result;
	}

	/**
	 * Create authenticated Exchange session
	 * @param baseUrl OWA base URL
	 * @param userName user login
	 * @param password user password
	 * @return authenticated session
	 * @throws IOException on error
	 */
	public static ExchangeSession getInstance(String baseUrl, String userName, String password) throws IOException {
		ExchangeSession session = null;
		try {
			String mode = Settings.getProperty("mt.ews.mode");
			if (Settings.O365.equals(mode)) {
				// force url with O365
				baseUrl = Settings.O365_URL;
			}

			PoolKey poolKey = new PoolKey(baseUrl, userName, password);

			synchronized (LOCK) {
				session = POOL_MAP.get(poolKey);
			}
			if (session != null) {
				log.debug("Got session " + session + " from cache");
			}

			if (session != null && session.isExpired()) {
				synchronized (LOCK) {
					session.close();
					log.debug("Session " + session + " for user " + session.userName + " expired");
					session = null;
					// expired session, remove from cache
					POOL_MAP.remove(poolKey);
				}
			}

			if (session == null) {
				// convert old setting
				if (mode == null) {
					if ("false".equals(Settings.getProperty("mt.ews.enableEws"))) {
						mode = Settings.WEBDAV;
					}
					else {
						mode = Settings.EWS;
					}
				}
				// check for overridden authenticator
				String authenticatorClass = Settings.getProperty("mt.ews.authenticator");
				if (authenticatorClass == null) {
					switch (mode) {
						case Settings.O365_MODERN:
							authenticatorClass = "ru.mos.mostech.ews.exchange.auth.O365Authenticator";
							break;
						case Settings.O365_INTERACTIVE:
							authenticatorClass = "ru.mos.mostech.ews.exchange.auth.O365InteractiveAuthenticator";
							if (GraphicsEnvironment.isHeadless()) {
								throw new MosTechEwsException("EXCEPTION_MT-EWS_CONFIGURATION",
										"O365Interactive not supported in headless mode");
							}
							break;
						case Settings.O365_MANUAL:
							authenticatorClass = "ru.mos.mostech.ews.exchange.auth.O365ManualAuthenticator";
							break;
					}
				}

				if (authenticatorClass != null) {
					ExchangeAuthenticator authenticator = (ExchangeAuthenticator) Class.forName(authenticatorClass)
						.getDeclaredConstructor()
						.newInstance();
					authenticator.setUsername(poolKey.userName);
					authenticator.setPassword(poolKey.password);
					authenticator.authenticate();
					session = new EwsExchangeSession(authenticator.getExchangeUri(), authenticator.getToken(),
							poolKey.userName);

				}
				else if (Settings.EWS.equals(mode) || Settings.O365.equals(mode)
				// direct EWS even if mode is different
						|| poolKey.url.toLowerCase().endsWith("/ews/exchange.asmx")
						|| poolKey.url.toLowerCase().endsWith("/ews/services.wsdl")) {
					if (poolKey.url.toLowerCase().endsWith("/ews/exchange.asmx")
							|| poolKey.url.toLowerCase().endsWith("/ews/services.wsdl")) {
						log.debug("Direct EWS authentication");
						session = new EwsExchangeSession(poolKey.url, poolKey.userName, poolKey.password);
					}
					else {
						log.debug("OWA authentication in EWS mode");
						ExchangeFormAuthenticator exchangeFormAuthenticator = new ExchangeFormAuthenticator();
						exchangeFormAuthenticator.setUrl(poolKey.url);
						exchangeFormAuthenticator.setUsername(poolKey.userName);
						exchangeFormAuthenticator.setPassword(poolKey.password);
						exchangeFormAuthenticator.authenticate();
						session = new EwsExchangeSession(exchangeFormAuthenticator.getHttpClientAdapter(),
								exchangeFormAuthenticator.getExchangeUri(), exchangeFormAuthenticator.getUsername());
					}
				}
				else {
					ExchangeFormAuthenticator exchangeFormAuthenticator = new ExchangeFormAuthenticator();
					exchangeFormAuthenticator.setUrl(poolKey.url);
					exchangeFormAuthenticator.setUsername(poolKey.userName);
					exchangeFormAuthenticator.setPassword(poolKey.password);
					exchangeFormAuthenticator.authenticate();
					try {
						session = new MosTechEwsExchangeSession(exchangeFormAuthenticator.getHttpClientAdapter(),
								exchangeFormAuthenticator.getExchangeUri(), exchangeFormAuthenticator.getUsername());
					}
					catch (WebdavNotAvailableException e) {
						if (Settings.AUTO.equals(mode)) {
							log.debug(e.getMessage() + ", retry with EWS");
							session = new EwsExchangeSession(poolKey.url, poolKey.userName, poolKey.password);
						}
						else {
							throw e;
						}
					}
				}
				checkWhiteList(session.getEmail());
				log.debug("Created new session " + session + " for user " + poolKey.userName);
			}
			// successful login, put session in cache
			synchronized (LOCK) {
				POOL_MAP.put(poolKey, session);
			}
			// session opened, future failure will mean network down
			configChecked = true;
			// Reset so next time an problem occurs message will be sent once
			errorSent = false;
		}
		catch (MosTechEwsException | IllegalStateException | NullPointerException exc) {
			throw exc;
		}
		catch (Exception exc) {
			handleNetworkDown(exc);
		}
		return session;
	}

	/**
	 * Check if whitelist is empty or email is allowed. userWhiteList is a comma separated
	 * list of values. \@company.com means all domain users are allowed
	 * @param email user email
	 */
	private static void checkWhiteList(String email) throws MosTechEwsAuthenticationException {
		String whiteListString = Settings.getProperty("mt.ews.userWhiteList");
		if (whiteListString != null && !whiteListString.isEmpty()) {
			for (String whiteListvalue : whiteListString.split(",")) {
				if (whiteListvalue.startsWith("@") && email.endsWith(whiteListvalue)) {
					return;
				}
				else if (email.equalsIgnoreCase(whiteListvalue)) {
					return;
				}
			}
			log.warn(email + " not allowed by whitelist");
			throw new MosTechEwsAuthenticationException("EXCEPTION_AUTHENTICATION_FAILED");
		}
	}

	/**
	 * Get a non expired session. If the current session is not expired, return current
	 * session, else try to create a new session
	 * @param currentSession current session
	 * @param userName user login
	 * @param password user password
	 * @return authenticated session
	 * @throws IOException on error
	 */
	public static ExchangeSession getInstance(ExchangeSession currentSession, String userName, String password)
			throws IOException {
		ExchangeSession session = currentSession;
		try {
			if (session.isExpired()) {
				log.debug("Session " + session + " expired, trying to open a new one");
				session = null;
				String baseUrl = Settings.getProperty("mt.ews.url");
				PoolKey poolKey = new PoolKey(baseUrl, userName, password);
				// expired session, remove from cache
				synchronized (LOCK) {
					POOL_MAP.remove(poolKey);
				}
				session = getInstance(userName, password);
			}
		}
		catch (MosTechEwsAuthenticationException exc) {
			log.debug("Unable to reopen session", exc);
			throw exc;
		}
		catch (Exception exc) {
			log.debug("Unable to reopen session", exc);
			handleNetworkDown(exc);
		}
		return session;
	}

	/**
	 * Send a request to Exchange server to check current settings.
	 * @throws IOException if unable to access Exchange server
	 */
	public static void checkConfig() throws IOException {
		String url = Settings.getProperty("mt.ews.url");
		if (url == null || (!url.startsWith("http://") && !url.startsWith("https://"))) {
			throw new MosTechEwsException("LOG_INVALID_URL", url);
		}
		try (HttpClientAdapter httpClientAdapter = new HttpClientAdapter(url);
				CloseableHttpResponse response = httpClientAdapter.execute(new GetRequest(url))) {
			// get webMail root url (will not follow redirects)
			int status = response.getStatusLine().getStatusCode();
			log.debug("Test configuration status: " + status);
			if (status != HttpStatus.SC_OK && status != HttpStatus.SC_UNAUTHORIZED
					&& !HttpClientAdapter.isRedirect(status)) {
				throw new MosTechEwsException("EXCEPTION_CONNECTION_FAILED", url, status);
			}
			// session opened, future failure will mean network down
			configChecked = true;
			// Reset so next time an problem occurs message will be sent once
			errorSent = false;
		}
		catch (Exception exc) {
			handleNetworkDown(exc);
		}

	}

	private static void handleNetworkDown(Exception exc) throws MosTechEwsException {
		if (!checkNetwork() || configChecked) {
			log.warn(BundleMessage.formatLog("EXCEPTION_NETWORK_DOWN"));
			// log full stack trace for unknown errors
			if (!((exc instanceof UnknownHostException) || (exc instanceof NetworkDownException))) {
				log.debug("{}", exc, exc);
			}
			throw new NetworkDownException("EXCEPTION_NETWORK_DOWN");
		}
		else {
			BundleMessage message = new BundleMessage("EXCEPTION_CONNECT", exc.getClass().getName(), exc.getMessage());
			if (errorSent) {
				log.warn("{}", message);
				throw new NetworkDownException("EXCEPTION_MT-EWS_CONFIGURATION", message);
			}
			else {
				// Mark that an error has been sent so you only get one
				// error in a row (not a repeating string of errors).
				errorSent = true;
				log.error("{}", message);
				throw new MosTechEwsException("EXCEPTION_MT-EWS_CONFIGURATION", message);
			}
		}
	}

	/**
	 * Get user password from session pool for SASL authentication
	 * @param userName Exchange user name
	 * @return user password
	 */
	public static String getUserPassword(String userName) {
		String fullUserName = convertUserName(userName);
		for (PoolKey poolKey : POOL_MAP.keySet()) {
			if (poolKey.userName.equals(fullUserName)) {
				return poolKey.password;
			}
		}
		return null;
	}

	/**
	 * Check if at least one network interface is up and active (i.e. has an address)
	 * @return true if network available
	 */
	static boolean checkNetwork() {
		boolean up = false;
		Enumeration<NetworkInterface> enumeration;
		try {
			enumeration = NetworkInterface.getNetworkInterfaces();
			if (enumeration != null) {
				while (!up && enumeration.hasMoreElements()) {
					NetworkInterface networkInterface = enumeration.nextElement();
					up = networkInterface.isUp() && !networkInterface.isLoopback()
							&& networkInterface.getInetAddresses().hasMoreElements();
				}
			}
		}
		catch (NoSuchMethodError error) {
			log.debug("Unable to test network interfaces (not available under Java 1.5)");
			up = true;
		}
		catch (SocketException exc) {
			log.error("MT-EWS configuration exception: \n Error listing network interfaces " + exc.getMessage(), exc);
		}
		return up;
	}

	/**
	 * Reset config check status and clear session pool.
	 */
	public static void shutdown() {
		configChecked = false;
		errorSent = false;
		synchronized (LOCK) {
			for (ExchangeSession session : POOL_MAP.values()) {
				session.close();
			}
			POOL_MAP.clear();
		}
	}

}
