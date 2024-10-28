/*
DIT
 */
package ru.mos.mostech.ews.exchange;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.exception.HttpNotFoundException;
import ru.mos.mostech.ews.exception.MosTechEwsException;
import ru.mos.mostech.ews.http.URIUtil;
import ru.mos.mostech.ews.ui.NotificationDialog;
import ru.mos.mostech.ews.util.StringUtil;

import javax.mail.MessagingException;
import javax.mail.internet.*;
import javax.mail.util.SharedByteArrayInputStream;
import java.io.*;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Обмен сессией через Outlook Web Access (DAV)
 */
@Slf4j
public abstract class ExchangeSession {

	/**
	 * Ссылка на часовой пояс GMT для форматирования дат
	 */
	public static final SimpleTimeZone GMT_TIMEZONE = new SimpleTimeZone(0, "GMT");

	protected static final int FREE_BUSY_INTERVAL = 15;

	protected static final String PUBLIC_ROOT = "/public/";

	protected static final String CALENDAR = "calendar";

	protected static final String TASKS = "tasks";

	/**
	 * Логическое имя папки контактов
	 */
	public static final String CONTACTS = "contacts";

	protected static final String ADDRESS_BOOK = "addressbook";

	protected static final String INBOX = "INBOX";

	protected static final String LOWER_CASE_INBOX = "inbox";

	protected static final String MIXED_CASE_INBOX = "Inbox";

	protected static final String SENT = "Sent";

	protected static final String SEND_MSG = "##MtEwsSubmissionURI##";

	protected static final String DRAFTS = "Drafts";

	protected static final String TRASH = "Trash";

	protected static final String JUNK = "Junk";

	protected static final String UNSENT = "Unsent Messages";

	protected static final List<String> SPECIAL = Arrays.asList(SENT, DRAFTS, TRASH, JUNK);

	public static final String FALSE_STRING = "false";

	public static final String YYYY_MMDD_T_HHMMSS_Z = "yyyyMMdd'T'HHmmss'Z'";

	public static final String FOLDER_CLASS = "folderclass";

	public static final String DISPLAY_NAME = "displayname";

	public static final String IPF_APPOINTMENT = "IPF.Appointment";

	public static final String X_0028_ = "_x0028_";

	public static final String X_0029_ = "_x0029_";

	static {
		// Adjust Mime decoder settings
		System.setProperty("mail.mime.ignoreunknownencoding", "true");
		System.setProperty("mail.mime.decodetext.strict", FALSE_STRING);
	}

	protected String publicFolderUrl;

	/**
	 * Базовый путь пользовательских почтовых ящиков (используется для выбора папки)
	 */
	protected String mailPath;

	protected String rootPath;

	protected String email;

	protected String alias;

	/**
	 * Путь Caldav в нижнем регистре к почтовому ящику текущего пользователя.
	 * /users/<i>email</i>
	 */
	protected String currentMailboxPath;

	protected String userName;

	protected String serverVersion;

	protected static final String YYYY_MM_DD_HH_MM_SS = "yyyy/MM/dd HH:mm:ss";

	private static final String YYYYMMDD_T_HHMMSS_Z = YYYY_MMDD_T_HHMMSS_Z;

	protected static final String YYYY_MM_DD_T_HHMMSS_Z = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	private static final String YYYY_MM_DD = "yyyy-MM-dd";

	private static final String YYYY_MM_DD_T_HHMMSS_SSS_Z = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	protected ExchangeSession() {
		// empty constructor
	}

	/**
	 * Закрыть сессию. Завершить работу менеджера соединений http-клиента
	 */
	public abstract void close();

	/**
	 * Форматировать дату в формат поиска для обмена.
	 * @param date объект даты
	 * @return отформатированная дата для поиска
	 */
	public abstract String formatSearchDate(Date date);

