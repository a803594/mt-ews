/*
DIT
 */
package ru.mos.mostech.ews.http;

import lombok.extern.slf4j.Slf4j;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import java.util.HashMap;

/**
 * Custom JAAS login configuration. Equivalent to the following configuration:
 * spnego-client { com.sun.security.auth.module.Krb5LoginModule required
 * useTicketCache=true renewTGT=true; }; spnego-server {
 * com.sun.security.auth.module.Krb5LoginModule required isInitiator=false useKeyTab=false
 * storeKey=true; };
 * <p/>
 */
@Slf4j
public class KerberosLoginConfiguration extends Configuration {

	protected static final AppConfigurationEntry[] CLIENT_LOGIN_MODULE;

	protected static final AppConfigurationEntry[] SERVER_LOGIN_MODULE;

	static {
		HashMap<String, String> clientLoginModuleOptions = new HashMap<>();
		if (log.isDebugEnabled()) {
			clientLoginModuleOptions.put("debug", "true");
		}

		clientLoginModuleOptions.put("useTicketCache", "true");
		clientLoginModuleOptions.put("renewTGT", "true");
		// clientLoginModuleOptions.put("doNotPrompt", "true");
		String krb5ccName = System.getenv().get("KRB5CCNAME");
		if (krb5ccName != null && !krb5ccName.isEmpty()) {
			clientLoginModuleOptions.put("ticketCache", krb5ccName);
		}
		// clientLoginModuleOptions.put("ticketCache",
		// FileCredentialsCache.getDefaultCacheName());
		// clientLoginModuleOptions.put("refreshKrb5Config", "true");
		// clientLoginModuleOptions.put("storeKey", "true");
		CLIENT_LOGIN_MODULE = new AppConfigurationEntry[] {
				new AppConfigurationEntry("com.sun.security.auth.module.Krb5LoginModule",
						AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, clientLoginModuleOptions) };

		HashMap<String, String> serverLoginModuleOptions = new HashMap<>();
		if (log.isDebugEnabled()) {
			serverLoginModuleOptions.put("debug", "true");
		}

		serverLoginModuleOptions.put("isInitiator", "false"); // acceptor (server) mode
		serverLoginModuleOptions.put("useKeyTab", "false"); // do not use credentials
															// stored in keytab file
		serverLoginModuleOptions.put("storeKey", "true"); // store credentials in subject
		SERVER_LOGIN_MODULE = new AppConfigurationEntry[] {
				new AppConfigurationEntry("com.sun.security.auth.module.Krb5LoginModule",
						AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, serverLoginModuleOptions) };
	}

	@Override
	public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
		if ("spnego-client".equals(name)) {
			return CLIENT_LOGIN_MODULE;
		}
		else if ("spnego-server".equals(name)) {
			return SERVER_LOGIN_MODULE;
		}
		else {
			return null;
		}
	}

	@Override
	public void refresh() {
		// nothing to do
	}

}