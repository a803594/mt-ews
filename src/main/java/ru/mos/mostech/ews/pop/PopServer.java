/*
DIT
 */
package ru.mos.mostech.ews.pop;


import ru.mos.mostech.ews.AbstractConnection;
import ru.mos.mostech.ews.AbstractServer;
import ru.mos.mostech.ews.Settings;

import java.net.Socket;

/**
 * Pop3 server
 */
public class PopServer extends AbstractServer {
    /**
     * Default POP port
     */
    public static final int DEFAULT_PORT = 110;

    /**
     * Create a ServerSocket to listen for connections.
     * Start the thread.
     *
     * @param port pop listen port, 110 if not defined (0)
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
