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
 * Generic connection common to pop3 and smtp implementations
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
		 * Read byteSize bytes from inputStream, return content as String.
		 * @param byteSize content size
		 * @return content
		 * @throws IOException on error
		 */
		public String readContentAsString(int byteSize) throws IOException {
			return new String(readContent(byteSize), encoding);
		}

		/**
		 * Read byteSize bytes from inputStream, return content as byte array.
		 * @param byteSize content size
		 * @return content
		 * @throws IOException on error
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
	 * Only set the thread name and socket
	 * @param name thread type name
	 * @param clientSocket client socket
	 */
	protected AbstractConnection(String name, Socket clientSocket) {
		super(name + '-' + clientSocket.getPort());
		this.client = clientSocket;
		setDaemon(true);
	}

	/**
	 * Initialize the streams and set thread name.
	 * @param name thread type name
	 * @param clientSocket client socket
	 * @param encoding socket stream encoding
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
	 * Send message to client followed by CRLF.
	 * @param message message
	 * @throws IOException on error
	 */
	public void sendClient(String message) throws IOException {
		sendClient(null, message);
	}

	/**
	 * Send prefix and message to client followed by CRLF.
	 * @param prefix prefix
	 * @param message message
	 * @throws IOException on error
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
	 * Send only bytes to client.
	 * @param messageBytes content
	 * @throws IOException on error
	 */
	public void sendClient(byte[] messageBytes) throws IOException {
		sendClient(messageBytes, 0, messageBytes.length);
	}

	/**
	 * Send only bytes to client.
	 * @param messageBytes content
	 * @param offset the start offset in the data.
	 * @param length the number of bytes to write.
	 * @throws IOException on error
	 */
	public void sendClient(byte[] messageBytes, int offset, int length) throws IOException {
		os.write(messageBytes, offset, length);
		os.flush();
	}

	/**
	 * Read a line from the client connection. Log message to logger
	 * @return command line or null
	 * @throws IOException when unable to read line
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
	 * Close client connection, streams and Exchange session .
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
