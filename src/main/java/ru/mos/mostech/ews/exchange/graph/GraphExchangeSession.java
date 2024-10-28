/*
DIT
 */

package ru.mos.mostech.ews.exchange.graph;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import ru.mos.mostech.ews.exception.HttpNotFoundException;
import ru.mos.mostech.ews.exchange.ExchangeSession;
import ru.mos.mostech.ews.exchange.auth.O365Token;
import ru.mos.mostech.ews.exchange.ews.EwsExchangeSession;
import ru.mos.mostech.ews.exchange.ews.ExtendedFieldURI;
import ru.mos.mostech.ews.exchange.ews.Field;
import ru.mos.mostech.ews.exchange.ews.FieldURI;
import ru.mos.mostech.ews.http.HttpClientAdapter;
import ru.mos.mostech.ews.util.StringUtil;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Реализовать ExchangeSession на основе Microsoft Graph
 */
@Slf4j
public class GraphExchangeSession extends ExchangeSession {

	protected class Folder extends ExchangeSession.Folder {

		public FolderId folderId;

	}

	// special folders https://learn.microsoft.com/en-us/graph/api/resources/mailfolder
	@SuppressWarnings("SpellCheckingInspection")
	public enum WellKnownFolderName {

		archive, deleteditems, calendar, contacts, tasks, drafts, inbox, outbox, sentitems, junkemail, msgfolderroot,
		searchfolders

	}

	protected static class FolderId {

		protected String mailbox;

		protected String id;

		protected String folderClass;

		public FolderId() {
		}

		public FolderId(String mailbox, String id) {
			this.mailbox = mailbox;
			this.id = id;
		}

		public FolderId(String mailbox, WellKnownFolderName wellKnownFolderName) {
			this.mailbox = mailbox;
			this.id = wellKnownFolderName.name();
		}

		public FolderId(String mailbox, WellKnownFolderName wellKnownFolderName, String folderClass) {
			this.mailbox = mailbox;
			this.id = wellKnownFolderName.name();
			this.folderClass = folderClass;
		}

	}

	HttpClientAdapter httpClient;

	O365Token token;

	/**
	 * Список свойств по умолчанию для папки
	 */
	protected static final HashSet<FieldURI> FOLDER_PROPERTIES = new HashSet<>();

	static {
		FOLDER_PROPERTIES.add(Field.get("folderDisplayName"));
		FOLDER_PROPERTIES.add(Field.get("lastmodified"));
		FOLDER_PROPERTIES.add(Field.get("folderclass"));
		FOLDER_PROPERTIES.add(Field.get("ctag"));
		FOLDER_PROPERTIES.add(Field.get("uidNext"));
	}

	public GraphExchangeSession(HttpClientAdapter httpClient, O365Token token, String userName) {
		this.httpClient = httpClient;
		this.token = token;
		this.userName = userName;
	}

	@Override
	public void close() {
		httpClient.close();
	}

	@Override
	public String formatSearchDate(Date date) {
		return null;
	}

	@Override
	protected void buildSessionInfo(URI uri) throws IOException {

	}

	@Override
	public Message createMessage(String folderPath, String messageName, Map<String, String> properties,
			MimeMessage mimeMessage) {
		return null;
	}

	@Override
	public void updateMessage(Message message, Map<String, String> properties) throws IOException {

	}

	@Override
	public void deleteMessage(Message message) throws IOException {

	}

	@Override
	protected byte[] getContent(Message message) throws IOException {
		return new byte[0];
	}

	@Override
	public MessageList searchMessages(String folderName, Set<String> attributes, Condition condition)
			throws IOException {
		return null;
	}

	static class AttributeCondition extends ExchangeSession.AttributeCondition {

		protected AttributeCondition(String attributeName, Operator operator, String value) {
			super(attributeName, operator, value);
		}

