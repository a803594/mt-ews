/*
DIT
 */
package ru.mos.mostech.ews.caldav;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import ru.mos.mostech.ews.AbstractConnection;
import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.MosTechEws;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.exception.*;
import ru.mos.mostech.ews.exchange.ExchangeSession;
import ru.mos.mostech.ews.exchange.ExchangeSessionFactory;
import ru.mos.mostech.ews.exchange.ICSBufferedReader;
import ru.mos.mostech.ews.exchange.XMLStreamUtil;
import ru.mos.mostech.ews.exchange.dav.WebdavExchangeSession;
import ru.mos.mostech.ews.http.URIUtil;
import ru.mos.mostech.ews.ui.tray.MosTechEwsTray;
import ru.mos.mostech.ews.util.IOUtil;
import ru.mos.mostech.ews.util.StringUtil;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Обработать соединение caldav.
 */
@Slf4j
@SuppressWarnings({ "java:S3776", "java:S6541" })
public class CaldavConnection extends AbstractConnection {

	/**
	 * Максимальное время поддержания соединения в секундах
	 */
	protected static final int MAX_KEEP_ALIVE_TIME = 300;

	public static final String PUBLIC = "public";

	public static final String INBOX = "inbox";

	public static final String OUTBOX = "outbox";

	public static final String USERS = "users";

	public static final String END_VCALENDAR = "END:VCALENDAR";

	public static final String GETCONTENTTYPE = "getcontenttype";

	public static final String D_GETCONTENTTYPE = "D:getcontenttype";

	public static final String TEXT_CALENDAR_COMPONENT_VEVENT = "text/calendar; component=vevent";

	public static final String GETETAG = "getetag";

	public static final String D_GETETAG = "D:getetag";

	public static final String RESOURCETYPE = "resourcetype";

	public static final String D_RESOURCETYPE = "D:resourcetype";

	public static final String DISPLAYNAME = "displayname";

	public static final String D_DISPLAYNAME = "D:displayname";

	public static final String D_COLLECTION = "<D:collection/>";

	public static final String PRINCIPALS_USERS = "/principals/users/";

	public static final String GETCTAG = "getctag";

	public static final String CS_GETCTAG = "CS:getctag";

	public static final String CS_HTTP_CALENDARSERVER_ORG_NS = "CS=\"http://calendarserver.org/ns/\"";

	public static final String CALENDAR = "calendar";

	public static final String C_SUPPORTED_CALENDAR_COMPONENT_SET = "C:supported-calendar-component-set";

	public static final String USERS_SLASH = "/users/";

	public static final String E_ADDRESSBOOK_HOME_SET = "E:addressbook-home-set";

	public static final String COMP_FILTER = "comp-filter";

	protected boolean closed;

	/**
	 * пользовательская кодировка URL-пути для iCal 5
	 */
	public static final BitSet ICAL_ALLOWED_ABS_PATH = new BitSet(256);

	static {
		ICAL_ALLOWED_ABS_PATH.or(URIUtil.allowed_abs_path);
		ICAL_ALLOWED_ABS_PATH.clear('@');
	}

	static String encodePath(CaldavRequest request, String path) {
		if (request.isIcal5()) {
			return URIUtil.encode(path, ICAL_ALLOWED_ABS_PATH);
		}
		else {
			return URIUtil.encodePath(path);
		}
	}

	/**
	 * Инициализация потоков и запуск потока.
	 * @param clientSocket Сокет клиента Caldav
	 */
	public CaldavConnection(Socket clientSocket) {
		super(CaldavConnection.class.getSimpleName(), clientSocket, "UTF-8");
	}

	private Map<String, String> parseHeaders() throws IOException {
		HashMap<String, String> headers = new HashMap<>();
		String line;
		while ((line = readClient()) != null && !line.isEmpty()) {
			int index = line.indexOf(':');
			if (index <= 0) {
				log.warn("Invalid header: {}", line);
				throw new MosTechEwsException("EXCEPTION_INVALID_HEADER");
			}
			headers.put(line.substring(0, index).toLowerCase(), line.substring(index + 1).trim());
		}
		return headers;
	}

	private String getContent(String contentLength) throws IOException {
		if (contentLength == null || contentLength.isEmpty()) {
			return null;
		}
		else {
			int size;
			try {
				size = Integer.parseInt(contentLength);
			}
			catch (NumberFormatException e) {
				throw new MosTechEwsException("EXCEPTION_INVALID_CONTENT_LENGTH", contentLength);
			}
			String content = in.readContentAsString(size);
			log.debug("< {}", content);
			return content;
		}
	}

	private void setSocketTimeout(String keepAliveValue) throws IOException {
		if (keepAliveValue != null && !keepAliveValue.isEmpty()) {
			int keepAlive;
			try {
				keepAlive = Integer.parseInt(keepAliveValue);
			}
			catch (NumberFormatException e) {
				throw new MosTechEwsException("EXCEPTION_INVALID_KEEPALIVE", keepAliveValue);
			}
			if (keepAlive > MAX_KEEP_ALIVE_TIME) {
				keepAlive = MAX_KEEP_ALIVE_TIME;
			}
			client.setSoTimeout(keepAlive * 1000);
			MosTechEwsTray.debug(new BundleMessage("LOG_SET_SOCKET_TIMEOUT", keepAlive));
		}
	}

	@SuppressWarnings({ "java:S3776", "java:S6541", "java:S135" })
	@Override
	public void doRun() {
		String line;
		StringTokenizer tokens;

		try {
			while (!closed) {
				line = readClient();
				// unable to read line, connection closed ?
				if (line == null) {
					break;
				}
				tokens = new StringTokenizer(line);
				String command = tokens.nextToken();
				Map<String, String> headers = parseHeaders();
				String encodedPath = StringUtil.encodePlusSign(tokens.nextToken());
				String path = URIUtil.decode(encodedPath);
				String content = getContent(headers.get("content-length"));
				setSocketTimeout(headers.get("keep-alive"));
				// client requested connection close
				closed = "close".equals(headers.get("connection"));
				if ("OPTIONS".equals(command)) {
					sendOptions();
				}
				else if (!headers.containsKey("authorization")) {
					sendUnauthorized();
				}
				else {
					decodeCredentials(headers.get("authorization"));
					// need to check session on each request, credentials may have changed
					// or session expired
					checkSessionForEachRequest(command, path, headers, content);
				}

				os.flush();
				MosTechEwsTray.resetIcon();
			}
		}
		catch (SocketTimeoutException e) {
			MosTechEwsTray.debug(new BundleMessage("LOG_CLOSE_CONNECTION_ON_TIMEOUT"));
		}
		catch (SocketException e) {
			MosTechEwsTray.debug(new BundleMessage("LOG_CONNECTION_CLOSED"));
		}
		catch (Exception e) {
			if (!(e instanceof HttpNotFoundException)) {
				MosTechEwsTray.log(e);
			}
			try {
				sendErr(e);
			}
			catch (IOException e2) {
				MosTechEwsTray.debug(new BundleMessage("LOG_EXCEPTION_SENDING_ERROR_TO_CLIENT"), e2);
			}
		}
		finally {
			close();
		}
		MosTechEwsTray.resetIcon();
	}

	private void checkSessionForEachRequest(String command, String path, Map<String, String> headers, String content)
			throws IOException {
		try {
			session = ExchangeSessionFactory.getInstance(userName, password);
			logConnection("LOGON", userName);
			handleRequest(command, path, headers, content);
		}
		catch (MosTechEwsAuthenticationException e) {
			logConnection("FAILED", userName);
			if (Settings.getBooleanProperty("mt.ews.enableKerberos")) {
				// authentication failed in Kerberos mode => not available
				throw new HttpServerErrorException("Kerberos authentication failed");
			}
			else {
				sendUnauthorized();
			}
		}
	}

	/**
	 * Обработать запрос caldav.
	 * @param command HTTP команда
	 * @param path путь запроса
	 * @param headers карта HTTP заголовков
	 * @param body тело запроса
	 * @throws IOException в случае ошибки
	 */
	private void handleRequest(String command, String path, Map<String, String> headers, String body)
			throws IOException {
		CaldavRequest request = new CaldavRequest(command, path, headers, body);
		if (request.isOptions()) {
			sendOptions();
		}
		else if (request.isPropFind() && request.isRoot()) {
			sendRoot(request);
		}
		else if (request.isGet() && request.isRoot()) {
			sendGetRoot();
		}
		else if (request.isPath(1, "principals")) {
			handlePrincipals(request);
		}
		else if (request.isPath(1, USERS)) {
			if (request.isPropFind() && request.isPathLength(3)) {
				sendUserRoot(request);
			}
			else {
				handleFolderOrItem(request);
			}
		}
		else if (request.isPath(1, PUBLIC)) {
			handleFolderOrItem(request);
		}
		else if (request.isPath(1, "directory")) {
			sendDirectory(request);
		}
		else if (request.isPath(1, ".well-known")) {
			sendWellKnown();
		}
		else {
			sendNotFound(request);
		}
	}

