/*
DIT
 */
package ru.mos.mostech.ews.caldav;

import ru.mos.mostech.ews.AbstractConnection;
import ru.mos.mostech.ews.AbstractServer;
import ru.mos.mostech.ews.Settings;

import java.net.Socket;

/**
 * Calendar server, handle HTTP Caldav requests.
 */
public class CaldavServer extends AbstractServer {

	/**
	 * Default HTTP Caldav port
	 */
	public static final int DEFAULT_PORT = 80;

	/**
	 * Create a ServerSocket to listen for connections. Start the thread.
	 * @param port pop listen port, 80 if not defined (0)
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