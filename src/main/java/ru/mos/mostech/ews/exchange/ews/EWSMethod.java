/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ContentType;
import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.exchange.XMLStreamUtil;
import ru.mos.mostech.ews.http.HttpClientAdapter;
import ru.mos.mostech.ews.util.StringUtil;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Метод EWS SOAP.
 */
@Slf4j
public abstract class EWSMethod extends HttpPost implements ResponseHandler<EWSMethod> {

	protected static final String CONTENT_TYPE = ContentType.create("text/xml", StandardCharsets.UTF_8).toString();

	protected static final int CHUNK_LENGTH = 131072;

	protected FolderQueryTraversal traversal;

	protected BaseShape baseShape;

	protected boolean includeMimeContent;

	protected FolderId folderId;

	protected FolderId savedItemFolderId;

	protected FolderId toFolderId;

	protected FolderId parentFolderId;

	protected ItemId itemId;

	protected List<ItemId> itemIds;

	protected ItemId parentItemId;

	protected Set<FieldURI> additionalProperties;

	protected Disposal deleteType;

	protected Set<AttributeOption> methodOptions;

	protected ElementOption unresolvedEntry;

	// paging request
	protected int maxCount;

	protected int offset;

	// paging response
	protected boolean includesLastItemInRange;

	protected List<FieldUpdate> updates;

	protected FileAttachment attachment;

	protected String attachmentId;

	protected final String itemType;

	protected final String methodName;

	protected final String responseCollectionName;

	protected List<Item> responseItems;

	protected String errorDetail;

	protected String errorDescription;

	protected String errorValue;

	protected long backOffMilliseconds;

	protected Item item;

	protected SearchExpression searchExpression;

	protected FieldOrder fieldOrder;

	protected String serverVersion;

	protected String timezoneContext;

	private HttpResponse response;

	/**
	 * Построить метод EWS
	 * @param itemType тип элемента
	 * @param methodName имя метода
	 */
	public EWSMethod(String itemType, String methodName) {
		this(itemType, methodName, itemType + 's');
	}

	/**
	 * Построить метод EWS
	 * @param itemType тип элемента
	 * @param methodName имя метода
	 * @param responseCollectionName имя коллекции ответов элемента
	 */
	protected EWSMethod(String itemType, String methodName, String responseCollectionName) {
		super(URI.create("/ews/exchange.asmx"));
		this.itemType = itemType;
		this.methodName = methodName;
		this.responseCollectionName = responseCollectionName;
		if (Settings.getBooleanProperty("mt.ews.acceptEncodingGzip", true)) {
			setHeader("Accept-Encoding", "gzip");
		}

		AbstractHttpEntity httpEntity = new AbstractHttpEntity() {
			byte[] content;

			@Override
			public boolean isRepeatable() {
				return true;
			}

			@Override
			public long getContentLength() {
				if (content == null) {
					content = generateSoapEnvelope();
				}
				return content.length;
			}

			@Override
			public InputStream getContent() throws UnsupportedOperationException {
				if (content == null) {
					content = generateSoapEnvelope();
				}
				return new ByteArrayInputStream(content);
			}

			@Override
			public void writeTo(OutputStream outputStream) throws IOException {
				boolean firstPass = content == null;
				if (content == null) {
					content = generateSoapEnvelope();
				}
				if (content.length < CHUNK_LENGTH) {
					outputStream.write(content);
				}
				else {
					int i = 0;
					while (i < content.length) {
						int length = CHUNK_LENGTH;
						if (i + CHUNK_LENGTH > content.length) {
							length = content.length - i;
						}
						outputStream.write(content, i, length);
						if (!firstPass) {
							log.debug("{}", new BundleMessage("LOG_UPLOAD_PROGRESS",
									String.valueOf((i + length) / 1024), (i + length) * 100 / content.length));
						}
						i += CHUNK_LENGTH;
					}
				}
			}

			@Override
			public boolean isStreaming() {
				return false;
			}
		};

		httpEntity.setContentType(CONTENT_TYPE);
		setEntity(httpEntity);
	}

	protected void addAdditionalProperty(FieldURI additionalProperty) {
		if (additionalProperties == null) {
			additionalProperties = new HashSet<>();
		}
		additionalProperties.add(additionalProperty);
	}

	protected void addMethodOption(AttributeOption attributeOption) {
		if (methodOptions == null) {
			methodOptions = new HashSet<>();
		}
		methodOptions.add(attributeOption);
	}

	protected void setSearchExpression(SearchExpression searchExpression) {
		this.searchExpression = searchExpression;
	}

	protected void setFieldOrder(FieldOrder fieldOrder) {
		this.fieldOrder = fieldOrder;
	}

