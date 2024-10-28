/*
DIT
 */

package ru.mos.mostech.ews.exchange.auth;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.htmlcleaner.*;
import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.exception.MosTechEwsAuthenticationException;
import ru.mos.mostech.ews.exception.MosTechEwsException;
import ru.mos.mostech.ews.exception.WebdavNotAvailableException;
import ru.mos.mostech.ews.http.HttpClientAdapter;
import ru.mos.mostech.ews.http.MosTechEwsOTPPrompt;
import ru.mos.mostech.ews.http.URIUtil;
import ru.mos.mostech.ews.http.request.GetRequest;
import ru.mos.mostech.ews.http.request.PostRequest;
import ru.mos.mostech.ews.http.request.ResponseWrapper;
import ru.mos.mostech.ews.util.StringUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Новый форма аутентификации обмена на основе HttpClient 4.
 */
@Slf4j
public class ExchangeFormAuthenticator implements ExchangeAuthenticator {

	/**
	 * Различные поля имени пользователя, найденные на пользовательских формах
	 * аутентификации Exchange
	 */
	protected static final Set<String> USER_NAME_FIELDS = new HashSet<>();

	static {
		USER_NAME_FIELDS.add("username");
		USER_NAME_FIELDS.add("txtusername");
		USER_NAME_FIELDS.add("userid");
		USER_NAME_FIELDS.add("SafeWordUser");
		USER_NAME_FIELDS.add("user_name");
		USER_NAME_FIELDS.add("login");
		USER_NAME_FIELDS.add("UserName");
	}

	/**
	 * Различные поля паролей, найденные на пользовательских формах аутентификации
	 * Exchange
	 */
	protected static final Set<String> PASSWORD_FIELDS = new HashSet<>();

	static {
		PASSWORD_FIELDS.add("password");
		PASSWORD_FIELDS.add("txtUserPass");
		PASSWORD_FIELDS.add("pw");
		PASSWORD_FIELDS.add("basicPassword");
		PASSWORD_FIELDS.add("passwd");
		PASSWORD_FIELDS.add("Password");
	}

	/**
	 * Различные поля OTP (однократный пароль), найденные на пользовательских формах
	 * аутентификации Exchange. Используются для открытия диалога OTP
	 */
	protected static final Set<String> TOKEN_FIELDS = new HashSet<>();

	static {
		TOKEN_FIELDS.add("SafeWordPassword");
		TOKEN_FIELDS.add("passcode");
	}

	/**
	 * Указанное пользователем имя пользователя. Старая синтаксис преаутентификации:
	 * preauthusername"username аутентификация Windows с доменом: domain\\username
	 * Обратите внимание, что OSX Mail.app не поддерживает обратную косую черту в имени
	 * пользователя, вместо этого установите домен по умолчанию в настройках MT-EWS
	 */
	private String username;

	/**
	 * Предоставленный пользователем пароль
	 */
	private String password;

	/**
	 * URL OWA или EWS
	 */
	private String url;

	/**
	 * Адаптер HttpClient 4
	 */
	private HttpClientAdapter httpClientAdapter;

	/**
	 * Страница предварительной аутентификации OTP может требовать другого имени
	 * пользователя.
	 */
	private String preAuthusername;

	/**
	 * Поля имени пользователя формы входа.
	 */
	private final List<String> usernameInputs = new ArrayList<>();

	/**
	 * Поле пароля формы входа, по умолчанию - пароль.
	 */
	private String passwordInput = null;

	/**
	 * Указывает, была ли найдена страница предварительной авторизации OTP во время
	 * навигации на вход.
	 */
	private boolean otpPreAuthFound = false;

	/**
	 * Позволяет пользователю несколько раз попытаться ввести ключ предварительной
	 * аутентификации OTP перед тем, как сдаться.
	 */
	private int otpPreAuthRetries = 0;

	/**
	 * Максимальное количество попыток, которые пользователь может сделать для ввода ключа
	 * OTP перед тем, как сдаться.
	 */
	private static final int MAX_OTP_RETRIES = 3;

	/**
	 * базовый URI обмена после аутентификации
	 */
	private java.net.URI exchangeUri;

