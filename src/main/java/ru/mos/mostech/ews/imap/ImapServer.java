/*
DIT
 */
package ru.mos.mostech.ews.imap;


import ru.mos.mostech.ews.AbstractConnection;
import ru.mos.mostech.ews.AbstractServer;
import ru.mos.mostech.ews.Settings;

import java.net.Socket;

/**
 * Pop3 server
 */
public class ImapServer extends AbstractServer {
    /**
     * Default IMAP port
     */
    public static final int DEFAULT_PORT = 143;

    /**
     * Create a ServerSocket to listen for connections.
     * Start the thread.
     *
     * @param port imap listen port, 143 if not defined (0)
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
