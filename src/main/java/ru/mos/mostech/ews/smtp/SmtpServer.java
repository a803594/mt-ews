/*
DIT
 */
package ru.mos.mostech.ews.smtp;

import ru.mos.mostech.ews.AbstractConnection;
import ru.mos.mostech.ews.AbstractServer;
import ru.mos.mostech.ews.Settings;

import java.net.Socket;

/**
 * SMTP server, handle message send requests.
 */
public class SmtpServer extends AbstractServer {
    /**
     * Default SMTP Caldav port
     */
    public static final int DEFAULT_PORT = 25;

    /**
     * Create a ServerSocket to listen for connections.
     * Start the thread.
     *
     * @param port smtp port
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
