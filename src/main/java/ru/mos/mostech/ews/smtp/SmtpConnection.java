/*
DIT
 */
package ru.mos.mostech.ews.smtp;

import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.AbstractConnection;
import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.MosTechEws;
import ru.mos.mostech.ews.exception.MosTechEwsException;
import ru.mos.mostech.ews.exchange.DoubleDotInputStream;
import ru.mos.mostech.ews.exchange.ExchangeSessionFactory;
import ru.mos.mostech.ews.util.IOUtil;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Реализация соединения SMTP для Dav Gateway
 */
@Slf4j
public class SmtpConnection extends AbstractConnection {

	public static final String BAD_SEQUENCE_OF_COMMANDS = "503 Bad sequence of commands";

	public static final String UNRECOGNIZED_COMMAND = "500 Unrecognized command";

	/**
	 * Инициализировать потоки и запустить поток.
	 * @param clientSocket SMTP сокет клиента
	 */
	public SmtpConnection(Socket clientSocket) {
		super(SmtpConnection.class.getSimpleName(), clientSocket, null);
	}

	@SuppressWarnings({ "java:S3776", "java:S6541", "java:S135" })
	@Override
	public void doRun() {
		String line;
		StringTokenizer tokens;
		List<String> recipients = new ArrayList<>();

		try {
			ExchangeSessionFactory.checkConfig();
			sendClient("220 MT-EWS " + MosTechEws.getCurrentVersion() + " SMTP ready at " + new Date());
			for (;;) {
				line = readClient();
				// unable to read line, connection closed ?
				if (line == null) {
					break;
				}

				tokens = new StringTokenizer(line);
				if (tokens.hasMoreTokens()) {
					String command = tokens.nextToken();

					if (state == State.LOGIN) {
						// AUTH LOGIN, read userName
						userName = IOUtil.decodeBase64AsString(line);
						sendClient("334 " + IOUtil.encodeBase64AsString("Password:"));
						state = State.PASSWORD;
					}
					else if (state == State.PASSWORD) {
						// AUTH LOGIN, read password
						password = IOUtil.decodeBase64AsString(line);
						authenticate();
					}
					else if ("QUIT".equalsIgnoreCase(command)) {
						sendClient("221 Closing connection");
						break;
					}
					else if ("NOOP".equalsIgnoreCase(command)) {
						sendClient("250 OK");
					}
					else if ("EHLO".equalsIgnoreCase(command)) {
						sendClient("250-" + tokens.nextToken());
						// inform server that AUTH is supported
						// actually it is mandatory (only way to get credentials)
						sendClient("250-AUTH LOGIN PLAIN");
						sendClient("250-8BITMIME");
						sendClient("250 Hello");
					}
					else if ("HELO".equalsIgnoreCase(command)) {
						sendClient("250 Hello");
					}
					else if ("AUTH".equalsIgnoreCase(command)) {
						if (tokens.hasMoreElements()) {
							String authType = tokens.nextToken();
							if ("PLAIN".equalsIgnoreCase(authType) && tokens.hasMoreElements()) {
								decodeCredentials(tokens.nextToken());
								authenticate();
							}
							else if ("LOGIN".equalsIgnoreCase(authType)) {
								if (tokens.hasMoreTokens()) {
									// user name sent on auth line
									userName = IOUtil.decodeBase64AsString(tokens.nextToken());
									sendClient("334 " + IOUtil.encodeBase64AsString("Password:"));
									state = State.PASSWORD;
								}
								else {
									sendClient("334 " + IOUtil.encodeBase64AsString("Username:"));
									state = State.LOGIN;
								}
							}
							else {
								sendClient("451 Error : unknown authentication type");
							}
						}
						else {
							sendClient("451 Error : authentication type not specified");
						}
					}
					else if ("MAIL".equalsIgnoreCase(command)) {
						if (state == State.AUTHENTICATED) {
							state = State.STARTMAIL;
							recipients.clear();
							sendClient("250 Sender OK");
						}
						else if (state == State.INITIAL) {
							sendClient("530 Authentication required");
						}
						else {
							state = State.INITIAL;
							sendClient(BAD_SEQUENCE_OF_COMMANDS);
						}
					}
					else if ("RCPT".equalsIgnoreCase(command)) {
						if (state == State.STARTMAIL || state == State.RECIPIENT) {
							if (line.toUpperCase().startsWith("RCPT TO:")) {
								state = State.RECIPIENT;
								rcptAddRecipient(line, recipients);
								sendClient("250 Recipient OK");
							}
							else {
								sendClient(UNRECOGNIZED_COMMAND);
							}

						}
						else {
							state = State.AUTHENTICATED;
							sendClient(BAD_SEQUENCE_OF_COMMANDS);
						}
					}
					else if ("DATA".equalsIgnoreCase(command)) {
						if (state == State.RECIPIENT) {
							state = State.MAILDATA;
							sendClient("354 Start mail input; end with <CRLF>.<CRLF>");

							dataSendMessage(recipients);

						}
						else {
							state = State.AUTHENTICATED;
							sendClient(BAD_SEQUENCE_OF_COMMANDS);
						}
					}
					else if ("RSET".equalsIgnoreCase(command)) {
						recipients.clear();

						if (state == State.STARTMAIL || state == State.RECIPIENT || state == State.MAILDATA
								|| state == State.AUTHENTICATED) {
							state = State.AUTHENTICATED;
						}
						else {
							state = State.INITIAL;
						}
						sendClient("250 OK Reset");
					}
					else {
						sendClient(UNRECOGNIZED_COMMAND);
					}
				}
				else {
					sendClient(UNRECOGNIZED_COMMAND);
				}

				os.flush();
			}

		}
		catch (SocketException e) {
			log.debug("{}", new BundleMessage("LOG_CONNECTION_CLOSED"));
		}
		catch (Exception e) {
			log.debug(e.getMessage(), e);
			try {
				// append a line feed to avoid thunderbird message drop
				sendClient("421 " + ((e.getMessage() == null) ? e : e.getMessage()) + "\n");
			}
			catch (IOException e2) {
				log.debug("{}", new BundleMessage("LOG_EXCEPTION_SENDING_ERROR_TO_CLIENT"), e2);
			}
		}
		finally {
			close();
		}
	}

