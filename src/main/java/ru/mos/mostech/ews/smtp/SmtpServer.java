/*
DIT
 */
package ru.mos.mostech.ews.smtp;

import ru.mos.mostech.ews.AbstractConnection;
import ru.mos.mostech.ews.AbstractServer;
import ru.mos.mostech.ews.Settings;

import java.net.Socket;

/**
 * SMTP сервер, обрабатывает запросы на отправку сообщений.
 */
public class SmtpServer extends AbstractServer {

	/**
	 * Порт по умолчанию для SMTP Caldav
	 */
	public static final int DEFAULT_PORT = 25;

	/**
	 * Создайте ServerSocket для ожидания подключений. Запустите поток.
	 * @param port порт smtp
	 */
	public SmtpServer(int port) {
		super(SmtpServer.class.getName(), port, SmtpServer.DEFAULT_PORT);
		nosslFlag = Settings.getBooleanProperty("mt.ews.ssl.nosecuresmtp");
	}

	@Override
	public String getProtocolName() {
		return "SMTP";
	}

	@Override
	public AbstractConnection createConnectionHandler(Socket clientSocket) {
		return new SmtpConnection(clientSocket);
	}

}