		protected FieldURI getFieldURI() {
			FieldURI fieldURI = Field.get(attributeName);
			// check to detect broken field mapping
			// noinspection ConstantConditions
			if (fieldURI == null) {
				throw new IllegalArgumentException("Unknown field: " + attributeName);
			}
			return fieldURI;
		}

		private String convertOperator(Operator operator) {
			if (Operator.IsEqualTo.equals(operator)) {
				return "eq";
			}
			// TODO other operators
			return operator.toString();
		}

		@Override
		public void appendTo(StringBuilder buffer) {
			FieldURI fieldURI = getFieldURI();
			if (Operator.StartsWith.equals(operator)) {
				buffer.append("startswith(")
					.append(getFieldURI().getGraphId())
					.append(",'")
					.append(StringUtil.davSearchEncode(value))
					.append("')");
			}
			else if (fieldURI instanceof ExtendedFieldURI) {
				buffer.append("singleValueExtendedProperties/Any(ep: ep/id eq '")
					.append(getFieldURI().getGraphId())
					.append("' and ep/value ")
					.append(convertOperator(operator))
					.append(" '")
					.append(StringUtil.davSearchEncode(value))
					.append("')");
			}
			else {
				buffer.append(getFieldURI().getGraphId())
					.append(" ")
					.append(convertOperator(operator))
					.append(" '")
					.append(StringUtil.davSearchEncode(value))
					.append("'");
			}
		}

		@Override
		public boolean isMatch(Contact contact) {
			return false;
		}

	}

	@Override
	public MultiCondition and(Condition... condition) {
		return null;
	}

	@Override
	public MultiCondition or(Condition... condition) {
		return null;
	}

	@Override
	public Condition not(Condition condition) {
		return null;
	}

	@Override
	public Condition isEqualTo(String attributeName, String value) {
		return new AttributeCondition(attributeName, Operator.IsEqualTo, value);
	}

	@Override
	public Condition isEqualTo(String attributeName, int value) {
		return null;
	}

	@Override
	public Condition headerIsEqualTo(String headerName, String value) {
		return null;
	}

	@Override
	public Condition gte(String attributeName, String value) {
		return null;
	}

	@Override
	public Condition gt(String attributeName, String value) {
		return null;
	}

	@Override
	public Condition lt(String attributeName, String value) {
		return null;
	}

	@Override
	public Condition lte(String attributeName, String value) {
		return null;
	}

	@Override
	public Condition contains(String attributeName, String value) {
		return null;
	}

	@Override
	public Condition startsWith(String attributeName, String value) {
		return new AttributeCondition(attributeName, Operator.StartsWith, value);
	}

	@Override
	public Condition isNull(String attributeName) {
		return new AttributeCondition(attributeName, Operator.IsEqualTo, "null");
	}

	@Override
	public Condition exists(String attributeName) {
		return null;
	}

	@Override
	public Condition isTrue(String attributeName) {
		return null;
	}

	@Override
	public Condition isFalse(String attributeName) {
		return null;
	}

	@Override
	public List<ExchangeSession.Folder> getSubFolders(String folderPath, Condition condition, boolean recursive)
			throws IOException {
		String baseFolderPath = folderPath;
		if (baseFolderPath.startsWith("/users/")) {
			int index = baseFolderPath.indexOf('/', "/users/".length());
			if (index >= 0) {
				baseFolderPath = baseFolderPath.substring(index + 1);
			}
		}
		List<ExchangeSession.Folder> folders = new ArrayList<>();
		appendSubFolders(folders, baseFolderPath, getFolderId(folderPath), condition, recursive);
		return folders;
	}

