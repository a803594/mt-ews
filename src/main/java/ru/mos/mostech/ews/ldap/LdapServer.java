/*
DIT
 */
package ru.mos.mostech.ews.ldap;

import ru.mos.mostech.ews.AbstractConnection;
import ru.mos.mostech.ews.AbstractServer;
import ru.mos.mostech.ews.Settings;

import java.net.Socket;

/**
 * Сервер LDAP, обработка запросов к каталогу LDAP.
 */
public class LdapServer extends AbstractServer {

	/**
	 * Порт по умолчанию для LDAP
	 */
	public static final int DEFAULT_PORT = 389;

	/**
	 * Создайте ServerSocket для прослушивания подключений. Запустите поток.
	 * @param port порт для прослушивания pop, 389, если не определён (0)
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