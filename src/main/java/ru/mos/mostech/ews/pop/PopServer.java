/*
DIT
 */
package ru.mos.mostech.ews.pop;

import ru.mos.mostech.ews.AbstractConnection;
import ru.mos.mostech.ews.AbstractServer;
import ru.mos.mostech.ews.Settings;

import java.net.Socket;

/**
 * POP3 сервер
 */
public class PopServer extends AbstractServer {

	/**
	 * Порт по умолчанию для POP
	 */
	public static final int DEFAULT_PORT = 110;

	/**
	 * Создайте ServerSocket для прослушивания соединений. Запустите поток.
	 * @param port порт прослушивания pop, 110 если не определен (0)
	 */
	public PopServer(int port) {
		super(PopServer.class.getName(), port, PopServer.DEFAULT_PORT);
		nosslFlag = Settings.getBooleanProperty("mt.ews.ssl.nosecurepop");
	}

	@Override
	public String getProtocolName() {
		return "POP";
	}

	@Override
	public AbstractConnection createConnectionHandler(Socket clientSocket) {
		return new PopConnection(clientSocket);
	}

}
