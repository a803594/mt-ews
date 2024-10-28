/*
DIT
 */
package ru.mos.mostech.ews.caldav;

import ru.mos.mostech.ews.AbstractConnection;
import ru.mos.mostech.ews.AbstractServer;
import ru.mos.mostech.ews.Settings;

import java.net.Socket;

/**
 * Календарный сервер, обрабатывает HTTP Caldav запросы.
 */
public class CaldavServer extends AbstractServer {

	/**
	 * Порт по умолчанию для HTTP Caldav
	 */
	public static final int DEFAULT_PORT = 80;

	/**
	 * Создайте ServerSocket для прослушивания соединений. Запустите поток.
	 * @param port порт прослушивания pop, 80 если не определен (0)
	 */
	public CaldavServer(int port) {
		super(CaldavServer.class.getName(), port, CaldavServer.DEFAULT_PORT);
		nosslFlag = Settings.getBooleanProperty("mt.ews.ssl.nosecurecaldav");
	}

	@Override
	public String getProtocolName() {
		return "CALDAV";
	}

	@Override
	public AbstractConnection createConnectionHandler(Socket clientSocket) {
		return new CaldavConnection(clientSocket);
	}

}