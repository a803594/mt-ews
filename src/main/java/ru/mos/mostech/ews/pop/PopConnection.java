/*
DIT
 */
package ru.mos.mostech.ews.pop;

import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.AbstractConnection;
import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.MosTechEws;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.exchange.DoubleDotOutputStream;
import ru.mos.mostech.ews.exchange.ExchangeSession;
import ru.mos.mostech.ews.exchange.ExchangeSessionFactory;
import ru.mos.mostech.ews.exchange.MessageLoadThread;
import ru.mos.mostech.ews.util.IOUtil;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Реализация подключения к Dav Gateway
 */
@Slf4j
public class PopConnection extends AbstractConnection {

	private List<ExchangeSession.Message> messages;

	/**
	 * Инициализировать потоки и запустить поток.
	 * @param clientSocket Сокет клиента POP
	 */
	public PopConnection(Socket clientSocket) {
		super(PopConnection.class.getSimpleName(), clientSocket, null);
	}

	protected long getTotalMessagesLength() {
		int result = 0;
		for (ExchangeSession.Message message : messages) {
			result += message.size;
		}
		return result;
	}

	protected void printCapabilities() throws IOException {
		sendClient("TOP");
		sendClient("USER");
		sendClient("UIDL");
		sendClient(".");
	}

	protected void printList() throws IOException {
		int i = 1;
		for (ExchangeSession.Message message : messages) {
			sendClient(i++ + " " + message.size);
		}
		sendClient(".");
	}

	protected void printUidList() throws IOException {
		int i = 1;
		for (ExchangeSession.Message message : messages) {
			sendClient(i++ + " " + message.getUid());
		}
		sendClient(".");
	}

