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

import java.awt.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Основной класс DavGateway
 */
@Slf4j
public final class MosTechEws {

	private static final Object LOCK = new Object();

	private static boolean shutdown = false;

	private MosTechEws() {
	}

	private static final ArrayList<AbstractServer> SERVER_LIST = new ArrayList<>();

	/**
	 * Запустите шлюз, слушайте на указанных портах smtp и pop3
	 * @param args параметр командной строки путь к файлу конфигурации
	 */
	public static void main(String[] args) throws IOException {
		boolean server = false;
		boolean token = false;
		for (String arg : args) {
			if (arg.startsWith("-")) {
				switch (arg) {
					case "-server" -> server = true;
					case "-token" -> token = true;
					case "-useconfig" -> Settings.setUserConfig(true);
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
                log.error("{} {}", e, e.getMessage());
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
			}

			start();

			// server mode: all threads are daemon threads, do not let main stop
			if (Settings.getBooleanProperty("mt.ews.server")) {
				Runtime.getRuntime().addShutdownHook(new Thread("Shutdown") {
					@Override
					public void run() {
						shutdown = true;
						log.debug("{}", new BundleMessage("LOG_GATEWAY_INTERRUPTED"));
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
						log.debug("{}", new BundleMessage("LOG_GATEWAY_INTERRUPTED"));
						Thread.currentThread().interrupt();
					}
				}

			}
		}
	}

	/**
	 * Запустить MT-EWS слушатели.
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

		boolean waitForExit = false;
		for (AbstractServer server : SERVER_LIST) {
			try {
				server.bind();
				server.start();
				log.info("{}",
						new BundleMessage("LOG_MT_EWS_GATEWAY_LISTENING",
								new BundleMessage("LOG_PROTOCOL_PORT", server.getProtocolName(), server.getPort()),
								Settings.getProperty("mt.ews.bindAddress")));
			}
			catch (MosTechEwsException e) {
				EwsErrorHolder.addError(e.getMessage());
				log.error("Ошибка при запуске приложения", e);
				if (!waitForExit) {
					waitForExit = true;
					exitAfterTimeout(Settings.getIntProperty("mt.ews.exitTimeout"));
				}
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
	 * Остановить все слушатели, завершить пул соединений и очистить кэш сессий.
	 */
	public static void stop() {
		MosTechEws.stopServers();
		// close pooled connections
		ExchangeSessionFactory.shutdown();
		log.info("{}", new BundleMessage("LOG_GATEWAY_STOP"));
	}

	private static void stopServers() {
		for (AbstractServer server : SERVER_LIST) {
			server.close();
			try {
				server.join();
			}
			catch (InterruptedException e) {
				log.warn("{}", new BundleMessage("LOG_EXCEPTION_WAITING_SERVER_THREAD_DIE"), e);
				Thread.currentThread().interrupt();
			}
		}
		HttpServer.stop();
	}

	/**
	 * Получить текущую версию MT-EWS.
	 * @return текущая версия
	 */
	public static String getCurrentVersion() {
		Package mtEwsPackage = MosTechEws.class.getPackage();
		String currentVersion = mtEwsPackage.getImplementationVersion();
		if (currentVersion == null) {
			currentVersion = "";
		}
		return currentVersion;
	}

	private static void exitAfterTimeout(long milliseconds) {
		String message = milliseconds != 0 ? "Приложение будет закрыто через " + milliseconds / 1000 + " секунд"
				: "Приложение закрывается";
		log.info(message);
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.schedule(() -> Runtime.getRuntime().halt(98), milliseconds, TimeUnit.MILLISECONDS);
	}

}
