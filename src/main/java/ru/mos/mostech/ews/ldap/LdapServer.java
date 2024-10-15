/*
DIT
 */
package ru.mos.mostech.ews.ldap;

import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.AbstractConnection;
import ru.mos.mostech.ews.AbstractServer;
import ru.mos.mostech.ews.Settings;

import java.net.Socket;

/**
 * LDAP server, handle LDAP directory requests.
 */

@Slf4j
public class LdapServer extends AbstractServer {
    /**
     * Default LDAP port
     */
    public static final int DEFAULT_PORT = 389;

    /**
     * Create a ServerSocket to listen for connections.
     * Start the thread.
     *
     * @param port pop listen port, 389 if not defined (0)
     */
    public LdapServer(int port) {
        super(LdapServer.class.getName(), port, LdapServer.DEFAULT_PORT);
        nosslFlag = Settings.getBooleanProperty("mt.ews.ssl.nosecureldap");
    }

    @Override
    public String getProtocolName() {
        return "LDAP";
    }

    @Override
    public AbstractConnection createConnectionHandler(Socket clientSocket) {
        return new LdapConnection(clientSocket);
    }
}