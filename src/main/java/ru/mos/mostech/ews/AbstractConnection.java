/*
DIT
 */
package ru.mos.mostech.ews;

import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.exception.MosTechEwsException;
import ru.mos.mostech.ews.exchange.ExchangeSession;
import ru.mos.mostech.ews.ui.tray.MosTechEwsTray;
import ru.mos.mostech.ews.util.MdcUserPathUtils;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Генерическое соединение, общее для реализаций pop3 и smtp
 */
@Slf4j
public abstract class AbstractConnection extends Thread implements Closeable {

	protected enum State {

		INITIAL, LOGIN, USER, PASSWORD, AUTHENTICATED, STARTMAIL, RECIPIENT, MAILDATA

	}

	@Override
	public void run() {
		MdcUserPathUtils.init(client.getPort());
		doRun();
	}

	protected abstract void doRun();

	protected static class LineReaderInputStream extends PushbackInputStream {

		final String encoding;

		protected LineReaderInputStream(InputStream in, String encoding) {
			super(in);
			this.encoding = Objects.requireNonNullElse(encoding, "ASCII");
		}

		@SuppressWarnings("java:S135") // break
		public String readLine() throws IOException {
			ByteArrayOutputStream baos = null;
			int b;
			while ((b = read()) > -1) {
				if (b == '\r') {
					int next = read();
					if (next != '\n') {
						unread(next);
					}
					break;
				}
				else if (b == '\n') {
					break;
				}
				if (baos == null) {
					baos = new ByteArrayOutputStream();
				}
				baos.write(b);
			}
			if (baos != null) {
				return baos.toString(encoding);
			}
			else {
				return null;
			}
		}

		/**
		 * Прочитать byteSize байт из inputStream, вернуть содержимое как String.
		 * @param byteSize размер содержимого
		 * @return содержимое
		 * @throws IOException при ошибке
		 */
		public String readContentAsString(int byteSize) throws IOException {
			return new String(readContent(byteSize), encoding);
		}

		/**
		 * Прочитать byteSize байт из inputStream, вернуть содержимое в виде массива байт.
		 * @param byteSize размер содержимого
		 * @return содержимое
		 * @throws IOException в случае ошибки
		 */
		public byte[] readContent(int byteSize) throws IOException {
			byte[] buffer = new byte[byteSize];
			int startIndex = 0;
			int count = 0;
			while (count >= 0 && startIndex < byteSize) {
				count = read(buffer, startIndex, byteSize - startIndex);
				startIndex += count;
			}
			if (startIndex < byteSize) {
				throw new MosTechEwsException("EXCEPTION_END_OF_STREAM");
			}

			return buffer;
		}

	}

	protected final Socket client;

	protected LineReaderInputStream in;

	protected OutputStream os;

	// user name and password initialized through connection
	protected String userName;

	protected String password;

	// connection state
	protected State state = State.INITIAL;

	// Exchange session proxy
	protected ExchangeSession session;

	/**
	 * Установить только имя потока и сокет
	 * @param name имя типа потока
	 * @param clientSocket сокет клиента
	 */
	protected AbstractConnection(String name, Socket clientSocket) {
		super(name + '-' + clientSocket.getPort());
		this.client = clientSocket;
		setDaemon(true);
	}

	/**
	 * Инициализировать потоки и установить имя потока.
	 * @param name имя типа потока
	 * @param clientSocket клиентский сокет
	 * @param encoding кодировка сокетного потока
	 */
	protected AbstractConnection(String name, Socket clientSocket, String encoding) {
		super(name + '-' + clientSocket.getPort());
		this.client = clientSocket;
		logConnection("CONNECT", "");
		try {
			in = new LineReaderInputStream(client.getInputStream(), encoding);
			os = new BufferedOutputStream(client.getOutputStream());
		}
		catch (IOException e) {
			close();
			MosTechEwsTray.error(new BundleMessage("LOG_EXCEPTION_GETTING_SOCKET_STREAMS"), e);
		}
	}

	public void logConnection(String action, String userName) {
		log.info("{} - {}:{} {}", action, client.getInetAddress().getHostAddress(), client.getPort(), userName);
	}