	private void handlePrincipals(CaldavRequest request) throws IOException {
		if (request.isPath(2, USERS)) {
			if (request.isPropFind() && request.isPathLength(4)) {
				sendPrincipal(request, USERS, URIUtil.decode(request.getPathElement(3)));
				// send back principal on search
			}
			else if (request.isReport() && request.isPathLength(3)) {
				sendPrincipal(request, USERS, session.getEmail());
				// iCal current-user-principal request
			}
			else if (request.isPropFind() && request.isPathLength(3)) {
				sendPrincipalsFolder(request);
			}
			else {
				sendNotFound(request);
			}
		}
		else if (request.isPath(2, PUBLIC)) {
			StringBuilder prefixBuffer = new StringBuilder(PUBLIC);
			for (int i = 3; i < request.getPathLength() - 1; i++) {
				prefixBuffer.append('/').append(request.getPathElement(i));
			}
			sendPrincipal(request, URIUtil.decode(prefixBuffer.toString()), URIUtil.decode(request.getLastPath()));
		}
		else {
			sendNotFound(request);
		}
	}

	private void handleFolderOrItem(CaldavRequest request) throws IOException {
		String lastPath = StringUtil.xmlDecode(request.getLastPath());
		// folder requests
		if (request.isPropFind() && INBOX.equals(lastPath)) {
			sendInbox(request);
		}
		else if (request.isPropFind() && OUTBOX.equals(lastPath)) {
			sendOutbox(request);
		}
		else if (request.isPost() && OUTBOX.equals(lastPath)) {
			if (request.isFreeBusy()) {
				sendFreeBusy(request.getBody());
			}
			else {
				int status = session.sendEvent(request.getBody());
				sendHttpResponse(status);
			}
		}
		else if (request.isPropFind()) {
			sendFolderOrItem(request);
		}
		else if (request.isPropPatch()) {
			patchCalendar(request);
		}
		else if (request.isReport()) {
			reportItems(request);
			// event requests
		}
		else if (request.isPut()) {
			String etag = request.getHeader("if-match");
			String noneMatch = request.getHeader("if-none-match");
			ExchangeSession.ItemResult itemResult = session.createOrUpdateItem(request.getFolderPath(), lastPath,
					request.getBody(), etag, noneMatch);
			sendHttpResponse(itemResult.status, buildEtagHeader(request, itemResult), null, "", true);

		}
		else if (request.isDelete()) {
			if (request.getFolderPath().endsWith(INBOX)) {
				session.processItem(request.getFolderPath(), lastPath);
			}
			else {
				session.deleteItem(request.getFolderPath(), lastPath);
			}
			sendHttpResponse(HttpStatus.SC_OK);
		}
		else if (request.isGet()) {
			if (request.path.endsWith("/")) {
				// GET request on a folder => build ics content of all folder events
				String folderPath = request.getFolderPath();
				ExchangeSession.Folder folder = session.getFolder(folderPath);
				if (folder.isContact()) {
					List<ExchangeSession.Contact> contacts = session.getAllContacts(folderPath,
							!isOldCardavClient(request));
					ChunkedResponse response = new ChunkedResponse(this, HttpStatus.SC_OK, "text/vcard;charset=UTF-8");

					for (ExchangeSession.Contact contact : contacts) {
						contact.setVCardVersion(getVCardVersion(request));
						String contactBody = contact.getBody();
						if (contactBody != null) {
							response.append(contactBody);
							response.append("\n");
						}
					}
					response.close();

				}
				else if (folder.isCalendar() || folder.isTask()) {
					List<ExchangeSession.Event> events = session.getAllEvents(folderPath);
					ChunkedResponse response = new ChunkedResponse(this, HttpStatus.SC_OK,
							"text/calendar;charset=UTF-8");
					response.append("BEGIN:VCALENDAR\r\n");
					response.append("VERSION:2.0\r\n");
					response.append("PRODID:-//mos.ru/NONSGML MT-EWS Calendar V1.1//EN\r\n");
					response.append("METHOD:PUBLISH\r\n");

					for (ExchangeSession.Event event : events) {
						String icsContent = StringUtil.getToken(event.getBody(), "BEGIN:VTIMEZONE", END_VCALENDAR);
						if (icsContent != null) {
							response.append("BEGIN:VTIMEZONE");
							response.append(icsContent);
						}
						else {
							icsContent = StringUtil.getToken(event.getBody(), "BEGIN:VEVENT", END_VCALENDAR);
							if (icsContent != null) {
								response.append("BEGIN:VEVENT");
								response.append(icsContent);
							}
						}
					}
					response.append(END_VCALENDAR);
					response.close();
				}
				else {
					sendHttpResponse(HttpStatus.SC_OK, buildEtagHeader(folder.getEtag()), "text/html", (byte[]) null,
							true);
				}
			}
			else {
				ExchangeSession.Item item = session.getItem(request.getFolderPath(), lastPath);
				if (item instanceof ExchangeSession.Contact contact) {
					contact.setVCardVersion(getVCardVersion(request));
				}
				sendHttpResponse(HttpStatus.SC_OK, buildEtagHeader(item.getEtag()), item.getContentType(),
						item.getBody(), true);
			}
		}
		else if (request.isHead()) {
			// test event
			ExchangeSession.Item item = session.getItem(request.getFolderPath(), lastPath);
			sendHttpResponse(HttpStatus.SC_OK, buildEtagHeader(item.getEtag()), item.getContentType(), (byte[]) null,
					true);
		}
		else if (request.isMkCalendar()) {
			HashMap<String, String> properties = new HashMap<>();
			int status = session.createCalendarFolder(request.getFolderPath(), properties);
			sendHttpResponse(status, null);
		}
		else if (request.isMove()) {
			String destinationUrl = request.getHeader("destination");
			session.moveItem(request.path, URIUtil.decode(new URL(destinationUrl).getPath()));
			sendHttpResponse(HttpStatus.SC_CREATED, null);
		}
		else {
			sendNotFound(request);
		}

	}

	private boolean isOldCardavClient(CaldavRequest request) {
		return request.isUserAgent("iOS/");
	}

	private String getVCardVersion(CaldavRequest request) {
		if (isOldCardavClient(request)) {
			return "3.0";
		}
		else {
			return "4.0";
		}
	}

	private HashMap<String, String> buildEtagHeader(CaldavRequest request, ExchangeSession.ItemResult itemResult) {
		HashMap<String, String> headers = null;
		if (itemResult.etag != null) {
			headers = new HashMap<>();
			headers.put("ETag", itemResult.etag);
		}
		if (itemResult.itemName != null) {
			if (headers == null) {
				headers = new HashMap<>();
			}
			headers.put("Location", buildEventPath(request, itemResult.itemName));
		}
		return headers;
	}

	private Map<String, String> buildEtagHeader(String etag) {
		if (etag != null) {
			HashMap<String, String> etagHeader = new HashMap<>();
			etagHeader.put("ETag", etag);
			return etagHeader;
		}
		else {
			return Map.of();
		}
	}

	private void appendContactsResponses(CaldavResponse response, CaldavRequest request,
			List<ExchangeSession.Contact> contacts) throws IOException {
		if (contacts != null) {
			int count = 0;
			for (ExchangeSession.Contact contact : contacts) {
				MosTechEwsTray.debug(new BundleMessage("LOG_LISTING_ITEM", ++count, contacts.size()));
				MosTechEwsTray.switchIcon();
				appendItemResponse(response, request, contact);
			}
		}
	}

	private void appendEventsResponses(CaldavResponse response, CaldavRequest request,
			List<ExchangeSession.Event> events) throws IOException {
		if (events != null) {
			int size = events.size();
			int count = 0;
			for (ExchangeSession.Event event : events) {
				MosTechEwsTray.debug(new BundleMessage("LOG_LISTING_ITEM", ++count, size));
				MosTechEwsTray.switchIcon();
				appendItemResponse(response, request, event);
			}
		}
	}

	private String buildEventPath(CaldavRequest request, String itemName) {
		StringBuilder eventPath = new StringBuilder();
		eventPath.append(encodePath(request, request.getFolderPath()));
		if (eventPath.charAt(eventPath.length() - 1) != '/') {
			eventPath.append('/');
		}
		eventPath.append(URIUtil.encodeWithinQuery(StringUtil.xmlEncode(itemName)));
		return eventPath.toString();
	}