	/**
	 * Вернуть стандартный форматер даты Zulu.
	 * @return форматер даты Zulu
	 */
	public static SimpleDateFormat getZuluDateFormat() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(YYYYMMDD_T_HHMMSS_Z, Locale.ENGLISH);
		dateFormat.setTimeZone(GMT_TIMEZONE);
		return dateFormat;
	}

	protected static SimpleDateFormat getVcardBdayFormat() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(YYYY_MM_DD, Locale.ENGLISH);
		dateFormat.setTimeZone(GMT_TIMEZONE);
		return dateFormat;
	}

	protected static SimpleDateFormat getExchangeDateFormat(String value) {
		SimpleDateFormat dateFormat;
		if (value.length() == 8) {
			dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
			dateFormat.setTimeZone(GMT_TIMEZONE);
		}
		else if (value.length() == 15) {
			dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ENGLISH);
			dateFormat.setTimeZone(GMT_TIMEZONE);
		}
		else if (value.length() == 16) {
			dateFormat = new SimpleDateFormat(YYYY_MMDD_T_HHMMSS_Z, Locale.ENGLISH);
			dateFormat.setTimeZone(GMT_TIMEZONE);
		}
		else {
			dateFormat = ExchangeSession.getExchangeZuluDateFormat();
		}
		return dateFormat;
	}

	protected static SimpleDateFormat getExchangeZuluDateFormat() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(YYYY_MM_DD_T_HHMMSS_Z, Locale.ENGLISH);
		dateFormat.setTimeZone(GMT_TIMEZONE);
		return dateFormat;
	}

	protected static SimpleDateFormat getExchangeZuluDateFormatMillisecond() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(YYYY_MM_DD_T_HHMMSS_SSS_Z, Locale.ENGLISH);
		dateFormat.setTimeZone(GMT_TIMEZONE);
		return dateFormat;
	}

	protected static Date parseDate(String dateString) throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		dateFormat.setTimeZone(GMT_TIMEZONE);
		return dateFormat.parse(dateString);
	}

	/**
	 * Проверяет, истекла ли сессия.
	 * @return true, если сессия истекла
	 * @throws NoRouteToHostException в случае ошибки
	 * @throws UnknownHostException в случае ошибки
	 */
	public boolean isExpired() throws NoRouteToHostException, UnknownHostException {
		boolean isExpired = false;
		try {
			getFolder("");
		}
		catch (UnknownHostException | NoRouteToHostException exc) {
			throw exc;
		}
		catch (IOException e) {
			isExpired = true;
		}

		return isExpired;
	}

	protected abstract void buildSessionInfo(java.net.URI uri) throws IOException;

	/**
	 * Создает сообщение в указанной папке. Перезапишет существующее сообщение с таким же
	 * заголовком в той же папке
	 * @param folderPath Путь к папке Exchange
	 * @param messageName Имя сообщения
	 * @param properties Свойства сообщения (флаги)
	 * @param mimeMessage MIME сообщение
	 * @throws IOException когда невозможно создать сообщение
	 */
	public abstract Message createMessage(String folderPath, String messageName, Map<String, String> properties,
			MimeMessage mimeMessage) throws IOException;

	/**
	 * Обновить заданные свойства в сообщении.
	 * @param message Сообщение обмена
	 * @param properties Карта свойств Webdav
	 * @throws IOException в случае ошибки
	 */
	public abstract void updateMessage(Message message, Map<String, String> properties) throws IOException;

	/**
	 * Удалить сообщение обмена.
	 * @param message Сообщение обмена
	 * @throws IOException в случае ошибки
	 */
	public abstract void deleteMessage(Message message) throws IOException;

	/**
	 * Получить сырое содержимое MIME-сообщения
	 * @param message Сообщение обмена
	 * @return тело сообщения
	 * @throws IOException при ошибке
	 */
	protected abstract byte[] getContent(Message message) throws IOException;

	protected static final Set<String> POP_MESSAGE_ATTRIBUTES = new HashSet<>();

	public static final String IMAP_UID = "imapUid";

	static {
		POP_MESSAGE_ATTRIBUTES.add("uid");
		POP_MESSAGE_ATTRIBUTES.add(IMAP_UID);
		POP_MESSAGE_ATTRIBUTES.add("messageSize");
	}

	/**
	 * Вернуть список сообщений папки только с идентификатором и размером (для слушателя
	 * POP3).
	 * @param folderName Название папки Exchange
	 * @return список сообщений папки
	 * @throws IOException при ошибке
	 */
	public MessageList getAllMessageUidAndSize(String folderName) throws IOException {
		return searchMessages(folderName, POP_MESSAGE_ATTRIBUTES, null);
	}

	protected static final Set<String> IMAP_MESSAGE_ATTRIBUTES = new HashSet<>();

	public static final String URL_COMPNAME = "urlcompname";

	public static final String LAST_MODIFIED = "lastmodified";

	public static final String KEYWORDS = "keywords";

	public static final String CONTENTCLASS = "contentclass";

	static {
		IMAP_MESSAGE_ATTRIBUTES.add("permanenturl");
		IMAP_MESSAGE_ATTRIBUTES.add(URL_COMPNAME);
		IMAP_MESSAGE_ATTRIBUTES.add("uid");
		IMAP_MESSAGE_ATTRIBUTES.add("messageSize");
		IMAP_MESSAGE_ATTRIBUTES.add(IMAP_UID);
		IMAP_MESSAGE_ATTRIBUTES.add("junk");
		IMAP_MESSAGE_ATTRIBUTES.add("flagStatus");
		IMAP_MESSAGE_ATTRIBUTES.add("messageFlags");
		IMAP_MESSAGE_ATTRIBUTES.add("lastVerbExecuted");
		IMAP_MESSAGE_ATTRIBUTES.add("read");
		IMAP_MESSAGE_ATTRIBUTES.add("deleted");
		IMAP_MESSAGE_ATTRIBUTES.add("date");
		IMAP_MESSAGE_ATTRIBUTES.add(LAST_MODIFIED);
		// OSX IMAP requests content-class
		IMAP_MESSAGE_ATTRIBUTES.add(CONTENTCLASS);
		IMAP_MESSAGE_ATTRIBUTES.add(KEYWORDS);
	}

	protected static final Set<String> UID_MESSAGE_ATTRIBUTES = new HashSet<>();

	static {
		UID_MESSAGE_ATTRIBUTES.add("uid");
	}

	/**
	 * Получить все сообщения в папке.
	 * @param folderPath Имя папки Exchange
	 * @return список сообщений
	 * @throws IOException при ошибке
	 */
	@SuppressWarnings("unused")
	public MessageList searchMessages(String folderPath) throws IOException {
		return searchMessages(folderPath, IMAP_MESSAGE_ATTRIBUTES, null);
	}

	/**
	 * Поиск папки для сообщений, соответствующих условиям, с атрибутами, необходимыми для
	 * IMAP слушателя.
	 * @param folderName Название папки Exchange
	 * @param condition фильтр поиска
	 * @return список сообщений
	 * @throws IOException при ошибке
	 */
	public MessageList searchMessages(String folderName, Condition condition) throws IOException {
		return searchMessages(folderName, IMAP_MESSAGE_ATTRIBUTES, condition);
	}

	/**
	 * Поиск папки на наличие сообщений, соответствующих условиям, с заданными атрибутами.
	 * @param folderName Имя папки Exchange
	 * @param attributes запрашиваемые атрибуты Webdav
	 * @param condition фильтр поиска
	 * @return список сообщений
	 * @throws IOException при ошибке
	 */
	public abstract MessageList searchMessages(String folderName, Set<String> attributes, Condition condition)
			throws IOException;

	@SuppressWarnings("java:S115")
	public enum Operator {

		Or, And, Not, IsEqualTo, IsGreaterThan, IsGreaterThanOrEqualTo, IsLessThan, IsLessThanOrEqualTo, IsNull, IsTrue,
		IsFalse, Like, StartsWith, Contains

	}

	/**
	 * Фильтр поиска обмена.
	 */
	public interface Condition {

		/**
		 * Добавить условие в буфер.
		 * @param buffer буфер фильтра поиска
		 */
		void appendTo(StringBuilder buffer);

		/**
		 * Истина, если условие пустое.
		 * @return истина, если условие пустое
		 */
		boolean isEmpty();

		/**
		 * Проверяет, соответствует ли контакт текущему условию.
		 * @param contact Контакт обмена
		 * @return true, если контакт соответствует условию
		 */
		boolean isMatch(ExchangeSession.Contact contact);

	}

	/**
	 * Условие атрибута.
	 */
	public abstract static class AttributeCondition implements Condition {

		@Getter
		protected final String attributeName;

		protected final Operator operator;

		@Getter
		protected final String value;

		protected AttributeCondition(String attributeName, Operator operator, String value) {
			this.attributeName = attributeName;
			this.operator = operator;
			this.value = value;
		}

		public boolean isEmpty() {
			return false;
		}

	}

	/**
	 * Множественное условие.
	 */
	@Getter
	public abstract static class MultiCondition implements Condition {

		protected final Operator operator;

		protected final List<Condition> conditions;

		protected MultiCondition(Operator operator, Condition... conditions) {
			this.operator = operator;
			this.conditions = new ArrayList<>();
			for (Condition condition : conditions) {
				if (condition != null) {
					this.conditions.add(condition);
				}
			}
		}

		/**
		 * Добавить новое условие.
		 * @param condition одно условие
		 */
		public void add(Condition condition) {
			if (condition != null) {
				conditions.add(condition);
			}
		}

		public boolean isEmpty() {
			boolean isEmpty = true;
			for (Condition condition : conditions) {
				if (!condition.isEmpty()) {
					isEmpty = false;
					break;
				}
			}
			return isEmpty;
		}

		public boolean isMatch(ExchangeSession.Contact contact) {
			if (operator == Operator.And) {
				for (Condition condition : conditions) {
					if (!condition.isMatch(contact)) {
						return false;
					}
				}
				return true;
			}
			else if (operator == Operator.Or) {
				for (Condition condition : conditions) {
					if (condition.isMatch(contact)) {
						return true;
					}
				}
				return false;
			}
			else {
				return false;
			}
		}

	}

	/**
	 * Не условие.
	 */
	public abstract static class NotCondition implements Condition {

		protected final Condition condition;

		protected NotCondition(Condition condition) {
			this.condition = condition;
		}

		public boolean isEmpty() {
			return condition.isEmpty();
		}

		public boolean isMatch(ExchangeSession.Contact contact) {
			return !condition.isMatch(contact);
		}

	}

	/**
	 * Условие единственного фильтра поиска.
	 */
	public abstract static class MonoCondition implements Condition {

		protected final String attributeName;

		protected final Operator operator;

		protected MonoCondition(String attributeName, Operator operator) {
			this.attributeName = attributeName;
			this.operator = operator;
		}

		public boolean isEmpty() {
			return false;
		}

		public boolean isMatch(ExchangeSession.Contact contact) {
			String actualValue = contact.get(attributeName);
			return (operator == Operator.IsNull && actualValue == null)
					|| (operator == Operator.IsFalse && FALSE_STRING.equals(actualValue))
					|| (operator == Operator.IsTrue && "true".equals(actualValue));
		}

	}

	/**
	 * И фильтр поиска.
	 * @param condition условия поиска
	 * @return condition
	 */
	public abstract MultiCondition and(Condition... condition);

	/**
	 * Или фильтр поиска.
	 * @param condition условия поиска
	 * @return условие
	 */
	public abstract MultiCondition or(Condition... condition);

	/**
	 * Не фильтр поиска.
	 * @param condition условие поиска
	 * @return condition
	 */
	public abstract Condition not(Condition condition);

	/**
	 * Условие равно.
	 * @param attributeName логическое имя атрибута Exchange
	 * @param value значение атрибута
	 * @return условие
	 */
	public abstract Condition isEqualTo(String attributeName, String value);

	/**
	 * Условие равно.
	 * @param attributeName логическое имя атрибута Exchange
	 * @param value значение атрибута
	 * @return условие
	 */
	public abstract Condition isEqualTo(String attributeName, int value);

	/**
	 * Условие равенства заголовка MIME.
	 * @param headerName Название заголовка MIME
	 * @param value Значение атрибута
	 * @return условие
	 */
	public abstract Condition headerIsEqualTo(String headerName, String value);

	/**
	 * Условие "больше или равно".
	 * @param attributeName логическое имя атрибута Exchange
	 * @param value значение атрибута
	 * @return условие
	 */
	public abstract Condition gte(String attributeName, String value);

	/**
	 * Условие "больше чем".
	 * @param attributeName название логического атрибута Exchange
	 * @param value значение атрибута
	 * @return условие
	 */
	public abstract Condition gt(String attributeName, String value);

	/**
	 * Условие "меньше чем".
	 * @param attributeName логическое имя атрибута Exchange
	 * @param value значение атрибута
	 * @return условие
	 */
	public abstract Condition lt(String attributeName, String value);

	/**
	 * Условие меньше или равно.
	 * @param attributeName логическое имя атрибута Exchange
	 * @param value значение атрибута
	 * @return условие
	 */
	@SuppressWarnings({ "UnusedDeclaration" })
	public abstract Condition lte(String attributeName, String value);

	/**
	 * Содержит условие.
	 * @param attributeName логическое имя атрибута Exchange
	 * @param value значение атрибута
	 * @return условие
	 */
	public abstract Condition contains(String attributeName, String value);

	/**
	 * Начинается с условия.
	 * @param attributeName логическое имя атрибута Exchange
	 * @param value значение атрибута
	 * @return условие
	 */
	public abstract Condition startsWith(String attributeName, String value);

	/**
	 * Условие для проверки на null.
	 * @param attributeName логическое имя атрибута Exchange
	 * @return условие
	 */
	public abstract Condition isNull(String attributeName);

	/**
	 * Условие существования.
	 * @param attributeName логическое имя атрибута Exchange
	 * @return условие
	 */
	public abstract Condition exists(String attributeName);

	/**
	 * Истинное условие.
	 * @param attributeName логическое имя атрибута обмена
	 * @return условие
	 */
	public abstract Condition isTrue(String attributeName);

	/**
	 * Является ли ложным условием.
	 * @param attributeName логическое имя атрибута Exchange
	 * @return условие
	 */
	public abstract Condition isFalse(String attributeName);

	/**
	 * Поиск почтовых и общих папок в заданной папке. Исключить папки календаря и
	 * контактов
	 * @param folderName Имя папки Exchange
	 * @param recursive глубокий поиск, если true
	 * @return список папок
	 * @throws IOException при ошибке
	 */
	public List<Folder> getSubFolders(String folderName, boolean recursive, boolean wildcard) throws IOException {
		MultiCondition folderCondition = and();
		if (!Settings.getBooleanProperty("mt.ews.imapIncludeSpecialFolders", false)) {
			folderCondition.add(or(isEqualTo(FOLDER_CLASS, "IPF.Note"),
					isEqualTo(FOLDER_CLASS, "IPF.Note.Microsoft.Conversation"), isNull(FOLDER_CLASS)));
		}
		if (wildcard) {
			folderCondition.add(startsWith(DISPLAY_NAME, folderName));
			folderName = "";
		}
		List<Folder> results = getSubFolders(folderName, folderCondition, recursive);
		// need to include base folder in recursive search, except on root
		if (recursive && !folderName.isEmpty()) {
			results.add(getFolder(folderName));
		}

		return results;
	}

	/**
	 * Поиск календарных папок в данной папке.
	 * @param folderName Имя папки Exchange
	 * @param recursive глубокий поиск, если true
	 * @return список папок
	 * @throws IOException при ошибке
	 */
	public List<Folder> getSubCalendarFolders(String folderName, boolean recursive) throws IOException {
		return getSubFolders(folderName, isEqualTo(FOLDER_CLASS, IPF_APPOINTMENT), recursive);
	}

	/**
	 * Ищет папки в заданной папке, соответствующие фильтру.
	 * @param folderName Название папки Exchange
	 * @param condition фильтр поиска
	 * @param recursive глубокий поиск, если true
	 * @return список папок
	 * @throws IOException в случае ошибки
	 */
	public abstract List<Folder> getSubFolders(String folderName, Condition condition, boolean recursive)
			throws IOException;

	/**
	 * Удалить старейшие сообщения в корзине. keepDelay - это количество дней, в течение
	 * которых сообщения будут храниться в корзине перед удалением
	 * @throws IOException при невозможности удалить сообщения
	 */
	public void purgeOldestTrashAndSentMessages() throws IOException {
		int keepDelay = Settings.getIntProperty("mt.ews.keepDelay");
		if (keepDelay != 0) {
			purgeOldestFolderMessages(TRASH, keepDelay);
		}
		// this is a new feature, default is : do nothing
		int sentKeepDelay = Settings.getIntProperty("mt.ews.sentKeepDelay");
		if (sentKeepDelay != 0) {
			purgeOldestFolderMessages(SENT, sentKeepDelay);
		}
	}

	protected void purgeOldestFolderMessages(String folderPath, int keepDelay) throws IOException {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, -keepDelay);
		log.debug("Delete messages in " + folderPath + " not modified since " + cal.getTime());

		MessageList messages = searchMessages(folderPath, UID_MESSAGE_ATTRIBUTES,
				lt(LAST_MODIFIED, formatSearchDate(cal.getTime())));

		for (Message message : messages) {
			message.delete();
		}
	}

	protected void convertResentHeader(MimeMessage mimeMessage, String headerName) throws MessagingException {
		String[] resentHeader = mimeMessage.getHeader("Resent-" + headerName);
		if (resentHeader != null) {
			mimeMessage.removeHeader("Resent-" + headerName);
			mimeMessage.removeHeader(headerName);
			for (String value : resentHeader) {
				mimeMessage.addHeader(headerName, value);
			}
		}
	}

	protected String lastSentMessageId;

	/**
	 * Отправить сообщение читателю получателям. Обнаружить видимых получателей в теле
	 * сообщения для определения получателей в скрытой копии (bcc)
	 * @param rcptToRecipients список получателей
	 * @param mimeMessage mime сообщение
	 * @throws IOException при ошибке
	 * @throws MessagingException при ошибке
	 */
	public void sendMessage(List<String> rcptToRecipients, MimeMessage mimeMessage)
			throws IOException, MessagingException {
		// detect duplicate send command
		String messageId = mimeMessage.getMessageID();
		if (lastSentMessageId != null && lastSentMessageId.equals(messageId)) {
			log.debug("Dropping message id {}: already sent", messageId);
			return;
		}
		lastSentMessageId = messageId;

		convertResentHeader(mimeMessage, "From");
		convertResentHeader(mimeMessage, "To");
		convertResentHeader(mimeMessage, "Cc");
		convertResentHeader(mimeMessage, "Bcc");
		convertResentHeader(mimeMessage, "Message-Id");

		// do not allow send as another user on Exchange 2003
		if ("Exchange2003".equals(serverVersion) || Settings.getBooleanProperty("mt.ews.smtpStripFrom", false)) {
			mimeMessage.removeHeader("From");
		}

		// remove visible recipients from list
		Set<String> visibleRecipients = new HashSet<>();
		List<InternetAddress> recipients = getAllRecipients(mimeMessage);
		for (InternetAddress address : recipients) {
			visibleRecipients.add((address.getAddress().toLowerCase()));
		}
		for (String recipient : rcptToRecipients) {
			if (!visibleRecipients.contains(recipient.toLowerCase())) {
				mimeMessage.addRecipient(javax.mail.Message.RecipientType.BCC, new InternetAddress(recipient));
			}
		}
		sendMessage(mimeMessage);

	}

	static final String[] RECIPIENT_HEADERS = { "to", "cc", "bcc" };

	protected List<InternetAddress> getAllRecipients(MimeMessage mimeMessage) throws MessagingException {
		List<InternetAddress> recipientList = new ArrayList<>();
		for (String recipientHeader : RECIPIENT_HEADERS) {
			final String recipientHeaderValue = mimeMessage.getHeader(recipientHeader, ",");
			if (recipientHeaderValue != null) {
				// parse headers in non strict mode
				recipientList.addAll(Arrays.asList(InternetAddress.parseHeader(recipientHeaderValue, false)));
			}

		}
		return recipientList;
	}

	/**
	 * Отправить MIME сообщение.
	 * @param mimeMessage MIME сообщение
	 * @throws IOException в случае ошибки
	 * @throws MessagingException в случае ошибки
	 */
	public abstract void sendMessage(MimeMessage mimeMessage) throws IOException, MessagingException;

	/**
	 * Получить объект папки. Имя папки может быть логическими именами INBOX, Drafts,
	 * Trash или календарь, или путем относительно базовой папки пользователя или
	 * абсолютным путем.
	 * @param folderPath путь к папке
	 * @return объект папки
	 * @throws IOException в случае ошибки
	 */
	public ExchangeSession.Folder getFolder(String folderPath) throws IOException {
		Folder folder = internalGetFolder(folderPath);
		if (isMainCalendar(folderPath)) {
			Folder taskFolder = internalGetFolder(TASKS);
			folder.ctag += taskFolder.ctag;
		}
		return folder;
	}

	protected abstract Folder internalGetFolder(String folderName) throws IOException;

	/**
	 * Проверить папку ctag и перезагрузить сообщения по мере необходимости.
	 * @param currentFolder текущая папка
	 * @return true, если папка изменилась
	 * @throws IOException в случае ошибки
	 */
	public boolean refreshFolder(Folder currentFolder) throws IOException {
		Folder newFolder = getFolder(currentFolder.folderPath);
		if (currentFolder.ctag == null || !currentFolder.ctag.equals(newFolder.ctag)
		// ctag stamp is limited to second, check message count
				|| currentFolder.count != newFolder.count) {
			if (log.isDebugEnabled()) {
				log.debug("Contenttag or count changed on {} ctag: {} => {} count: {} => {}, reloading messages",
						currentFolder.folderPath, currentFolder.ctag, newFolder.ctag, currentFolder.count,
						newFolder.count);
			}
			currentFolder.hasChildren = newFolder.hasChildren;
			currentFolder.noInferiors = newFolder.noInferiors;
			currentFolder.unreadCount = newFolder.unreadCount;
			currentFolder.ctag = newFolder.ctag;
			currentFolder.etag = newFolder.etag;
			if (newFolder.uidNext > currentFolder.uidNext) {
				currentFolder.uidNext = newFolder.uidNext;
			}
			currentFolder.loadMessages();
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Создать папку для сообщений обмена.
	 * @param folderName логическое имя папки
	 * @throws IOException при ошибке
	 */
	public void createMessageFolder(String folderName) throws IOException {
		createFolder(folderName, "IPF.Note", null);
	}

	/**
	 * Создать папку календаря Exchange.
	 * @param folderName логическое имя папки
	 * @param properties свойства папки
	 * @return статус
	 * @throws IOException при ошибке
	 */
	public int createCalendarFolder(String folderName, Map<String, String> properties) throws IOException {
		return createFolder(folderName, IPF_APPOINTMENT, properties);
	}

	/**
	 * Создать папку контактов Exchange.
	 * @param folderName логическое имя папки
	 * @param properties свойства папки
	 * @throws IOException в случае ошибки
	 */
	public void createContactFolder(String folderName, Map<String, String> properties) throws IOException {
		createFolder(folderName, "IPF.Contact", properties);
	}

	/**
	 * Создать папку Exchange с заданным классом папки.
	 * @param folderName логическое имя папки
	 * @param folderClass класс папки
	 * @param properties свойства папки
	 * @return статус
	 * @throws IOException в случае ошибки
	 */
	public abstract int createFolder(String folderName, String folderClass, Map<String, String> properties)
			throws IOException;

	/**
	 * Обновить свойства папки Exchange.
	 * @param folderName логическое имя папки
	 * @param properties свойства папки
	 * @return статус
	 * @throws IOException в случае ошибки
	 */
	public abstract int updateFolder(String folderName, Map<String, String> properties) throws IOException;

	/**
	 * Удалить папку Exchange.
	 * @param folderName логическое имя папки
	 * @throws IOException в случае ошибки
	 */
	public abstract void deleteFolder(String folderName) throws IOException;

	/**
	 * Скопировать сообщение в целевую папку
	 * @param message Сообщение обмена
	 * @param targetFolder Целевая папка
	 * @throws IOException при ошибке
	 */
	public abstract void copyMessage(Message message, String targetFolder) throws IOException;

	public void copyMessages(List<Message> messages, String targetFolder) throws IOException {
		for (Message message : messages) {
			copyMessage(message, targetFolder);
		}
	}

	/**
	 * Переместить сообщение в целевую папку
	 * @param message Сообщение обмена
	 * @param targetFolder целевая папка
	 * @throws IOException при ошибке
	 */
	public abstract void moveMessage(Message message, String targetFolder) throws IOException;

	public void moveMessages(List<Message> messages, String targetFolder) throws IOException {
		for (Message message : messages) {
			moveMessage(message, targetFolder);
		}
	}

	/**
	 * Переместить папку в целевое имя.
	 * @param folderName текущее имя/путь папки
	 * @param targetName целевое имя/путь папки
	 * @throws IOException при ошибке
	 */
	public abstract void moveFolder(String folderName, String targetName) throws IOException;

	/**
	 * Переместить элемент из исходного пути в целевой путь.
	 * @param sourcePath путь источника элемента
	 * @param targetPath путь назначения элемента
	 * @throws IOException при ошибке
	 */
	public abstract void moveItem(String sourcePath, String targetPath) throws IOException;

	protected abstract void moveToTrash(Message message) throws IOException;

	/**
	 * Преобразовать значение ключевого слова в флаг IMAP.
	 * @param value значение ключевого слова
	 * @return флаг IMAP
	 */
	public String convertKeywordToFlag(String value) {
		// first test for keyword in settings
		Properties flagSettings = Settings.getSubProperties("mt.ews.imapFlags");
		Enumeration<?> flagSettingsEnum = flagSettings.propertyNames();
		while (flagSettingsEnum.hasMoreElements()) {
			String key = (String) flagSettingsEnum.nextElement();
			if (value.equalsIgnoreCase(flagSettings.getProperty(key))) {
				return key;
			}
		}

		ResourceBundle flagBundle = ResourceBundle.getBundle("imapflags");
		Enumeration<String> flagBundleEnum = flagBundle.getKeys();
		while (flagBundleEnum.hasMoreElements()) {
			String key = flagBundleEnum.nextElement();
			if (value.equalsIgnoreCase(flagBundle.getString(key))) {
				return key;
			}
		}

		// fall back to raw value
		return value;
	}

	/**
	 * Преобразовать IMAP-флаг в значение ключевого слова.
	 * @param value IMAP-флаг
	 * @return значение ключевого слова
	 */
	public String convertFlagToKeyword(String value) {
		// first test for flag in settings
		Properties flagSettings = Settings.getSubProperties("mt.ews.imapFlags");
		// case insensitive lookup
		for (String key : flagSettings.stringPropertyNames()) {
			if (key.equalsIgnoreCase(value)) {
				return flagSettings.getProperty(key);
			}
		}

		// fall back to predefined flags
		ResourceBundle flagBundle = ResourceBundle.getBundle("imapflags");
		for (String key : flagBundle.keySet()) {
			if (key.equalsIgnoreCase(value)) {
				return flagBundle.getString(key);
			}
		}

		// fall back to raw value
		return value;
	}

	/**
	 * Преобразовать флаги IMAP в значение ключевого слова.
	 * @param flags Флаги IMAP
	 * @return значение ключевого слова
	 */
	public String convertFlagsToKeywords(Set<String> flags) {
		HashSet<String> keywordSet = new HashSet<>();
		for (String flag : flags) {
			keywordSet.add(decodeKeyword(convertFlagToKeyword(flag)));
		}
		return StringUtil.join(keywordSet, ",");
	}

	protected String decodeKeyword(String keyword) {
		String result = keyword;
		if (keyword.contains(X_0028_) || keyword.contains(X_0029_)) {
			result = result.replace(X_0028_, "(").replace(X_0029_, ")");
		}
		return result;
	}

	protected String encodeKeyword(String keyword) {
		String result = keyword;
		if (keyword.indexOf('(') >= 0 || keyword.indexOf(')') >= 0) {
			result = result.replace("(", X_0028_).replace(")", X_0029_);
		}
		return result;
	}

	/**
	 * Папка обмена с IMAP свойствами
	 */
	@Setter
	@Getter
	public class Folder {

		/**
		 * Логический (IMAP) путь к папке.
		 */
		private String folderPath;

		/**
		 * Имя пользователя.
		 */
		private String displayName;

		/**
		 * Класс папки (PR_CONTAINER_CLASS).
		 */
		private String folderClass;

		/**
		 * Количество сообщений в папке.
		 */
		private int count;

		/**
		 * Количество непрочитанных сообщений в папке.
		 */
		private int unreadCount;

		/**
		 * истинно, если папка имеет подпапки (DAV:hassubs).
		 */
		private boolean hasChildren;

		/**
		 * истинно, если папка не содержит подкаталогов (DAV:nosubs).
		 */
		private boolean noInferiors;

		/**
		 * Тег содержимого папки (для обнаружения изменений в содержимом папки).
		 */
		private String ctag;

		/**
		 * Этикет папки (для обнаружения изменений в объекте папки).
		 */
		private String etag;

		/**
		 * Следующий IMAP uid
		 */
		private long uidNext;

		/**
		 * недавнее количество
		 */
		private int recent;

		/**
		 * Список сообщений папки, пуст перед вызовом loadMessages.
		 */
		private ExchangeSession.MessageList messages;

		/**
		 * Постоянная uid (PR_SEARCH_KEY) к IMAP UID сопоставление.
		 */
		private final Map<String, Long> permanentUrlToImapUidMap = new HashMap<>();

		/**
		 * Получить флаги папки IMAP.
		 * @return флаги папки в формате IMAP
		 */
		public String getFlags() {
			String specialFlag = "";
			if (isSpecial()) {
				specialFlag = "\\" + folderPath + " ";
			}
			if (noInferiors) {
				return specialFlag + "\\NoInferiors";
			}
			else if (hasChildren) {
				return specialFlag + "\\HasChildren";
			}
			else {
				return specialFlag + "\\HasNoChildren";
			}
		}

		/**
		 * Специальный флаг папки (Отправленные, Черновики, Корзина, Спам).
		 * @return true если папка специальная
		 */
		public boolean isSpecial() {
			return SPECIAL.contains(folderPath);
		}

		/**
		 * Загрузить сообщения из папки.
		 * @throws IOException при ошибке
		 */
		public void loadMessages() throws IOException {
			messages = ExchangeSession.this.searchMessages(folderPath, null);
			fixUids(messages);
			recent = 0;
			for (Message message : messages) {
				if (message.recent) {
					recent++;
				}
			}
			long computedUidNext = 1;
			if (!messages.isEmpty()) {
				computedUidNext = messages.get(messages.size() - 1).getImapUid() + 1;
			}
			if (computedUidNext > uidNext) {
				uidNext = computedUidNext;
			}
		}

		/**
		 * Ищет сообщения в папке, соответствующие запросу.
		 * @param condition поисковый запрос
		 * @return список сообщений
		 * @throws IOException при ошибке
		 */
		public MessageList searchMessages(Condition condition) throws IOException {
			MessageList localMessages = ExchangeSession.this.searchMessages(folderPath, condition);
			fixUids(localMessages);
			return localMessages;
		}

		/**
		 * Восстановить предыдущие идентификаторы, измененные с помощью PROPPATCH
		 * (изменение флага).
		 * @param messages список сообщений
		 */
		protected void fixUids(MessageList messages) {
			boolean sortNeeded = false;
			for (Message message : messages) {
				if (permanentUrlToImapUidMap.containsKey(message.getPermanentId())) {
					long previousUid = permanentUrlToImapUidMap.get(message.getPermanentId());
					if (message.getImapUid() != previousUid) {
						log.debug("Restoring IMAP uid " + message.getImapUid() + " -> " + previousUid + " for message "
								+ message.getPermanentId());
						message.setImapUid(previousUid);
						sortNeeded = true;
					}
				}
				else {
					// add message to uid map
					permanentUrlToImapUidMap.put(message.getPermanentId(), message.getImapUid());
				}
			}
			if (sortNeeded) {
				Collections.sort(messages);
			}
		}

		/**
		 * Количество сообщений в папке.
		 * @return количество сообщений
		 */
		public int getMessagesCount() {
			if (messages == null) {
				return count;
			}
			else {
				return messages.size();
			}
		}

		/**
		 * Получить сообщение по индексу.
		 * @param index индекс сообщения
		 * @return сообщение
		 */
		public Message get(int index) {
			return messages.get(index);
		}

		/**
		 * Получить UID сообщений папки и флаги imap
		 * @return список uid imap
		 */
		public TreeMap<Long, String> getImapFlagMap() {
			TreeMap<Long, String> imapFlagMap = new TreeMap<>();
			for (ExchangeSession.Message message : messages) {
				imapFlagMap.put(message.getImapUid(), message.getImapFlags());
			}
			return imapFlagMap;
		}

		/**
		 * Флаг календарной папки.
		 * @return true, если это календарная папка
		 */
		public boolean isCalendar() {
			return IPF_APPOINTMENT.equals(folderClass);
		}

		/**
		 * Флаг папки контактов.
		 * @return true, если это папка календаря
		 */
		public boolean isContact() {
			return "IPF.Contact".equals(folderClass);
		}

		/**
		 * Флаг папки задач.
		 * @return true, если это папка задач
		 */
		public boolean isTask() {
			return "IPF.Task".equals(folderClass);
		}

		/**
		 * сбросить кэшированное сообщение
		 */
		public void clearCache() {
			messages.cachedMimeContent = null;
			messages.cachedMimeMessage = null;
			messages.cachedMessageImapUid = 0;
		}

	}

	/**
	 * Сообщение обмена.
	 */
	public abstract class Message implements Comparable<Message> {

		/**
		 * список вложенных сообщений
		 */
		public MessageList messageList;

		/**
		 * URL сообщения.
		 */
		public String messageUrl;

		/**
		 * Постоянный URL сообщения (не меняется при перемещении сообщения).
		 */
		public String permanentUrl;

		/**
		 * Уникальный идентификатор сообщения.
		 */
		public String uid;

		/**
		 * Класс содержимого сообщения.
		 */
		public String contentClass;

		/**
		 * Ключевые слова сообщения (категории).
		 */
		public String keywords;

		/**
		 * Сообщение IMAP uid, уникальное в папке (x0e230003).
		 */
		public long imapUid;

		/**
		 * Размер сообщения MAPI.
		 */
		public int size;

		/**
		 * Дата сообщения (urn:schemas:mailheader:date).
		 */
		public String date;

		/**
		 * Флаг сообщения: прочитано.
		 */
		public boolean read;

		/**
		 * Флаг сообщения: удалено.
		 */
		public boolean deleted;

		/**
		 * Флаг сообщения: мусор.
		 */
		public boolean junk;

		/**
		 * Флаг сообщения: помечено.
		 */
		public boolean flagged;

		/**
		 * Флаг сообщения: недавнее.
		 */
		public boolean recent;

		/**
		 * Флаг сообщения: черновик.
		 */
		public boolean draft;

		/**
		 * Флаг сообщения: отвечено.
		 */
		public boolean answered;

		/**
		 * Сообщение пометкой: переслано.
		 */
		public boolean forwarded;

		/**
		 * Необработанное содержимое сообщения.
		 */
		protected byte[] mimeContent;

		/**
		 * Содержимое сообщения, разобранное в MIME-сообщении.
		 */
		protected MimeMessage mimeMessage;

		/**
		 * Получить постоянный идентификатор сообщения. permanentUrl через WebDav или
		 * IitemId через EWS
		 * @return постоянный идентификатор
		 */
		public abstract String getPermanentId();

		/**
		 * IMAP uid, уникальный в папке (x0e230003)
		 * @return IMAP uid
		 */
		public long getImapUid() {
			return imapUid;
		}

		/**
		 * Установить IMAP uid.
		 * @param imapUid новый uid
		 */
		public void setImapUid(long imapUid) {
			this.imapUid = imapUid;
		}

		/**
		 * Обмен uid.
		 * @return uid
		 */
		public String getUid() {
			return uid;
		}

		/**
		 * Вернуть флаги сообщений в формате IMAP.
		 * @return Флаги IMAP
		 */
		public String getImapFlags() {
			StringBuilder buffer = new StringBuilder();
			if (read) {
				buffer.append("\\Seen ");
			}
			if (deleted) {
				buffer.append("\\Deleted ");
			}
			if (recent) {
				buffer.append("\\Recent ");
			}
			if (flagged) {
				buffer.append("\\Flagged ");
			}
			if (junk) {
				buffer.append("Junk ");
			}
			if (draft) {
				buffer.append("\\Draft ");
			}
			if (answered) {
				buffer.append("\\Answered ");
			}
			if (forwarded) {
				buffer.append("$Forwarded ");
			}
			if (keywords != null) {
				for (String keyword : keywords.split(",")) {
					buffer.append(encodeKeyword(convertKeywordToFlag(keyword))).append(" ");
				}
			}
			return buffer.toString().trim();
		}

		/**
		 * Загруить содержимое сообщения в Mime-сообщение
		 * @throws IOException в случае ошибки
		 * @throws MessagingException в случае ошибки
		 */
		public void loadMimeMessage() throws IOException, MessagingException {
			if (mimeMessage == null) {
				// try to get message content from cache
				if (this.imapUid == messageList.cachedMessageImapUid
						// make sure we never return null even with broken 0 uid message
						&& messageList.cachedMimeContent != null && messageList.cachedMimeMessage != null) {
					mimeContent = messageList.cachedMimeContent;
					mimeMessage = messageList.cachedMimeMessage;
					log.debug("Got message content for " + imapUid + " from cache");
				}
				else {
					// load and parse message
					mimeContent = getContent(this);
					mimeMessage = new MimeMessage(null, new SharedByteArrayInputStream(mimeContent));
					// workaround for Exchange 2003 ActiveSync bug
					if (mimeMessage.getHeader("MAIL FROM") != null) {
						// find start of actual message
						byte[] mimeContentCopy = new byte[((SharedByteArrayInputStream) mimeMessage.getRawInputStream())
							.available()];
						int offset = mimeContent.length - mimeContentCopy.length;
						// remove unwanted header
						System.arraycopy(mimeContent, offset, mimeContentCopy, 0, mimeContentCopy.length);
						mimeContent = mimeContentCopy;
						mimeMessage = new MimeMessage(null, new SharedByteArrayInputStream(mimeContent));
					}
					log.debug("Downloaded full message content for IMAP UID " + imapUid + " (" + mimeContent.length
							+ " bytes)");
				}
			}
		}

		/**
		 * Получить содержимое сообщения в виде Mime-сообщения.
		 * @return mime-сообщение
		 * @throws IOException в случае ошибки
		 * @throws MessagingException в случае ошибки
		 */
		public MimeMessage getMimeMessage() throws IOException, MessagingException {
			loadMimeMessage();
			return mimeMessage;
		}

		public Enumeration<?> getMatchingHeaderLinesFromHeaders(String[] headerNames) throws MessagingException {
			Enumeration<?> result = null;
			if (mimeMessage == null) {
				// message not loaded, try to get headers only
				InputStream headers = getMimeHeaders();
				if (headers != null) {
					InternetHeaders internetHeaders = new InternetHeaders(headers);
					if (internetHeaders.getHeader("Subject") == null) {
						// invalid header content
						return null;
					}
					if (headerNames == null) {
						result = internetHeaders.getAllHeaderLines();
					}
					else {
						result = internetHeaders.getMatchingHeaderLines(headerNames);
					}
				}
			}
			return result;
		}

		public Enumeration<?> getMatchingHeaderLines(String[] headerNames) throws MessagingException, IOException {
			Enumeration<?> result = getMatchingHeaderLinesFromHeaders(headerNames);
			if (result == null) {
				if (headerNames == null) {
					result = getMimeMessage().getAllHeaderLines();
				}
				else {
					result = getMimeMessage().getMatchingHeaderLines(headerNames);
				}

			}
			return result;
		}

		protected abstract InputStream getMimeHeaders();

		/**
		 * Получить размер тела сообщения.
		 * @return размер MIME-сообщения
		 * @throws IOException в случае ошибки
		 * @throws MessagingException в случае ошибки
		 */
		public int getMimeMessageSize() throws IOException, MessagingException {
			loadMimeMessage();
			return mimeContent.length;
		}

		/**
		 * Получить поток ввода тела сообщения.
		 * @return поток InputStream для mime-сообщения
		 * @throws IOException при ошибке
		 * @throws MessagingException при ошибке
		 */
		public InputStream getRawInputStream() throws IOException, MessagingException {
			loadMimeMessage();
			return new SharedByteArrayInputStream(mimeContent);
		}

		/**
		 * Удалить MIME-сообщение, чтобы избежать хранения содержимого сообщения в памяти,
		 * сохранить одно сообщение в кэше MessageList для обработки выборки по частям.
		 */
		public void dropMimeMessage() {
			// update single message cache
			if (mimeMessage != null) {
				messageList.cachedMessageImapUid = imapUid;
				messageList.cachedMimeContent = mimeContent;
				messageList.cachedMimeMessage = mimeMessage;
			}
			// drop curent message body to save memory
			mimeMessage = null;
			mimeContent = null;
		}

		public boolean isLoaded() {
			// check and retrieve cached content
			if (imapUid == messageList.cachedMessageImapUid) {
				mimeContent = messageList.cachedMimeContent;
				mimeMessage = messageList.cachedMimeMessage;
			}
			return mimeMessage != null;
		}

		/**
		 * Удалить сообщение.
		 * @throws IOException при ошибке
		 */
		public void delete() throws IOException {
			deleteMessage(this);
		}

		/**
		 * Переместить сообщение в корзину, отметить сообщение как прочитанное.
		 * @throws IOException при ошибке
		 */
		public void moveToTrash() throws IOException {
			markRead();

			ExchangeSession.this.moveToTrash(this);
		}

		/**
		 * Отметить сообщение как прочитанное.
		 * @throws IOException в случае ошибки
		 */
		public void markRead() throws IOException {
			HashMap<String, String> properties = new HashMap<>();
			properties.put("read", "1");
			updateMessage(this, properties);
		}

		/**
		 * Компаратор для сортировки сообщений по IMAP uid
		 * @param message другое сообщение
		 * @return результат сравнения imapUid
		 */
		public int compareTo(Message message) {
			long compareValue = (imapUid - message.imapUid);
			if (compareValue > 0) {
				return 1;
			}
			else if (compareValue < 0) {
				return -1;
			}
			else {
				return 0;
			}
		}

		/**
		 * Переопределить equals, сравнить IMAP uid
		 * @param message другое сообщение
		 * @return true, если IMAP uid равны
		 */
		@Override
		public boolean equals(Object message) {
			return message instanceof Message && imapUid == ((Message) message).imapUid;
		}

		/**
		 * Переопределите hashCode, верните хэш-код imapUid.
		 * @return хэш-код imapUid
		 */
		@Override
		public int hashCode() {
			return (int) (imapUid ^ (imapUid >>> 32));
		}

		public String removeFlag(String flag) {
			if (keywords != null) {
				final String exchangeFlag = convertFlagToKeyword(flag);
				Set<String> keywordSet = new HashSet<>();
				String[] keywordArray = keywords.split(",");
				for (String value : keywordArray) {
					if (!value.equalsIgnoreCase(exchangeFlag)) {
						keywordSet.add(value);
					}
				}
				keywords = StringUtil.join(keywordSet, ",");
			}
			return keywords;
		}

		public String addFlag(String flag) {
			final String exchangeFlag = convertFlagToKeyword(flag);
			HashSet<String> keywordSet = new HashSet<>();
			boolean hasFlag = false;
			if (keywords != null) {
				String[] keywordArray = keywords.split(",");
				for (String value : keywordArray) {
					keywordSet.add(value);
					if (value.equalsIgnoreCase(exchangeFlag)) {
						hasFlag = true;
					}
				}
			}
			if (!hasFlag) {
				keywordSet.add(exchangeFlag);
			}
			keywords = StringUtil.join(keywordSet, ",");
			return keywords;
		}

		public String setFlags(HashSet<String> flags) {
			keywords = convertFlagsToKeywords(flags);
			return keywords;
		}

	}

	/**
	 * Список сообщений, включает кэш одного сообщения
	 */
	public static class MessageList extends ArrayList<Message> {

		/**
		 * Закэшированное содержимое сообщения, разобранное в MIME-сообщении.
		 */
		protected transient MimeMessage cachedMimeMessage;

		/**
		 * Закэшированное uid сообщения.
		 */
		protected transient long cachedMessageImapUid;

		/**
		 * Кэшированное неразобранное сообщение
		 */
		protected transient byte[] cachedMimeContent;

	}

	/**
	 * Общий элемент папки.
	 */
	public abstract static class Item extends HashMap<String, String> {

		protected String folderPath;

		protected String itemName;

		protected String permanentUrl;

		/**
		 * Отображаемое имя.
		 */
		public String displayName;

		/**
		 * элемент etag
		 */
		public String etag;

		protected String noneMatch;

		/**
		 * Создать экземпляр элемента.
		 * @param folderPath путь к папке
		 * @param itemName имя класса элемента
		 * @param etag etag элемента
		 * @param noneMatch флаг отсутствия совпадений
		 */
		public Item(String folderPath, String itemName, String etag, String noneMatch) {
			this.folderPath = folderPath;
			this.itemName = itemName;
			this.etag = etag;
			this.noneMatch = noneMatch;
		}

		/**
		 * Конструктор по умолчанию.
		 */
		protected Item() {
		}

		/**
		 * Вернуть тип содержимого элемента
		 * @return тип содержимого
		 */
		public abstract String getContentType();

		/**
		 * Получить тело элемента от Exchange
		 * @return тело элемента
		 * @throws IOException при ошибке
		 */
		public abstract String getBody() throws IOException;

		/**
		 * Получить имя события (часть имени файла в URL).
		 * @return имя события
		 */
		public String getName() {
			return itemName;
		}

		/**
		 * Получить etag события (метка последнего изменения).
		 * @return etag события
		 */
		public String getEtag() {
			return etag;
		}

		/**
		 * Установить ссылку элемента.
		 * @param href ссылка элемента
		 */
		public void setHref(String href) {
			int index = href.lastIndexOf('/');
			if (index >= 0) {
				folderPath = href.substring(0, index);
				itemName = href.substring(index + 1);
			}
			else {
				throw new IllegalArgumentException(href);
			}
		}

		/**
		 * Возвращает href элемента.
		 * @return href элемента
		 */
		public String getHref() {
			return folderPath + '/' + itemName;
		}

		public void setItemName(String itemName) {
			this.itemName = itemName;
		}

	}

	/**
	 * Объект контакта
	 */
	public abstract class Contact extends Item {

		protected ArrayList<String> distributionListMembers = null;

		protected String vCardVersion;

		public Contact(String folderPath, String itemName, Map<String, String> properties, String etag,
				String noneMatch) {
			super(folderPath,
					itemName.endsWith(".vcf") ? itemName.substring(0, itemName.length() - 3) + "EML" : itemName, etag,
					noneMatch);
			this.putAll(properties);
		}

		protected Contact() {
		}

		public void setVCardVersion(String vCardVersion) {
			this.vCardVersion = vCardVersion;
		}

		public abstract ItemResult createOrUpdate() throws IOException;

		/**
		 * Преобразовать расширение EML в vcf.
		 * @return имя элемента
		 */
		@Override
		public String getName() {
			String name = super.getName();
			if (name.endsWith(".EML")) {
				name = name.substring(0, name.length() - 3) + "vcf";
			}
			return name;
		}

		/**
		 * Установить имя контакта
		 * @param name имя контакта
		 */
		public void setName(String name) {
			this.itemName = name;
		}

		/**
		 * Вычислить vcard uid из имени.
		 * @return uid
		 */
		public String getUid() {
			String uid = getName();
			int dotIndex = uid.lastIndexOf('.');
			if (dotIndex > 0) {
				uid = uid.substring(0, dotIndex);
			}
			return URIUtil.encodePath(uid);
		}

		@Override
		public String getContentType() {
			return "text/vcard";
		}

		public void addMember(String member) {
			if (distributionListMembers == null) {
				distributionListMembers = new ArrayList<>();
			}
			distributionListMembers.add(member);
		}

		@Override
		public String getBody() {
			// build RFC 2426 VCard from contact information
			VCardWriter writer = new VCardWriter();
			writer.startCard(vCardVersion);
			writer.appendProperty("UID", getUid());
			// common name
			String cn = get("cn");
			if (cn == null) {
				cn = get(DISPLAY_NAME);
			}
			String sn = get("sn");
			if (sn == null) {
				sn = cn;
			}
			writer.appendProperty("FN", cn);
			// RFC 2426: Family Name, Given Name, Additional Names, Honorific Prefixes,
			// and Honorific Suffixes
			writer.appendProperty("N", sn, get("givenName"), get("middlename"), get("personaltitle"),
					get("namesuffix"));

			if (distributionListMembers != null) {
				writer.appendProperty("KIND", "group");
				for (String member : distributionListMembers) {
					writer.appendProperty("MEMBER", member);
				}
			}

			writer.appendProperty("TEL;TYPE=cell", get("mobile"));
			writer.appendProperty("TEL;TYPE=work", get("telephoneNumber"));
			writer.appendProperty("TEL;TYPE=home", get("homePhone"));
			writer.appendProperty("TEL;TYPE=fax", get("facsimiletelephonenumber"));
			writer.appendProperty("TEL;TYPE=pager", get("pager"));
			writer.appendProperty("TEL;TYPE=car", get("othermobile"));
			writer.appendProperty("TEL;TYPE=home,fax", get("homefax"));
			writer.appendProperty("TEL;TYPE=isdn", get("internationalisdnnumber"));
			writer.appendProperty("TEL;TYPE=msg", get("otherTelephone"));

			// The structured type value corresponds, in sequence, to the post office box;
			// the extended address;
			// the street address; the locality (e.g., city); the region (e.g., state or
			// province);
			// the postal code; the country name
			writer.appendProperty("ADR;TYPE=home", get("homepostofficebox"), null, get("homeStreet"), get("homeCity"),
					get("homeState"), get("homePostalCode"), get("homeCountry"));
			writer.appendProperty("ADR;TYPE=work", get("postofficebox"), get("roomnumber"), get("street"), get("l"),
					get("st"), get("postalcode"), get("co"));
			writer.appendProperty("ADR;TYPE=other", get("otherpostofficebox"), null, get("otherstreet"),
					get("othercity"), get("otherstate"), get("otherpostalcode"), get("othercountry"));

			writer.appendProperty("EMAIL;TYPE=work", get("smtpemail1"));
			writer.appendProperty("EMAIL;TYPE=home", get("smtpemail2"));
			writer.appendProperty("EMAIL;TYPE=other", get("smtpemail3"));

			writer.appendProperty("ORG", get("o"), get("department"));
			writer.appendProperty("URL;TYPE=work", get("businesshomepage"));
			writer.appendProperty("URL;TYPE=home", get("personalHomePage"));
			writer.appendProperty("TITLE", get("title"));
			writer.appendProperty("NOTE", get("description"));

			writer.appendProperty("CUSTOM1", get("extensionattribute1"));
			writer.appendProperty("CUSTOM2", get("extensionattribute2"));
			writer.appendProperty("CUSTOM3", get("extensionattribute3"));
			writer.appendProperty("CUSTOM4", get("extensionattribute4"));

			writer.appendProperty("ROLE", get("profession"));
			writer.appendProperty("NICKNAME", get("nickname"));
			writer.appendProperty("X-AIM", get("im"));

			writer.appendProperty("BDAY", convertZuluDateToBday(get("bday")));
			writer.appendProperty("ANNIVERSARY", convertZuluDateToBday(get("anniversary")));

			String gender = get("gender");
			if ("1".equals(gender)) {
				writer.appendProperty("SEX", "2");
			}
			else if ("2".equals(gender)) {
				writer.appendProperty("SEX", "1");
			}

			writer.appendProperty("CATEGORIES", get(KEYWORDS));

			writer.appendProperty("FBURL", get("fburl"));

			if ("1".equals(get("private"))) {
				writer.appendProperty("CLASS", "PRIVATE");
			}

			writer.appendProperty("X-ASSISTANT", get("secretarycn"));
			writer.appendProperty("X-MANAGER", get("manager"));
			writer.appendProperty("X-SPOUSE", get("spousecn"));

			writer.appendProperty("REV", get(LAST_MODIFIED));

			ContactPhoto contactPhoto = null;

			if (Settings.getBooleanProperty("mt.ews.carddavReadPhoto", true)) {
				if (("true".equals(get("haspicture")))) {
					try {
						contactPhoto = getContactPhoto(this);
					}
					catch (IOException e) {
						log.warn("Unable to get photo from contact " + this.get("cn"));
					}
				}

				if (contactPhoto == null) {
					contactPhoto = getADPhoto(get("smtpemail1"));
				}
			}

			if (contactPhoto != null) {
				writer.writeLine("PHOTO;TYPE=" + contactPhoto.contentType + ";ENCODING=BASE64:");
				writer.writeLine(contactPhoto.content, true);
			}

			writer.endCard();
			return writer.toString();
		}

	}

	/**
	 * Объект календарного события.
	 */
	public abstract class Event extends Item {

		protected String contentClass;

		protected String subject;

		protected VCalendar vCalendar;

		public Event(String folderPath, String itemName, String contentClass, String itemBody, String etag,
				String noneMatch) throws IOException {
			super(folderPath, itemName, etag, noneMatch);
			this.contentClass = contentClass;
			fixICS(itemBody.getBytes(StandardCharsets.UTF_8), false);
			// fix task item name
			if (vCalendar.isTodo() && this.itemName.endsWith(".ics")) {
				this.itemName = itemName.substring(0, itemName.length() - 3) + "EML";
			}
		}

		protected Event() {
		}

		@Override
		public String getContentType() {
			return "text/calendar;charset=UTF-8";
		}

		@Override
		public String getBody() throws IOException {
			if (vCalendar == null) {
				fixICS(getEventContent(), true);
			}
			return vCalendar.toString();
		}

		protected HttpNotFoundException buildHttpNotFoundException(Exception e) {
			String message = "Unable to get event " + getName() + " subject: " + subject + " at " + permanentUrl + ": "
					+ e.getMessage();
			log.warn(message);
			return new HttpNotFoundException(message);
		}

		/**
		 * Получить тело элемента из Exchange
		 * @return содержимое элемента
		 * @throws IOException в случае ошибки
		 */
		public abstract byte[] getEventContent() throws IOException;

		protected static final String TEXT_CALENDAR = "text/calendar";

		protected static final String APPLICATION_ICS = "application/ics";

		protected boolean isCalendarContentType(String contentType) {
			return TEXT_CALENDAR.regionMatches(true, 0, contentType, 0, TEXT_CALENDAR.length())
					|| APPLICATION_ICS.regionMatches(true, 0, contentType, 0, APPLICATION_ICS.length());
		}

		protected MimePart getCalendarMimePart(MimeMultipart multiPart) throws IOException, MessagingException {
			MimePart bodyPart = null;
			for (int i = 0; i < multiPart.getCount(); i++) {
				String contentType = multiPart.getBodyPart(i).getContentType();
				if (isCalendarContentType(contentType)) {
					bodyPart = (MimePart) multiPart.getBodyPart(i);
					break;
				}
				else if (contentType.startsWith("multipart")) {
					Object content = multiPart.getBodyPart(i).getContent();
					if (content instanceof MimeMultipart) {
						bodyPart = getCalendarMimePart((MimeMultipart) content);
					}
				}
			}

			return bodyPart;
		}

		/**
		 * Загрузка ICS содержимого из входного потока MIME-сообщения
		 * @param mimeInputStream входной поток MIME-сообщения
		 * @return тело вложения ics мим-сообщения
		 * @throws IOException в случае ошибки
		 * @throws MessagingException в случае ошибки
		 */
		protected byte[] getICS(InputStream mimeInputStream) throws IOException, MessagingException {
			byte[] result;
			MimeMessage mimeMessage = new MimeMessage(null, mimeInputStream);
			String[] contentClassHeader = mimeMessage.getHeader("Content-class");
			// task item, return null
			if (contentClassHeader != null && contentClassHeader.length > 0
					&& "urn:content-classes:task".equals(contentClassHeader[0])) {
				return null;
			}
			Object mimeBody = mimeMessage.getContent();
			MimePart bodyPart = null;
			if (mimeBody instanceof MimeMultipart) {
				bodyPart = getCalendarMimePart((MimeMultipart) mimeBody);
			}
			else if (isCalendarContentType(mimeMessage.getContentType())) {
				// no multipart, single body
				bodyPart = mimeMessage;
			}

			if (bodyPart != null) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				bodyPart.getDataHandler().writeTo(baos);
				baos.close();
				result = baos.toByteArray();
			}
			else {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				mimeMessage.writeTo(baos);
				baos.close();
				throw new MosTechEwsException("EXCEPTION_INVALID_MESSAGE_CONTENT",
						new String(baos.toByteArray(), StandardCharsets.UTF_8));
			}
			return result;
		}

		protected void fixICS(byte[] icsContent, boolean fromServer) throws IOException {
			if (log.isDebugEnabled() && fromServer) {
				dumpIndex++;
				String icsBody = new String(icsContent, StandardCharsets.UTF_8);
				dumpICS(icsBody, true, false);
				log.debug("Vcalendar body received from server:\n" + icsBody);
			}
			vCalendar = new VCalendar(icsContent, getEmail(), getVTimezone());
			vCalendar.fixVCalendar(fromServer);
			if (log.isDebugEnabled() && !fromServer) {
				String resultString = vCalendar.toString();
				log.debug("Fixed Vcalendar body to server:\n" + resultString);
				dumpICS(resultString, false, true);
			}
		}

		protected void dumpICS(String icsBody, boolean fromServer, boolean after) {
			String logFileDirectory = Settings.getLogFileDirectory();

			// additional setting to activate ICS dump (not available in GUI)
			int dumpMax = Settings.getIntProperty("mt.ews.dumpICS");
			if (dumpMax > 0) {
				if (dumpIndex > dumpMax) {
					// Delete the oldest dump file
					final int oldest = dumpIndex - dumpMax;
					try {
						File[] oldestFiles = (new File(logFileDirectory)).listFiles((dir, name) -> {
							if (name.endsWith(".ics")) {
								int dashIndex = name.indexOf('-');
								if (dashIndex > 0) {
									try {
										int fileIndex = Integer.parseInt(name.substring(0, dashIndex));
										return fileIndex < oldest;
									}
									catch (NumberFormatException nfe) {
										// ignore
									}
								}
							}
							return false;
						});
						if (oldestFiles != null) {
							for (File file : oldestFiles) {
								if (!file.delete()) {
									log.warn("Unable to delete " + file.getAbsolutePath());
								}
							}
						}
					}
					catch (Exception ex) {
						log.warn("Error deleting ics dump: " + ex.getMessage());
					}
				}

				StringBuilder filePath = new StringBuilder();
				filePath.append(logFileDirectory)
					.append('/')
					.append(dumpIndex)
					.append(after ? "-to" : "-from")
					.append((after ^ fromServer) ? "-server" : "-client")
					.append(".ics");
				if ((icsBody != null) && (icsBody.length() > 0)) {
					OutputStreamWriter writer = null;
					try {
						writer = new OutputStreamWriter(new FileOutputStream(filePath.toString()),
								StandardCharsets.UTF_8);
						writer.write(icsBody);
					}
					catch (IOException e) {
						log.error("", e);
					}
					finally {
						if (writer != null) {
							try {
								writer.close();
							}
							catch (IOException e) {
								log.error("", e);
							}
						}
					}

				}
			}

		}

		/**
		 * Создать Mime тело для события или сообщения события.
		 * @return mimeContent в виде массива байтов или null
		 * @throws IOException в случае ошибки
		 */
		public byte[] createMimeContent() throws IOException {
			String boundary = UUID.randomUUID().toString();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			MimeOutputStreamWriter writer = new MimeOutputStreamWriter(baos);

			writer.writeHeader("Content-Transfer-Encoding", "7bit");
			writer.writeHeader("Content-class", contentClass);
			// append date
			writer.writeHeader("Date", new Date());

			// Make sure invites have a proper subject line
			String vEventSubject = vCalendar.getFirstVeventPropertyValue("SUMMARY");
			if (vEventSubject == null) {
				vEventSubject = BundleMessage.format("MEETING_REQUEST");
			}

			// Write a part of the message that contains the
			// ICS description so that invites contain the description text
			String description = vCalendar.getFirstVeventPropertyValue("DESCRIPTION");

			// handle notifications
			if ("urn:content-classes:calendarmessage".equals(contentClass)) {
				// need to parse attendees and organizer to build recipients
				VCalendar.Recipients recipients = vCalendar.getRecipients(true);
				String to;
				String cc;
				String notificationSubject;
				if (email.equalsIgnoreCase(recipients.organizer)) {
					// current user is organizer => notify all
					to = recipients.attendees;
					cc = recipients.optionalAttendees;
					notificationSubject = subject;
				}
				else {
					String status = vCalendar.getAttendeeStatus();
					// notify only organizer
					to = recipients.organizer;
					cc = null;
					notificationSubject = (status != null) ? (BundleMessage.format(status) + vEventSubject) : subject;
					description = "";
				}

				// Allow end user notification edit
				if (Settings.getBooleanProperty("mt.ews.caldavEditNotifications")) {
					// create notification edit dialog
					NotificationDialog notificationDialog = new NotificationDialog(to, cc, notificationSubject,
							description);
					if (!notificationDialog.getSendNotification()) {
						log.debug("Notification canceled by user");
						return null;
					}
					// get description from dialog
					to = notificationDialog.getTo();
					cc = notificationDialog.getCc();
					notificationSubject = notificationDialog.getSubject();
					description = notificationDialog.getBody();
				}

				// do not send notification if no recipients found
				if ((to == null || to.length() == 0) && (cc == null || cc.length() == 0)) {
					return null;
				}

				writer.writeHeader("To", to);
				writer.writeHeader("Cc", cc);
				writer.writeHeader("Subject", notificationSubject);

				if (log.isDebugEnabled()) {
					StringBuilder logBuffer = new StringBuilder("Sending notification ");
					if (to != null) {
						logBuffer.append("to: ").append(to);
					}
					if (cc != null) {
						logBuffer.append("cc: ").append(cc);
					}
					log.debug(logBuffer.toString());
				}
			}
			else {
				// need to parse attendees and organizer to build recipients
				VCalendar.Recipients recipients = vCalendar.getRecipients(false);
				// storing appointment, full recipients header
				if (recipients.attendees != null) {
					writer.writeHeader("To", recipients.attendees);
				}
				else {
					// use current user as attendee
					writer.writeHeader("To", email);
				}
				writer.writeHeader("Cc", recipients.optionalAttendees);

				if (recipients.organizer != null) {
					writer.writeHeader("From", recipients.organizer);
				}
				else {
					writer.writeHeader("From", email);
				}
			}
			if (vCalendar.getMethod() == null) {
				vCalendar.setPropertyValue("METHOD", "REQUEST");
			}
			writer.writeHeader("MIME-Version", "1.0");
			writer.writeHeader("Content-Type",
					"multipart/alternative;\r\n" + "\tboundary=\"----=_NextPart_" + boundary + '\"');
			writer.writeLn();
			writer.writeLn("This is a multi-part message in MIME format.");
			writer.writeLn();
			writer.writeLn("------=_NextPart_" + boundary);

			if (description != null && description.length() > 0) {
				writer.writeHeader("Content-Type", "text/plain;\r\n" + "\tcharset=\"utf-8\"");
				writer.writeHeader("content-transfer-encoding", "8bit");
				writer.writeLn();
				writer.flush();
				baos.write(description.getBytes(StandardCharsets.UTF_8));
				writer.writeLn();
				writer.writeLn("------=_NextPart_" + boundary);
			}
			writer.writeHeader("Content-class", contentClass);
			writer.writeHeader("Content-Type",
					"text/calendar;\r\n" + "\tmethod=" + vCalendar.getMethod() + ";\r\n" + "\tcharset=\"utf-8\"");
			writer.writeHeader("Content-Transfer-Encoding", "8bit");
			writer.writeLn();
			writer.flush();
			baos.write(vCalendar.toString().getBytes(StandardCharsets.UTF_8));
			writer.writeLn();
			writer.writeLn("------=_NextPart_" + boundary + "--");
			writer.close();
			return baos.toByteArray();
		}

		/**
		 * Создать или обновить элемент
		 * @return результат действия
		 * @throws IOException в случае ошибки
		 */
		public abstract ItemResult createOrUpdate() throws IOException;

	}

	protected abstract Set<String> getItemProperties();

	/**
	 * Поиск контактов в указанной папке.
	 * @param folderPath Путь к папке Exchange
	 * @param includeDistList включить списки рассылки
	 * @return список контактов
	 * @throws IOException при ошибке
	 */
	public List<ExchangeSession.Contact> getAllContacts(String folderPath, boolean includeDistList) throws IOException {
		return searchContacts(folderPath, ExchangeSession.CONTACT_ATTRIBUTES,
				isEqualTo("outlookmessageclass", "IPM.Contact"), 0);
	}

	/**
	 * Поиск контактов в указанной папке, соответствующих поисковому запросу.
	 * @param folderPath Путь к папке Exchange
	 * @param attributes запрашиваемые атрибуты
	 * @param condition Поисковый запрос Exchange
	 * @param maxCount максимальное количество элементов
	 * @return список контактов
	 * @throws IOException при ошибке
	 */
	public abstract List<Contact> searchContacts(String folderPath, Set<String> attributes, Condition condition,
			int maxCount) throws IOException;

	/**
	 * Поиск сообщений календаря в указанной папке.
	 * @param folderPath Путь к папке Exchange
	 * @return список сообщений календаря в виде объектов Event
	 * @throws IOException при ошибке
	 */
	public abstract List<Event> getEventMessages(String folderPath) throws IOException;

	/**
	 * Поиск событий календаря в указанной папке.
	 * @param folderPath Путь к папке Exchange
	 * @return список событий календаря
	 * @throws IOException при ошибке
	 */
	public List<Event> getAllEvents(String folderPath) throws IOException {
		List<Event> results = searchEvents(folderPath, getCalendarItemCondition(getPastDelayCondition("dtstart")));

		if (!Settings.getBooleanProperty("mt.ews.caldavDisableTasks", false) && isMainCalendar(folderPath)) {
			// retrieve tasks from main tasks folder
			results.addAll(searchTasksOnly(TASKS));
		}

		return results;
	}

	protected abstract Condition getCalendarItemCondition(Condition dateCondition);

	protected Condition getPastDelayCondition(String attribute) {
		int caldavPastDelay = Settings.getIntProperty("mt.ews.caldavPastDelay");
		Condition dateCondition = null;
		if (caldavPastDelay != 0) {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_MONTH, -caldavPastDelay);
			dateCondition = gt(attribute, formatSearchDate(cal.getTime()));
		}
		return dateCondition;
	}

	protected Condition getRangeCondition(String timeRangeStart, String timeRangeEnd) throws IOException {
		try {
			SimpleDateFormat parser = getZuluDateFormat();
			ExchangeSession.MultiCondition andCondition = and();
			if (timeRangeStart != null) {
				andCondition.add(gt("dtend", formatSearchDate(parser.parse(timeRangeStart))));
			}
			if (timeRangeEnd != null) {
				andCondition.add(lt("dtstart", formatSearchDate(parser.parse(timeRangeEnd))));
			}
			return andCondition;
		}
		catch (ParseException e) {
			throw new IOException(e + " " + e.getMessage());
		}
	}

	/**
	 * Поиск событий между началом и концом.
	 * @param folderPath Путь к папке обмена
	 * @param timeRangeStart Начало диапазона дат в формате zulu
	 * @param timeRangeEnd Конец диапазона дат в формате zulu
	 * @return список календарных событий
	 * @throws IOException в случае ошибки
	 */
	public List<Event> searchEvents(String folderPath, String timeRangeStart, String timeRangeEnd) throws IOException {
		Condition dateCondition = getRangeCondition(timeRangeStart, timeRangeEnd);
		Condition condition = getCalendarItemCondition(dateCondition);

		return searchEvents(folderPath, condition);
	}

	/**
	 * Ищет события между началом и концом, исключая задачи.
	 * @param folderPath Путь к папке Exchange
	 * @param timeRangeStart начало диапазона дат в формате зулу
	 * @param timeRangeEnd конец диапазона дат в формате зулу
	 * @return список календарных событий
	 * @throws IOException при ошибке
	 */
	public List<Event> searchEventsOnly(String folderPath, String timeRangeStart, String timeRangeEnd)
			throws IOException {
		Condition dateCondition = getRangeCondition(timeRangeStart, timeRangeEnd);
		return searchEvents(folderPath, getCalendarItemCondition(dateCondition));
	}

	/**
	 * Искать только задачи (VTODO).
	 * @param folderPath Путь к папке Exchange
	 * @return список задач
	 * @throws IOException в случае ошибки
	 */
	public List<Event> searchTasksOnly(String folderPath) throws IOException {
		return searchEvents(folderPath, and(isEqualTo("outlookmessageclass", "IPM.Task"),
				or(isNull("datecompleted"), getPastDelayCondition("datecompleted"))));
	}

	/**
	 * Ищет события календаря в предоставленной папке.
	 * @param folderPath Путь к папке Exchange
	 * @param filter фильтр поиска
	 * @return список событий календаря
	 * @throws IOException при ошибке
	 */
	public List<Event> searchEvents(String folderPath, Condition filter) throws IOException {

		Condition privateCondition = null;
		if (isSharedFolder(folderPath) && Settings.getBooleanProperty("mt.ews.excludePrivateEvents", true)) {
			log.debug("Shared or public calendar: exclude private events");
			privateCondition = isEqualTo("sensitivity", 0);
		}

		return searchEvents(folderPath, getItemProperties(), and(filter, privateCondition));
	}

	/**
	 * Поиск событий календаря или сообщений в указанной папке, соответствующих запросу
	 * поиска.
	 * @param folderPath Путь к папке Exchange
	 * @param attributes запрашиваемые атрибуты
	 * @param condition Запрос поиска Exchange
	 * @return список календарных сообщений в виде объектов Event
	 * @throws IOException в случае ошибки
	 */
	public abstract List<Event> searchEvents(String folderPath, Set<String> attributes, Condition condition)
			throws IOException;

	/**
	 * преобразовать расширение vcf в EML.
	 * @param itemName имя элемента
	 * @return имя элемента EML
	 */
	protected String convertItemNameToEML(String itemName) {
		if (itemName.endsWith(".vcf")) {
			return itemName.substring(0, itemName.length() - 3) + "EML";
		}
		else {
			return itemName;
		}
	}

	/**
	 * Получить элемент с именем eventName в папке
	 * @param folderPath Путь к папке Exchange
	 * @param itemName Имя события
	 * @return Объект события
	 * @throws IOException в случае ошибки
	 */
	public abstract Item getItem(String folderPath, String itemName) throws IOException;

	/**
	 * Контактное изображение
	 */
	public static class ContactPhoto {

		/**
		 * Тип содержимого изображения контакта (всегда image/jpeg при чтении)
		 */
		public String contentType;

		/**
		 * Содержимое изображения в кодировке Base64
		 */
		public String content;

	}

	/**
	 * Получить фотографию контакта, прикрепленную к контакту
	 * @param contact контакт адресной книги
	 * @return фотография контакта
	 * @throws IOException в случае ошибки
	 */
	public abstract ContactPhoto getContactPhoto(Contact contact) throws IOException;

	/**
	 * Получить фото контакта из AD
	 * @param адрес книги контактов
	 * @return фото контакта
	 */
	public ContactPhoto getADPhoto(String email) {
		return null;
	}

	/**
	 * Удалить событие с именем itemName в папке
	 * @param folderPath Путь к папке Exchange
	 * @param itemName имя элемента
	 * @throws IOException в случае ошибки
	 */
	public abstract void deleteItem(String folderPath, String itemName) throws IOException;

	/**
	 * Пометить событие как обработанное с именем eventName в папке
	 * @param folderPath Путь к папке Exchange
	 * @param itemName имя элемента
	 * @throws IOException в случае ошибки
	 */
	public abstract void processItem(String folderPath, String itemName) throws IOException;

	private static int dumpIndex;

	/**
	 * Заменить пути принципала iCal4 (Snow Leopard) на выражение mailto
	 * @param value значение участника или строка ics
	 * @return исправленное значение
	 */
	protected String replaceIcal4Principal(String value) {
		if (value != null && value.contains("/principals/__uuids__/")) {
			return value.replaceAll("/principals/__uuids__/([^/]*)__AT__([^/]*)/", "mailto:$1@$2");
		}
		else {
			return value;
		}
	}

	/**
	 * Объект результата события для хранения HTTP статуса и etag события
	 * создания/обновления.
	 */
	public static class ItemResult {

		/**
		 * Статус HTTP
		 */
		public int status;

		/**
		 * Событие etag из HTTP заголовка ответа
		 */
		public String etag;

		/**
		 * Название созданного элемента
		 */
		public String itemName;

	}

	/**
	 * Собрать и отправить MIME-сообщение для предоставленного события ICS.
	 * @param icsBody событие в формате iCalendar
	 * @return HTTP статус
	 * @throws IOException в случае ошибки
	 */
	public abstract int sendEvent(String icsBody) throws IOException;

	/**
	 * Создать или обновить элемент (событие или контакт) на сервере Exchange
	 * @param folderPath Путь к папке Exchange
	 * @param itemName имя события
	 * @param itemBody тело события в формате iCalendar
	 * @param etag предыдущий etag события для обнаружения одновременных обновлений
	 * @param noneMatch значение заголовка if-none-match
	 * @return HTTP-ответ результат события (статус и etag)
	 * @throws IOException при ошибке
	 */
	public ItemResult createOrUpdateItem(String folderPath, String itemName, String itemBody, String etag,
			String noneMatch) throws IOException {
		if (itemBody.startsWith("BEGIN:VCALENDAR")) {
			return internalCreateOrUpdateEvent(folderPath, itemName, "urn:content-classes:appointment", itemBody, etag,
					noneMatch);
		}
		else if (itemBody.startsWith("BEGIN:VCARD")) {
			return createOrUpdateContact(folderPath, itemName, itemBody, etag, noneMatch);
		}
		else {
			throw new IOException(BundleMessage.format("EXCEPTION_INVALID_MESSAGE_CONTENT", itemBody));
		}
	}

	static final String[] VCARD_N_PROPERTIES = { "sn", "givenName", "middlename", "personaltitle", "namesuffix" };
	static final String[] VCARD_ADR_HOME_PROPERTIES = { "homepostofficebox", null, "homeStreet", "homeCity",
			"homeState", "homePostalCode", "homeCountry" };
	static final String[] VCARD_ADR_WORK_PROPERTIES = { "postofficebox", "roomnumber", "street", "l", "st",
			"postalcode", "co" };
	static final String[] VCARD_ADR_OTHER_PROPERTIES = { "otherpostofficebox", null, "otherstreet", "othercity",
			"otherstate", "otherpostalcode", "othercountry" };
	static final String[] VCARD_ORG_PROPERTIES = { "o", "department" };

	protected void convertContactProperties(Map<String, String> properties, String[] contactProperties,
			List<String> values) {
		for (int i = 0; i < values.size() && i < contactProperties.length; i++) {
			if (contactProperties[i] != null) {
				properties.put(contactProperties[i], values.get(i));
			}
		}
	}

	protected ItemResult createOrUpdateContact(String folderPath, String itemName, String itemBody, String etag,
			String noneMatch) throws IOException {
		// parse VCARD body to build contact property map
		Map<String, String> properties = new HashMap<>();

		VObject vcard = new VObject(new ICSBufferedReader(new StringReader(itemBody)));
		if ("group".equalsIgnoreCase(vcard.getPropertyValue("KIND"))) {
			properties.put("outlookmessageclass", "IPM.DistList");
			properties.put(DISPLAY_NAME, vcard.getPropertyValue("FN"));
		}
		else {
			properties.put("outlookmessageclass", "IPM.Contact");

			for (VProperty property : vcard.getProperties()) {
				if ("FN".equals(property.getKey())) {
					properties.put("cn", property.getValue());
					properties.put("subject", property.getValue());
					properties.put("fileas", property.getValue());

				}
				else if ("N".equals(property.getKey())) {
					convertContactProperties(properties, VCARD_N_PROPERTIES, property.getValues());
				}
				else if ("NICKNAME".equals(property.getKey())) {
					properties.put("nickname", property.getValue());
				}
				else if ("TEL".equals(property.getKey())) {
					if (property.hasParam("TYPE", "cell") || property.hasParam("X-GROUP", "cell")) {
						properties.put("mobile", property.getValue());
					}
					else if (property.hasParam("TYPE", "work") || property.hasParam("X-GROUP", "work")) {
						properties.put("telephoneNumber", property.getValue());
					}
					else if (property.hasParam("TYPE", "home") || property.hasParam("X-GROUP", "home")) {
						properties.put("homePhone", property.getValue());
					}
					else if (property.hasParam("TYPE", "fax")) {
						if (property.hasParam("TYPE", "home")) {
							properties.put("homefax", property.getValue());
						}
						else {
							properties.put("facsimiletelephonenumber", property.getValue());
						}
					}
					else if (property.hasParam("TYPE", "pager")) {
						properties.put("pager", property.getValue());
					}
					else if (property.hasParam("TYPE", "car")) {
						properties.put("othermobile", property.getValue());
					}
					else {
						properties.put("otherTelephone", property.getValue());
					}
				}
				else if ("ADR".equals(property.getKey())) {
					// address
					if (property.hasParam("TYPE", "home")) {
						convertContactProperties(properties, VCARD_ADR_HOME_PROPERTIES, property.getValues());
					}
					else if (property.hasParam("TYPE", "work")) {
						convertContactProperties(properties, VCARD_ADR_WORK_PROPERTIES, property.getValues());
						// any other type goes to other address
					}
					else {
						convertContactProperties(properties, VCARD_ADR_OTHER_PROPERTIES, property.getValues());
					}
				}
				else if ("EMAIL".equals(property.getKey())) {
					if (property.hasParam("TYPE", "home")) {
						properties.put("email2", property.getValue());
						properties.put("smtpemail2", property.getValue());
					}
					else if (property.hasParam("TYPE", "other")) {
						properties.put("email3", property.getValue());
						properties.put("smtpemail3", property.getValue());
					}
					else {
						properties.put("email1", property.getValue());
						properties.put("smtpemail1", property.getValue());
					}
				}
				else if ("ORG".equals(property.getKey())) {
					convertContactProperties(properties, VCARD_ORG_PROPERTIES, property.getValues());
				}
				else if ("URL".equals(property.getKey())) {
					if (property.hasParam("TYPE", "work")) {
						properties.put("businesshomepage", property.getValue());
					}
					else if (property.hasParam("TYPE", "home")) {
						properties.put("personalHomePage", property.getValue());
					}
					else {
						// default: set personal home page
						properties.put("personalHomePage", property.getValue());
					}
				}
				else if ("TITLE".equals(property.getKey())) {
					properties.put("title", property.getValue());
				}
				else if ("NOTE".equals(property.getKey())) {
					properties.put("description", property.getValue());
				}
				else if ("CUSTOM1".equals(property.getKey())) {
					properties.put("extensionattribute1", property.getValue());
				}
				else if ("CUSTOM2".equals(property.getKey())) {
					properties.put("extensionattribute2", property.getValue());
				}
				else if ("CUSTOM3".equals(property.getKey())) {
					properties.put("extensionattribute3", property.getValue());
				}
				else if ("CUSTOM4".equals(property.getKey())) {
					properties.put("extensionattribute4", property.getValue());
				}
				else if ("ROLE".equals(property.getKey())) {
					properties.put("profession", property.getValue());
				}
				else if ("X-AIM".equals(property.getKey())) {
					properties.put("im", property.getValue());
				}
				else if ("BDAY".equals(property.getKey())) {
					properties.put("bday", convertBDayToZulu(property.getValue()));
				}
				else if ("ANNIVERSARY".equals(property.getKey()) || "X-ANNIVERSARY".equals(property.getKey())) {
					properties.put("anniversary", convertBDayToZulu(property.getValue()));
				}
				else if ("CATEGORIES".equals(property.getKey())) {
					properties.put(KEYWORDS, property.getValue());
				}
				else if ("CLASS".equals(property.getKey())) {
					if ("PUBLIC".equals(property.getValue())) {
						properties.put("sensitivity", "0");
						properties.put("private", FALSE_STRING);
					}
					else {
						properties.put("sensitivity", "2");
						properties.put("private", "true");
					}
				}
				else if ("SEX".equals(property.getKey())) {
					String propertyValue = property.getValue();
					if ("1".equals(propertyValue)) {
						properties.put("gender", "2");
					}
					else if ("2".equals(propertyValue)) {
						properties.put("gender", "1");
					}
				}
				else if ("FBURL".equals(property.getKey())) {
					properties.put("fburl", property.getValue());
				}
				else if ("X-ASSISTANT".equals(property.getKey())) {
					properties.put("secretarycn", property.getValue());
				}
				else if ("X-MANAGER".equals(property.getKey())) {
					properties.put("manager", property.getValue());
				}
				else if ("X-SPOUSE".equals(property.getKey())) {
					properties.put("spousecn", property.getValue());
				}
				else if ("PHOTO".equals(property.getKey())) {
					properties.put("photo", property.getValue());
					properties.put("haspicture", "true");
				}
			}
			log.debug("Create or update contact " + itemName + ": " + properties);
			// reset missing properties to null
			for (String key : CONTACT_ATTRIBUTES) {
				if (!IMAP_UID.equals(key) && !"etag".equals(key) && !URL_COMPNAME.equals(key)
						&& !LAST_MODIFIED.equals(key) && !"sensitivity".equals(key) && !properties.containsKey(key)) {
					properties.put(key, null);
				}
			}
		}

		Contact contact = buildContact(folderPath, itemName, properties, etag, noneMatch);
		for (VProperty property : vcard.getProperties()) {
			if ("MEMBER".equals(property.getKey())) {
				String member = property.getValue();
				if (member.startsWith("urn:uuid:")) {
					Item item = getItem(folderPath, member.substring(9) + ".EML");
					if (item != null) {
						if (item.get("smtpemail1") != null) {
							member = "mailto:" + item.get("smtpemail1");
						}
						else if (item.get("smtpemail2") != null) {
							member = "mailto:" + item.get("smtpemail2");
						}
						else if (item.get("smtpemail3") != null) {
							member = "mailto:" + item.get("smtpemail3");
						}
					}
				}
				contact.addMember(member);
			}
		}
		return contact.createOrUpdate();
	}

	protected String convertZuluDateToBday(String value) {
		String result = null;
		if (value != null && value.length() > 0) {
			try {
				SimpleDateFormat parser = ExchangeSession.getZuluDateFormat();
				Calendar cal = Calendar.getInstance();
				cal.setTime(parser.parse(value));
				cal.add(Calendar.HOUR_OF_DAY, 12);
				result = ExchangeSession.getVcardBdayFormat().format(cal.getTime());
			}
			catch (ParseException e) {
				log.warn("Invalid date: " + value);
			}
		}
		return result;
	}

	protected String convertBDayToZulu(String value) {
		String result = null;
		if (value != null && value.length() > 0) {
			try {
				SimpleDateFormat parser;
				if (value.length() == 10) {
					parser = ExchangeSession.getVcardBdayFormat();
				}
				else if (value.length() == 15) {
					parser = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ENGLISH);
					parser.setTimeZone(GMT_TIMEZONE);
				}
				else {
					parser = ExchangeSession.getExchangeZuluDateFormat();
				}
				result = ExchangeSession.getExchangeZuluDateFormatMillisecond().format(parser.parse(value));
			}
			catch (ParseException e) {
				log.warn("Invalid date: " + value);
			}
		}

		return result;
	}

	protected abstract Contact buildContact(String folderPath, String itemName, Map<String, String> properties,
			String etag, String noneMatch) throws IOException;

	protected abstract ItemResult internalCreateOrUpdateEvent(String folderPath, String itemName, String contentClass,
			String icsBody, String etag, String noneMatch) throws IOException;

	/**
	 * Получить текущее имя алиаса обмена из имени пользователя
	 * @return имя пользователя
	 */
	public String getAliasFromLogin() {
		// login is email, not alias
		if (this.userName.indexOf('@') >= 0) {
			return null;
		}
		String result = this.userName;
		// remove domain name
		int index = Math.max(result.indexOf('\\'), result.indexOf('/'));
		if (index >= 0) {
			result = result.substring(index + 1);
		}
		return result;
	}

	/**
	 * Проверяет, находится ли folderPath внутри почтового ящика пользователя.
	 * @param folderPath абсолютный путь к папке
	 * @return true, если folderPath является публичной или общей папкой
	 */
	public abstract boolean isSharedFolder(String folderPath);

	/**
	 * Проверить, является ли folderPath основным календарем.
	 * @param folderPath абсолютный путь к папке
	 * @return true, если folderPath является публичной или общей папкой
	 */
	public abstract boolean isMainCalendar(String folderPath) throws IOException;

	protected static final String MAILBOX_BASE = "/cn=";

	/**
	 * Получить электронную почту текущего пользователя
	 * @return электронная почта пользователя
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Получить alias текущего пользователя
	 * @return email пользователя
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * Поиск глобального списка адресов
	 * @param condition фильтр поиска
	 * @param returningAttributes возвращаемые атрибуты
	 * @param sizeLimit лимит размера
	 * @return соответствующие контакты из gal
	 * @throws IOException при ошибке
	 */
	public abstract Map<String, Contact> galFind(Condition condition, Set<String> returningAttributes, int sizeLimit)
			throws IOException;

	/**
	 * Полный список атрибутов контакта
	 */
	public static final Set<String> CONTACT_ATTRIBUTES = new HashSet<>();

	static {
		CONTACT_ATTRIBUTES.add(IMAP_UID);
		CONTACT_ATTRIBUTES.add("etag");
		CONTACT_ATTRIBUTES.add(URL_COMPNAME);

		CONTACT_ATTRIBUTES.add("extensionattribute1");
		CONTACT_ATTRIBUTES.add("extensionattribute2");
		CONTACT_ATTRIBUTES.add("extensionattribute3");
		CONTACT_ATTRIBUTES.add("extensionattribute4");
		CONTACT_ATTRIBUTES.add("bday");
		CONTACT_ATTRIBUTES.add("anniversary");
		CONTACT_ATTRIBUTES.add("businesshomepage");
		CONTACT_ATTRIBUTES.add("personalHomePage");
		CONTACT_ATTRIBUTES.add("cn");
		CONTACT_ATTRIBUTES.add("co");
		CONTACT_ATTRIBUTES.add("department");
		CONTACT_ATTRIBUTES.add("smtpemail1");
		CONTACT_ATTRIBUTES.add("smtpemail2");
		CONTACT_ATTRIBUTES.add("smtpemail3");
		CONTACT_ATTRIBUTES.add("facsimiletelephonenumber");
		CONTACT_ATTRIBUTES.add("givenName");
		CONTACT_ATTRIBUTES.add("homeCity");
		CONTACT_ATTRIBUTES.add("homeCountry");
		CONTACT_ATTRIBUTES.add("homePhone");
		CONTACT_ATTRIBUTES.add("homePostalCode");
		CONTACT_ATTRIBUTES.add("homeState");
		CONTACT_ATTRIBUTES.add("homeStreet");
		CONTACT_ATTRIBUTES.add("homepostofficebox");
		CONTACT_ATTRIBUTES.add("l");
		CONTACT_ATTRIBUTES.add("manager");
		CONTACT_ATTRIBUTES.add("mobile");
		CONTACT_ATTRIBUTES.add("namesuffix");
		CONTACT_ATTRIBUTES.add("nickname");
		CONTACT_ATTRIBUTES.add("o");
		CONTACT_ATTRIBUTES.add("pager");
		CONTACT_ATTRIBUTES.add("personaltitle");
		CONTACT_ATTRIBUTES.add("postalcode");
		CONTACT_ATTRIBUTES.add("postofficebox");
		CONTACT_ATTRIBUTES.add("profession");
		CONTACT_ATTRIBUTES.add("roomnumber");
		CONTACT_ATTRIBUTES.add("secretarycn");
		CONTACT_ATTRIBUTES.add("sn");
		CONTACT_ATTRIBUTES.add("spousecn");
		CONTACT_ATTRIBUTES.add("st");
		CONTACT_ATTRIBUTES.add("street");
		CONTACT_ATTRIBUTES.add("telephoneNumber");
		CONTACT_ATTRIBUTES.add("title");
		CONTACT_ATTRIBUTES.add("description");
		CONTACT_ATTRIBUTES.add("im");
		CONTACT_ATTRIBUTES.add("middlename");
		CONTACT_ATTRIBUTES.add(LAST_MODIFIED);
		CONTACT_ATTRIBUTES.add("otherstreet");
		CONTACT_ATTRIBUTES.add("otherstate");
		CONTACT_ATTRIBUTES.add("otherpostofficebox");
		CONTACT_ATTRIBUTES.add("otherpostalcode");
		CONTACT_ATTRIBUTES.add("othercountry");
		CONTACT_ATTRIBUTES.add("othercity");
		CONTACT_ATTRIBUTES.add("haspicture");
		CONTACT_ATTRIBUTES.add(KEYWORDS);
		CONTACT_ATTRIBUTES.add("othermobile");
		CONTACT_ATTRIBUTES.add("otherTelephone");
		CONTACT_ATTRIBUTES.add("gender");
		CONTACT_ATTRIBUTES.add("private");
		CONTACT_ATTRIBUTES.add("sensitivity");
		CONTACT_ATTRIBUTES.add("fburl");
	}

	protected static final Set<String> DISTRIBUTION_LIST_ATTRIBUTES = new HashSet<>();

	static {
		DISTRIBUTION_LIST_ATTRIBUTES.add(IMAP_UID);
		DISTRIBUTION_LIST_ATTRIBUTES.add("etag");
		DISTRIBUTION_LIST_ATTRIBUTES.add(URL_COMPNAME);

		DISTRIBUTION_LIST_ATTRIBUTES.add("cn");
		DISTRIBUTION_LIST_ATTRIBUTES.add("members");
	}

	/**
	 * Получить строку данных о занятости из Exchange.
	 * @param attendee адрес электронной почты участника
	 * @param start дата начала в формате zulu Exchange
	 * @param end дата окончания в формате zulu Exchange
	 * @param interval интервал занятости в минутах
	 * @return данные о занятости или null
	 * @throws IOException при ошибке
	 */
	protected abstract String getFreeBusyData(String attendee, String start, String end, int interval)
			throws IOException;

	/**
	 * Получить информацию о занятости для участника между начальной и конечной датой.
	 * @param attendee почта участника
	 * @param startDateValue начальная дата
	 * @param endDateValue конечная дата
	 * @return Информация о занятости
	 * @throws IOException в случае ошибки
	 */
	public FreeBusy getFreebusy(String attendee, String startDateValue, String endDateValue) throws IOException {
		// replace ical encoded attendee name
		attendee = replaceIcal4Principal(attendee);

		// then check that email address is valid to avoid InvalidSmtpAddress error
		if (attendee == null || attendee.indexOf('@') < 0 || attendee.charAt(attendee.length() - 1) == '@') {
			return null;
		}

		if (attendee.startsWith("mailto:") || attendee.startsWith("MAILTO:")) {
			attendee = attendee.substring("mailto:".length());
		}

		SimpleDateFormat exchangeZuluDateFormat = getExchangeZuluDateFormat();
		SimpleDateFormat icalDateFormat = getZuluDateFormat();

		Date startDate;
		Date endDate;
		try {
			if (startDateValue.length() == 8) {
				startDate = parseDate(startDateValue);
			}
			else {
				startDate = icalDateFormat.parse(startDateValue);
			}
			if (endDateValue.length() == 8) {
				endDate = parseDate(endDateValue);
			}
			else {
				endDate = icalDateFormat.parse(endDateValue);
			}
		}
		catch (ParseException e) {
			throw new MosTechEwsException("EXCEPTION_INVALID_DATES", e.getMessage());
		}

		FreeBusy freeBusy = null;
		String fbdata = getFreeBusyData(attendee, exchangeZuluDateFormat.format(startDate),
				exchangeZuluDateFormat.format(endDate), FREE_BUSY_INTERVAL);
		if (fbdata != null) {
			freeBusy = new FreeBusy(icalDateFormat, startDate, fbdata);
		}

		if (freeBusy != null && freeBusy.knownAttendee) {
			return freeBusy;
		}
		else {
			return null;
		}
	}

	/**
	 * Обмен на парсер iCalendar Занятости/Свободного времени. Свободное время возвращает
	 * 0, Предварительное возвращает 1, Занятое возвращает 2, а Вне офиса (OOF) возвращает
	 * 3
	 */
	public static final class FreeBusy {

		final SimpleDateFormat icalParser;

		boolean knownAttendee = true;
		static final HashMap<Character, String> FBTYPES = new HashMap<>();

		static {
			FBTYPES.put('1', "BUSY-TENTATIVE");
			FBTYPES.put('2', "BUSY");
			FBTYPES.put('3', "BUSY-UNAVAILABLE");
		}

		final HashMap<String, StringBuilder> busyMap = new HashMap<>();

		StringBuilder getBusyBuffer(char type) {
			String fbType = FBTYPES.get(type);
			StringBuilder buffer = busyMap.get(fbType);
			if (buffer == null) {
				buffer = new StringBuilder();
				busyMap.put(fbType, buffer);
			}
			return buffer;
		}

		void startBusy(char type, Calendar currentCal) {
			if (type == '4') {
				knownAttendee = false;
			}
			else if (type != '0') {
				StringBuilder busyBuffer = getBusyBuffer(type);
				if (busyBuffer.length() > 0) {
					busyBuffer.append(',');
				}
				busyBuffer.append(icalParser.format(currentCal.getTime()));
			}
		}

		void endBusy(char type, Calendar currentCal) {
			if (type != '0' && type != '4') {
				getBusyBuffer(type).append('/').append(icalParser.format(currentCal.getTime()));
			}
		}

		FreeBusy(SimpleDateFormat icalParser, Date startDate, String fbdata) {
			this.icalParser = icalParser;
			if (fbdata.length() > 0) {
				Calendar currentCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
				currentCal.setTime(startDate);

				startBusy(fbdata.charAt(0), currentCal);
				for (int i = 1; i < fbdata.length() && knownAttendee; i++) {
					currentCal.add(Calendar.MINUTE, FREE_BUSY_INTERVAL);
					char previousState = fbdata.charAt(i - 1);
					char currentState = fbdata.charAt(i);
					if (previousState != currentState) {
						endBusy(previousState, currentCal);
						startBusy(currentState, currentCal);
					}
				}
				currentCal.add(Calendar.MINUTE, FREE_BUSY_INTERVAL);
				endBusy(fbdata.charAt(fbdata.length() - 1), currentCal);
			}
		}

		/**
		 * Добавить информацию о занятости к буферу.
		 * @param buffer Строковый буфер
		 */
		public void appendTo(StringBuilder buffer) {
			for (Map.Entry<String, StringBuilder> entry : busyMap.entrySet()) {
				buffer.append("FREEBUSY;FBTYPE=")
					.append(entry.getKey())
					.append(':')
					.append(entry.getValue())
					.append((char) 13)
					.append((char) 10);
			}
		}

	}

	protected VObject vTimezone;

	/**
	 * Загрузить и вернуть текущий часовой пояс OWA пользователя.
	 * @return текущий часовой пояс
	 */
	public VObject getVTimezone() {
		if (vTimezone == null) {
			// need to load Timezone info from OWA
			loadVtimezone();
		}
		return vTimezone;
	}

	protected abstract void loadVtimezone();

}
