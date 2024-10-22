/*
DIT
 */
package ru.mos.mostech.ews;

import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.caldav.CaldavServer;
import ru.mos.mostech.ews.exception.MosTechEwsException;
import ru.mos.mostech.ews.exchange.ExchangeSessionFactory;
import ru.mos.mostech.ews.exchange.auth.ExchangeAuthenticator;
import ru.mos.mostech.ews.imap.ImapServer;
import ru.mos.mostech.ews.ldap.LdapServer;
import ru.mos.mostech.ews.server.HttpServer;
import ru.mos.mostech.ews.smtp.SmtpServer;
import ru.mos.mostech.ews.ui.SimpleUi;
import ru.mos.mostech.ews.ui.tray.MosTechEwsTray;

import java.awt.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * DavGateway main class
 */
@Slf4j
public final class MosTechEws {

	private static final Object LOCK = new Object();

	private static boolean shutdown = false;

	private MosTechEws() {
	}

	private static final ArrayList<AbstractServer> SERVER_LIST = new ArrayList<>();

	/**
	 * Start the gateway, listen on specified smtp and pop3 ports
	 * @param args command line parameter config file path
	 */
	public static void main(String[] args) throws IOException {
		boolean notray = false;
		boolean server = false;
		boolean token = false;
		for (String arg : args) {
			if (arg.startsWith("-")) {
				if ("-notray".equals(arg)) {
					notray = true;
				}
				else if ("-server".equals(arg)) {
					server = true;
				}
				else if ("-token".equals(arg)) {
					token = true;
				}
				else if ("-useconfig".equals(arg)) {
					Settings.setUserConfig(true);
				}
			}
			else {
				Settings.setConfigFilePath(arg);
			}
		}

		Settings.load();
		if (token) {
			try {
				ExchangeAuthenticator authenticator = (ExchangeAuthenticator) Class
					.forName("ru.mos.mostech.ews.exchange.auth.O365InteractiveAuthenticator")
					.getDeclaredConstructor()
					.newInstance();
				authenticator.setUsername("");
				authenticator.authenticate();
				log.debug(authenticator.getToken().getRefreshToken());
			}
			catch (IOException | ClassNotFoundException | NoSuchMethodException | InstantiationException
					| IllegalAccessException | InvocationTargetException e) {
				log.error(e + " " + e.getMessage());
			}
			// force shutdown on Linux
			System.exit(0);
		}
		else {

			if (GraphicsEnvironment.isHeadless()) {
				// force server mode
				log.debug("Headless mode, do not create GUI");
				server = true;
			}
			if (server) {
				Settings.setProperty("mt.ews.server", "true");
			}

			if (Settings.getBooleanProperty("mt.ews.server")) {
				log.debug("Start MT-EWS in server mode");
			}
			else {
				log.debug("Start MT-EWS in GUI mode");
				if (!notray) {
					SimpleUi.start();
				}
			}

			start();

			// server mode: all threads are daemon threads, do not let main stop
			if (Settings.getBooleanProperty("mt.ews.server")) {
				Runtime.getRuntime().addShutdownHook(new Thread("Shutdown") {
					@Override
					public void run() {
						shutdown = true;
						MosTechEwsTray.debug(new BundleMessage("LOG_GATEWAY_INTERRUPTED"));
						MosTechEws.stop();
						synchronized (LOCK) {
							LOCK.notifyAll();
						}
					}
				});

				synchronized (LOCK) {
					try {
						while (!shutdown) {
							LOCK.wait();
						}
					}
					catch (InterruptedException e) {
						MosTechEwsTray.debug(new BundleMessage("LOG_GATEWAY_INTERRUPTED"));
						Thread.currentThread().interrupt();
					}
				}

			}
		}
	}

	/**
	 * Start MT-EWS listeners.
	 */
	public static void start() {
		SERVER_LIST.clear();

		int smtpPort = Settings.getIntProperty("mt.ews.smtpPort");
		if (smtpPort != 0) {
			SERVER_LIST.add(new SmtpServer(smtpPort));
		}
		int imapPort = Settings.getIntProperty("mt.ews.imapPort");
		if (imapPort != 0) {
			SERVER_LIST.add(new ImapServer(imapPort));
		}
		int caldavPort = Settings.getIntProperty("mt.ews.caldavPort");
		if (caldavPort != 0) {
			SERVER_LIST.add(new CaldavServer(caldavPort));
		}
		int ldapPort = Settings.getIntProperty("mt.ews.ldapPort");
		if (ldapPort != 0) {
			SERVER_LIST.add(new LdapServer(ldapPort));
		}

		for (AbstractServer server : SERVER_LIST) {
			try {
				server.bind();
				server.start();
				MosTechEwsTray.info(new BundleMessage("LOG_MT_EWS_GATEWAY_LISTENING",
						new BundleMessage("LOG_PROTOCOL_PORT", server.getProtocolName(), server.getPort()),
						Settings.getProperty("mt.ews.bindAddress")));
			}
			catch (MosTechEwsException e) {
				log.error("Ошибка при запуске приложения", e);
				System.exit(98);
			}
		}

		try {
			HttpServer.start(Settings.getIntProperty("mt.ews.httpPort"));
		}
		catch (Exception e) {
			log.error("Ошибка при запуске приложения", e);
			System.exit(98);
		}

	}

	/**
	 * Stop all listeners, shutdown connection pool and clear session cache.
	 */
	public static void stop() {
		MosTechEws.stopServers();
		// close pooled connections
		ExchangeSessionFactory.shutdown();
		MosTechEwsTray.info(new BundleMessage("LOG_GATEWAY_STOP"));
		MosTechEwsTray.dispose();
	}

	/**
	 * Stop all listeners and clear session cache.
	 */
	public static void restart() {
		MosTechEws.stopServers();
		// clear session cache
		ExchangeSessionFactory.shutdown();
		MosTechEws.start();
	}

	private static void stopServers() {
		for (AbstractServer server : SERVER_LIST) {
			server.close();
			try {
				server.join();
			}
			catch (InterruptedException e) {
				MosTechEwsTray.warn(new BundleMessage("LOG_EXCEPTION_WAITING_SERVER_THREAD_DIE"), e);
				Thread.currentThread().interrupt();
			}
		}
		HttpServer.stop();
	}

	/**
	 * Get current MT-EWS version.
	 * @return current version
	 */
	public static String getCurrentVersion() {
		Package mtEwsPackage = MosTechEws.class.getPackage();
		String currentVersion = mtEwsPackage.getImplementationVersion();
		if (currentVersion == null) {
			currentVersion = "";
		}
		return currentVersion;
	}

}