	private void appendItemResponse(CaldavResponse response, CaldavRequest request, ExchangeSession.Item item)
			throws IOException {
		response.startResponse(buildEventPath(request, item.getName()));
		response.startPropstat();
		if (request.hasProperty("calendar-data") && item instanceof ExchangeSession.Event) {
			response.appendCalendarData(item.getBody());
		}
		if (request.hasProperty("address-data") && item instanceof ExchangeSession.Contact contact) {
			contact.setVCardVersion(getVCardVersion(request));
			response.appendContactData(item.getBody());
		}
		if (request.hasProperty(GETCONTENTTYPE)) {
			if (item instanceof ExchangeSession.Event) {
				response.appendProperty(D_GETCONTENTTYPE, TEXT_CALENDAR_COMPONENT_VEVENT);
			}
			else if (item instanceof ExchangeSession.Contact) {
				response.appendProperty(D_GETCONTENTTYPE, "text/vcard");
			}
		}
		if (request.hasProperty(GETETAG)) {
			response.appendProperty(D_GETETAG, item.getEtag());
		}
		if (request.hasProperty(RESOURCETYPE)) {
			response.appendProperty(D_RESOURCETYPE);
		}
		if (request.hasProperty(DISPLAYNAME)) {
			response.appendProperty(D_DISPLAYNAME, StringUtil.xmlEncode(item.getName()));
		}
		response.endPropStatOK();
		response.endResponse();
	}

	/**
	 * Добавить объект папки к ответу Caldav.
	 * @param response Ответ Caldav
	 * @param request Запрос Caldav
	 * @param folder объект папки
	 * @param subFolder путь к календарной папке относительно пути запроса
	 * @throws IOException при ошибке
	 */
	private void appendFolderOrItem(CaldavResponse response, CaldavRequest request, ExchangeSession.Folder folder,
			String subFolder) throws IOException {
		response.startResponse(encodePath(request, request.getPath(subFolder)));
		response.startPropstat();

		if (request.hasProperty(RESOURCETYPE)) {
			if (folder.isContact()) {
				response.appendProperty(D_RESOURCETYPE, D_COLLECTION + "<E:addressbook/>");
			}
			else if (folder.isCalendar() || folder.isTask()) {
				response.appendProperty(D_RESOURCETYPE, D_COLLECTION + "<C:calendar/>");
			}
			else {
				response.appendProperty(D_RESOURCETYPE, D_COLLECTION);
			}

		}
		if (request.hasProperty("owner")) {
			if (USERS.equals(request.getPathElement(1))) {
				response.appendHrefProperty("D:owner", PRINCIPALS_USERS + request.getPathElement(2));
			}
			else {
				response.appendHrefProperty("D:owner", "/principals" + request.getPath());
			}
		}
		if (request.hasProperty(GETCONTENTTYPE)) {
			if (folder.isContact()) {
				response.appendProperty(D_GETCONTENTTYPE, "text/x-vcard");
			}
			else if (folder.isCalendar()) {
				response.appendProperty(D_GETCONTENTTYPE, TEXT_CALENDAR_COMPONENT_VEVENT);
			}
			else if (folder.isTask()) {
				response.appendProperty(D_GETCONTENTTYPE, "text/calendar; component=vtodo");
			}
		}
		if (request.hasProperty(GETETAG)) {
			response.appendProperty(D_GETETAG, folder.getEtag());
		}
		if (request.hasProperty(GETCTAG)) {
			response.appendProperty(CS_GETCTAG, CS_HTTP_CALENDARSERVER_ORG_NS,
					IOUtil.encodeBase64AsString(folder.getCtag()));
		}
		if (request.hasProperty(DISPLAYNAME)) {
			if (subFolder == null || subFolder.isEmpty()) {
				// use i18n calendar name as display name
				String displayname = request.getLastPath();
				if (CALENDAR.equals(displayname)) {
					displayname = folder.getDisplayName();
				}
				response.appendProperty(D_DISPLAYNAME, displayname);
			}
			else {
				response.appendProperty(D_DISPLAYNAME, subFolder);
			}
		}
		if (request.hasProperty("calendar-description")) {
			response.appendProperty("C:calendar-description", "");
		}
		if (request.hasProperty("supported-calendar-component-set")) {
			if (folder.isCalendar()) {
				response.appendProperty(C_SUPPORTED_CALENDAR_COMPONENT_SET,
						"<C:comp name=\"VEVENT\"/><C:comp name=\"VTODO\"/>");
			}
			else if (folder.isTask()) {
				response.appendProperty(C_SUPPORTED_CALENDAR_COMPONENT_SET, "<C:comp name=\"VTODO\"/>");
			}
		}

		if (request.hasProperty("current-user-privilege-set")) {
			response.appendProperty("D:current-user-privilege-set", "<D:privilege><D:read/><D:write/></D:privilege>");
		}

		response.endPropStatOK();
		response.endResponse();
	}

	/**
	 * Добавить объект календарного входящего потока к ответу Caldav.
	 * @param response Ответ Caldav
	 * @param request Запрос Caldav
	 * @param subFolder Путь к папке входящих сообщений относительно пути запроса
	 * @throws IOException при ошибке
	 */
	private void appendInbox(CaldavResponse response, CaldavRequest request, String subFolder) throws IOException {
		String ctag = "0";
		String etag = "0";
		String folderPath = request.getFolderPath(subFolder);
		// do not try to access inbox on shared calendar
		if (!session.isSharedFolder(folderPath)) {
			try {
				ExchangeSession.Folder folder = session.getFolder(folderPath);
				ctag = IOUtil.encodeBase64AsString(folder.getCtag());
				etag = IOUtil.encodeBase64AsString(folder.getEtag());
			}
			catch (HttpResponseException e) {
				// unauthorized access, probably an inbox on shared calendar
				MosTechEwsTray.debug(new BundleMessage("LOG_ACCESS_FORBIDDEN", folderPath, e.getMessage()));
			}
		}
		response.startResponse(encodePath(request, request.getPath(subFolder)));
		response.startPropstat();

		if (request.hasProperty(RESOURCETYPE)) {
			response.appendProperty(D_RESOURCETYPE,
					D_COLLECTION + "<C:schedule-inbox xmlns:C=\"urn:ietf:params:xml:ns:caldav\"/>");
		}
		if (request.hasProperty(GETCONTENTTYPE)) {
			response.appendProperty(D_GETCONTENTTYPE, TEXT_CALENDAR_COMPONENT_VEVENT);
		}
		if (request.hasProperty(GETCTAG)) {
			response.appendProperty(CS_GETCTAG, CS_HTTP_CALENDARSERVER_ORG_NS, ctag);
		}
		if (request.hasProperty(GETETAG)) {
			response.appendProperty(D_GETETAG, etag);
		}
		if (request.hasProperty(DISPLAYNAME)) {
			response.appendProperty(D_DISPLAYNAME, INBOX);
		}
		response.endPropStatOK();
		response.endResponse();
	}

	/**
	 * Добавить объект календаря в папку "Исходящие" в ответ Caldav.
	 * @param response Ответ Caldav
	 * @param request Запрос Caldav
	 * @param subFolder Путь к папке "Исходящие", относительный к пути запроса
	 * @throws IOException в случае ошибки
	 */
	private void appendOutbox(CaldavResponse response, CaldavRequest request, String subFolder) throws IOException {
		response.startResponse(encodePath(request, request.getPath(subFolder)));
		response.startPropstat();

		if (request.hasProperty(RESOURCETYPE)) {
			response.appendProperty(D_RESOURCETYPE,
					D_COLLECTION + "<C:schedule-outbox xmlns:C=\"urn:ietf:params:xml:ns:caldav\"/>");
		}
		if (request.hasProperty(GETCTAG)) {
			response.appendProperty(CS_GETCTAG, CS_HTTP_CALENDARSERVER_ORG_NS, "0");
		}
		if (request.hasProperty(GETETAG)) {
			response.appendProperty(D_GETETAG, "0");
		}
		if (request.hasProperty(DISPLAYNAME)) {
			response.appendProperty(D_DISPLAYNAME, OUTBOX);
		}
		response.endPropStatOK();
		response.endResponse();
	}

	/**
	 * Отправить простой HTML-ответ на GET /.
	 * @throws IOException при ошибке
	 */
	public void sendGetRoot() throws IOException {
		String buffer = "Connected to MT-EWS" + MosTechEws.getCurrentVersion() + "<br/>" + "UserName: " + userName
				+ "<br/>" + "Email: " + session.getEmail() + "<br/>";
		sendHttpResponse(HttpStatus.SC_OK, null, "text/html;charset=UTF-8", buffer, true);
	}