	private void dataSendMessage(List<String> recipients) throws IOException {
		try {
			// read message in buffer
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DoubleDotInputStream doubleDotInputStream = new DoubleDotInputStream(in);
			int b;
			while ((b = doubleDotInputStream.read()) >= 0) {
				baos.write(b);
			}
			MimeMessage mimeMessage = new MimeMessage(null, new SharedByteArrayInputStream(baos.toByteArray()));
			session.sendMessage(recipients, mimeMessage);
			state = State.AUTHENTICATED;
			sendClient("250 Queued mail for delivery");
		}
		catch (Exception e) {
			state = State.AUTHENTICATED;
			String error = e.getMessage();
			if (error == null) {
				error = e.toString();
			}
			sendClient("451 Error : " + error.replaceAll("[\\r\\n]", ""));
		}
	}

	private static void rcptAddRecipient(String line, List<String> recipients) throws MosTechEwsException {
		try {
			InternetAddress internetAddress = new InternetAddress(line.substring("RCPT TO:".length()));
			recipients.add(internetAddress.getAddress());
		}
		catch (AddressException e) {
			throw new MosTechEwsException("EXCEPTION_INVALID_RECIPIENT", line);
		}
	}

	/**
	 * Создать аутентифицированную сессию с сервером Exchange
	 * @throws IOException при ошибке
	 */
	protected void authenticate() throws IOException {
		try {
			session = ExchangeSessionFactory.getInstance(userName, password);
			logConnection("LOGON", userName);
			sendClient("235 OK Authenticated");
			state = State.AUTHENTICATED;
		}
		catch (Exception e) {
			logConnection("FAILED", userName);
			String message = e.getMessage();
			if (message == null) {
				message = e.toString();
			}
			message = message.replace("\n", " ");
			sendClient("535 Authentication failed " + message);
			state = State.INITIAL;
		}

	}

	/**
	 * Декодировать учетные данные SMTP
	 * @param encodedCredentials закодированные учетные данные SMTP
	 * @throws IOException если учетные данные неверны
	 */
	protected void decodeCredentials(String encodedCredentials) throws IOException {
		String decodedCredentials = IOUtil.decodeBase64AsString(encodedCredentials);
		int startIndex = decodedCredentials.indexOf((char) 0);
		if (startIndex >= 0) {
			int endIndex = decodedCredentials.indexOf((char) 0, startIndex + 1);
			if (endIndex >= 0) {
				userName = decodedCredentials.substring(startIndex + 1, endIndex);
				password = decodedCredentials.substring(endIndex + 1);
			}
			else {
				throw new MosTechEwsException("EXCEPTION_INVALID_CREDENTIALS");
			}
		}
		else {
			throw new MosTechEwsException("EXCEPTION_INVALID_CREDENTIALS");
		}
	}

}