	/**
	 * Отправить сообщение клиенту, за которым следует CRLF.
	 * @param message сообщение
	 * @throws IOException в случае ошибки
	 */
	public void sendClient(String message) throws IOException {
		sendClient(null, message);
	}

	/**
	 * Отправить префикс и сообщение клиенту, завершая CRLF.
	 * @param prefix префикс
	 * @param message сообщение
	 * @throws IOException при ошибке
	 */
	public void sendClient(String prefix, String message) throws IOException {
		if (prefix != null) {
			os.write(prefix.getBytes(StandardCharsets.UTF_8));
			MosTechEwsTray.debug(new BundleMessage("LOG_SEND_CLIENT_PREFIX_MESSAGE", prefix, message));
		}
		else {
			MosTechEwsTray.debug(new BundleMessage("LOG_SEND_CLIENT_MESSAGE", message));
		}
		os.write(message.getBytes(StandardCharsets.UTF_8));
		os.write((char) 13);
		os.write((char) 10);
		os.flush();
	}

	/**
	 * Отправить только байты клиенту.
	 * @param messageBytes контент
	 * @throws IOException в случае ошибки
	 */
	public void sendClient(byte[] messageBytes) throws IOException {
		sendClient(messageBytes, 0, messageBytes.length);
	}

	/**
	 * Отправить только байты клиенту.
	 * @param messageBytes содержимое
	 * @param offset начальное смещение в данных.
	 * @param length количество байтов для записи.
	 * @throws IOException в случае ошибки
	 */
	public void sendClient(byte[] messageBytes, int offset, int length) throws IOException {
		os.write(messageBytes, offset, length);
		os.flush();
	}

	/**
	 * Прочитать строку из клиентского соединения. Записать сообщение в лог
	 * @return строку команды или null
	 * @throws IOException если невозможно прочитать строку
	 */
	public String readClient() throws IOException {
		String line = in.readLine();
		if (line != null) {
			if (line.startsWith("PASS")) {
				MosTechEwsTray.debug(new BundleMessage("LOG_READ_CLIENT_PASS"));
				// SMTP LOGIN
			}
			else if (line.startsWith("AUTH LOGIN ")) {
				MosTechEwsTray.debug(new BundleMessage("LOG_READ_CLIENT_AUTH_LOGIN"));
				// IMAP LOGIN
			}
			else if (state == State.INITIAL && line.indexOf(' ') >= 0
					&& line.substring(line.indexOf(' ') + 1).toUpperCase().startsWith("LOGIN")) {
				MosTechEwsTray.debug(new BundleMessage("LOG_READ_CLIENT_LOGIN"));
			}
			else if (state == State.PASSWORD) {
				MosTechEwsTray.debug(new BundleMessage("LOG_READ_CLIENT_PASSWORD"));
				// HTTP Basic Authentication
			}
			else if (line.startsWith("Authorization:")) {
				MosTechEwsTray.debug(new BundleMessage("LOG_READ_CLIENT_AUTHORIZATION"));
			}
			else if (line.startsWith("AUTH PLAIN")) {
				MosTechEwsTray.debug(new BundleMessage("LOG_READ_CLIENT_AUTH_PLAIN"));
			}
			else {
				MosTechEwsTray.debug(new BundleMessage("LOG_READ_CLIENT_LINE", line));
			}
		}
		MosTechEwsTray.switchIcon();
		return line;
	}

	/**
	 * Закрыть соединение с клиентом, потоки и сессию обмена.
	 */
	public void close() {
		try {
			doClose();
		}
		finally {
			MdcUserPathUtils.clear();
		}
	}

	private void doClose() {
		logConnection("DISCONNECT", "");
		if (in != null) {
			try {
				in.close();
			}
			catch (IOException e2) {
				// ignore
			}
		}
		if (os != null) {
			try {
				os.close();
			}
			catch (IOException e2) {
				// ignore
			}
		}
		try {
			client.close();
		}
		catch (IOException e2) {
			MosTechEwsTray.debug(new BundleMessage("LOG_EXCEPTION_CLOSING_CLIENT_SOCKET"), e2);
		}
	}

}