	/**
	 * Отправить ответ в почтовый ящик на запрос.
	 * @param request Запрос Caldav
	 * @throws IOException при ошибке
	 */
	private void sendInbox(CaldavRequest request) throws IOException {
		CaldavResponse response = new CaldavResponse(this, HttpStatus.SC_MULTI_STATUS);
		response.startMultistatus();
		appendInbox(response, request, null);
		// do not try to access inbox on shared calendar
		if (!session.isSharedFolder(request.getFolderPath(null)) && request.getDepth() == 1 && !request.isLightning()) {
			try {
				MosTechEwsTray.debug(new BundleMessage("LOG_SEARCHING_CALENDAR_MESSAGES"));
				List<ExchangeSession.Event> events = session.getEventMessages(request.getFolderPath());
				MosTechEwsTray.debug(new BundleMessage("LOG_FOUND_CALENDAR_MESSAGES", events.size()));
				appendEventsResponses(response, request, events);
			}
			catch (HttpResponseException e) {
				// unauthorized access, probably an inbox on shared calendar
				MosTechEwsTray
					.debug(new BundleMessage("LOG_ACCESS_FORBIDDEN", request.getFolderPath(), e.getMessage()));
			}
		}
		response.endMultistatus();
		response.close();
	}

	/**
	 * Отправить ответ из исходящих сообщений для запроса.
	 * @param request Запрос Caldav
	 * @throws IOException в случае ошибки
	 */
	private void sendOutbox(CaldavRequest request) throws IOException {
		CaldavResponse response = new CaldavResponse(this, HttpStatus.SC_MULTI_STATUS);
		response.startMultistatus();
		appendOutbox(response, request, null);
		response.endMultistatus();
		response.close();
	}

	/**
	 * Отправить ответ на запрос календаря.
	 * @param request Запрос Caldav
	 * @throws IOException при ошибке
	 */
	private void sendFolderOrItem(CaldavRequest request) throws IOException {
		String folderPath = request.getFolderPath();
		// process request before sending response to avoid sending headers twice on error
		ExchangeSession.Folder folder = session.getFolder(folderPath);
		List<ExchangeSession.Contact> contacts = null;
		List<ExchangeSession.Event> events = null;
		List<ExchangeSession.Folder> folderList = null;
		if (request.getDepth() == 1) {
			if (folder.isContact()) {
				contacts = session.getAllContacts(folderPath, !isOldCardavClient(request));
			}
			else if (folder.isCalendar() || folder.isTask()) {
				events = session.getAllEvents(folderPath);
				if (!folderPath.startsWith("/public")) {
					folderList = session.getSubCalendarFolders(folderPath, false);
				}
			}
		}

		CaldavResponse response = new CaldavResponse(this, HttpStatus.SC_MULTI_STATUS);
		response.startMultistatus();
		appendFolderOrItem(response, request, folder, null);
		if (request.getDepth() == 1) {
			if (folder.isContact()) {
				appendContactsResponses(response, request, contacts);
			}
			else if (folder.isCalendar() || folder.isTask()) {
				appendEventsResponses(response, request, events);
				// Send sub folders for multi-calendar support under iCal, except for
				// public folders
				if (folderList != null) {
					for (ExchangeSession.Folder subFolder : folderList) {
						appendFolderOrItem(response, request, subFolder,
								subFolder.getFolderPath().substring(subFolder.getFolderPath().indexOf('/') + 1));
					}
				}
			}
		}
		response.endMultistatus();
		response.close();
	}

	/**
	 * Фейковый ответ PROPPATCH для запроса.
	 * @param request Запрос Caldav
	 * @throws IOException в случае ошибки
	 */
	private void patchCalendar(CaldavRequest request) throws IOException {
		String displayname = request.getProperty(DISPLAYNAME);
		String folderPath = request.getFolderPath();
		if (displayname != null) {
			String targetPath = request.getParentFolderPath() + '/' + displayname;
			if (!targetPath.equals(folderPath)) {
				session.moveFolder(folderPath, targetPath);
			}
		}
		CaldavResponse response = new CaldavResponse(this, HttpStatus.SC_MULTI_STATUS);
		response.startMultistatus();
		// ical calendar folder proppatch
		if (request.hasProperty("calendar-color") || request.hasProperty("calendar-order")) {
			response.startPropstat();
			if (request.hasProperty("calendar-color")) {
				response.appendProperty("x1:calendar-color", "x1=\"http://apple.com/ns/ical/\"", null);
			}
			if (request.hasProperty("calendar-order")) {
				response.appendProperty("x1:calendar-order", "x1=\"http://apple.com/ns/ical/\"", null);
			}
			response.endPropStatOK();
		}
		response.endMultistatus();
		response.close();
	}

	private String getEventFileNameFromPath(String path) {
		int index = path.lastIndexOf('/');
		if (index < 0) {
			return null;
		}
		else {
			return StringUtil.xmlDecode(path.substring(index + 1));
		}
	}

	/**
	 * Отчет о предметах, указанных в запросе.
	 * @param request Запрос Caldav
	 * @throws IOException в случае ошибки
	 */
	private void reportItems(CaldavRequest request) throws IOException {
		String folderPath = request.getFolderPath();
		List<ExchangeSession.Event> events;
		List<String> notFound = new ArrayList<>();

		CaldavResponse response = new CaldavResponse(this, HttpStatus.SC_MULTI_STATUS);
		response.startMultistatus();
		if (request.isMultiGet()) {
			int count = 0;
			int total = request.getHrefs().size();
			for (String href : request.getHrefs()) {
				MosTechEwsTray.debug(new BundleMessage("LOG_REPORT_ITEM", ++count, total));
				MosTechEwsTray.switchIcon();
				String eventName = getEventFileNameFromPath(href);
				try {
					// ignore cases for Sunbird
					if (eventName != null && !eventName.isEmpty() && !INBOX.equals(eventName)
							&& !CALENDAR.equals(eventName)) {
						ExchangeSession.Item item = getItem(request, folderPath, eventName);
						if (!eventName.equals(item.getName())) {
							MosTechEwsTray.warn(new BundleMessage("LOG_MESSAGE",
									"wrong item name requested " + eventName + " received " + item.getName()));
							// force item name to requested value
							item.setItemName(eventName);
						}
						appendItemResponse(response, request, item);
					}
				}
				catch (SocketException e) {
					// rethrow SocketException (client closed connection)
					throw e;
				}
				catch (Exception e) {
					log.debug(e.getMessage(), e);
					MosTechEwsTray.warn(new BundleMessage("LOG_ITEM_NOT_AVAILABLE", eventName, href));
					notFound.add(href);
				}
			}
		}
		else if (request.isPath(1, USERS) && request.isPath(3, INBOX)) {
			events = session.getEventMessages(request.getFolderPath());
			appendEventsResponses(response, request, events);
		}
		else {
			ExchangeSession.Folder folder = session.getFolder(folderPath);
			if (folder.isContact()) {
				List<ExchangeSession.Contact> contacts = session.getAllContacts(folderPath,
						!isOldCardavClient(request));
				appendContactsResponses(response, request, contacts);
			}
			else {
				if (request.vTodoOnly) {
					events = session.searchTasksOnly(request.getFolderPath());
				}
				else if (request.vEventOnly) {
					events = session.searchEventsOnly(request.getFolderPath(), request.timeRangeStart,
							request.timeRangeEnd);
				}
				else {
					events = session.searchEvents(request.getFolderPath(), request.timeRangeStart,
							request.timeRangeEnd);
				}
				appendEventsResponses(response, request, events);
			}
		}

		// send not found events errors
		for (String href : notFound) {
			response.startResponse(encodePath(request, href));
			response.appendPropstatNotFound();
			response.endResponse();
		}
		response.endMultistatus();
		response.close();
	}

	private ExchangeSession.Item getItem(CaldavRequest request, String folderPath, String eventName)
			throws IOException {
		ExchangeSession.Item item;
		try {
			item = session.getItem(folderPath, eventName);
		}
		catch (HttpNotFoundException e) {
			// workaround for Lightning bug
			if (request.isBrokenLightning() && eventName.indexOf('%') >= 0) {
				item = session.getItem(folderPath, URIUtil.decode(StringUtil.encodePlusSign(eventName)));
			}
			else {
				throw e;
			}

		}
		return item;
	}

	/**
	 * Отправить папку принципалов.
	 * @param request Запрос Caldav
	 * @throws IOException при ошибке
	 */
	private void sendPrincipalsFolder(CaldavRequest request) throws IOException {
		CaldavResponse response = new CaldavResponse(this, HttpStatus.SC_MULTI_STATUS);
		response.startMultistatus();
		response.startResponse(encodePath(request, request.getPath()));
		response.startPropstat();

		if (request.hasProperty("current-user-principal")) {
			response.appendHrefProperty("D:current-user-principal",
					encodePath(request, PRINCIPALS_USERS + session.getEmail()));
		}
		response.endPropStatOK();
		response.endResponse();
		response.endMultistatus();
		response.close();
	}