	@SuppressWarnings({ "java:S3776", "java:S6541", "java:S135" })
	@Override
	public void doRun() {
		String line;
		StringTokenizer tokens;

		try {
			ExchangeSessionFactory.checkConfig();
			sendOK("MT-EWS " + MosTechEws.getCurrentVersion() + " POP ready at " + new Date());

			for (;;) {
				line = readClient();
				// unable to read line, connection closed ?
				if (line == null) {
					break;
				}

				tokens = new StringTokenizer(line);
				if (tokens.hasMoreTokens()) {
					String command = tokens.nextToken();

					if ("QUIT".equalsIgnoreCase(command)) {
						// delete messages before quit
						if (session != null) {
							session.purgeOldestTrashAndSentMessages();
						}
						sendOK("Bye");
						break;
					}
					else if ("USER".equalsIgnoreCase(command)) {
						userName = null;
						password = null;
						session = null;
						if (tokens.hasMoreTokens()) {
							userName = line.substring("USER ".length());
							sendOK("USER : " + userName);
							state = State.USER;
						}
						else {
							sendERR("invalid syntax");
							state = State.INITIAL;
						}
					}
					else if ("PASS".equalsIgnoreCase(command)) {
						if (state != State.USER) {
							sendERR("invalid state");
							state = State.INITIAL;
						}
						else if (!tokens.hasMoreTokens()) {
							sendERR("invalid syntax");
						}
						else {
							// bug 2194492 : allow space in password
							password = line.substring("PASS".length() + 1);
							try {
								session = ExchangeSessionFactory.getInstance(userName, password);
								logConnection("LOGON", userName);
								sendOK("PASS");
								state = State.AUTHENTICATED;
							}
							catch (SocketException e) {
								logConnection("FAILED", userName);
								// can not send error to client after a socket exception
								log.warn("{}", BundleMessage.formatLog("LOG_CLIENT_CLOSED_CONNECTION"));
							}
							catch (Exception e) {
								log.error("", e);
								sendERR(e);
							}
						}
					}
					else if ("CAPA".equalsIgnoreCase(command)) {
						sendOK("Capability list follows");
						printCapabilities();
					}
					else if (state != State.AUTHENTICATED) {
						sendERR("Invalid state not authenticated");
					}
					else {
						// load messages (once)
						if (messages == null) {
							messages = session.getAllMessageUidAndSize("INBOX");
						}
						if ("STAT".equalsIgnoreCase(command)) {
							sendOK(messages.size() + " " + getTotalMessagesLength());
						}
						else if ("NOOP".equalsIgnoreCase(command)) {
							sendOK("");
						}
						else if ("LIST".equalsIgnoreCase(command)) {
							if (tokens.hasMoreTokens()) {
								String token = tokens.nextToken();
								try {
									int messageNumber = Integer.parseInt(token);
									ExchangeSession.Message message = messages.get(messageNumber - 1);
									sendOK("" + messageNumber + ' ' + message.size);
								}
								catch (NumberFormatException | IndexOutOfBoundsException e) {
									sendERR("Invalid message index: " + token);
								}
							}
							else {
								sendOK(messages.size() + " messages (" + getTotalMessagesLength() + " octets)");
								printList();
							}
						}
						else if ("UIDL".equalsIgnoreCase(command)) {
							if (tokens.hasMoreTokens()) {
								String token = tokens.nextToken();
								try {
									int messageNumber = Integer.parseInt(token);
									sendOK(messageNumber + " " + messages.get(messageNumber - 1).getUid());
								}
								catch (NumberFormatException | IndexOutOfBoundsException e) {
									sendERR("Invalid message index: " + token);
								}
							}
							else {
								sendOK(messages.size() + " messages (" + getTotalMessagesLength() + " octets)");
								printUidList();
							}
						}
						else if ("RETR".equalsIgnoreCase(command)) {
							if (tokens.hasMoreTokens()) {
								try {
									int messageNumber = Integer.parseInt(tokens.nextToken()) - 1;
									ExchangeSession.Message message = messages.get(messageNumber);

									// load big messages in a separate thread
									os.write("+OK ".getBytes(StandardCharsets.US_ASCII));
									os.flush();
									MessageLoadThread.loadMimeMessage(message, os);
									sendClient("");

									DoubleDotOutputStream doubleDotOutputStream = new DoubleDotOutputStream(os);
									IOUtil.write(message.getRawInputStream(), doubleDotOutputStream);
									doubleDotOutputStream.close();
									if (Settings.getBooleanProperty("mt.ews.popMarkReadOnRetr")) {
										message.markRead();
									}
								}
								catch (SocketException e) {
									// can not send error to client after a socket
									// exception
									log.warn(BundleMessage.formatLog("LOG_CLIENT_CLOSED_CONNECTION"));
								}
								catch (Exception e) {
									log.error("{}", new BundleMessage("LOG_ERROR_RETRIEVING_MESSAGE"), e);
									sendERR("error retrieving message " + e + ' ' + e.getMessage());
								}
							}
							else {
								sendERR("invalid message index");
							}
						}
						else if ("DELE".equalsIgnoreCase(command)) {
							if (tokens.hasMoreTokens()) {
								ExchangeSession.Message message;
								try {
									int messageNumber = Integer.parseInt(tokens.nextToken()) - 1;
									message = messages.get(messageNumber);
									message.moveToTrash();
									sendOK("DELETE");
								}
								catch (NumberFormatException | IndexOutOfBoundsException e) {
									sendERR("invalid message index");
								}
							}
							else {
								sendERR("invalid message index");
							}
						}
						else if ("TOP".equalsIgnoreCase(command)) {
							int message = 0;
							try {
								message = Integer.parseInt(tokens.nextToken());
								int lines = Integer.parseInt(tokens.nextToken());
								ExchangeSession.Message m = messages.get(message - 1);
								sendOK("");
								DoubleDotOutputStream doubleDotOutputStream = new DoubleDotOutputStream(os);
								IOUtil.write(m.getRawInputStream(), new TopOutputStream(doubleDotOutputStream, lines));
								doubleDotOutputStream.close();
							}
							catch (NumberFormatException e) {
								sendERR("invalid command");
							}
							catch (IndexOutOfBoundsException e) {
								sendERR("invalid message index: " + message);
							}
							catch (Exception e) {
								sendERR("error retreiving top of messages");
							}
						}
						else if ("RSET".equalsIgnoreCase(command)) {
							sendOK("RSET");
						}
						else {
							sendERR("unknown command");
						}
					}

				}
				else {
					sendERR("unknown command");
				}

				os.flush();
			}
		}
		catch (SocketException e) {
			log.debug("{}", new BundleMessage("LOG_CONNECTION_CLOSED"));
		}
		catch (Exception e) {
			log.error("", e);
			try {
				sendERR(e.getMessage());
			}
			catch (IOException e2) {
				log.debug("{}", new BundleMessage("LOG_EXCEPTION_SENDING_ERROR_TO_CLIENT"), e2);
			}
		}
		finally {
			close();
		}
	}

	protected void sendOK(String message) throws IOException {
		sendClient("+OK ", message);
	}

	protected void sendERR(Exception e) throws IOException {
		String message = e.getMessage();
		if (message == null) {
			message = e.toString();
		}
		sendERR(message);
	}

	protected void sendERR(String message) throws IOException {
		sendClient("-ERR ", message.replaceAll("\\n", " "));
	}

	/**
	 * Фильтр для ограничения количества строк вывода до максимального количества строк
	 * тела после заголовка
	 */
	private static final class TopOutputStream extends FilterOutputStream {

		protected enum State {

			START, CR, CRLF, CRLFCR, BODY

		}

		private int maxLines;

		private State state = State.START;

		private TopOutputStream(OutputStream os, int maxLines) {
			super(os);
			this.maxLines = maxLines;
		}

		@Override
		public void write(int b) throws IOException {
			if (state != State.BODY || maxLines > 0) {
				super.write(b);
			}
			if (state == State.BODY) {
				if (b == '\n') {
					maxLines--;
				}
			}
			else if (state == State.START) {
				if (b == '\r') {
					state = State.CR;
				}
			}
			else if (state == State.CR) {
				if (b == '\n') {
					state = State.CRLF;
				}
				else {
					state = State.START;
				}
			}
			else if (state == State.CRLF) {
				if (b == '\r') {
					state = State.CRLFCR;
				}
				else {
					state = State.START;
				}
			}
			else if (state == State.CRLFCR) {
				if (b == '\n') {
					state = State.BODY;
				}
				else {
					state = State.START;
				}
			}
		}

	}

}