	@Override
	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	public void setPassword(String password) {
		this.password = password;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public void authenticate() throws MosTechEwsException {
		try {
			// create HttpClient adapter, enable pooling as this instance will be passed
			// to ExchangeSession
			httpClientAdapter = new HttpClientAdapter(url, true);
			boolean isHttpAuthentication = isHttpAuthentication(httpClientAdapter, url);

			// The user may have configured an OTP pre-auth username. It is processed
			// so early because OTP pre-auth may disappear in the Exchange LAN and this
			// helps the user to not change is account settings in mail client at each
			// network change.
			if (preAuthusername == null) {
				// Searches for the delimiter in configured username for the pre-auth
				// user.
				// The double-quote is not allowed inside email addresses anyway.
				int doubleQuoteIndex = this.username.indexOf('"');
				if (doubleQuoteIndex > 0) {
					preAuthusername = this.username.substring(0, doubleQuoteIndex);
					this.username = this.username.substring(doubleQuoteIndex + 1);
				}
				else {
					// No doublequote: the pre-auth user is the full username, or it is
					// not used at all.
					preAuthusername = this.username;
				}
			}

			// set real credentials on http client
			httpClientAdapter.setCredentials(username, password);

			// get webmail root url
			// providing credentials
			// manually follow redirect
			GetRequest getRequest = httpClientAdapter.executeFollowRedirect(new GetRequest(url));

			if (!this.isAuthenticated(getRequest)) {
				if (isHttpAuthentication) {
					int status = getRequest.getStatusCode();

					if (status == HttpStatus.SC_UNAUTHORIZED) {
						throw new MosTechEwsAuthenticationException("EXCEPTION_AUTHENTICATION_FAILED");
					}
					else if (status != HttpStatus.SC_OK) {
						throw HttpClientAdapter.buildHttpResponseException(getRequest, getRequest.getHttpResponse());
					}
					// workaround for basic authentication on /exchange and form based
					// authentication at /owa
					if ("/owa/auth/logon.aspx".equals(getRequest.getURI().getPath())) {
						formLogin(httpClientAdapter, getRequest, password);
					}
				}
				else {
					formLogin(httpClientAdapter, getRequest, password);
				}
			}

		}
		catch (MosTechEwsAuthenticationException exc) {
			close();
			log.error(exc.getMessage());
			throw exc;
		}
		catch (ConnectException | UnknownHostException exc) {
			close();
			BundleMessage message = new BundleMessage("EXCEPTION_CONNECT", exc.getClass().getName(), exc.getMessage());
			log.error("{}", message);
			throw new MosTechEwsException("EXCEPTION_MT-EWS_CONFIGURATION", message);
		}
		catch (WebdavNotAvailableException exc) {
			close();
			throw exc;
		}
		catch (IOException exc) {
			close();
			log.error(BundleMessage.formatLog("EXCEPTION_EXCHANGE_LOGIN_FAILED", exc));
			throw new MosTechEwsException("EXCEPTION_EXCHANGE_LOGIN_FAILED", exc);
		}
		log.debug("Successfully authenticated to " + exchangeUri);
	}

	/**
	 * Тест режима аутентификации: на основе формы или базовой.
	 * @param url базовый URL обмена
	 * @param httpClient экземпляр httpClientAdapter
	 * @return true, если обнаружена базовая аутентификация
	 */
	protected boolean isHttpAuthentication(HttpClientAdapter httpClient, String url) {
		boolean isHttpAuthentication = false;
		HttpGet httpGet = new HttpGet(url);
		// Create a local context to avoid cookies in main httpClient
		HttpClientContext context = HttpClientContext.create();
		context.setCookieStore(new BasicCookieStore());
		try (CloseableHttpResponse response = httpClient.execute(httpGet, context)) {
			isHttpAuthentication = response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED;
		}
		catch (IOException e) {
			// ignore
		}
		return isHttpAuthentication;
	}

	/**
	 * Ищет куки сессии.
	 * @return true, если куки сессии доступны
	 */
	protected boolean isAuthenticated(ResponseWrapper getRequest) {
		boolean authenticated = false;
		if (getRequest.getStatusCode() == HttpStatus.SC_OK
				&& "/ews/services.wsdl".equalsIgnoreCase(getRequest.getURI().getPath())) {
			// direct EWS access returned wsdl
			authenticated = true;
		}
		else {
			// check cookies
			for (Cookie cookie : httpClientAdapter.getCookies()) {
				// Exchange 2003 cookies
				if (cookie.getName().startsWith("cadata") || "sessionid".equals(cookie.getName())
				// Exchange 2007 cookie
						|| "UserContext".equals(cookie.getName())
						// Federated Authentication
						|| "TimeWindowSig".equals(cookie.getName())) {
					authenticated = true;
					break;
				}
			}
		}
		return authenticated;
	}

	protected void formLogin(HttpClientAdapter httpClient, ResponseWrapper initRequest, String password)
			throws IOException {
		log.debug("Form based authentication detected");

		PostRequest postRequest = buildLogonMethod(httpClient, initRequest);
		if (postRequest == null) {
			log.debug("Authentication form not found at " + initRequest.getURI() + ", trying default url");
			postRequest = new PostRequest("/owa/auth/owaauth.dll");
		}

		exchangeUri = postLogonMethod(httpClient, postRequest, password).getURI();
	}

	/**
	 * Попробуйте найти путь метода входа из тела формы входа.
	 * @param httpClient экземпляр httpClientAdapter
	 * @param responseWrapper инициализировать обертку ответа запроса
	 * @return метод входа
	 */
	protected PostRequest buildLogonMethod(HttpClientAdapter httpClient, ResponseWrapper responseWrapper) {
		PostRequest logonMethod = null;

		// create an instance of HtmlCleaner
		HtmlCleaner cleaner = new HtmlCleaner();
		// In the federated auth flow, an input field may contain a saml xml assertion
		// with > characters
		cleaner.getProperties().setAllowHtmlInsideAttributes(true);

		// A OTP token authentication form in a previous page could have username fields
		// with different names
		usernameInputs.clear();

		try {
			URI uri = responseWrapper.getURI();
			String responseBody = responseWrapper.getResponseBodyAsString();
			TagNode node = cleaner.clean(new ByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8)));
			List<? extends TagNode> forms = node.getElementListByName("form", true);
			TagNode logonForm = null;
			// select form
			if (forms.size() == 1) {
				logonForm = forms.get(0);
			}
			else if (forms.size() > 1) {
				for (TagNode form : forms) {
					if ("logonForm".equals(form.getAttributeByName("name"))) {
						logonForm = form;
					}
					else if ("loginForm".equals(form.getAttributeByName("id"))) {
						logonForm = form;
					}

				}
			}
			if (logonForm != null) {
				String logonMethodPath = logonForm.getAttributeByName("action");

				// workaround for broken form with empty action
				if (logonMethodPath != null && logonMethodPath.isEmpty()) {
					logonMethodPath = "/owa/auth.owa";
				}

				logonMethod = new PostRequest(getAbsoluteUri(uri, logonMethodPath));

				// retrieve lost inputs attached to body
				List<? extends TagNode> inputList = node.getElementListByName("input", true);

				for (TagNode input : inputList) {
					String type = input.getAttributeByName("type");
					String name = input.getAttributeByName("name");
					String value = input.getAttributeByName("value");
					if ("hidden".equalsIgnoreCase(type) && name != null && value != null) {
						// decode XML SAML assertion correctly from hidden field value
						if ("wresult".equals(name)) {
							String decoded = value.replaceAll("&quot;", "\"").replaceAll("&lt;", "<");
							logonMethod.setParameter(name, decoded);
							// The OWA accepting this assertion needs the Referer set, but
							// it can be anything
							logonMethod.setRequestHeader("Referer", url);
						}
						else if ("wctx".equals(name)) {
							String decoded = value.replaceAll("&amp;", "&");
							logonMethod.setParameter(name, decoded);
						}
						else {
							logonMethod.setParameter(name, value);
						}
					}
					// custom login form
					if (USER_NAME_FIELDS.contains(name) && !usernameInputs.contains(name)) {
						usernameInputs.add(name);
					}
					else if (PASSWORD_FIELDS.contains(name)) {
						passwordInput = name;
					}
					else if ("addr".equals(name)) {
						// this is not a logon form but a redirect form
						logonMethod = buildLogonMethod(httpClient, httpClient.executeFollowRedirect(logonMethod));
					}
					else if (TOKEN_FIELDS.contains(name)) {
						// one time password, ask it to the user
						logonMethod.setParameter(name, MosTechEwsOTPPrompt.getOneTimePassword());
					}
					else if ("otc".equals(name)) {
						// captcha image, get image and ask user
						String pinsafeUser = getAliasFromLogin();
						if (pinsafeUser == null) {
							pinsafeUser = username;
						}
						HttpGet pinRequest = new HttpGet("/PINsafeISAFilter.dll?username=" + pinsafeUser);
						try (CloseableHttpResponse pinResponse = httpClient.execute(pinRequest)) {
							int status = pinResponse.getStatusLine().getStatusCode();
							if (status != HttpStatus.SC_OK) {
								throw HttpClientAdapter.buildHttpResponseException(pinRequest,
										pinResponse.getStatusLine());
							}
							BufferedImage captchaImage = ImageIO.read(pinResponse.getEntity().getContent());
							logonMethod.setParameter(name, MosTechEwsOTPPrompt.getCaptchaValue(captchaImage));
						}
					}
				}
			}
			else {
				List<? extends TagNode> frameList = node.getElementListByName("frame", true);
				if (frameList.size() == 1) {
					String src = frameList.get(0).getAttributeByName("src");
					if (src != null) {
						log.debug("Frames detected in form page, try frame content");
						logonMethod = buildLogonMethod(httpClient,
								httpClient.executeFollowRedirect(new GetRequest(src)));
					}
				}
				else {
					// another failover for script based logon forms (Exchange 2007)
					List<? extends TagNode> scriptList = node.getElementListByName("script", true);
					for (TagNode script : scriptList) {
						List<? extends BaseToken> contents = script.getAllChildren();
						for (Object content : contents) {
							if (content instanceof CommentNode) {
								String scriptValue = ((CommentNode) content).getCommentedContent();
								String sUrl = StringUtil.getToken(scriptValue, "var a_sUrl = \"", "\"");
								String sLgn = StringUtil.getToken(scriptValue, "var a_sLgnQS = \"", "\"");
								if (sLgn == null) {
									sLgn = StringUtil.getToken(scriptValue, "var a_sLgn = \"", "\"");
								}
								if (sUrl != null && sLgn != null) {
									URI src = getScriptBasedFormURL(uri, sLgn + sUrl);
									log.debug("Detected script based logon, redirect to form at " + src);
									logonMethod = buildLogonMethod(httpClient,
											httpClient.executeFollowRedirect(new GetRequest(src)));
								}

							}
							else if (content instanceof ContentNode) {
								// Microsoft Forefront Unified Access Gateway redirect
								String scriptValue = ((ContentNode) content).getContent();
								String location = StringUtil.getToken(scriptValue, "window.location.replace(\"", "\"");
								if (location != null) {
									log.debug("Post logon redirect to: " + location);
									logonMethod = buildLogonMethod(httpClient,
											httpClient.executeFollowRedirect(new GetRequest(location)));
								}
							}
						}
					}
				}
			}
		}
		catch (IOException | URISyntaxException e) {
			log.error("Error parsing login form at " + responseWrapper.getURI());
		}