	/**
	 * Отправить ответ пользователя на запрос.
	 * @param request Запрос Caldav
	 * @throws IOException в случае ошибки
	 */
	private void sendUserRoot(CaldavRequest request) throws IOException {
		CaldavResponse response = new CaldavResponse(this, HttpStatus.SC_MULTI_STATUS);
		response.startMultistatus();
		response.startResponse(encodePath(request, request.getPath()));
		response.startPropstat();

		if (request.hasProperty(RESOURCETYPE)) {
			response.appendProperty(D_RESOURCETYPE, D_COLLECTION);
		}
		if (request.hasProperty(DISPLAYNAME)) {
			response.appendProperty(D_DISPLAYNAME, request.getLastPath());
		}
		if (request.hasProperty(GETCTAG)) {
			ExchangeSession.Folder rootFolder = session.getFolder("");
			response.appendProperty(CS_GETCTAG, CS_HTTP_CALENDARSERVER_ORG_NS,
					IOUtil.encodeBase64AsString(rootFolder.getCtag()));
		}
		response.endPropStatOK();
		if (request.getDepth() == 1) {
			appendInbox(response, request, INBOX);
			appendOutbox(response, request, OUTBOX);
			appendFolderOrItem(response, request, session.getFolder(request.getFolderPath(CALENDAR)), CALENDAR);
			appendFolderOrItem(response, request, session.getFolder(request.getFolderPath("contacts")), "contacts");
		}
		response.endResponse();
		response.endMultistatus();
		response.close();
	}

	/**
	 * Отправить ответ caldav на / запрос.
	 * @param request Запрос Caldav
	 * @throws IOException при ошибке
	 */
	private void sendRoot(CaldavRequest request) throws IOException {
		CaldavResponse response = new CaldavResponse(this, HttpStatus.SC_MULTI_STATUS);
		response.startMultistatus();
		response.startResponse("/");
		response.startPropstat();

		if (request.hasProperty("principal-collection-set")) {
			response.appendHrefProperty("D:principal-collection-set", PRINCIPALS_USERS);
		}
		if (request.hasProperty(DISPLAYNAME)) {
			response.appendProperty(D_DISPLAYNAME, "ROOT");
		}
		if (request.hasProperty(RESOURCETYPE)) {
			response.appendProperty(D_RESOURCETYPE, D_COLLECTION);
		}
		if (request.hasProperty("current-user-principal")) {
			response.appendHrefProperty("D:current-user-principal",
					encodePath(request, PRINCIPALS_USERS + session.getEmail()));
		}
		response.endPropStatOK();
		response.endResponse();
		if (request.depth == 1) {
			// iPhone workaround: send calendar subfolder
			response.startResponse(USERS_SLASH + session.getEmail() + "/calendar");
			response.startPropstat();
			if (request.hasProperty(RESOURCETYPE)) {
				response.appendProperty(D_RESOURCETYPE,
						D_COLLECTION + "<C:calendar xmlns:C=\"urn:ietf:params:xml:ns:caldav\"/>");
			}
			if (request.hasProperty(DISPLAYNAME)) {
				response.appendProperty(D_DISPLAYNAME, session.getEmail());
			}
			if (request.hasProperty("supported-calendar-component-set")) {
				response.appendProperty(C_SUPPORTED_CALENDAR_COMPONENT_SET, "<C:comp name=\"VEVENT\"/>");
			}
			response.endPropStatOK();
			response.endResponse();

			response.startResponse("/users");
			response.startPropstat();
			if (request.hasProperty(DISPLAYNAME)) {
				response.appendProperty(D_DISPLAYNAME, USERS);
			}
			if (request.hasProperty(RESOURCETYPE)) {
				response.appendProperty(D_RESOURCETYPE, D_COLLECTION);
			}
			response.endPropStatOK();
			response.endResponse();

			response.startResponse("/principals");
			response.startPropstat();
			if (request.hasProperty(DISPLAYNAME)) {
				response.appendProperty(D_DISPLAYNAME, "principals");
			}
			if (request.hasProperty(RESOURCETYPE)) {
				response.appendProperty(D_RESOURCETYPE, D_COLLECTION);
			}
			response.endPropStatOK();
			response.endResponse();
		}
		response.endMultistatus();
		response.close();
	}

	/**
	 * Отправить ответ caldav для запроса /directory/.
	 * @param request Запрос Caldav
	 * @throws IOException в случае ошибки
	 */
	private void sendDirectory(CaldavRequest request) throws IOException {
		CaldavResponse response = new CaldavResponse(this, HttpStatus.SC_MULTI_STATUS);
		response.startMultistatus();
		response.startResponse("/directory/");
		response.startPropstat();
		if (request.hasProperty("current-user-privilege-set")) {
			response.appendProperty("D:current-user-privilege-set", "<D:privilege><D:read/></D:privilege>");
		}
		response.endPropStatOK();
		response.endResponse();
		response.endMultistatus();
		response.close();
	}

	/**
	 * Отправить ответ caldav для запроса /.well-known/.
	 * @throws IOException при ошибке
	 */
	public void sendWellKnown() throws IOException {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Location", "/");
		sendHttpResponse(HttpStatus.SC_MOVED_PERMANENTLY, headers);
	}

	/**
	 * Отправить ответ по принципалу Caldav.
	 * @param request Запрос Caldav
	 * @param prefix префикс принципала (пользователи или публичный)
	 * @param principal имя принципала (адрес электронной почты для пользователей)
	 * @throws IOException при ошибке
	 */
	private void sendPrincipal(CaldavRequest request, String prefix, String principal) throws IOException {
		// actual principal is email address
		String actualPrincipal = principal;
		if (USERS.equals(prefix) && (principal.equalsIgnoreCase(session.getAlias())
				|| (principal.equalsIgnoreCase(session.getAliasFromLogin())))) {
			actualPrincipal = session.getEmail();
		}

		CaldavResponse response = new CaldavResponse(this, HttpStatus.SC_MULTI_STATUS);
		response.startMultistatus();
		response.startResponse(encodePath(request, "/principals/" + prefix + '/' + principal));
		response.startPropstat();

		if (request.hasProperty("principal-URL") && request.isIcal5()) {
			response.appendHrefProperty("D:principal-URL",
					encodePath(request, "/principals/" + prefix + '/' + actualPrincipal));
		}

		if (request.hasProperty("calendar-home-set")) {
			if (USERS.equals(prefix)) {
				response.appendHrefProperty("C:calendar-home-set",
						encodePath(request, USERS_SLASH + actualPrincipal + "/calendar/"));
			}
			else {
				response.appendHrefProperty("C:calendar-home-set",
						encodePath(request, '/' + prefix + '/' + actualPrincipal));
			}
		}

		if (request.hasProperty("calendar-user-address-set") && USERS.equals(prefix)) {
			response.appendHrefProperty("C:calendar-user-address-set", "mailto:" + actualPrincipal);
		}

		if (request.hasProperty("addressbook-home-set")) {
			if (request.isUserAgent("Address%20Book") || request.isUserAgent("Darwin")) {
				response.appendHrefProperty(E_ADDRESSBOOK_HOME_SET,
						encodePath(request, '/' + prefix + '/' + actualPrincipal + '/'));
			}
			else if (USERS.equals(prefix)) {
				response.appendHrefProperty(E_ADDRESSBOOK_HOME_SET,
						encodePath(request, USERS_SLASH + actualPrincipal + "/contacts/"));
			}
			else {
				response.appendHrefProperty(E_ADDRESSBOOK_HOME_SET,
						encodePath(request, '/' + prefix + '/' + actualPrincipal + '/'));
			}
		}

		if (USERS.equals(prefix)) {
			if (request.hasProperty("schedule-inbox-URL")) {
				response.appendHrefProperty("C:schedule-inbox-URL",
						encodePath(request, USERS_SLASH + actualPrincipal + "/inbox/"));
			}

			if (request.hasProperty("schedule-outbox-URL")) {
				response.appendHrefProperty("C:schedule-outbox-URL",
						encodePath(request, USERS_SLASH + actualPrincipal + "/outbox/"));
			}
		}
		else {
			// public calendar, send root href as inbox url (always empty) for Lightning
			if (request.isLightning() && request.hasProperty("schedule-inbox-URL")) {
				response.appendHrefProperty("C:schedule-inbox-URL", "/");
			}
			// send user outbox
			if (request.hasProperty("schedule-outbox-URL")) {
				response.appendHrefProperty("C:schedule-outbox-URL",
						encodePath(request, USERS_SLASH + session.getEmail() + "/outbox/"));
			}
		}

		if (request.hasProperty(DISPLAYNAME)) {
			response.appendProperty(D_DISPLAYNAME, actualPrincipal);
		}
		if (request.hasProperty(RESOURCETYPE)) {
			response.appendProperty(D_RESOURCETYPE, "<D:collection/><D:principal/>");
		}
		if (request.hasProperty("supported-report-set")) {
			response.appendProperty("D:supported-report-set",
					"<D:supported-report><D:report><C:calendar-multiget/></D:report></D:supported-report>");
		}
		response.endPropStatOK();
		response.endResponse();
		response.endMultistatus();
		response.close();
	}

