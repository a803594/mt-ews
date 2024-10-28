/*
DIT
 */
package ru.mos.mostech.ews;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.exception.MosTechEwsException;
import ru.mos.mostech.ews.util.KeysUtils;

import javax.net.ServerSocketFactory;
import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.HashSet;

/**
 * Обобщенный абстрактный сервер, общий для реализаций SMTP и POP3
 */
@Slf4j
public abstract class AbstractServer extends Thread {

	protected boolean nosslFlag; // will cause same behavior as before with unchanged
									// config files

	@Getter
	private final int port;

	private ServerSocket serverSocket;

	/**
	 * Получить имя протокола сервера (SMTP, POP, IMAP и т.д.).
	 * @return имя протокола сервера
	 */
	public abstract String getProtocolName();

	/**
	 * Создайте ServerSocket для прослушивания соединений. Запустите поток.
	 * @param name имя потока
	 * @param port выбранный порт tcp сокета
	 * @param defaultPort стандартный порт tcp сокета
	 */
	protected AbstractServer(String name, int port, int defaultPort) {
		super(name);
		setDaemon(true);
		if (port == 0) {
			this.port = defaultPort;
		}
		else {
			this.port = port;
		}
	}

	/**
	 * Привязать серверный сокет к заданному порту.
	 * @throws MosTechEwsException невозможно создать серверный сокет
	 */
	@SuppressWarnings("java:S3776")
	public void bind() throws MosTechEwsException {
		String bindAddress = Settings.getProperty("mt.ews.bindAddress");
		String keystoreFile = Settings.getProperty("mt.ews.ssl.keystoreFile");

		ServerSocketFactory serverSocketFactory;
		if (keystoreFile == null || keystoreFile.isEmpty() || nosslFlag) {
			serverSocketFactory = ServerSocketFactory.getDefault();
		}
		else {
			try {

				// SSLContext is environment for implementing JSSE...
				// create ServerSocketFactory
				SSLContext sslContext = SSLContext.getInstance("TLS");

				// initialize sslContext to work with key managers
				sslContext.init(getKeyManagers(), getTrustManagers(), null);

				// create ServerSocketFactory from sslContext
				serverSocketFactory = sslContext.getServerSocketFactory();

				Settings.setSecure(true);
			}
			catch (IOException | GeneralSecurityException ex) {
				throw new MosTechEwsException("LOG_EXCEPTION_CREATING_SSL_SERVER_SOCKET", getProtocolName(), port,
						ex.getMessage() == null ? ex.toString() : ex.getMessage());
			}
		}
		try {
			// create the server socket
			if (bindAddress == null || bindAddress.isEmpty()) {
				serverSocket = serverSocketFactory.createServerSocket(port);
			}
			else {
				serverSocket = serverSocketFactory.createServerSocket(port, 0, InetAddress.getByName(bindAddress));
			}
			if (serverSocket instanceof SSLServerSocket sslServerSocket) {
				// CVE-2014-3566 disable SSLv3
				HashSet<String> protocols = new HashSet<>();
				for (String protocol : sslServerSocket.getEnabledProtocols()) {
					if (!protocol.startsWith("SSL")) {
						protocols.add(protocol);
					}
				}
				sslServerSocket.setEnabledProtocols(protocols.toArray(new String[0]));
				sslServerSocket.setNeedClientAuth(Settings.getBooleanProperty("mt.ews.ssl.needClientAuth", false));
			}

		}
		catch (IOException e) {
			throw new MosTechEwsException("LOG_SOCKET_BIND_FAILED", getProtocolName(), port);
		}
	}

	/**
	 * Создать менеджеры доверия из файла хранилища доверия.
	 * @return менеджеры доверия
	 * @throws CertificateException при ошибке
	 * @throws NoSuchAlgorithmException при ошибке
	 * @throws IOException при ошибке
	 * @throws KeyStoreException при ошибке
	 */
	protected TrustManager[] getTrustManagers()
			throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException {
		return KeysUtils.getTrustManagers();
	}

	/**
	 * Создание менеджеров ключей из файла хранилища ключей.
	 * @return менеджеры ключей
	 * @throws CertificateException в случае ошибки
	 * @throws NoSuchAlgorithmException в случае ошибки
	 * @throws IOException в случае ошибки
	 * @throws KeyStoreException в случае ошибки
	 */
	protected KeyManager[] getKeyManagers() throws CertificateException, NoSuchAlgorithmException, IOException,
			KeyStoreException, UnrecoverableKeyException {
		return KeysUtils.getKeyManagers();
	}

	/**
	 * Основная часть потока сервера. Цикл без конца, прослушивающий и принимающий
	 * подключения от клиентов. Для каждого соединения создается объект Connection для
	 * обработки коммуникации через новый сокет.
	 */
	@Override
	public void run() {
		AbstractConnection connection = null;
		Socket clientSocket = null;
		try {
			while (!serverSocket.isClosed()) {
				clientSocket = serverSocket.accept();
				// set default timeout to 5 minutes
				clientSocket.setSoTimeout(Settings.getIntProperty("mt.ews.clientSoTimeout", 300) * 1000);
				log.debug("{}", new BundleMessage("LOG_CONNECTION_FROM", clientSocket.getInetAddress(), port));
				// only accept localhost connections for security reasons
				if (Settings.getBooleanProperty("mt.ews.allowRemote")
						|| clientSocket.getInetAddress().isLoopbackAddress() ||
						// OSX link local address on loopback interface
						clientSocket.getInetAddress()
							.equals(InetAddress.getByAddress(new byte[] { (byte) 0xfe, (byte) 0x80, 0, 0, 0, 0, 0, 0, 0,
									0, 0, 0, 0, 0, 0, 1 }))) {
					connection = createConnectionHandler(clientSocket);
					connection.start();

				}
				else {
					log.warn("{}", new BundleMessage("LOG_EXTERNAL_CONNECTION_REFUSED"));
				}
			}

		}
		catch (IOException e) {
			// do not warn if exception on socket close (gateway restart)
			if (!serverSocket.isClosed()) {
				log.warn("{}", new BundleMessage("LOG_EXCEPTION_LISTENING_FOR_CONNECTIONS"), e);
			}
		}
		finally {
			try {
				if (clientSocket != null) {
					clientSocket.close();
				}
			}
			catch (IOException e) {
				log.warn("{}", new BundleMessage("LOG_EXCEPTION_CLOSING_CLIENT_SOCKET"), e);
			}
			if (connection != null) {
				connection.close();
			}
		}

	}

	/**
	 * Создайте обработчик подключения для текущего слушателя.
	 * @param clientSocket сокет клиента
	 * @return обработчик подключения
	 */
	public abstract AbstractConnection createConnectionHandler(Socket clientSocket);

	/**
	 * Закрыть серверный сокет
	 */
	public void close() {
		try {
			if (serverSocket != null) {
				serverSocket.close();
			}
		}
		catch (IOException e) {
			log.warn("{}", new BundleMessage("LOG_EXCEPTION_CLOSING_SERVER_SOCKET"), e);
		}
	}

}
