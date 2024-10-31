/*
DIT
 */
package ru.mos.mostech.ews.http;

import lombok.extern.slf4j.Slf4j;
import org.ietf.jgss.*;
import ru.mos.mostech.ews.Settings;

import javax.security.auth.RefreshFailedException;
import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.PrivilegedAction;
import java.security.Security;

/**
 * Вспомогательный класс Kerberos.
 */
@Slf4j
public class KerberosHelper {

	protected static final Object LOCK = new Object();

	protected static final KerberosCallbackHandler KERBEROS_CALLBACK_HANDLER;

	private static LoginContext clientLoginContext;

	static {
		// Load Jaas configuration from class
		Security.setProperty("login.configuration.provider", "mt.ews.http.KerberosLoginConfiguration");
		// Kerberos callback handler singleton
		KERBEROS_CALLBACK_HANDLER = new KerberosCallbackHandler();
	}

	private KerberosHelper() {
	}

	@SuppressWarnings("UseOfSystemOutOrSystemErr")
	protected static class KerberosCallbackHandler implements CallbackHandler {

		String principal;

		String password;

		public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
			for (Callback callback : callbacks) {
				if (callback instanceof NameCallback nameCallback) {
					if (principal == null) {
						System.out.print(nameCallback.getPrompt());
						BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
						principal = inReader.readLine();
					}
					if (principal == null) {
						throw new IOException("KerberosCallbackHandler: failed to retrieve principal");
					}
					nameCallback.setName(principal);

				}
				else if (callback instanceof PasswordCallback passwordCallback) {
					if (password == null) {
						// if we get there kerberos token is missing or invalid
						if (Settings.getBooleanProperty("mt.ews.server") || GraphicsEnvironment.isHeadless()) {
							// headless or server mode
							System.out.print(passwordCallback.getPrompt());
							BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
							password = inReader.readLine();
						}
					}
					if (password == null) {
						throw new IOException("KerberosCallbackHandler: failed to retrieve password");
					}
					passwordCallback.setPassword(password.toCharArray());

				}
				else {
					throw new UnsupportedCallbackException(callback);
				}
			}
		}

	}

	/**
	 * Принудительный клиентский принципал в обработчике обратного вызова
	 * @param principal клиентский принципал
	 */
	public static void setClientPrincipal(String principal) {
		KERBEROS_CALLBACK_HANDLER.principal = principal;
	}

	/**
	 * Принудительно устанавливает пароль клиента в обработчике обратного вызова
	 * @param password пароль клиента
	 */
	public static void setClientPassword(String password) {
		KERBEROS_CALLBACK_HANDLER.password = password;
	}

	/**
	 * Получить токен Kerberos ответа для хоста с предоставленным токеном.
	 * @param protocol целевой протокол
	 * @param host целевой хост
	 * @param token входной токен
	 * @return токен ответа
	 * @throws GSSException при ошибке
	 * @throws LoginException при ошибке
	 */
	public static byte[] initSecurityContext(final String protocol, final String host, final byte[] token)
			throws GSSException, LoginException {
		return initSecurityContext(protocol, host, null, token);
	}

	/**
	 * Получите токен Kerberos ответа для хоста с указанным токеном, используйте
	 * предоставленные клиентом делегированные учетные данные. Используется для
	 * аутентификации с целевым хостом на шлюзовом сервере с учетными данными клиента,
	 * шлюз должен иметь собственного принципала, уполномоченного на делегирование
	 * @param protocol целевой протокол
	 * @param host целевой хост
	 * @param delegatedCredentials клиентские делегированные учетные данные
	 * @param token входной токен
	 * @return токен ответа
	 * @throws GSSException при ошибке
	 * @throws LoginException при ошибке
	 */
	public static byte[] initSecurityContext(final String protocol, final String host,
			final GSSCredential delegatedCredentials, final byte[] token) throws GSSException, LoginException {
		log.debug("KerberosHelper.initSecurityContext " + protocol + '@' + host + ' ' + token.length + " bytes token");

		synchronized (LOCK) {
			// check cached TGT
			if (clientLoginContext != null) {
				for (Object ticket : clientLoginContext.getSubject().getPrivateCredentials(KerberosTicket.class)) {
					KerberosTicket kerberosTicket = (KerberosTicket) ticket;
					if (kerberosTicket.getServer().getName().startsWith("krbtgt") && !kerberosTicket.isCurrent()) {
						log.debug("KerberosHelper.clientLogin cached TGT expired, try to relogin");
						clientLoginContext = null;
					}
				}
			}
			// create client login context
			if (clientLoginContext == null) {
				final LoginContext localLoginContext = new LoginContext("spnego-client", KERBEROS_CALLBACK_HANDLER);
				localLoginContext.login();
				clientLoginContext = localLoginContext;
			}
			// try to renew almost expired tickets
			for (Object ticket : clientLoginContext.getSubject().getPrivateCredentials(KerberosTicket.class)) {
				KerberosTicket kerberosTicket = (KerberosTicket) ticket;
				log.debug("KerberosHelper.clientLogin ticket for " + kerberosTicket.getServer().getName()
						+ " expires at " + kerberosTicket.getEndTime());
				if (kerberosTicket.getEndTime().getTime() < System.currentTimeMillis() + 10000) {
					if (kerberosTicket.isRenewable()) {
						try {
							kerberosTicket.refresh();
						}
						catch (RefreshFailedException e) {
							log.debug("KerberosHelper.clientLogin failed to renew ticket " + kerberosTicket);
						}
					}
					else {
						log.debug("KerberosHelper.clientLogin ticket is not renewable");
					}
				}
			}

			Object result = internalInitSecContext(protocol, host, delegatedCredentials, token);
			if (result instanceof GSSException exception) {
				log.info("KerberosHelper.initSecurityContext exception code " + exception.getMajor() + " minor code "
						+ exception.getMinor() + " message " + ((Throwable) result).getMessage());
				throw exception;
			}

			log.debug("KerberosHelper.initSecurityContext return " + ((byte[]) result).length + " bytes token");
			return (byte[]) result;
		}
	}

	protected static Object internalInitSecContext(final String protocol, final String host,
			final GSSCredential delegatedCredentials, final byte[] token) {
		return Subject.doAs(clientLoginContext.getSubject(), (PrivilegedAction<Object>) () -> {
			Object result;
			GSSContext context = null;
			try {
				GSSManager manager = GSSManager.getInstance();
				GSSName serverName = manager.createName(protocol + '@' + host, GSSName.NT_HOSTBASED_SERVICE);
				// Kerberos v5 OID
				Oid krb5Oid = new Oid("1.2.840.113554.1.2.2");

				context = manager.createContext(serverName, krb5Oid, delegatedCredentials, GSSContext.DEFAULT_LIFETIME);

				// TODO: used by IIS to pass token to Exchange ?
				context.requestCredDeleg(true);

				result = context.initSecContext(token, 0, token.length);
			}
			catch (GSSException e) {
				result = e;
			}
			finally {
				if (context != null) {
					try {
						context.dispose();
					}
					catch (GSSException e) {
						log.debug("KerberosHelper.internalInitSecContext " + e + ' ' + e.getMessage());
					}
				}
			}
			return result;
		});
	}

	/**
	 * Создать контекст входа Kerberos на стороне сервера для предоставленных учетных
	 * данных.
	 * @param serverPrincipal серверный принципал
	 * @param serverPassword серверный пароль
	 * @return LoginContext контекст входа сервера
	 * @throws LoginException в случае ошибки
	 */
	public static LoginContext serverLogin(final String serverPrincipal, final String serverPassword)
			throws LoginException {
		LoginContext serverLoginContext = new LoginContext("spnego-server", callbacks -> {
			for (Callback callback : callbacks) {
				if (callback instanceof NameCallback nameCallback) {
					nameCallback.setName(serverPrincipal);
				}
				else if (callback instanceof PasswordCallback passCallback) {
					passCallback.setPassword(serverPassword.toCharArray());
				}
				else {
					throw new UnsupportedCallbackException(callback);
				}
			}

		});
		serverLoginContext.login();
		return serverLoginContext;
	}

	/**
	 * Содержит информацию о контексте Kerberos сервера в серверном режиме.
	 */
	public static class SecurityContext {

		/**
		 * токен ответа
		 */
		public byte[] token;

		/**
		 * аутентифицированный принципал
		 */
		public String principal;

		/**
		 * делегированные учетные данные клиента
		 */
		public GSSCredential clientCredential;

	}

	/**
	 * Проверьте предоставленный клиентом токен Kerberos в контексте входа на сервер
	 * @param serverLoginContext контекст входа на сервер
	 * @param token токен клиента Kerberos
	 * @return результат с клиентским принципалом и необязательным возвращаемым токеном
	 * Kerberos
	 * @throws GSSException в случае ошибки
	 */
	public static SecurityContext acceptSecurityContext(LoginContext serverLoginContext, final byte[] token)
			throws GSSException {
		Object result = Subject.doAs(serverLoginContext.getSubject(), (PrivilegedAction<Object>) () -> {
			Object innerResult;
			SecurityContext securityContext = new SecurityContext();
			GSSContext context = null;
			try {
				GSSManager manager = GSSManager.getInstance();

				// get server credentials from context
				Oid krb5oid = new Oid("1.2.840.113554.1.2.2");
				GSSCredential serverCreds = manager.createCredential(
						null/* используйте имя из контекста входа */, GSSCredential.DEFAULT_LIFETIME, krb5oid,
						GSSCredential.ACCEPT_ONLY/* режим сервера */);
				context = manager.createContext(serverCreds);

				securityContext.token = context.acceptSecContext(token, 0, token.length);
				if (context.isEstablished()) {
					securityContext.principal = context.getSrcName().toString();
					log.debug("Authenticated user: " + securityContext.principal);
					if (!context.getCredDelegState()) {
						log.debug("Credentials can not be delegated");
					}
					else {
						// Get client delegated credentials from context (gateway mode)
						securityContext.clientCredential = context.getDelegCred();
					}
				}
				innerResult = securityContext;
			}
			catch (GSSException e) {
				innerResult = e;
			}
			finally {
				if (context != null) {
					try {
						context.dispose();
					}
					catch (GSSException e) {
						log.debug("KerberosHelper.acceptSecurityContext " + e + ' ' + e.getMessage());
					}
				}
			}
			return innerResult;
		});
		if (result instanceof GSSException exception) {
			log.info("KerberosHelper.acceptSecurityContext exception code " + exception.getMajor() + " minor code "
					+ exception.getMinor() + " message " + ((Throwable) result).getMessage());
			throw exception;
		}
		return (SecurityContext) result;
	}

}