	/**
	 * Отправить ответ о занятости для тела запроса.
	 * @param body тело запроса
	 * @throws IOException при ошибке
	 */
	public void sendFreeBusy(String body) throws IOException {
		HashMap<String, String> valueMap = new HashMap<>();
		ArrayList<String> attendees = new ArrayList<>();
		HashMap<String, String> attendeeKeyMap = new HashMap<>();
		ICSBufferedReader reader = new ICSBufferedReader(new StringReader(body));
		String line;
		String key;
		while ((line = reader.readLine()) != null) {
			int index = line.indexOf(':');
			if (index <= 0) {
				throw new MosTechEwsException("EXCEPTION_INVALID_REQUEST", body);
			}
			String fullkey = line.substring(0, index);
			String value = line.substring(index + 1);
			int semicolonIndex = fullkey.indexOf(';');
			if (semicolonIndex > 0) {
				key = fullkey.substring(0, semicolonIndex);
			}
			else {
				key = fullkey;
			}
			if ("ATTENDEE".equals(key)) {
				attendees.add(value);
				attendeeKeyMap.put(value, fullkey);
			}
			else {
				valueMap.put(key, value);
			}
		}
		// get freebusy for each attendee
		HashMap<String, ExchangeSession.FreeBusy> freeBusyMap = new HashMap<>();
		for (String attendee : attendees) {
			ExchangeSession.FreeBusy freeBusy = session.getFreebusy(attendee, valueMap.get("DTSTART"),
					valueMap.get("DTEND"));
			if (freeBusy != null) {
				freeBusyMap.put(attendee, freeBusy);
			}
		}
		CaldavResponse response = new CaldavResponse(this, HttpStatus.SC_OK);
		response.startScheduleResponse();

		for (Map.Entry<String, ExchangeSession.FreeBusy> entry : freeBusyMap.entrySet()) {
			String attendee = entry.getKey();
			response.startRecipientResponse(attendee);

			StringBuilder ics = new StringBuilder();
			ics.append("BEGIN:VCALENDAR")
				.append((char) 13)
				.append((char) 10)
				.append("VERSION:2.0")
				.append((char) 13)
				.append((char) 10)
				.append("PRODID:-//mos.ru/NONSGML MT-EWS Calendar V1.1//EN")
				.append((char) 13)
				.append((char) 10)
				.append("METHOD:REPLY")
				.append((char) 13)
				.append((char) 10)
				.append("BEGIN:VFREEBUSY")
				.append((char) 13)
				.append((char) 10)
				.append("DTSTAMP:")
				.append(valueMap.get("DTSTAMP"))
				.append((char) 13)
				.append((char) 10)
				.append("ORGANIZER:")
				.append(valueMap.get("ORGANIZER"))
				.append((char) 13)
				.append((char) 10)
				.append("DTSTART:")
				.append(valueMap.get("DTSTART"))
				.append((char) 13)
				.append((char) 10)
				.append("DTEND:")
				.append(valueMap.get("DTEND"))
				.append((char) 13)
				.append((char) 10)
				.append("UID:")
				.append(valueMap.get("UID"))
				.append((char) 13)
				.append((char) 10)
				.append(attendeeKeyMap.get(attendee))
				.append(':')
				.append(attendee)
				.append((char) 13)
				.append((char) 10);
			entry.getValue().appendTo(ics);
			ics.append("END:VFREEBUSY").append((char) 13).append((char) 10).append(END_VCALENDAR);
			response.appendCalendarData(ics.toString());
			response.endRecipientResponse();

		}
		response.endScheduleResponse();
		response.close();

	}

	/**
	 * Отправить Http-ошибку в ответ на исключение
	 * @param e исключение
	 * @throws IOException при ошибке
	 */
	public void sendErr(Exception e) throws IOException {
		String message = e.getMessage();
		if (message == null) {
			message = e.toString();
		}
		if (e instanceof HttpNotFoundException) {
			sendErr(HttpStatus.SC_NOT_FOUND, message);
		}
		else if (e instanceof HttpPreconditionFailedException) {
			sendErr(HttpStatus.SC_PRECONDITION_FAILED, message);
		}
		else {
			// workaround for Lightning bug: sleep for 1 second
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
			sendErr(HttpStatus.SC_SERVICE_UNAVAILABLE, message);
		}
	}

	/**
	 * Отправка 404 не найдено для неизвестного запроса.
	 * @param request Запрос Caldav
	 * @throws IOException в случае ошибки
	 */
	private void sendNotFound(CaldavRequest request) throws IOException {
		BundleMessage message = new BundleMessage("LOG_UNSUPPORTED_REQUEST", request);
		MosTechEwsTray.warn(message);
		sendErr(HttpStatus.SC_NOT_FOUND, message.format());
	}

	/**
	 * Отправить статус ошибки Http и сообщение.
	 * @param status Статус Http
	 * @param message сообщение об ошибке
	 * @throws IOException при ошибке
	 */
	public void sendErr(int status, String message) throws IOException {
		sendHttpResponse(status, null, "text/plain;charset=UTF-8", message, false);
	}

	/**
	 * Отправить ответ OPTIONS.
	 * @throws IOException в случае ошибки
	 */
	public void sendOptions() throws IOException {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Allow", "OPTIONS, PROPFIND, HEAD, GET, REPORT, PROPPATCH, PUT, DELETE, POST");
		sendHttpResponse(HttpStatus.SC_OK, headers);
	}

	/**
	 * Отправить ответ с кодом 401 Неавторизован.
	 * @throws IOException при ошибке
	 */
	public void sendUnauthorized() throws IOException {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("WWW-Authenticate", "Basic realm=\"" + BundleMessage.format("UI_MT_EWS_GATEWAY") + '\"');
		sendHttpResponse(HttpStatus.SC_UNAUTHORIZED, headers, null, (byte[]) null, true);
	}

	/**
	 * Отправить Http ответ с заданным статусом.
	 * @param status Http статус
	 * @throws IOException при ошибке
	 */
	public void sendHttpResponse(int status) throws IOException {
		sendHttpResponse(status, null, null, (byte[]) null, true);
	}

	/**
	 * Отправить Http ответ с заданным статусом и заголовками.
	 * @param status Http статус
	 * @param headers Http заголовки
	 * @throws IOException в случае ошибки
	 */
	public void sendHttpResponse(int status, Map<String, String> headers) throws IOException {
		sendHttpResponse(status, headers, null, (byte[]) null, true);
	}

	/**
	 * Отправить HTTP-ответ с указанным статусом в режиме кусочной передачи.
	 * @param status HTTP статус
	 * @param contentType MIME тип содержимого
	 * @throws IOException при ошибке
	 */
	public void sendChunkedHttpResponse(int status, String contentType) throws IOException {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Transfer-Encoding", "chunked");
		sendHttpResponse(status, headers, contentType, (byte[]) null, true);
	}

	/**
	 * Отправить Http ответ с заданным статусом, заголовками, типом контента и содержимым.
	 * Закройте соединение, если keepAlive false
	 * @param status Http статус
	 * @param headers Http заголовки
	 * @param contentType MIME тип контента
	 * @param content тело ответа в виде строки
	 * @param keepAlive поддерживать соединение открытым
	 * @throws IOException при ошибке
	 */
	public void sendHttpResponse(int status, Map<String, String> headers, String contentType, String content,
			boolean keepAlive) throws IOException {
		sendHttpResponse(status, headers, contentType, content.getBytes(StandardCharsets.UTF_8), keepAlive);
	}