	protected void writeShape(Writer writer) throws IOException {
		if (baseShape != null) {
			writer.write("<m:");
			writer.write(itemType);
			writer.write("Shape>");
			baseShape.write(writer);
			if (includeMimeContent) {
				writer.write("<t:IncludeMimeContent>true</t:IncludeMimeContent>");
			}
			if (additionalProperties != null) {
				writer.write("<t:AdditionalProperties>");
				StringBuilder buffer = new StringBuilder();
				for (FieldURI fieldURI : additionalProperties) {
					fieldURI.appendTo(buffer);
				}
				writer.write(buffer.toString());
				writer.write("</t:AdditionalProperties>");
			}
			writer.write("</m:");
			writer.write(itemType);
			writer.write("Shape>");
		}
	}

	protected void writeItemId(Writer writer) throws IOException {
		if (itemId != null || itemIds != null) {
			if (updates == null) {
				writer.write("<m:ItemIds>");
			}
			if (itemId != null) {
				itemId.write(writer);
			}
			if (itemIds != null) {
				for (ItemId localItemId : itemIds) {
					localItemId.write(writer);
				}
			}
			if (updates == null) {
				writer.write("</m:ItemIds>");
			}
		}
	}

	protected void writeParentItemId(Writer writer) throws IOException {
		if (parentItemId != null) {
			writer.write("<m:ParentItemId Id=\"");
			writer.write(parentItemId.id);
			if (parentItemId.changeKey != null) {
				writer.write("\" ChangeKey=\"");
				writer.write(parentItemId.changeKey);
			}
			writer.write("\"/>");
		}
	}

	protected void writeFolderId(Writer writer) throws IOException {
		if (folderId != null) {
			if (updates == null) {
				writer.write("<m:FolderIds>");
			}
			folderId.write(writer);
			if (updates == null) {
				writer.write("</m:FolderIds>");
			}
		}
	}

	protected void writeSavedItemFolderId(Writer writer) throws IOException {
		if (savedItemFolderId != null) {
			writer.write("<m:SavedItemFolderId>");
			savedItemFolderId.write(writer);
			writer.write("</m:SavedItemFolderId>");
		}
	}

	protected void writeToFolderId(Writer writer) throws IOException {
		if (toFolderId != null) {
			writer.write("<m:ToFolderId>");
			toFolderId.write(writer);
			writer.write("</m:ToFolderId>");
		}
	}

	protected void writeParentFolderId(Writer writer) throws IOException {
		if (parentFolderId != null) {
			writer.write("<m:ParentFolderId");
			if (item == null) {
				writer.write("s");
			}
			writer.write(">");
			parentFolderId.write(writer);
			writer.write("</m:ParentFolderId");
			if (item == null) {
				writer.write("s");
			}
			writer.write(">");
		}
	}

	protected void writeItem(Writer writer) throws IOException {
		if (item != null) {
			writer.write("<m:");
			writer.write(itemType);
			writer.write("s>");
			item.write(writer);
			writer.write("</m:");
			writer.write(itemType);
			writer.write("s>");
		}
	}

	protected void writeRestriction(Writer writer) throws IOException {
		if (searchExpression != null) {
			writer.write("<m:Restriction>");
			StringBuilder buffer = new StringBuilder();
			searchExpression.appendTo(buffer);
			writer.write(buffer.toString());
			writer.write("</m:Restriction>");
		}
	}

	protected void writeSortOrder(Writer writer) throws IOException {
		if (fieldOrder != null) {
			writer.write("<m:SortOrder>");
			StringBuilder buffer = new StringBuilder();
			fieldOrder.appendTo(buffer);
			writer.write(buffer.toString());
			writer.write("</m:SortOrder>");
		}
	}

	protected void startChanges(Writer writer) throws IOException {
		// noinspection VariableNotUsedInsideIf
		if (updates != null) {
			writer.write("<m:");
			writer.write(itemType);
			writer.write("Changes>");
			writer.write("<t:");
			writer.write(itemType);
			writer.write("Change>");
		}
	}

	protected void writeUpdates(Writer writer) throws IOException {
		if (updates != null) {
			writer.write("<t:Updates>");
			for (FieldUpdate fieldUpdate : updates) {
				fieldUpdate.write(itemType, writer);
			}
			writer.write("</t:Updates>");
		}
	}

	protected void writeUnresolvedEntry(Writer writer) throws IOException {
		if (unresolvedEntry != null) {
			unresolvedEntry.write(writer);
		}
	}

	protected void endChanges(Writer writer) throws IOException {
		// noinspection VariableNotUsedInsideIf
		if (updates != null) {
			writer.write("</t:");
			writer.write(itemType);
			writer.write("Change>");
			writer.write("</m:");
			writer.write(itemType);
			writer.write("Changes>");
		}
	}

