/*
DIT
 */
package ru.mos.mostech.ews.imap;

import ru.mos.mostech.ews.AbstractConnection;
import ru.mos.mostech.ews.AbstractServer;
import ru.mos.mostech.ews.Settings;

import java.net.Socket;

/**
 * Сервер Pop3
 */
public class ImapServer extends AbstractServer {

	/**
	 * Порт IMAP по умолчанию
	 */
	public static final int DEFAULT_PORT = 143;

	/**
	 * Создать ServerSocket для прослушивания подключений. Запустить поток.
	 * @param port порт прослушивания imap, 143, если не определен (0)
	 */
	public ImapServer(int port) {
		super(ImapServer.class.getName(), port, ImapServer.DEFAULT_PORT);
		nosslFlag = Settings.getBooleanProperty("mt.ews.ssl.nosecureimap");
	}

	@Override
	public String getProtocolName() {
		return "IMAP";
	}

	@Override
	public AbstractConnection createConnectionHandler(Socket clientSocket) {
		return new ImapConnection(clientSocket);
	}

}