	/**
	 * Отправить Http-ответ с указанным статусом, заголовками, типом контента и
	 * содержимым. Закрыть соединение, если keepAlive равно false
	 * @param status Http статус
	 * @param headers Http заголовки
	 * @param contentType MIME тип контента
	 * @param content тело ответа в виде массива байтов
	 * @param keepAlive поддерживать соединение открытым
	 * @throws IOException при ошибке
	 */
	public void sendHttpResponse(int status, Map<String, String> headers, String contentType, byte[] content,
			boolean keepAlive) throws IOException {
		sendClient("HTTP/1.1 " + status + ' ' + EnglishReasonPhraseCatalog.INSTANCE.getReason(status, Locale.ENGLISH));
		if (status != HttpStatus.SC_UNAUTHORIZED) {
			sendClient("Server: MT-EWS Gateway " + MosTechEws.getCurrentVersion());
			String scheduleMode;
			// enable automatic scheduling over EWS, can be disabled
			if (Settings.getBooleanProperty("mt.ews.caldavAutoSchedule", true)
					&& !(session instanceof WebdavExchangeSession)) {
				scheduleMode = "calendar-auto-schedule";
			}
			else {
				scheduleMode = "calendar-schedule";
			}
			sendClient("DAV: 1, calendar-access, " + scheduleMode + ", calendarserver-private-events, addressbook");
			SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
			// force GMT timezone
			formatter.setTimeZone(ExchangeSession.GMT_TIMEZONE);
			String now = formatter.format(new Date());
			sendClient("Date: " + now);
			sendClient("Expires: " + now);
			sendClient("Cache-Control: private, max-age=0");
		}
		if (headers != null) {
			for (Map.Entry<String, String> header : headers.entrySet()) {
				sendClient(header.getKey() + ": " + header.getValue());
			}
		}
		if (contentType != null) {
			sendClient("Content-Type: " + contentType);
		}
		closed = closed || !keepAlive;
		sendClient("Connection: " + (closed ? "close" : "keep-alive"));
		if (content != null && content.length > 0) {
			sendClient("Content-Length: " + content.length);
		}
		else if (headers == null || !"chunked".equals(headers.get("Transfer-Encoding"))) {
			sendClient("Content-Length: 0");
		}
		sendClient("");
		if (content != null && content.length > 0) {
			// full debug trace
			if (log.isDebugEnabled()) {
				log.debug("> {}", new String(content, StandardCharsets.UTF_8));
			}
			sendClient(content);
		}
	}

	/**
	 * Декодировать HTTP учетные данные
	 * @param authorization значение заголовка http авторизации
	 * @throws IOException если учетные данные неверны
	 */
	private void decodeCredentials(String authorization) throws IOException {
		int index = authorization.indexOf(' ');
		if (index > 0) {
			String mode = authorization.substring(0, index).toLowerCase();
			if (!"basic".equals(mode)) {
				throw new MosTechEwsException("EXCEPTION_UNSUPPORTED_AUTHORIZATION_MODE", mode);
			}
			String encodedCredentials = authorization.substring(index + 1);
			String decodedCredentials = IOUtil.decodeBase64AsString(encodedCredentials);
			index = decodedCredentials.indexOf(':');
			if (index > 0) {
				userName = decodedCredentials.substring(0, index);
				password = decodedCredentials.substring(index + 1);
			}
			else {
				throw new MosTechEwsException("EXCEPTION_INVALID_CREDENTIALS");
			}
		}
		else {
			throw new MosTechEwsException("EXCEPTION_INVALID_CREDENTIALS");
		}

	}

	protected static class CaldavRequest {

		protected final String command;

		@Getter
		protected final String path;

		protected final String[] pathElements;

		protected final Map<String, String> headers;

		@Getter
		protected int depth;

		@Getter
		protected final String body;

		protected final HashMap<String, String> properties = new HashMap<>();

		protected HashSet<String> hrefs;

		protected boolean isMultiGet;

		protected String timeRangeStart;

		protected String timeRangeEnd;

		protected boolean vTodoOnly;

		protected boolean vEventOnly;

		protected CaldavRequest(String command, String path, Map<String, String> headers, String body)
				throws IOException {
			this.command = command;
			this.path = path.replace("//", "/");
			pathElements = this.path.split("/");
			this.headers = headers;
			buildDepth();
			this.body = body;

			if (isPropFind() || isReport() || isMkCalendar() || isPropPatch()) {
				parseXmlBody();
			}
		}

		public boolean isOptions() {
			return "OPTIONS".equals(command);
		}

		public boolean isPropFind() {
			return "PROPFIND".equals(command);
		}

		public boolean isPropPatch() {
			return "PROPPATCH".equals(command);
		}

		public boolean isReport() {
			return "REPORT".equals(command);
		}

		public boolean isGet() {
			return "GET".equals(command);
		}

		public boolean isHead() {
			return "HEAD".equals(command);
		}

		public boolean isPut() {
			return "PUT".equals(command);
		}

		public boolean isPost() {
			return "POST".equals(command);
		}

		public boolean isDelete() {
			return "DELETE".equals(command);
		}

		public boolean isMkCalendar() {
			return "MKCALENDAR".equals(command);
		}

		public boolean isMove() {
			return "MOVE".equals(command);
		}

		/**
		 * Проверьте, является ли этот запрос запросом на папку.
		 * @return true, если это запрос на папку (не событие)
		 */
		public boolean isFolder() {
			return path.endsWith("/") || isPropFind() || isReport() || isPropPatch() || isOptions() || isPost();
		}

		public boolean isRoot() {
			return (pathElements.length == 0 || pathElements.length == 1);
		}

		public boolean isPathLength(int length) {
			return pathElements.length == length;
		}

		public int getPathLength() {
			return pathElements.length;
		}

		public String getPath(String subFolder) {
			String folderPath;
			if (subFolder == null || subFolder.isEmpty()) {
				folderPath = path;
			}
			else if (path.endsWith("/")) {
				folderPath = path + subFolder;
			}
			else {
				folderPath = path + '/' + subFolder;
			}
			if (folderPath.endsWith("/")) {
				return folderPath;
			}
			else {
				return folderPath + '/';
			}
		}

		/**
		 * Проверить, является ли элемент пути по индексу значением
		 * @param index индекс элемента пути
		 * @param value значение пути
		 * @return true, если элемент пути по индексу является значением
		 */
		public boolean isPath(int index, String value) {
			return value != null && value.equals(getPathElement(index));
		}

		protected String getPathElement(int index) {
			if (index < pathElements.length) {
				return pathElements[index];
			}
			else {
				return null;
			}
		}

		public String getLastPath() {
			return getPathElement(getPathLength() - 1);
		}

		protected boolean isBrokenHrefEncoding() {
			return isUserAgent("DAVKit/3") || isUserAgent("eM Client/3") || isBrokenLightning();
		}

		protected boolean isBrokenLightning() {
			return isUserAgent("Lightning/1.0b2");
		}

		protected boolean isLightning() {
			return isUserAgent("Lightning/") || isUserAgent("Thunderbird/");
		}

		protected boolean isIcal5() {
			return isUserAgent("CoreDAV/") || isUserAgent("iOS/")
			// iCal 6
					|| isUserAgent("Mac OS X/10.8");
		}

		protected boolean isUserAgent(String key) {
			String userAgent = headers.get("user-agent");
			return userAgent != null && userAgent.contains(key);
		}

		public boolean isFreeBusy() {
			return body != null && body.contains("VFREEBUSY");
		}

		protected void buildDepth() {
			String depthValue = headers.get("depth");
			if ("infinity".equalsIgnoreCase(depthValue)) {
				depth = Integer.MAX_VALUE;
			}
			else if (depthValue != null) {
				try {
					depth = Integer.parseInt(depthValue);
				}
				catch (NumberFormatException e) {
					MosTechEwsTray.warn(new BundleMessage("LOG_INVALID_DEPTH", depthValue));
				}
			}
		}

		public String getHeader(String headerName) {
			return headers.get(headerName);
		}

		protected void parseXmlBody() throws IOException {
			if (body == null) {
				throw new MosTechEwsException("EXCEPTION_INVALID_CALDAV_REQUEST", "Missing body");
			}
			XMLStreamReader streamReader = null;
			try {
				streamReader = XMLStreamUtil.createXMLStreamReader(body);
				while (streamReader.hasNext()) {
					streamReader.next();
					if (XMLStreamUtil.isStartTag(streamReader)) {
						String tagLocalName = streamReader.getLocalName();
						if ("prop".equals(tagLocalName)) {
							handleProp(streamReader);
						}
						else if ("calendar-multiget".equals(tagLocalName)
								|| "addressbook-multiget".equals(tagLocalName)) {
							isMultiGet = true;
						}
						else if (COMP_FILTER.equals(tagLocalName)) {
							handleCompFilter(streamReader);
						}
						else if ("href".equals(tagLocalName)) {
							if (hrefs == null) {
								hrefs = new HashSet<>();
							}
							if (isBrokenHrefEncoding()) {
								hrefs.add(streamReader.getElementText());
							}
							else {
								hrefs.add(URIUtil.decode(StringUtil.encodePlusSign(streamReader.getElementText())));
							}
						}
					}
				}
			}
			catch (XMLStreamException e) {
				throw new MosTechEwsException("EXCEPTION_INVALID_CALDAV_REQUEST", e.getMessage());
			}
			finally {
				try {
					if (streamReader != null) {
						streamReader.close();
					}
				}
				catch (XMLStreamException e) {
					MosTechEwsTray.error(e);
				}
			}
		}

