/*
DIT
 */
package ru.mos.mostech.ews.http;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.AuthProvider;
import java.security.Provider;
import java.security.Security;

/**
 * Add the SunPKCS11 Provider.
 */
public final class SunPKCS11ProviderHandler {

	private SunPKCS11ProviderHandler() {
	}

	/**
	 * Register PKCS11 provider.
	 * @param pkcs11Config PKCS11 config string
	 */
	public static void registerProvider(String pkcs11Config) {
		Provider p;

		try {
			@SuppressWarnings("unchecked")
			Class<AuthProvider> sunPkcs11Class = (Class<AuthProvider>) Class.forName("sun.security.pkcs11.SunPKCS11");
			Constructor<AuthProvider> sunPkcs11Constructor = sunPkcs11Class.getDeclaredConstructor(InputStream.class);
			p = sunPkcs11Constructor
				.newInstance(new ByteArrayInputStream(pkcs11Config.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchMethodException e) {
			// try java 9 configuration
			p = configurePkcs11Provider(pkcs11Config);
		}
		catch (Exception e) {
			throw new PKCS11ProviderException(buildErrorMessage(e));
		}

		Security.addProvider(p);
	}

	private static Provider configurePkcs11Provider(String pkcs11Config) {
		Provider p;
		try {
			p = Security.getProvider("SunPKCS11");
			// new Java 9 configure method
			Method configureMethod = Provider.class.getDeclaredMethod("configure", String.class);
			configureMethod.invoke(p, "--" + pkcs11Config);
		}
		catch (Exception e) {
			throw new PKCS11ProviderException(buildErrorMessage(e));
		}
		return p;
	}

	private static String buildErrorMessage(Exception e) {
		StringBuilder errorMessage = new StringBuilder("Unable to configure SunPKCS11 provider");
		Throwable cause = e.getCause();
		while (cause != null) {
			errorMessage.append(" ").append(cause.getMessage());
			cause = cause.getCause();
		}
		return errorMessage.toString();
	}

	static final class PKCS11ProviderException extends RuntimeException {

		public PKCS11ProviderException(String message) {
			super(message);
		}

	}

}