	protected void appendSubFolders(List<ExchangeSession.Folder> folders, String parentFolderPath,
			FolderId parentFolderId, Condition condition, boolean recursive) throws IOException {
		int resultCount = 0;

		GraphRequestBuilder httpRequestBuilder = new GraphRequestBuilder().setMethod("GET")
			.setObjectType("mailFolders")
			.setMailbox(parentFolderId.mailbox)
			.setObjectId(parentFolderId.id)
			.setChildType("childFolders")
			.setExpandFields(FOLDER_PROPERTIES);
		log.debug("appendSubFolders " + parentFolderId.mailbox + parentFolderPath);
		if (condition != null && !condition.isEmpty()) {
			StringBuilder filter = new StringBuilder();
			condition.appendTo(filter);
			log.debug("search filter " + filter);
			httpRequestBuilder.setFilter(filter.toString());
		}

		// TODO handle paging
		GraphIterator graphIterator = executeSearchRequest(httpRequestBuilder);

		while (graphIterator.hasNext()) {
			resultCount++;
			Folder folder = buildFolder(graphIterator.next());
			folder.folderId.mailbox = parentFolderId.mailbox;
			if (!parentFolderPath.isEmpty()) {
				if (parentFolderPath.endsWith("/")) {
					folder.setFolderPath(parentFolderPath + folder.getDisplayName());
				}
				else {
					folder.setFolderPath(parentFolderPath + '/' + folder.getDisplayName());
				}
				// TODO folderIdMap?
			}
			else {
				folder.setFolderPath(folder.getDisplayName());
			}
			folders.add(folder);
			if (recursive && folder.isHasChildren()) {
				appendSubFolders(folders, folder.getFolderPath(), folder.folderId, condition, true);
			}
		}

	}

	@Override
	public void sendMessage(MimeMessage mimeMessage) throws IOException, MessagingException {

	}

	@Override
	protected Folder internalGetFolder(String folderPath) throws IOException {
		FolderId folderId = getFolderId(folderPath);

		// base folder get https://graph.microsoft.com/v1.0/me/mailFolders/inbox
		GraphRequestBuilder httpRequestBuilder = new GraphRequestBuilder().setMethod("GET")
			.setObjectType("mailFolders")
			.setMailbox(folderId.mailbox)
			.setObjectId(folderId.id)
			.setExpandFields(FOLDER_PROPERTIES);

		JSONObject jsonResponse = executeJsonRequest(httpRequestBuilder);

		// todo check missing folder
		// throw new HttpNotFoundException("Folder " + folderPath + " not found");

		Folder folder = buildFolder(jsonResponse);
		folder.setFolderPath(folderPath);

		return folder;
	}