		public void handleCompFilter(XMLStreamReader reader) throws XMLStreamException {
			while (reader.hasNext() && !XMLStreamUtil.isEndTag(reader, COMP_FILTER)) {
				reader.next();
				if (XMLStreamUtil.isStartTag(reader, COMP_FILTER)) {
					String name = reader.getAttributeValue(null, "name");
					if ("VEVENT".equals(name)) {
						vEventOnly = true;
					}
					else if ("VTODO".equals(name)) {
						vTodoOnly = true;
					}
				}
				else if (XMLStreamUtil.isStartTag(reader, "time-range")) {
					timeRangeStart = reader.getAttributeValue(null, "start");
					timeRangeEnd = reader.getAttributeValue(null, "end");
				}
			}
		}

		public void handleProp(XMLStreamReader reader) throws XMLStreamException {
			while (reader.hasNext() && !XMLStreamUtil.isEndTag(reader, "prop")) {
				reader.next();
				if (XMLStreamUtil.isStartTag(reader)) {
					String tagLocalName = reader.getLocalName();
					String tagText = null;
					if (DISPLAYNAME.equals(tagLocalName) || reader.hasText()) {
						tagText = XMLStreamUtil.getElementText(reader);
					}
					properties.put(tagLocalName, tagText);
				}
			}
		}

		public boolean hasProperty(String propertyName) {
			return properties.containsKey(propertyName);
		}

		public String getProperty(String propertyName) {
			return properties.get(propertyName);
		}

		public boolean isMultiGet() {
			return isMultiGet && hrefs != null;
		}

		public Set<String> getHrefs() {
			return hrefs;
		}

		@Override
		public String toString() {
			return command + ' ' + path + " Depth: " + depth + '\n' + body;
		}

		/**
		 * Получить путь к папке запроса.
		 * @return путь к папке обмена
		 */
		public String getFolderPath() {
			return getFolderPath(null);
		}

		public String getParentFolderPath() {
			int endIndex;
			if (isFolder()) {
				endIndex = getPathLength() - 1;
			}
			else {
				endIndex = getPathLength() - 2;
			}
			return getFolderPath(endIndex, null);
		}

		/**
		 * Получить путь к папке запроса с подпапкой.
		 * @param subFolder путь к подпапке
		 * @return путь к папке
		 */
		public String getFolderPath(String subFolder) {
			int endIndex;
			if (isFolder()) {
				endIndex = getPathLength();
			}
			else {
				endIndex = getPathLength() - 1;
			}
			return getFolderPath(endIndex, subFolder);
		}

		protected String getFolderPath(int endIndex, String subFolder) {

			StringBuilder calendarPath = new StringBuilder();
			for (int i = 0; i < endIndex; i++) {
				if (!getPathElement(i).isEmpty()) {
					calendarPath.append('/').append(getPathElement(i));
				}
			}
			if (subFolder != null && !subFolder.isEmpty()) {
				calendarPath.append('/').append(subFolder);
			}
			if (this.isUserAgent("Address%20Book") || this.isUserAgent("Darwin")) {
				/*
				 * ВНИМАНИЕ - Это временное решение - Если в вашем пути к адресной книге в
				 * публичной папке есть пробелы, то приложение "Адресная книга" просто
				 * игнорирует эту учетную запись. Это временное решение позволяет вам
				 * указать путь, в котором пробелы закодированы как ___. Это заставит
				 * адресную книгу не игнорировать учетную запись и взаимодействовать с
				 * MT-EWS. Здесь мы заменяем ___ в пути на пробелы. Будьте осторожны, если
				 * ваш фактический путь к адресной книге содержит ___, это приведет к
				 * сбою.
				 */
				String result = calendarPath.toString();
				// replace unsupported spaces
				if (result.indexOf(' ') >= 0) {
					result = result.replace("___", " ");
				}
				// replace /addressbook suffix on public folders
				if (result.startsWith("/public")) {
					result = result.replace("/addressbook", "");
				}

				return result;
			}
			else {
				return calendarPath.toString();
			}
		}

	}

	/**
	 * HTTP ответ с чанками.
	 */
	private static class ChunkedResponse {

		@Getter(AccessLevel.PROTECTED)
		private final Writer writer;

		private ChunkedResponse(CaldavConnection caldavConnection, int status, String contentType) throws IOException {
			this.writer = new OutputStreamWriter(new BufferedOutputStream(new OutputStream() {
				@Override
				public void write(byte[] data, int offset, int length) throws IOException {
					caldavConnection.sendClient(Integer.toHexString(length));
					caldavConnection.sendClient(data, offset, length);
					if (log.isDebugEnabled()) {
						log.debug("> {}", new String(data, offset, length, StandardCharsets.UTF_8));
					}
					caldavConnection.sendClient("");
				}

				@Override
				public void write(int b) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void close() throws IOException {
					caldavConnection.sendClient("0");
					caldavConnection.sendClient("");
				}
			}), StandardCharsets.UTF_8);

			caldavConnection.sendChunkedHttpResponse(status, contentType);
		}

		public void append(String data) throws IOException {
			writer.write(data);
		}

		public void close() throws IOException {
			writer.close();
		}

	}

	/**
	 * Обертка для ответа Caldav, содержимое отправляется в режиме кусочков, чтобы
	 * избежать таймаута
	 */
	private static class CaldavResponse extends ChunkedResponse {

		protected CaldavResponse(CaldavConnection caldavConnection, int status) throws IOException {
			super(caldavConnection, status, "text/xml;charset=UTF-8");
			getWriter().write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		}

		public void startMultistatus() throws IOException {
			getWriter().write(
					"<D:multistatus xmlns:D=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:caldav\" xmlns:E=\"urn:ietf:params:xml:ns:carddav\">");
		}

		public void startResponse(String href) throws IOException {
			getWriter().write("<D:response>");
			getWriter().write("<D:href>");
			getWriter().write(StringUtil.xmlEncode(href));
			getWriter().write("</D:href>");
		}

		public void startPropstat() throws IOException {
			getWriter().write("<D:propstat>");
			getWriter().write("<D:prop>");
		}

		public void appendCalendarData(String ics) throws IOException {
			if (ics != null && !ics.isEmpty()) {
				getWriter().write("<C:calendar-data xmlns:C=\"urn:ietf:params:xml:ns:caldav\"");
				getWriter().write(" C:content-type=\"text/calendar\" C:version=\"2.0\">");
				getWriter().write(StringUtil.xmlEncode(ics));
				getWriter().write("</C:calendar-data>");
			}
		}

		public void appendContactData(String vcard) throws IOException {
			if (vcard != null && !vcard.isEmpty()) {
				getWriter().write("<E:address-data>");
				getWriter().write(StringUtil.xmlEncode(vcard));
				getWriter().write("</E:address-data>");
			}
		}

		public void appendHrefProperty(String propertyName, String propertyValue) throws IOException {
			appendProperty(propertyName, null, "<D:href>" + StringUtil.xmlEncode(propertyValue) + "</D:href>");
		}

		public void appendProperty(String propertyName) throws IOException {
			appendProperty(propertyName, null);
		}

		public void appendProperty(String propertyName, String propertyValue) throws IOException {
			appendProperty(propertyName, null, propertyValue);
		}

		public void appendProperty(String propertyName, String namespace, String propertyValue) throws IOException {
			if (propertyValue != null) {
				startTag(propertyName, namespace);
				getWriter().write('>');
				getWriter().write(propertyValue);
				getWriter().write("</");
				getWriter().write(propertyName);
				getWriter().write('>');
			}
			else {
				startTag(propertyName, namespace);
				getWriter().write("/>");
			}
		}

		private void startTag(String propertyName, String namespace) throws IOException {
			getWriter().write('<');
			getWriter().write(propertyName);
			if (namespace != null) {
				getWriter().write(" xmlns:");
				getWriter().write(namespace);
			}
		}

		public void endPropStatOK() throws IOException {
			getWriter().write("</D:prop><D:status>HTTP/1.1 200 OK</D:status></D:propstat>");
		}

		public void appendPropstatNotFound() throws IOException {
			getWriter().write("<D:propstat><D:status>HTTP/1.1 404 Not Found</D:status></D:propstat>");
		}

		public void endResponse() throws IOException {
			getWriter().write("</D:response>");
		}

		public void endMultistatus() throws IOException {
			getWriter().write("</D:multistatus>");
		}

		public void startScheduleResponse() throws IOException {
			getWriter().write("<C:schedule-response xmlns:D=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:caldav\">");
		}

		public void startRecipientResponse(String recipient) throws IOException {
			getWriter().write("<C:response><C:recipient><D:href>");
			getWriter().write(recipient);
			getWriter().write("</D:href></C:recipient><C:request-status>2.0;Success</C:request-status>");
		}

		public void endRecipientResponse() throws IOException {
			getWriter().write("</C:response>");
		}

		public void endScheduleResponse() throws IOException {
			getWriter().write("</C:schedule-response>");
		}

	}

}