		return logonMethod;
	}

	protected ResponseWrapper postLogonMethod(HttpClientAdapter httpClient, PostRequest logonMethod, String password)
			throws IOException {

		setAuthFormFields(logonMethod, httpClient, password);

		// add exchange 2010 PBack cookie in compatibility mode
		BasicClientCookie pBackCookie = new BasicClientCookie("PBack", "0");
		pBackCookie.setPath("/");
		pBackCookie.setDomain(httpClientAdapter.getHost());
		httpClient.addCookie(pBackCookie);

		ResponseWrapper resultRequest = httpClient.executeFollowRedirect(logonMethod);

		// test form based authentication
		checkFormLoginQueryString(resultRequest);

		// workaround for post logon script redirect
		if (!isAuthenticated(resultRequest)) {
			// try to get new method from script based redirection
			logonMethod = buildLogonMethod(httpClient, resultRequest);

			if (logonMethod != null) {
				if (otpPreAuthFound && otpPreAuthRetries < MAX_OTP_RETRIES) {
					// A OTP pre-auth page has been found, it is needed to restart the
					// login process.
					// This applies to both the case the user entered a good OTP code (the
					// usual login process
					// takes place) and the case the user entered a wrong OTP code
					// (another code will be asked to him).
					// The user has up to MAX_OTP_RETRIES chances to input a valid OTP
					// key.
					return postLogonMethod(httpClient, logonMethod, password);
				}

				// if logonMethod is not null, try to follow redirection
				resultRequest = httpClient.executeFollowRedirect(logonMethod);

				checkFormLoginQueryString(resultRequest);
				// also check cookies
				if (!isAuthenticated(resultRequest)) {
					throwAuthenticationFailed();
				}
			}
			else {
				// authentication failed
				throwAuthenticationFailed();
			}
		}

		// check for language selection form
		if ("/owa/languageselection.aspx".equals(resultRequest.getURI().getPath())) {
			// need to submit form
			resultRequest = submitLanguageSelectionForm(resultRequest.getURI(),
					resultRequest.getResponseBodyAsString());
		}
		return resultRequest;
	}

	protected ResponseWrapper submitLanguageSelectionForm(URI uri, String responseBodyAsString) throws IOException {
		PostRequest postLanguageFormMethod;
		// create an instance of HtmlCleaner
		HtmlCleaner cleaner = new HtmlCleaner();

		try {
			TagNode node = cleaner.clean(responseBodyAsString);
			List<? extends TagNode> forms = node.getElementListByName("form", true);
			TagNode languageForm;
			// select form
			if (forms.size() == 1) {
				languageForm = forms.get(0);
			}
			else {
				throw new IOException("Form not found");
			}
			String languageMethodPath = languageForm.getAttributeByName("action");

			postLanguageFormMethod = new PostRequest(getAbsoluteUri(uri, languageMethodPath));

			List<? extends TagNode> inputList = languageForm.getElementListByName("input", true);
			for (TagNode input : inputList) {
				String name = input.getAttributeByName("name");
				String value = input.getAttributeByName("value");
				if (name != null && value != null) {
					postLanguageFormMethod.setParameter(name, value);
				}
			}
			List<? extends TagNode> selectList = languageForm.getElementListByName("select", true);
			for (TagNode select : selectList) {
				String name = select.getAttributeByName("name");
				List<? extends TagNode> optionList = select.getElementListByName("option", true);
				String value = null;
				for (TagNode option : optionList) {
					if (option.getAttributeByName("selected") != null) {
						value = option.getAttributeByName("value");
						break;
					}
				}
				if (name != null && value != null) {
					postLanguageFormMethod.setParameter(name, value);
				}
			}
		}
		catch (IOException | URISyntaxException e) {
			String errorMessage = "Error parsing language selection form at " + uri;
			log.error(errorMessage);
			throw new IOException(errorMessage);
		}

		return httpClientAdapter.executeFollowRedirect(postLanguageFormMethod);
	}

	protected void setAuthFormFields(HttpRequestBase logonMethod, HttpClientAdapter httpClient, String password)
			throws IllegalArgumentException {
		String usernameInput;
		if (usernameInputs.size() == 2) {
			String userid;
			// multiple username fields, split userid|username on |
			int pipeIndex = username.indexOf('|');
			if (pipeIndex < 0) {
				log.debug(
						"Multiple user fields detected, please use userid|username as user name in client, except when userid is username");
				userid = username;
			}
			else {
				userid = username.substring(0, pipeIndex);
				username = username.substring(pipeIndex + 1);
				// adjust credentials
				httpClient.setCredentials(username, password);
			}
			((PostRequest) logonMethod).removeParameter("userid");
			((PostRequest) logonMethod).setParameter("userid", userid);

			usernameInput = "username";
		}
		else if (usernameInputs.size() == 1) {
			// simple username field
			usernameInput = usernameInputs.get(0);
		}
		else {
			// should not happen
			usernameInput = "username";
		}
		// make sure username and password fields are empty
		((PostRequest) logonMethod).removeParameter(usernameInput);
		if (passwordInput != null) {
			((PostRequest) logonMethod).removeParameter(passwordInput);
		}
		((PostRequest) logonMethod).removeParameter("trusted");
		((PostRequest) logonMethod).removeParameter("flags");

		if (passwordInput == null) {
			// This is a OTP pre-auth page. A different username may be required.
			otpPreAuthFound = true;
			otpPreAuthRetries++;
			((PostRequest) logonMethod).setParameter(usernameInput, preAuthusername);
		}
		else {
			otpPreAuthFound = false;
			otpPreAuthRetries = 0;
			// This is a regular Exchange login page
			((PostRequest) logonMethod).setParameter(usernameInput, username);
			((PostRequest) logonMethod).setParameter(passwordInput, password);
			((PostRequest) logonMethod).setParameter("trusted", "4");
			((PostRequest) logonMethod).setParameter("flags", "4");
		}
	}

	protected URI getAbsoluteUri(URI uri, String path) throws URISyntaxException {
		URIBuilder uriBuilder = new URIBuilder(uri);
		if (path != null) {
			// reset query string
			uriBuilder.clearParameters();
			if (path.startsWith("/")) {
				// path is absolute, replace method path
				uriBuilder.setPath(path);
			}
			else if (path.startsWith("http://") || path.startsWith("https://")) {
				return URI.create(path);
			}
			else {
				// relative path, build new path
				String currentPath = uri.getPath();
				int end = currentPath.lastIndexOf('/');
				if (end >= 0) {
					uriBuilder.setPath(currentPath.substring(0, end + 1) + path);
				}
				else {
					throw new URISyntaxException(uriBuilder.build().toString(), "Invalid path");
				}
			}
		}
		return uriBuilder.build();
	}

	protected URI getScriptBasedFormURL(URI uri, String pathQuery) throws URISyntaxException, IOException {
		URIBuilder uriBuilder = new URIBuilder(uri);
		int queryIndex = pathQuery.indexOf('?');
		if (queryIndex >= 0) {
			if (queryIndex > 0) {
				// update path
				String newPath = pathQuery.substring(0, queryIndex);
				if (newPath.startsWith("/")) {
					// absolute path
					uriBuilder.setPath(newPath);
				}
				else {
					String currentPath = uriBuilder.getPath();
					int folderIndex = currentPath.lastIndexOf('/');
					if (folderIndex >= 0) {
						// replace relative path
						uriBuilder.setPath(currentPath.substring(0, folderIndex + 1) + newPath);
					}
					else {
						// should not happen
						uriBuilder.setPath('/' + newPath);
					}
				}
			}
			uriBuilder.setCustomQuery(URIUtil.decode(pathQuery.substring(queryIndex + 1)));
		}
		return uriBuilder.build();
	}

	protected void checkFormLoginQueryString(ResponseWrapper logonMethod) throws MosTechEwsAuthenticationException {
		String queryString = logonMethod.getURI().getRawQuery();
		if (queryString != null && (queryString.contains("reason=2") || queryString.contains("reason=4"))) {
			throwAuthenticationFailed();
		}
	}

	protected void throwAuthenticationFailed() throws MosTechEwsAuthenticationException {
		if (this.username != null && this.username.contains("\\")) {
			throw new MosTechEwsAuthenticationException("EXCEPTION_AUTHENTICATION_FAILED");
		}
		else {
			throw new MosTechEwsAuthenticationException("EXCEPTION_AUTHENTICATION_FAILED_RETRY");
		}
	}

	/**
	 * Получить текущее имя алиаса обмена из имени пользователя
	 * @return имя пользователя
	 */
	public String getAliasFromLogin() {
		// login is email, not alias
		if (this.username.indexOf('@') >= 0) {
			return null;
		}
		String result = this.username;
		// remove domain name
		int index = Math.max(result.indexOf('\\'), result.indexOf('/'));
		if (index >= 0) {
			result = result.substring(index + 1);
		}
		return result;
	}

	/**
	 * Закрыть сессию. Завершить работу менеджера соединений HTTP-клиента
	 */
	public void close() {
		httpClientAdapter.close();
	}

	/**
	 * Oauth токен. Только для аутентификаторов Office 365
	 * @return неподдерживаемый
	 */
	@Override
	public O365Token getToken() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Базовый URL обмена. Страница приветствия для Exchange 2003, URL EWS для Exchange
	 * 2007 и более поздних версий
	 * @return URL обмена
	 */
	@Override
	public java.net.URI getExchangeUri() {
		return exchangeUri;
	}

	/**
	 * Вернуть аутентифицированный HttpClient 4 HttpClientAdapter
	 * @return экземпляр HttpClientAdapter
	 */
	public HttpClientAdapter getHttpClientAdapter() {
		return httpClientAdapter;
	}

	/**
	 * Реальное имя пользователя. может отличаться от введенного имени пользователя с
	 * предварительной авторизацией
	 * @return имя пользователя
	 */
	public String getUsername() {
		return username;
	}

}