	protected byte[] generateSoapEnvelope() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
			writer.write("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" "
					+ "xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\" "
					+ "xmlns:m=\"http://schemas.microsoft.com/exchange/services/2006/messages\">");
			writer.write("<soap:Header>");
			if (serverVersion != null) {
				writer.write("<t:RequestServerVersion Version=\"");
				writer.write(serverVersion);
				writer.write("\"/>");
			}
			if (timezoneContext != null) {
				writer.write("<t:TimeZoneContext><t:TimeZoneDefinition Id=\"");
				writer.write(timezoneContext);
				writer.write("\"/></t:TimeZoneContext>");
			}
			writer.write("</soap:Header>");

			writer.write("<soap:Body>");
			writer.write("<m:");
			writer.write(methodName);
			if (traversal != null) {
				traversal.write(writer);
			}
			if (deleteType != null) {
				deleteType.write(writer);
			}
			if (methodOptions != null) {
				for (AttributeOption attributeOption : methodOptions) {
					attributeOption.write(writer);
				}
			}
			writer.write(">");
			writeSoapBody(writer);
			writer.write("</m:");
			writer.write(methodName);
			writer.write(">");
			writer.write("</soap:Body>" + "</soap:Envelope>");
			writer.flush();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		return baos.toByteArray();
	}

	protected void writeSoapBody(Writer writer) throws IOException {
		startChanges(writer);
		writeShape(writer);
		writeIndexedPageView(writer);
		writeRestriction(writer);
		writeSortOrder(writer);
		writeParentFolderId(writer);
		writeToFolderId(writer);
		writeItemId(writer);
		writeParentItemId(writer);
		writeAttachments(writer);
		writeAttachmentId(writer);
		writeFolderId(writer);
		writeSavedItemFolderId(writer);
		writeItem(writer);
		writeUpdates(writer);
		writeUnresolvedEntry(writer);
		endChanges(writer);
	}

	protected void writeIndexedPageView(Writer writer) throws IOException {
		if (maxCount > 0) {
			writer.write("<m:IndexedPage" + itemType + "View MaxEntriesReturned=\"");
			writer.write(String.valueOf(maxCount));
			writer.write("\" Offset=\"");
			writer.write(String.valueOf(offset));
			writer.write("\" BasePoint=\"Beginning\"/>");

		}
	}

	protected void writeAttachmentId(Writer writer) throws IOException {
		if (attachmentId != null) {
			if ("CreateAttachment".equals(methodName)) {
				writer.write("<m:AttachmentShape>");
				writer.write("<t:IncludeMimeContent>true</t:IncludeMimeContent>");
				writer.write("</m:AttachmentShape>");
			}
			writer.write("<m:AttachmentIds>");
			writer.write("<t:AttachmentId Id=\"");
			writer.write(attachmentId);
			writer.write("\"/>");
			writer.write("</m:AttachmentIds>");
		}
	}

	protected void writeAttachments(Writer writer) throws IOException {
		if (attachment != null) {
			writer.write("<m:Attachments>");
			attachment.write(writer);
			writer.write("</m:Attachments>");
		}
	}

	/**
	 * Получить версию сервера Exchange, Exchange2013, Exchange2010 или Exchange2007_SP1
	 * @return версия сервера
	 */
	public String getServerVersion() {
		return serverVersion;
	}

	/**
	 * Установите версию сервера Exchange, Exchange2010 или Exchange2007_SP1
	 * @param serverVersion версия сервера
	 */
	public void setServerVersion(String serverVersion) {
		this.serverVersion = serverVersion;
	}

	/**
	 * Установить контекст часового пояса обмена
	 * @param timezoneContext контекст часового пояса пользователя
	 */
	public void setTimezoneContext(String timezoneContext) {
		this.timezoneContext = timezoneContext;
	}

	/**
	 * Объект участника встречи
	 */
	public static class Attendee {

		/**
		 * роль участника
		 */
		public String role;

		/**
		 * адрес электронной почты участника
		 */
		public String email;

		/**
		 * статус участия участника
		 */
		public String partstat;

		/**
		 * полное имя участника
		 */
		public String name;

	}

	/**
	 * Повторяющееся событие
	 */
	public static class Occurrence {

		/**
		 * Дата начала оригинального события
		 */
		public String originalStart;

		/**
		 * Идентификатор элемента
		 */
		public ItemId itemId;

	}

	/**
	 * Элемент
	 */
	public static class Item extends HashMap<String, String> {

		/**
		 * Тип элемента.
		 */
		public String type;

		protected byte[] mimeContent;

		protected List<FieldUpdate> fieldUpdates;

		protected List<FileAttachment> attachments;

		protected List<Attendee> attendees;

		protected final List<String> fieldNames = new ArrayList<>();

		protected List<Occurrence> occurrences;

		protected List<String> members;

		protected ItemId referenceItemId;

		@Override
		public String toString() {
			return "type: " + type + ' ' + super.toString();
		}

		@Override
		public String put(String key, String value) {
			if (value != null) {
				if (get(key) == null) {
					fieldNames.add(key);
				}
				return super.put(key, value);
			}
			else {
				return null;
			}
		}

		/**
		 * Записать XML-содержимое в писатель.
		 * @param writer писатель
		 * @throws IOException при ошибке
		 */
		public void write(Writer writer) throws IOException {
			writer.write("<t:");
			writer.write(type);
			writer.write(">");
			if (mimeContent != null) {
				writer.write("<t:MimeContent>");
				for (byte c : mimeContent) {
					writer.write(c);
				}
				writer.write("</t:MimeContent>");
			}
			// write ordered fields
			for (String key : fieldNames) {
				if ("MeetingTimeZone".equals(key)) {
					writer.write("<t:MeetingTimeZone TimeZoneName=\"");
					writer.write(StringUtil.xmlEncodeAttribute(get(key)));
					writer.write("\"></t:MeetingTimeZone>");
				}
				else if ("StartTimeZone".equals(key)) {
					writer.write("<t:StartTimeZone Id=\"");
					writer.write(StringUtil.xmlEncodeAttribute(get(key)));
					writer.write("\"></t:StartTimeZone>");
				}
				else if ("Body".equals(key)) {
					writer.write("<t:Body BodyType=\"Text\">");
					writer.write(StringUtil.xmlEncode(get(key)));
					writer.write("</t:Body>");
				}
				else {
					writer.write("<t:");
					writer.write(key);
					writer.write(">");
					writer.write(StringUtil.xmlEncode(get(key)));
					writer.write("</t:");
					writer.write(key);
					writer.write(">");
				}
			}
			if (fieldUpdates != null) {
				for (FieldUpdate fieldUpdate : fieldUpdates) {
					fieldUpdate.write(null, writer);
				}
			}
			if (referenceItemId != null) {
				referenceItemId.write(writer);
			}
			writer.write("</t:");
			writer.write(type);
			writer.write(">");
		}

		/**
		 * Обновления полей.
		 * @param fieldUpdates обновления полей
		 */
		public void setFieldUpdates(List<FieldUpdate> fieldUpdates) {
			this.fieldUpdates = fieldUpdates;
		}

		/**
		 * Получить значение свойства как int
		 * @param key имя свойства в ответе
		 * @return значение свойства
		 */
		public int getInt(String key) {
			int result = 0;
			String value = get(key);
			if (value != null && !value.isEmpty()) {
				result = Integer.parseInt(value);
			}
			return result;
		}

		/**
		 * Получить значение свойства как long
		 * @param key название свойства в ответе
		 * @return значение свойства
		 */
		public long getLong(String key) {
			long result = 0;
			String value = get(key);
			if (value != null && !value.isEmpty()) {
				result = Long.parseLong(value);
			}
			return result;
		}

		/**
		 * Получить значение свойства как булевое
		 * @param key имя свойства в ответе
		 * @return значение свойства
		 */
		public boolean getBoolean(String key) {
			boolean result = false;
			String value = get(key);
			if (value != null && !value.isEmpty()) {
				result = Boolean.parseBoolean(value);
			}
			return result;
		}

		/**
		 * Получить файл по имени вложения
		 * @param attachmentName имя вложения
		 * @return вложение
		 */
		public FileAttachment getAttachmentByName(String attachmentName) {
			FileAttachment result = null;
			if (attachments != null) {
				for (FileAttachment fileAttachment : attachments) {
					if (attachmentName.equals(fileAttachment.name)) {
						result = fileAttachment;
						break;
					}
				}
			}
			return result;
		}

		/**
		 * Получить всех участников.
		 * @return всех участников
		 */
		public List<Attendee> getAttendees() {
			return attendees;
		}

		/**
		 * Добавить участника.
		 * @param attendee объект участника
		 */
		public void addAttendee(Attendee attendee) {
			if (attendees == null) {
				attendees = new ArrayList<>();
			}
			attendees.add(attendee);
		}

		/**
		 * Добавить событие.
		 * @param occurrence событие
		 */
		public void addOccurrence(Occurrence occurrence) {
			if (occurrences == null) {
				occurrences = new ArrayList<>();
			}
			occurrences.add(occurrence);
		}

		/**
		 * ПолучитьOccurrences.
		 * @return событие поведения
		 */
		public List<Occurrence> getOccurrences() {
			return occurrences;
		}

		/**
		 * Добавить участника.
		 * @param member элемент списка участника
		 */
		public void addMember(String member) {
			if (members == null) {
				members = new ArrayList<>();
			}
			members.add(member);
		}

		/**
		 * Получить участников.
		 * @return участники события
		 */
		public List<String> getMembers() {
			return members;
		}

	}

	/**
	 * Проверка успешности метода.
	 * @throws EWSException при ошибке
	 */
	public void checkSuccess() throws EWSException {
		if ("The server cannot service this request right now. Try again later.".equals(errorDetail)) {
			throw new EWSThrottlingException(errorDetail);
		}
		if (errorDetail != null && (!"ErrorAccessDenied".equals(errorDetail)
				&& !"ErrorMailRecipientNotFound".equals(errorDetail) && !"ErrorItemNotFound".equals(errorDetail)
				&& !"ErrorCalendarOccurrenceIsDeletedFromRecurrence".equals(errorDetail))) {
			throw new EWSException(errorDetail + ' ' + ((errorDescription != null) ? errorDescription : "") + ' '
					+ ((errorValue != null) ? errorValue : "") + "\n request: "
					+ new String(generateSoapEnvelope(), StandardCharsets.UTF_8));

		}
		if (getStatusCode() == HttpStatus.SC_BAD_REQUEST || getStatusCode() == HttpStatus.SC_INSUFFICIENT_STORAGE) {
			throw new EWSException(response.getStatusLine().getReasonPhrase());
		}
	}

	public int getStatusCode() {
		if ("ErrorAccessDenied".equals(errorDetail)) {
			return HttpStatus.SC_FORBIDDEN;
		}
		else if ("ErrorItemNotFound".equals(errorDetail)) {
			return HttpStatus.SC_NOT_FOUND;
		}
		else {
			return response.getStatusLine().getStatusCode();
		}
	}

	/**
	 * Получить элементы ответа.
	 * @return элементы ответа
	 * @throws EWSException при ошибке
	 */
	public List<Item> getResponseItems() throws EWSException {
		checkSuccess();
		if (responseItems != null) {
			return responseItems;
		}
		else {
			return new ArrayList<>();
		}
	}

	/**
	 * Получить единичный элемент ответа.
	 * @return элемент ответа
	 * @throws EWSException в случае ошибки
	 */
	public Item getResponseItem() throws EWSException {
		checkSuccess();
		if (responseItems != null && !responseItems.isEmpty()) {
			return responseItems.get(0);
		}
		else {
			return null;
		}
	}

	/**
	 * Получить MIME-содержимое ответа.
	 * @return MIME-содержимое
	 * @throws EWSException в случае ошибки
	 */
	public byte[] getMimeContent() throws EWSException {
		checkSuccess();
		Item responseItem = getResponseItem();
		if (responseItem != null) {
			return responseItem.mimeContent;
		}
		else {
			return null;
		}
	}

	protected String handleTag(XMLStreamReader reader, String localName) throws XMLStreamException {
		StringBuilder result = null;
		int event = reader.getEventType();
		if (event == XMLStreamConstants.START_ELEMENT && localName.equals(reader.getLocalName())) {
			result = new StringBuilder();
			while (reader.hasNext()
					&& !(event == XMLStreamConstants.END_ELEMENT && localName.equals(reader.getLocalName()))) {
				event = reader.next();
				if (event == XMLStreamConstants.CHARACTERS) {
					result.append(reader.getText());
				}
				else if ("MessageXml".equals(localName) && event == XMLStreamConstants.START_ELEMENT) {
					String attributeValue = null;
					for (int i = 0; i < reader.getAttributeCount(); i++) {
						if (result.length() > 0) {
							result.append(", ");
						}
						attributeValue = reader.getAttributeValue(i);
						result.append(reader.getAttributeLocalName(i)).append(": ").append(reader.getAttributeValue(i));
					}
					// catch BackOffMilliseconds value
					if ("BackOffMilliseconds".equals(attributeValue)) {
						try {
							backOffMilliseconds = Long.parseLong(reader.getElementText());
						}
						catch (NumberFormatException e) {
							log.error("Unable to parse BackOffMilliseconds");
						}
					}
				}
			}
		}
		if (result != null && result.length() > 0) {
			return result.toString();
		}
		else {
			return null;
		}
	}

	protected void handleErrors(XMLStreamReader reader) throws XMLStreamException {
		String result = handleTag(reader, "ResponseCode");
		// store error description
		String messageText = handleTag(reader, "MessageText");
		if (messageText != null) {
			errorDescription = messageText;
		}
		String messageXml = handleTag(reader, "MessageXml");
		if (messageXml != null) {
			// contains BackOffMilliseconds on ErrorServerBusy
			errorValue = messageXml;
		}
		if (errorDetail == null && result != null && !"NoError".equals(result)
				&& !"ErrorNameResolutionMultipleResults".equals(result)
				&& !"ErrorNameResolutionNoResults".equals(result) && !"ErrorFolderExists".equals(result)) {
			errorDetail = result;
		}
		if (XMLStreamUtil.isStartTag(reader, "faultstring")) {
			errorDetail = XMLStreamUtil.getElementText(reader);
		}
	}

	protected Item handleItem(XMLStreamReader reader) throws XMLStreamException {
		Item responseItem = new Item();
		responseItem.type = reader.getLocalName();
		while (reader.hasNext() && !XMLStreamUtil.isEndTag(reader, responseItem.type)) {
			reader.next();
			if (XMLStreamUtil.isStartTag(reader)) {
				String tagLocalName = reader.getLocalName();
				String value = null;
				if ("ExtendedProperty".equals(tagLocalName)) {
					addExtendedPropertyValue(reader, responseItem);
				}
				else if ("Members".equals(tagLocalName)) {
					handleMembers(reader, responseItem);
				}
				else if (tagLocalName.endsWith("MimeContent")) {
					handleMimeContent(reader, responseItem);
				}
				else if ("Attachments".equals(tagLocalName)) {
					responseItem.attachments = handleAttachments(reader);
				}
				else if ("EmailAddresses".equals(tagLocalName)) {
					handleEmailAddresses(reader, responseItem);
				}
				else if ("RequiredAttendees".equals(tagLocalName) || "OptionalAttendees".equals(tagLocalName)) {
					handleAttendees(reader, responseItem, tagLocalName);
				}
				else if ("ModifiedOccurrences".equals(tagLocalName)) {
					handleModifiedOccurrences(reader, responseItem);
				}
				else {
					if (tagLocalName.endsWith("Id")) {
						value = getAttributeValue(reader, "Id");
						// get change key
						responseItem.put("ChangeKey", getAttributeValue(reader, "ChangeKey"));
					}
					if (value == null) {
						value = getTagContent(reader);
					}
					if (value != null) {
						responseItem.put(tagLocalName, value);
					}
				}
			}
		}
		return responseItem;
	}

	protected void handleEmailAddresses(XMLStreamReader reader, Item item) throws XMLStreamException {
		while (reader.hasNext() && !(XMLStreamUtil.isEndTag(reader, "EmailAddresses"))) {
			reader.next();
			if (XMLStreamUtil.isStartTag(reader)) {
				String tagLocalName = reader.getLocalName();
				if ("Entry".equals(tagLocalName)) {
					item.put(reader.getAttributeValue(null, "Key"), XMLStreamUtil.getElementText(reader));
				}
			}
		}
	}

	protected void handleAttendees(XMLStreamReader reader, Item item, String attendeeType) throws XMLStreamException {
		while (reader.hasNext() && !(XMLStreamUtil.isEndTag(reader, attendeeType))) {
			reader.next();
			if (XMLStreamUtil.isStartTag(reader)) {
				String tagLocalName = reader.getLocalName();
				if ("Attendee".equals(tagLocalName)) {
					handleAttendee(reader, item, attendeeType);
				}
			}
		}
	}

	protected void handleModifiedOccurrences(XMLStreamReader reader, Item item) throws XMLStreamException {
		while (reader.hasNext() && !(XMLStreamUtil.isEndTag(reader, "ModifiedOccurrences"))) {
			reader.next();
			if (XMLStreamUtil.isStartTag(reader)) {
				String tagLocalName = reader.getLocalName();
				if ("Occurrence".equals(tagLocalName)) {
					handleOccurrence(reader, item);
				}
			}
		}
	}

	protected void handleOccurrence(XMLStreamReader reader, Item item) throws XMLStreamException {
		Occurrence occurrence = new Occurrence();
		while (reader.hasNext() && !(XMLStreamUtil.isEndTag(reader, "Occurrence"))) {
			reader.next();
			if (XMLStreamUtil.isStartTag(reader)) {
				String tagLocalName = reader.getLocalName();
				if ("ItemId".equals(tagLocalName)) {
					occurrence.itemId = new ItemId("ItemId", getAttributeValue(reader, "Id"),
							getAttributeValue(reader, "ChangeKey"));
				}
				if ("OriginalStart".equals(tagLocalName)) {
					occurrence.originalStart = XMLStreamUtil.getElementText(reader);
				}
			}
		}
		item.addOccurrence(occurrence);
	}

	protected void handleMembers(XMLStreamReader reader, Item responseItem) throws XMLStreamException {
		while (reader.hasNext() && !XMLStreamUtil.isEndTag(reader, "Members")) {
			reader.next();
			if (XMLStreamUtil.isStartTag(reader)) {
				String tagLocalName = reader.getLocalName();
				if ("Member".equals(tagLocalName)) {
					handleMember(reader, responseItem);
				}
			}
		}
	}

	protected void handleMember(XMLStreamReader reader, Item responseItem) throws XMLStreamException {
		String member = null;
		while (reader.hasNext() && !XMLStreamUtil.isEndTag(reader, "Member")) {
			reader.next();
			if (XMLStreamUtil.isStartTag(reader)) {
				String tagLocalName = reader.getLocalName();
				if ("EmailAddress".equals(tagLocalName) && member == null) {
					member = "mailto:" + XMLStreamUtil.getElementText(reader);
				}
			}
		}
		if (member != null) {
			responseItem.addMember(member);
		}
	}

	/**
	 * Преобразовать тип ответа в значение partstat
	 * @param responseType тип ответа
	 * @return значение partstat
	 */
	public static String responseTypeToPartstat(String responseType) {
		if ("Accept".equals(responseType) || "Organizer".equals(responseType)) {
			return "ACCEPTED";
		}
		else if ("Tentative".equals(responseType)) {
			return "TENTATIVE";
		}
		else if ("Decline".equals(responseType)) {
			return "DECLINED";
		}
		else {
			return "NEEDS-ACTION";
		}
	}

	protected void handleAttendee(XMLStreamReader reader, Item item, String attendeeType) throws XMLStreamException {
		Attendee attendee = new Attendee();
		if ("RequiredAttendees".equals(attendeeType)) {
			attendee.role = "REQ-PARTICIPANT";
		}
		else {
			attendee.role = "OPT-PARTICIPANT";
		}
		while (reader.hasNext() && !(XMLStreamUtil.isEndTag(reader, "Attendee"))) {
			reader.next();
			if (XMLStreamUtil.isStartTag(reader)) {
				String tagLocalName = reader.getLocalName();
				if ("EmailAddress".equals(tagLocalName)) {
					attendee.email = reader.getElementText();
				}
				else if ("Name".equals(tagLocalName)) {
					attendee.name = XMLStreamUtil.getElementText(reader);
				}
				else if ("ResponseType".equals(tagLocalName)) {
					String responseType = XMLStreamUtil.getElementText(reader);
					attendee.partstat = responseTypeToPartstat(responseType);
				}
			}
		}
		item.addAttendee(attendee);
	}

	protected List<FileAttachment> handleAttachments(XMLStreamReader reader) throws XMLStreamException {
		List<FileAttachment> attachments = new ArrayList<>();
		while (reader.hasNext() && !(XMLStreamUtil.isEndTag(reader, "Attachments"))) {
			reader.next();
			if (XMLStreamUtil.isStartTag(reader)) {
				String tagLocalName = reader.getLocalName();
				if ("FileAttachment".equals(tagLocalName)) {
					attachments.add(handleFileAttachment(reader));
				}
			}
		}
		return attachments;
	}

	protected FileAttachment handleFileAttachment(XMLStreamReader reader) throws XMLStreamException {
		FileAttachment fileAttachment = new FileAttachment();
		while (reader.hasNext() && !(XMLStreamUtil.isEndTag(reader, "FileAttachment"))) {
			reader.next();
			if (XMLStreamUtil.isStartTag(reader)) {
				String tagLocalName = reader.getLocalName();
				if ("AttachmentId".equals(tagLocalName)) {
					fileAttachment.attachmentId = getAttributeValue(reader, "Id");
				}
				else if ("Name".equals(tagLocalName)) {
					fileAttachment.name = getTagContent(reader);
				}
				else if ("ContentType".equals(tagLocalName)) {
					fileAttachment.contentType = getTagContent(reader);
				}
			}
		}
		return fileAttachment;
	}

	protected void handleMimeContent(XMLStreamReader reader, Item responseItem) throws XMLStreamException {
		responseItem.mimeContent = Base64.decodeBase64(reader.getElementText().getBytes(StandardCharsets.US_ASCII));
	}

	protected void addExtendedPropertyValue(XMLStreamReader reader, Item item) throws XMLStreamException {
		String propertyTag = null;
		String propertyValue = null;
		while (reader.hasNext() && !(XMLStreamUtil.isEndTag(reader, "ExtendedProperty"))) {
			reader.next();
			if (XMLStreamUtil.isStartTag(reader)) {
				String tagLocalName = reader.getLocalName();
				if ("ExtendedFieldURI".equals(tagLocalName)) {
					propertyTag = getAttributeValue(reader, "PropertyTag");
					// property name is in PropertyId or PropertyName with
					// DistinguishedPropertySetId
					if (propertyTag == null) {
						propertyTag = getAttributeValue(reader, "PropertyId");
					}
					if (propertyTag == null) {
						propertyTag = getAttributeValue(reader, "PropertyName");
					}
				}
				else if ("Value".equals(tagLocalName)) {
					propertyValue = XMLStreamUtil.getElementText(reader);
				}
				else if ("Values".equals(tagLocalName)) {
					StringBuilder buffer = new StringBuilder();
					while (reader.hasNext() && !(XMLStreamUtil.isEndTag(reader, "Values"))) {
						reader.next();
						if (XMLStreamUtil.isStartTag(reader)) {

							if (buffer.length() > 0) {
								buffer.append(',');
							}
							String singleValue = XMLStreamUtil.getElementText(reader);
							if (singleValue != null) {
								buffer.append(singleValue);
							}
						}
					}
					propertyValue = buffer.toString();
				}
			}
		}
		if ((propertyTag != null) && (propertyValue != null)) {
			item.put(propertyTag, propertyValue);
		}
	}

	protected String getTagContent(XMLStreamReader reader) throws XMLStreamException {
		String tagLocalName = reader.getLocalName();
		while (reader.hasNext() && (reader.getEventType() != XMLStreamConstants.END_ELEMENT)) {
			reader.next();
			if (reader.getEventType() == XMLStreamConstants.CHARACTERS) {
				return reader.getText();
			}
		}
		// empty tag
		if (reader.hasNext()) {
			return null;
		}
		else {
			throw new XMLStreamException("End element for " + tagLocalName + " not found");
		}
	}

	protected String getAttributeValue(XMLStreamReader reader, String attributeName) {
		for (int i = 0; i < reader.getAttributeCount(); i++) {
			if (attributeName.equals(reader.getAttributeLocalName(i))) {
				return reader.getAttributeValue(i);
			}
		}
		return null;
	}

	@Override
	public EWSMethod handleResponse(HttpResponse response) {
		this.response = response;
		org.apache.http.Header contentTypeHeader = response.getFirstHeader("Content-Type");
		if (contentTypeHeader != null && "text/xml; charset=utf-8".equals(contentTypeHeader.getValue())) {
			try (InputStream inputStream = response.getEntity().getContent()) {
				if (HttpClientAdapter.isGzipEncoded(response)) {
					processResponseStream(new GZIPInputStream(inputStream));
				}
				else {
					processResponseStream(inputStream);
				}
			}
			catch (IOException e) {
				log.error("Error while parsing soap response: " + e, e);
			}
		}
		return this;
	}

	protected void processResponseStream(InputStream inputStream) {
		responseItems = new ArrayList<>();
		XMLStreamReader reader = null;
		try {
			inputStream = new FilterInputStream(inputStream) {
				int totalCount;

				int lastLogCount;

				@Override
				public int read(byte[] buffer, int offset, int length) throws IOException {
					int count = super.read(buffer, offset, length);
					totalCount += count;
					if (totalCount - lastLogCount > 1024 * 128) {
						log.debug("{}", new BundleMessage("LOG_DOWNLOAD_PROGRESS", String.valueOf(totalCount / 1024),
								EWSMethod.this.getURI()));
						lastLogCount = totalCount;
					}
					return count;
				}
			};
			reader = XMLStreamUtil.createXMLStreamReader(inputStream);
			while (reader.hasNext()) {
				reader.next();
				handleErrors(reader);
				if (serverVersion == null && XMLStreamUtil.isStartTag(reader, "ServerVersionInfo")) {
					String majorVersion = getAttributeValue(reader, "MajorVersion");
					String minorVersion = getAttributeValue(reader, "MinorVersion");
					if ("15".equals(majorVersion)) {
						if ("0".equals(minorVersion)) {
							serverVersion = "Exchange2013";
						}
						else {
							serverVersion = "Exchange2013_SP1";
						}
					}
					else if ("14".equals(majorVersion)) {
						if ("0".equals(minorVersion)) {
							serverVersion = "Exchange2010";
						}
						else {
							serverVersion = "Exchange2010_SP1";
						}
					}
					else {
						serverVersion = "Exchange2007_SP1";
					}
				}
				else if (XMLStreamUtil.isStartTag(reader, "RootFolder")) {
					includesLastItemInRange = "true".equals(reader.getAttributeValue(null, "IncludesLastItemInRange"));
				}
				else if (XMLStreamUtil.isStartTag(reader, responseCollectionName)) {
					handleItems(reader);
				}
				else {
					handleCustom(reader);
				}
			}
		}
		catch (XMLStreamException e) {
			errorDetail = e.getMessage();
			log.error("Error while parsing soap response: " + e, e);
			if (reader != null) {
				try {
					String content = reader.getText();
					if (content != null && content.length() > 4096) {
						content = content.substring(0, 4096) + " ...";
					}
					log.debug("Current text: " + content);
				}
				catch (IllegalStateException ise) {
					log.error(e + " " + e.getMessage());
				}
			}
		}
		if (errorDetail != null) {
			log.debug(errorDetail);
		}
	}

	@SuppressWarnings({ "NoopMethodInAbstractClass" })
	protected void handleCustom(XMLStreamReader reader) throws XMLStreamException {
		// override to handle custom content
	}

	private void handleItems(XMLStreamReader reader) throws XMLStreamException {
		while (reader.hasNext() && !XMLStreamUtil.isEndTag(reader, responseCollectionName)) {
			reader.next();
			if (XMLStreamUtil.isStartTag(reader)) {
				responseItems.add(handleItem(reader));
			}
		}

	}

}