	private Folder buildFolder(JSONObject jsonResponse) throws IOException {
		try {
			Folder folder = new Folder();
			folder.folderId = new FolderId();
			folder.folderId.id = jsonResponse.getString("id");
			// TODO: reevaluate folder name encoding over graph
			folder.setDisplayName(EwsExchangeSession.encodeFolderName(jsonResponse.optString("displayName")));
			folder.setCount(jsonResponse.getInt("totalItemCount"));
			folder.setUnreadCount(jsonResponse.getInt("unreadItemCount"));
			// fake recent value
			folder.setRecent(folder.getUnreadCount());
			// hassubs computed from childFolderCount
			folder.setHasChildren(jsonResponse.getInt("childFolderCount") > 0);

			// retrieve property values
			JSONArray singleValueExtendedProperties = jsonResponse.optJSONArray("singleValueExtendedProperties");
			if (singleValueExtendedProperties != null) {
				for (int i = 0; i < singleValueExtendedProperties.length(); i++) {
					JSONObject singleValueProperty = singleValueExtendedProperties.getJSONObject(i);
					String singleValueId = singleValueProperty.getString("id");
					String singleValue = singleValueProperty.getString("value");
					if (Field.get("lastmodified").getGraphId().equals(singleValueId)) {
						folder.setEtag(singleValue);
					}
					else if (Field.get("folderclass").getGraphId().equals(singleValueId)) {
						folder.setFolderClass(singleValue);
					}
					else if (Field.get("uidNext").getGraphId().equals(singleValueId)) {
						folder.setUidNext(Long.parseLong(singleValue));
					}
					else if (Field.get("ctag").getGraphId().equals(singleValueId)) {
						folder.setCtag(singleValue);
					}

				}
			}

			return folder;
		}
		catch (JSONException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	/**
	 * Вычисляет folderId из folderName
	 * @param folderPath имя папки (путь)
	 * @return идентификатор папки
	 */
	private FolderId getFolderId(String folderPath) throws IOException {
		FolderId folderId = getFolderIdIfExists(folderPath);
		if (folderId == null) {
			throw new HttpNotFoundException("Folder '" + folderPath + "' not found");
		}
		return folderId;
	}

	protected static final String USERS_ROOT = "/users/";

	protected static final String ARCHIVE_ROOT = "/archive/";

	private FolderId getFolderIdIfExists(String folderPath) throws IOException {
		String lowerCaseFolderPath = folderPath.toLowerCase();
		if (lowerCaseFolderPath.equals(currentMailboxPath)) {
			return getSubFolderIdIfExists(null, "");
		}
		else if (lowerCaseFolderPath.startsWith(currentMailboxPath + '/')) {
			return getSubFolderIdIfExists(null, folderPath.substring(currentMailboxPath.length() + 1));
		}
		else if (folderPath.startsWith(USERS_ROOT)) {
			int slashIndex = folderPath.indexOf('/', USERS_ROOT.length());
			String mailbox;
			String subFolderPath;
			if (slashIndex >= 0) {
				mailbox = folderPath.substring(USERS_ROOT.length(), slashIndex);
				subFolderPath = folderPath.substring(slashIndex + 1);
			}
			else {
				mailbox = folderPath.substring(USERS_ROOT.length());
				subFolderPath = "";
			}
			return getSubFolderIdIfExists(mailbox, subFolderPath);
		}
		else {
			return getSubFolderIdIfExists(null, folderPath);
		}
	}

	private FolderId getSubFolderIdIfExists(String mailbox, String folderPath) throws IOException {
		String[] folderNames;
		FolderId currentFolderId;

		// TODO test various use cases
		if ("/public".equals(folderPath)) {
			throw new UnsupportedOperationException("public folders not supported on Graph");
		}
		else if ("/archive".equals(folderPath)) {
			return new FolderId(mailbox, WellKnownFolderName.archive);
		}
		else if (isSubFolderOf(folderPath, PUBLIC_ROOT)) {
			throw new UnsupportedOperationException("public folders not supported on Graph");
		}
		else if (isSubFolderOf(folderPath, ARCHIVE_ROOT)) {
			currentFolderId = new FolderId(mailbox, WellKnownFolderName.archive);
			folderNames = folderPath.substring(ARCHIVE_ROOT.length()).split("/");
		}
		else if (isSubFolderOf(folderPath, INBOX) || isSubFolderOf(folderPath, LOWER_CASE_INBOX)
				|| isSubFolderOf(folderPath, MIXED_CASE_INBOX)) {
			currentFolderId = new FolderId(mailbox, WellKnownFolderName.inbox);
			folderNames = folderPath.substring(INBOX.length()).split("/");
		}
		else if (isSubFolderOf(folderPath, CALENDAR)) {
			currentFolderId = new FolderId(mailbox, WellKnownFolderName.calendar, "IPF.Appointment");
			// TODO subfolders not supported with graph
			folderNames = folderPath.substring(CALENDAR.length()).split("/");
		}
		else if (isSubFolderOf(folderPath, TASKS)) {
			currentFolderId = new FolderId(mailbox, WellKnownFolderName.tasks, "IPF.Task");
			folderNames = folderPath.substring(TASKS.length()).split("/");
		}
		else if (isSubFolderOf(folderPath, CONTACTS)) {
			currentFolderId = new FolderId(mailbox, WellKnownFolderName.contacts, "IPF.Contact");
			// TODO subfolders not supported with graph
			folderNames = folderPath.substring(CONTACTS.length()).split("/");
		}
		else if (isSubFolderOf(folderPath, SENT)) {
			currentFolderId = new FolderId(mailbox, WellKnownFolderName.sentitems);
			folderNames = folderPath.substring(SENT.length()).split("/");
		}
		else if (isSubFolderOf(folderPath, DRAFTS)) {
			currentFolderId = new FolderId(mailbox, WellKnownFolderName.drafts);
			folderNames = folderPath.substring(DRAFTS.length()).split("/");
		}
		else if (isSubFolderOf(folderPath, TRASH)) {
			currentFolderId = new FolderId(mailbox, WellKnownFolderName.deleteditems);
			folderNames = folderPath.substring(TRASH.length()).split("/");
		}
		else if (isSubFolderOf(folderPath, JUNK)) {
			currentFolderId = new FolderId(mailbox, WellKnownFolderName.junkemail);
			folderNames = folderPath.substring(JUNK.length()).split("/");
		}
		else if (isSubFolderOf(folderPath, UNSENT)) {
			currentFolderId = new FolderId(mailbox, WellKnownFolderName.outbox);
			folderNames = folderPath.substring(UNSENT.length()).split("/");
		}
		else {
			currentFolderId = new FolderId(mailbox, WellKnownFolderName.msgfolderroot);
			folderNames = folderPath.split("/");
		}
		if (currentFolderId != null) {
			String folderClass = currentFolderId.folderClass;
			for (String folderName : folderNames) {
				if (!folderName.isEmpty()) {
					currentFolderId = getSubFolderByName(currentFolderId, folderName);
					if (currentFolderId == null) {
						break;
					}
					currentFolderId.folderClass = folderClass;
				}
			}
		}
		return currentFolderId;
	}

	/**
	 * Поиск подпапки по имени, возвращает null, если папки не найдены
	 * @param currentFolderId id родительской папки
	 * @param folderName имя дочерней папки
	 * @return id дочерней папки, если существует
	 * @throws IOException в случае ошибки
	 */
	protected FolderId getSubFolderByName(FolderId currentFolderId, String folderName) throws IOException {
		// TODO rename davSearchEncode
		GraphRequestBuilder httpRequestBuilder = new GraphRequestBuilder().setMethod("GET")
			.setObjectType("mailFolders")
			.setMailbox(currentFolderId.mailbox)
			.setObjectId(currentFolderId.id)
			.setChildType("childFolders")
			.setExpandFields(FOLDER_PROPERTIES)
			.setFilter("displayName eq '" + StringUtil.davSearchEncode(EwsExchangeSession.decodeFolderName(folderName))
					+ "'");

		JSONObject jsonResponse = executeJsonRequest(httpRequestBuilder);

		FolderId folderId = null;
		try {
			JSONArray values = jsonResponse.getJSONArray("value");
			if (values.length() > 0) {
				folderId = new FolderId(currentFolderId.mailbox, values.getJSONObject(0).getString("id"));
			}
		}
		catch (JSONException e) {
			throw new IOException(e.getMessage(), e);
		}

		return folderId;
	}

	private boolean isSubFolderOf(String folderPath, String baseFolder) {
		if (PUBLIC_ROOT.equals(baseFolder) || ARCHIVE_ROOT.equals(baseFolder)) {
			return folderPath.startsWith(baseFolder);
		}
		else {
			return folderPath.startsWith(baseFolder)
					&& (folderPath.length() == baseFolder.length() || folderPath.charAt(baseFolder.length()) == '/');
		}
	}

	@Override
	public int createFolder(String folderName, String folderClass, Map<String, String> properties) throws IOException {
		return 0;
	}

	@Override
	public int updateFolder(String folderName, Map<String, String> properties) throws IOException {
		return 0;
	}

	@Override
	public void deleteFolder(String folderName) throws IOException {

	}

	@Override
	public void copyMessage(Message message, String targetFolder) throws IOException {

	}

	@Override
	public void moveMessage(Message message, String targetFolder) throws IOException {

	}

	@Override
	public void moveFolder(String folderName, String targetName) throws IOException {

	}

	@Override
	public void moveItem(String sourcePath, String targetPath) throws IOException {

	}

	@Override
	protected void moveToTrash(Message message) throws IOException {

	}

	@Override
	protected Set<String> getItemProperties() {
		return null;
	}

	@Override
	public List<Contact> searchContacts(String folderPath, Set<String> attributes, Condition condition, int maxCount)
			throws IOException {
		return null;
	}

	@Override
	public List<Event> getEventMessages(String folderPath) throws IOException {
		return null;
	}

	@Override
	protected Condition getCalendarItemCondition(Condition dateCondition) {
		return null;
	}

	@Override
	public List<Event> searchEvents(String folderPath, Set<String> attributes, Condition condition) throws IOException {
		return null;
	}

	@Override
	public Item getItem(String folderPath, String itemName) throws IOException {
		return null;
	}

	@Override
	public ContactPhoto getContactPhoto(Contact contact) throws IOException {
		return null;
	}

	@Override
	public void deleteItem(String folderPath, String itemName) throws IOException {

	}

	@Override
	public void processItem(String folderPath, String itemName) throws IOException {

	}

	@Override
	public int sendEvent(String icsBody) throws IOException {
		return 0;
	}

	@Override
	protected Contact buildContact(String folderPath, String itemName, Map<String, String> properties, String etag,
			String noneMatch) throws IOException {
		return null;
	}

	@Override
	protected ItemResult internalCreateOrUpdateEvent(String folderPath, String itemName, String contentClass,
			String icsBody, String etag, String noneMatch) throws IOException {
		return null;
	}

	@Override
	public boolean isSharedFolder(String folderPath) {
		return false;
	}

	@Override
	public boolean isMainCalendar(String folderPath) throws IOException {
		return false;
	}

	@Override
	public Map<String, Contact> galFind(Condition condition, Set<String> returningAttributes, int sizeLimit)
			throws IOException {
		return null;
	}

	@Override
	protected String getFreeBusyData(String attendee, String start, String end, int interval) throws IOException {
		return null;
	}

	@Override
	protected void loadVtimezone() {

	}

	class GraphIterator {

		private JSONObject jsonObject;

		private JSONArray values;

		private String nextLink;

		private int index;

		public GraphIterator(JSONObject jsonObject) throws JSONException {
			this.jsonObject = jsonObject;
			nextLink = jsonObject.optString("@odata.nextLink", null);
			values = jsonObject.getJSONArray("value");
		}

		public boolean hasNext() {
			return nextLink != null || index < values.length();
		}

		public JSONObject next() throws IOException {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			try {
				if (index >= values.length() && nextLink != null) {
					fetchNextPage();
				}
				return values.getJSONObject(index++);
			}
			catch (JSONException e) {
				throw new IOException(e.getMessage(), e);
			}
		}

		private void fetchNextPage() throws IOException, JSONException {
			HttpGet request = new HttpGet(nextLink);
			request.setHeader("Authorization", "Bearer " + token.getAccessToken());
			try (CloseableHttpResponse response = httpClient.execute(request)) {
				jsonObject = new JsonResponseHandler().handleResponse(response);
				nextLink = jsonObject.optString("@odata.nextLink", null);
				values = jsonObject.getJSONArray("value");
				index = 0;
			}
		}

	}

	private GraphIterator executeSearchRequest(GraphRequestBuilder httpRequestBuilder) throws IOException {
		try {
			JSONObject jsonResponse = executeJsonRequest(httpRequestBuilder);
			return new GraphIterator(jsonResponse);
		}
		catch (JSONException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	private JSONObject executeJsonRequest(GraphRequestBuilder httpRequestBuilder) throws IOException {
		// TODO handle throttling
		HttpRequestBase request = httpRequestBuilder.setAccessToken(token.getAccessToken()).build();
		JSONObject jsonResponse;
		try (CloseableHttpResponse response = httpClient.execute(request)) {
			jsonResponse = new JsonResponseHandler().handleResponse(response);
		}
		return jsonResponse;
	}

}
