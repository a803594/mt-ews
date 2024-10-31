/*
DIT
 */

package ru.mos.mostech.ews.http;

import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.Settings;

import javax.net.ssl.*;
import javax.security.auth.callback.PasswordCallback;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.security.*;
import java.util.ArrayList;

/**
 * Реализация SSLSocketFactory. Обертка для DavGatewaySSLProtocolSocketFactory,
 * используемая HttpClient 4
 */
@Slf4j
public class MosTechEwsSSLSocketFactory extends SSLSocketFactory {

	private SSLContext sslcontext;

	private SSLContext getSSLContext() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException,
			InvalidAlgorithmParameterException {
		if (this.sslcontext == null) {
			this.sslcontext = createSSLContext();
		}
		return this.sslcontext;
	}

	private SSLContext createSSLContext() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException,
			KeyManagementException, KeyStoreException {
		// PKCS11 client certificate settings
		String pkcs11Library = Settings.getProperty("mt.ews.ssl.pkcs11Library");

		String clientKeystoreType = Settings.getProperty("mt.ews.ssl.clientKeystoreType");
		// set default keystore type
		if (clientKeystoreType == null || clientKeystoreType.isEmpty()) {
			clientKeystoreType = "PKCS11";
		}

		if (pkcs11Library != null && !pkcs11Library.isEmpty() && "PKCS11".equals(clientKeystoreType)) {
			StringBuilder pkcs11Buffer = new StringBuilder();
			pkcs11Buffer.append("name=MT-EWS\n");
			pkcs11Buffer.append("library=").append(pkcs11Library).append('\n');
			String pkcs11Config = Settings.getProperty("mt.ews.ssl.pkcs11Config");
			if (pkcs11Config != null && !pkcs11Config.isEmpty()) {
				pkcs11Buffer.append(pkcs11Config).append('\n');
			}
			SunPKCS11ProviderHandler.registerProvider(pkcs11Buffer.toString());
		}
		String algorithm = KeyManagerFactory.getDefaultAlgorithm();
		if ("SunX509".equals(algorithm)) {
			algorithm = "NewSunX509";
		}
		else if ("IbmX509".equals(algorithm)) {
			algorithm = "NewIbmX509";
		}
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(algorithm);

		ArrayList<KeyStore.Builder> keyStoreBuilders = new ArrayList<>();
		// PKCS11 (smartcard) keystore with password callback
		KeyStore.Builder scBuilder = KeyStore.Builder.newInstance("PKCS11", null, getProtectionParameter(null));
		keyStoreBuilders.add(scBuilder);

		String clientKeystoreFile = Settings.getProperty("mt.ews.ssl.clientKeystoreFile");
		String clientKeystorePass = Settings.getProperty("mt.ews.ssl.clientKeystorePass");
		if (clientKeystoreFile != null && !clientKeystoreFile.isEmpty()
				&& ("PKCS12".equals(clientKeystoreType) || "JKS".equals(clientKeystoreType))) {
			// PKCS12 file based keystore
			KeyStore.Builder fsBuilder = KeyStore.Builder.newInstance(clientKeystoreType, null,
					new File(clientKeystoreFile), getProtectionParameter(clientKeystorePass));
			keyStoreBuilders.add(fsBuilder);
		}
		// Enable native Windows SmartCard access through MSCAPI (no PKCS11 config
		// required)
		if ("MSCAPI".equals(clientKeystoreType)) {
			try {
				KeyStore keyStore = KeyStore.getInstance("Windows-MY");
				keyStore.load(null, null);
				keyStoreBuilders.add(KeyStore.Builder.newInstance(keyStore, new KeyStore.PasswordProtection(null)));
			}
			catch (Exception e) {
				// ignore
			}
		}

		ManagerFactoryParameters keyStoreBuilderParameters = new KeyStoreBuilderParameters(keyStoreBuilders);
		keyManagerFactory.init(keyStoreBuilderParameters);

		// Get a list of key managers
		KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

		// Walk through the key managers and replace all X509 Key Managers with
		// a specialized wrapped MT-EWS X509 Key Manager
		for (int i = 0; i < keyManagers.length; i++) {
			KeyManager keyManager = keyManagers[i];
			if (keyManager instanceof X509KeyManager x509KeyManager) {
				keyManagers[i] = new MosTechEwsX509KeyManager(x509KeyManager);
			}
		}

		SSLContext context = SSLContext.getInstance("TLS");
		context.init(keyManagers, new TrustManager[] { new MosTechEwsX509TrustManager() }, null);
		return context;
	}

	private KeyStore.ProtectionParameter getProtectionParameter(String password) {
		if (password != null && !password.isEmpty()) {
			// password provided: create a PasswordProtection
			return new KeyStore.PasswordProtection(password.toCharArray());
		}
		else {
			// request password at runtime through a callback
			return new KeyStore.CallbackHandlerProtection(callbacks -> {
				if (callbacks.length > 0 && callbacks[0] instanceof PasswordCallback) {
					throw new UnsupportedOperationException("");
				}
			});
		}
	}

	@Override
	public String[] getDefaultCipherSuites() {
		try {
			return getSSLContext().getSocketFactory().getDefaultCipherSuites();
		}
		catch (Exception e) {
			// ignore
		}
		return new String[] {};
	}

	@Override
	public String[] getSupportedCipherSuites() {
		try {
			return getSSLContext().getSocketFactory().getSupportedCipherSuites();
		}
		catch (Exception e) {
			// ignore
		}
		return new String[] {};
	}

	@Override
	public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
		log.debug("createSocket " + host + " " + port);
		try {
			return getSSLContext().getSocketFactory().createSocket(socket, host, port, autoClose);
		}
		catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException
				| InvalidAlgorithmParameterException e) {
			throw new IOException(e + " " + e.getMessage());
		}
	}

	@Override
	public Socket createSocket(String host, int port) throws IOException {
		log.debug("createSocket " + host + " " + port);
		try {
			return getSSLContext().getSocketFactory().createSocket(host, port);
		}
		catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException
				| InvalidAlgorithmParameterException e) {
			throw new IOException(e + " " + e.getMessage());
		}
	}

	@Override
	public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort) throws IOException {
		log.debug("createSocket " + host + " " + port + " " + clientHost + " " + clientPort);
		try {
			return getSSLContext().getSocketFactory().createSocket(host, port, clientHost, clientPort);
		}
		catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException
				| InvalidAlgorithmParameterException e) {
			throw new IOException(e + " " + e.getMessage());
		}
	}

	@Override
	public Socket createSocket(InetAddress host, int port) throws IOException {
		log.debug("createSocket " + host + " " + port);
		try {
			return getSSLContext().getSocketFactory().createSocket(host, port);
		}
		catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException
				| InvalidAlgorithmParameterException e) {
			throw new IOException(e + " " + e.getMessage());
		}
	}

	@Override
	public Socket createSocket(InetAddress host, int port, InetAddress clientHost, int clientPort) throws IOException {
		log.debug("createSocket " + host + " " + port + " " + clientHost + " " + clientPort);
		try {
			return getSSLContext().getSocketFactory().createSocket(host, port, clientHost, clientPort);
		}
		catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException
				| InvalidAlgorithmParameterException e) {
			throw new IOException(e + " " + e.getMessage());
		}
	}

}
