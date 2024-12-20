/*
DIT
 */
package ru.mos.mostech.ews.exchange.dav;

import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.PropEntry;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.Namespace;
import org.apache.jackrabbit.webdav.xml.XmlSerializable;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.http.request.ExchangePropPatchRequest;
import ru.mos.mostech.ews.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Поле WebDav
 */
public class Field {

	protected static final Map<DistinguishedPropertySetType, String> distinguishedPropertySetMap = new HashMap<>();

	static {
		distinguishedPropertySetMap.put(DistinguishedPropertySetType.Meeting, "6ed8da90-450b-101b-98da-00aa003f1305");
		distinguishedPropertySetMap.put(DistinguishedPropertySetType.Appointment,
				"00062002-0000-0000-c000-000000000046");
		distinguishedPropertySetMap.put(DistinguishedPropertySetType.Common, "00062008-0000-0000-c000-000000000046");
		distinguishedPropertySetMap.put(DistinguishedPropertySetType.PublicStrings,
				"00020329-0000-0000-c000-000000000046");
		distinguishedPropertySetMap.put(DistinguishedPropertySetType.Address, "00062004-0000-0000-c000-000000000046");
		distinguishedPropertySetMap.put(DistinguishedPropertySetType.InternetHeaders,
				"00020386-0000-0000-c000-000000000046");
		distinguishedPropertySetMap.put(DistinguishedPropertySetType.UnifiedMessaging,
				"4442858e-a9e3-4e80-b900-317a210cc15b");
		distinguishedPropertySetMap.put(DistinguishedPropertySetType.Task, "00062003-0000-0000-c000-000000000046");
	}

	protected static final Namespace EMPTY = Namespace.getNamespace("");

	protected static final Namespace XML = Namespace.getNamespace("xml:");

	protected static final Namespace DAV = Namespace.getNamespace("DAV:");

	protected static final Namespace URN_SCHEMAS_HTTPMAIL = Namespace.getNamespace("urn:schemas:httpmail:");

	protected static final Namespace URN_SCHEMAS_MAILHEADER = Namespace.getNamespace("urn:schemas:mailheader:");

	protected static final Namespace SCHEMAS_EXCHANGE = Namespace
		.getNamespace("http://schemas.microsoft.com/exchange/");

	protected static final Namespace SCHEMAS_MAPI = Namespace.getNamespace("http://schemas.microsoft.com/mapi/");

	protected static final Namespace SCHEMAS_MAPI_PROPTAG = Namespace
		.getNamespace("http://schemas.microsoft.com/mapi/proptag/");

	protected static final Namespace SCHEMAS_MAPI_ID = Namespace.getNamespace("http://schemas.microsoft.com/mapi/id/");

	protected static final Namespace SCHEMAS_MAPI_STRING = Namespace
		.getNamespace("http://schemas.microsoft.com/mapi/string/");

	protected static final Namespace SCHEMAS_REPL = Namespace.getNamespace("http://schemas.microsoft.com/repl/");

	protected static final Namespace URN_SCHEMAS_CONTACTS = Namespace.getNamespace("urn:schemas:contacts:");

	protected static final Namespace URN_SCHEMAS_CALENDAR = Namespace.getNamespace("urn:schemas:calendar:");

	protected static final Namespace SCHEMAS_MAPI_STRING_INTERNET_HEADERS = Namespace
		.getNamespace(SCHEMAS_MAPI_STRING.getURI() + '{'
				+ distinguishedPropertySetMap.get(DistinguishedPropertySetType.InternetHeaders) + "}/");

	protected static final Map<PropertyType, String> propertyTypeMap = new HashMap<>();

	static {
		propertyTypeMap.put(PropertyType.Integer, "0003"); // PT_INT
		propertyTypeMap.put(PropertyType.Boolean, "000b"); // PT_BOOLEAN
		propertyTypeMap.put(PropertyType.SystemTime, "0040"); // PT_SYSTIME
		propertyTypeMap.put(PropertyType.String, "001f"); // 001f is PT_UNICODE_STRING,
															// 001E is PT_STRING
		propertyTypeMap.put(PropertyType.Binary, "0102"); // PT_BINARY
		propertyTypeMap.put(PropertyType.Double, "0005"); // PT_DOUBLE
	}

	@SuppressWarnings({ "UnusedDeclaration" })
	protected enum DistinguishedPropertySetType {

		Meeting, Appointment, Common, PublicStrings, Address, InternetHeaders, CalendarAssistant, UnifiedMessaging, Task

	}

	protected static final Map<String, Field> fieldMap = new HashMap<>();

	static {
		// well known folders
		createField(URN_SCHEMAS_HTTPMAIL, "inbox");
		createField(URN_SCHEMAS_HTTPMAIL, "deleteditems");
		createField(URN_SCHEMAS_HTTPMAIL, "sentitems");
		createField(URN_SCHEMAS_HTTPMAIL, "sendmsg");
		createField(URN_SCHEMAS_HTTPMAIL, "drafts");
		createField(URN_SCHEMAS_HTTPMAIL, "calendar");
		createField(URN_SCHEMAS_HTTPMAIL, "tasks");
		createField(URN_SCHEMAS_HTTPMAIL, "contacts");
		createField(URN_SCHEMAS_HTTPMAIL, "outbox");

		// folder
		createField("folderclass", SCHEMAS_EXCHANGE, "outlookfolderclass");
		createField(DAV, "hassubs");
		createField(DAV, "nosubs");
		createField("count", DAV, "objectcount");
		createField(URN_SCHEMAS_HTTPMAIL, "unreadcount");
		createField(SCHEMAS_REPL, "contenttag");

		createField("uidNext", 0x6751, PropertyType.Integer);// PR_ARTICLE_NUM_NEXT
		createField("highestUid", 0x6752, PropertyType.Integer);// PR_IMAP_LAST_ARTICLE_ID

		createField(DAV, "isfolder");

		// item uid, do not use as search parameter, see
		// http://support.microsoft.com/kb/320749
		createField(DAV, "uid"); // based on PR_RECORD_KEY

		// POP and IMAP message
		createField("messageSize", 0x0e08, PropertyType.Integer);// PR_MESSAGE_SIZE
		createField("imapUid", 0x0e23, PropertyType.Integer);// PR_INTERNET_ARTICLE_NUMBER
		createField("junk", 0x1083, PropertyType.Integer); // PR_SPAMTYPE
		createField("flagStatus", 0x1090, PropertyType.Integer);// PR_FLAG_STATUS
		createField("messageFlags", 0x0e07, PropertyType.Integer);// PR_MESSAGE_FLAGS
		createField("lastVerbExecuted", 0x1081, PropertyType.Integer);// PR_LAST_VERB_EXECUTED
		createField("iconIndex", 0x1080, PropertyType.Integer);// PR_ICON_INDEX
		createField(URN_SCHEMAS_HTTPMAIL, "read");
		// createField("read", 0x0e69, PropertyType.Boolean);//PR_READ

		if (Settings.getBooleanProperty("mt.ews.popCommonDeleted", true)) {
			// deleted flag, see
			// http://microsoft.public.win32.programmer.messaging.narkive.com/w7Mrsrsx/how-to-detect-deleted-imap-messages-using-mapi-outlook-object-model-api
			createField("deleted", DistinguishedPropertySetType.Common, 0x8570, "deleted", PropertyType.String);
		}
		else {
			createField("deleted", DistinguishedPropertySetType.PublicStrings);
		}

		// createField(URN_SCHEMAS_HTTPMAIL, "date");//PR_CLIENT_SUBMIT_TIME, 0x0039
		createField("date", 0x0e06, PropertyType.SystemTime);// PR_MESSAGE_DELIVERY_TIME
		createField(URN_SCHEMAS_MAILHEADER, "bcc");// PS_INTERNET_HEADERS/bcc
		createField(URN_SCHEMAS_HTTPMAIL, "datereceived");// PR_MESSAGE_DELIVERY_TIME,
															// 0x0E06

		// unused: force message encoding
		createField("messageFormat", 0x5909, PropertyType.Integer);// PR_MSG_EDITOR_FORMAT
																	// EDITOR_FORMAT_PLAINTEXT
																	// = 1
																	// EDITOR_FORMAT_HTML
																	// = 2
		createField("mailOverrideFormat", 0x5902, PropertyType.Integer);// PR_INETMAIL_OVERRIDE_FORMAT
																		// ENCODING_PREFERENCE
																		// = 2
																		// BODY_ENCODING_TEXT_AND_HTML
																		// = 1
																		// ENCODING_MIME =
																		// 4

		// IMAP search

		createField(URN_SCHEMAS_HTTPMAIL, "subject"); // DistinguishedPropertySetType.InternetHeaders/Subject/String
		// createField("subject", 0x0037, PropertyType.String);//PR_SUBJECT
		createField("body", 0x1000, PropertyType.String);// PR_BODY
		createField("messageheaders", 0x007D, PropertyType.String);// PR_TRANSPORT_MESSAGE_HEADERS
		createField(URN_SCHEMAS_HTTPMAIL, "from");
		// createField("from", DistinguishedPropertySetType.PublicStrings,
		// 0x001f);//urn:schemas:httpmail:from
		createField(URN_SCHEMAS_MAILHEADER, "to"); // DistinguishedPropertySetType.InternetHeaders/To/String
		createField(URN_SCHEMAS_MAILHEADER, "cc"); // DistinguishedPropertySetType.InternetHeaders/To/String
		createField(URN_SCHEMAS_MAILHEADER, "message-id"); // DistinguishedPropertySetType.InternetHeaders/message-id/String
		createField(URN_SCHEMAS_MAILHEADER, "htmldescription"); // DistinguishedPropertySetType.InternetHeaders/htmldescription/String

		createField("lastmodified", DAV, "getlastmodified"); // PR_LAST_MODIFICATION_TIME
																// 0x3008 SystemTime

		// failover search
		createField(DAV, "displayname");
		createField("urlcompname", 0x10f3, PropertyType.String); // PR_URL_COMP_NAME

		// items
		createField("etag", DAV, "getetag");

		// calendar
		createField(SCHEMAS_EXCHANGE, "permanenturl");
		createField(URN_SCHEMAS_CALENDAR, "instancetype"); // DistinguishedPropertySetType.PublicStrings/urn:schemas:calendar:instancetype/Integer
		createField(URN_SCHEMAS_CALENDAR, "dtstart"); // 0x10C3 SystemTime
		createField(URN_SCHEMAS_CALENDAR, "dtend"); // 0x10C4 SystemTime

		// DistinguishedPropertySetType.PublicStrings/urn:schemas:calendar:prodid/String
		createField("calendarversion", URN_SCHEMAS_CALENDAR, "version"); // DistinguishedPropertySetType.PublicStrings/urn:schemas:calendar:version/String
		createField(URN_SCHEMAS_CALENDAR, "method"); // //
														// DistinguishedPropertySetType.PublicStrings/urn:schemas:calendar:method/String

		createField("calendarlastmodified", URN_SCHEMAS_CALENDAR, "lastmodified"); // DistinguishedPropertySetType.PublicStrings/urn:schemas:calendar:isorganizer/Boolean
		createField(URN_SCHEMAS_CALENDAR, "dtstamp"); // PidLidOwnerCriticalChange
		createField("calendaruid", URN_SCHEMAS_CALENDAR, "uid"); // DistinguishedPropertySetType.PublicStrings/urn:schemas:calendar:uid/String
		createField(URN_SCHEMAS_CALENDAR, "transparent"); // DistinguishedPropertySetType.PublicStrings/urn:schemas:calendar:transparent/String

		createField(URN_SCHEMAS_CALENDAR, "organizer");
		createField(URN_SCHEMAS_CALENDAR, "created"); // DistinguishedPropertySetType.PublicStrings/urn:schemas:calendar:created/SystemTime
		createField(URN_SCHEMAS_CALENDAR, "alldayevent"); // DistinguishedPropertySetType.Appointment/0x8215
															// Boolean

		createField(URN_SCHEMAS_CALENDAR, "rrule"); // DistinguishedPropertySetType.PublicStrings/urn:schemas:calendar:rrule/PtypMultipleString
		createField(URN_SCHEMAS_CALENDAR, "exdate"); // DistinguishedPropertySetType.PublicStrings/urn:schemas:calendar:exdate/PtypMultipleTime

		createField(SCHEMAS_MAPI, "reminderset"); // PidLidReminderSet
		createField(SCHEMAS_MAPI, "reminderdelta"); // PidLidReminderDelta

		// TODO
		createField(SCHEMAS_MAPI, "allattendeesstring"); // PidLidAllAttendeesString
		createField(SCHEMAS_MAPI, "required_attendees"); // PidLidRequiredAttendees
		createField(SCHEMAS_MAPI, "apptendtime"); // PidLidAppointmentEndTime
		createField(SCHEMAS_MAPI, "apptstateflags"); // PidLidAppointmentStateFlags 1:
														// Meeting, 2: Received, 4:
														// Cancelled

		createField(URN_SCHEMAS_CALENDAR, "isorganizer"); // DistinguishedPropertySetType.PublicStrings/urn:schemas:calendar:isorganizer/Boolean
		createField(URN_SCHEMAS_CALENDAR, "location"); // DistinguishedPropertySetType.Appointment/0x8208
														// String
		createField(URN_SCHEMAS_CALENDAR, "attendeerole"); // DistinguishedPropertySetType.PublicStrings/urn:schemas:calendar:attendeerole/Integer
		createField(URN_SCHEMAS_CALENDAR, "busystatus"); // DistinguishedPropertySetType.PublicStrings/urn:schemas:calendar:busystatus/String
		createField(URN_SCHEMAS_CALENDAR, "exrule"); // DistinguishedPropertySetType.PublicStrings/urn:schemas:calendar:exrule/PtypMultipleString
		createField(URN_SCHEMAS_CALENDAR, "recurrenceidrange"); // DistinguishedPropertySetType.PublicStrings/urn:schemas:calendar:recurrenceidrange/String
		createField(URN_SCHEMAS_CALENDAR, "rdate"); // DistinguishedPropertySetType.PublicStrings/urn:schemas:calendar:rdate/PtypMultipleTime
		createField(URN_SCHEMAS_CALENDAR, "reminderoffset"); // DistinguishedPropertySetType.PublicStrings/urn:schemas:calendar:reminderoffset/Integer
		createField(URN_SCHEMAS_CALENDAR, "timezone"); // DistinguishedPropertySetType.PublicStrings/urn:schemas:calendar:timezone/String

		createField(SCHEMAS_EXCHANGE, "sensitivity"); // PR_SENSITIVITY 0x0036 Integer
		createField(URN_SCHEMAS_CALENDAR, "timezoneid"); // DistinguishedPropertySetType.PublicStrings/urn:schemas:calendar:timezoneid/Integer
		// should use PidLidServerProcessed ?
		createField("processed", 0x65e8, PropertyType.Boolean);// PR_MESSAGE_PROCESSED

		createField(DAV, "contentclass");
		createField("internetContent", 0x6659, PropertyType.Binary);

		// contact

		createField(SCHEMAS_EXCHANGE, "outlookmessageclass");
		createField(URN_SCHEMAS_HTTPMAIL, "subject");

		createField(URN_SCHEMAS_CONTACTS, "middlename"); // PR_MIDDLE_NAME 0x3A44
		createField(URN_SCHEMAS_CONTACTS, "fileas"); // urn:schemas:contacts:fileas
														// PS_PUBLIC_STRINGS

		// createField("id", 0x0ff6, PropertyType.Binary); // PR_INSTANCE_KEY
		// http://support.microsoft.com/kb/320749

		createField(URN_SCHEMAS_CONTACTS, "homepostaladdress"); // homeAddress
																// DistinguishedPropertySetType.Address/0x0000801A/String
		createField(URN_SCHEMAS_CONTACTS, "otherpostaladdress"); // otherAddress
																	// DistinguishedPropertySetType.Address/0x0000801C/String
		createField(URN_SCHEMAS_CONTACTS, "mailingaddressid"); // postalAddressId
																// DistinguishedPropertySetType.Address/0x00008022/String
		createField(URN_SCHEMAS_CONTACTS, "workaddress"); // workAddress
															// DistinguishedPropertySetType.Address/0x0000801B/String

		createField(URN_SCHEMAS_CONTACTS, "alternaterecipient"); // alternaterecipient
																	// DistinguishedPropertySetType.PublicStrings/urn:schemas:contacts:alternaterecipient/String

		createField(SCHEMAS_EXCHANGE, "extensionattribute1"); // DistinguishedPropertySetType.Address/0x0000804F/String
		createField(SCHEMAS_EXCHANGE, "extensionattribute2"); // DistinguishedPropertySetType.Address/0x00008050/String
		createField(SCHEMAS_EXCHANGE, "extensionattribute3"); // DistinguishedPropertySetType.Address/0x00008051/String
		createField(SCHEMAS_EXCHANGE, "extensionattribute4"); // DistinguishedPropertySetType.Address/0x00008052/String

		createField(URN_SCHEMAS_CONTACTS, "bday"); // PR_BIRTHDAY 0x3A42 SystemTime
		createField("anniversary", URN_SCHEMAS_CONTACTS, "weddinganniversary"); // WeddingAnniversary
		createField(URN_SCHEMAS_CONTACTS, "businesshomepage"); // PR_BUSINESS_HOME_PAGE
																// 0x3A51 String
		createField(URN_SCHEMAS_CONTACTS, "personalHomePage"); // PR_PERSONAL_HOME_PAGE
																// 0x3A50 String
		createField(URN_SCHEMAS_CONTACTS, "cn"); // PR_DISPLAY_NAME 0x3001 String
		createField(URN_SCHEMAS_CONTACTS, "co"); // workAddressCountry
													// DistinguishedPropertySetType.Address/0x00008049/String
		createField(URN_SCHEMAS_CONTACTS, "department"); // PR_DEPARTMENT_NAME 0x3A18
															// String

		// smtp email
		createField("smtpemail1", DistinguishedPropertySetType.Address, 0x8084, "smtpemail1"); // Email1OriginalDisplayName
		createField("smtpemail2", DistinguishedPropertySetType.Address, 0x8094, "smtpemail2"); // Email2OriginalDisplayName
		createField("smtpemail3", DistinguishedPropertySetType.Address, 0x80A4, "smtpemail3"); // Email3OriginalDisplayName

		// native email
		createField("email1", DistinguishedPropertySetType.Address, 0x8083, "email1"); // Email1EmailAddress
		createField("email2", DistinguishedPropertySetType.Address, 0x8093, "email2"); // Email2EmailAddress
		createField("email3", DistinguishedPropertySetType.Address, 0x80A3, "email3"); // Email3EmailAddress

		// email type
		createField("email1type", DistinguishedPropertySetType.Address, 0x8082, "email1type"); // Email1AddressType
		createField("email2type", DistinguishedPropertySetType.Address, 0x8092, "email2type"); // Email2AddressType
		createField("email3type", DistinguishedPropertySetType.Address, 0x80A2, "email3type"); // Email3AddressType

		createField(URN_SCHEMAS_CONTACTS, "facsimiletelephonenumber"); // PR_BUSINESS_FAX_NUMBER
																		// 0x3A24 String
		createField(URN_SCHEMAS_CONTACTS, "givenName"); // PR_GIVEN_NAME 0x3A06 String
		createField(URN_SCHEMAS_CONTACTS, "homepostofficebox"); // PR_HOME_ADDRESS_POST_OFFICE_BOX
																// 0x3A5E String
		createField(URN_SCHEMAS_CONTACTS, "homeCity"); // PR_HOME_ADDRESS_CITY 0x3A59
														// String
		createField(URN_SCHEMAS_CONTACTS, "homeCountry"); // PR_HOME_ADDRESS_COUNTRY
															// 0x3A5A String
		createField(URN_SCHEMAS_CONTACTS, "homePhone"); // PR_HOME_TELEPHONE_NUMBER 0x3A09
														// String
		createField(URN_SCHEMAS_CONTACTS, "homePostalCode"); // PR_HOME_ADDRESS_POSTAL_CODE
																// 0x3A5B String
		createField(URN_SCHEMAS_CONTACTS, "homeState"); // PR_HOME_ADDRESS_STATE_OR_PROVINCE
														// 0x3A5C String
		createField(URN_SCHEMAS_CONTACTS, "homeStreet"); // PR_HOME_ADDRESS_STREET 0x3A5D
															// String
		createField(URN_SCHEMAS_CONTACTS, "l"); // workAddressCity
												// DistinguishedPropertySetType.Address/0x00008046/String
		createField(URN_SCHEMAS_CONTACTS, "manager"); // PR_MANAGER_NAME 0x3A4E String
		createField(URN_SCHEMAS_CONTACTS, "mobile"); // PR_MOBILE_TELEPHONE_NUMBER 0x3A1C
														// String
		createField(URN_SCHEMAS_CONTACTS, "namesuffix"); // PR_GENERATION 0x3A05 String
		createField(URN_SCHEMAS_CONTACTS, "nickname"); // PR_NICKNAME 0x3A4F String
		createField(URN_SCHEMAS_CONTACTS, "o"); // PR_COMPANY_NAME 0x3A16 String
		createField(URN_SCHEMAS_CONTACTS, "pager"); // PR_PAGER_TELEPHONE_NUMBER 0x3A21
													// String
		createField(URN_SCHEMAS_CONTACTS, "personaltitle"); // PR_DISPLAY_NAME_PREFIX
															// 0x3A45 String
		createField(URN_SCHEMAS_CONTACTS, "postalcode"); // workAddressPostalCode
															// DistinguishedPropertySetType.Address/0x00008048/String
		createField(URN_SCHEMAS_CONTACTS, "postofficebox"); // workAddressPostOfficeBox
															// DistinguishedPropertySetType.Address/0x0000804A/String
		createField(URN_SCHEMAS_CONTACTS, "profession"); // PR_PROFESSION 0x3A46 String
		createField(URN_SCHEMAS_CONTACTS, "roomnumber"); // PR_OFFICE_LOCATION 0x3A19
															// String
		createField(URN_SCHEMAS_CONTACTS, "secretarycn"); // PR_ASSISTANT 0x3A30 String
		createField(URN_SCHEMAS_CONTACTS, "sn"); // PR_SURNAME 0x3A11 String
		createField(URN_SCHEMAS_CONTACTS, "spousecn"); // PR_SPOUSE_NAME 0x3A48 String
		createField(URN_SCHEMAS_CONTACTS, "st"); // workAddressState
													// DistinguishedPropertySetType.Address/0x00008047/String
		createField(URN_SCHEMAS_CONTACTS, "street"); // workAddressStreet
														// DistinguishedPropertySetType.Address/0x00008045/String
		createField(URN_SCHEMAS_CONTACTS, "telephoneNumber"); // PR_BUSINESS_TELEPHONE_NUMBER
																// 0x3A08 String
		createField(URN_SCHEMAS_CONTACTS, "title"); // PR_TITLE 0x3A17 String
		createField("description", URN_SCHEMAS_HTTPMAIL, "textdescription"); // PR_BODY
																				// 0x1000
																				// String
		createField("im", SCHEMAS_MAPI, "InstMsg"); // InstantMessagingAddress
													// DistinguishedPropertySetType.Address/0x00008062/String
		createField(URN_SCHEMAS_CONTACTS, "othermobile"); // PR_CAR_TELEPHONE_NUMBER
															// 0x3A1E String
		createField(URN_SCHEMAS_CONTACTS, "internationalisdnnumber"); // PR_ISDN_NUMBER
																		// 0x3A2D String

		createField(URN_SCHEMAS_CONTACTS, "otherTelephone"); // PR_OTHER_TELEPHONE_NUMBER
																// 0x3A21 String
		createField(URN_SCHEMAS_CONTACTS, "homefax"); // PR_HOME_FAX_NUMBER 0x3A25 String

		createField(URN_SCHEMAS_CONTACTS, "otherstreet"); // PR_OTHER_ADDRESS_STREET
															// 0x3A63 String
		createField(URN_SCHEMAS_CONTACTS, "otherstate"); // PR_OTHER_ADDRESS_STATE_OR_PROVINCE
															// 0x3A62 String
		createField(URN_SCHEMAS_CONTACTS, "otherpostofficebox"); // PR_OTHER_ADDRESS_POST_OFFICE_BOX
																	// 0x3A64 String
		createField(URN_SCHEMAS_CONTACTS, "otherpostalcode"); // PR_OTHER_ADDRESS_POSTAL_CODE
																// 0x3A61 String
		createField(URN_SCHEMAS_CONTACTS, "othercountry"); // PR_OTHER_ADDRESS_COUNTRY
															// 0x3A60 String
		createField(URN_SCHEMAS_CONTACTS, "othercity"); // PR_OTHER_ADDRESS_CITY 0x3A5F
														// String

		createField(URN_SCHEMAS_CONTACTS, "gender"); // PR_GENDER 0x3A4D Integer16

		createField("keywords", SCHEMAS_EXCHANGE, "keywords-utf8", PropertyType.StringArray); // PS_PUBLIC_STRINGS
																								// Keywords
																								// String
		// createField("keywords", DistinguishedPropertySetType.PublicStrings, "Keywords",
		// ); // PS_PUBLIC_STRINGS Keywords String

		// contact private flags
		createField("private", DistinguishedPropertySetType.Common, 0x8506, "private", PropertyType.Boolean); // True/False
		createField("sensitivity", 0x0036, PropertyType.Integer); // PR_SENSITIVITY
																	// SENSITIVITY_PRIVATE
																	// = 2,
																	// SENSITIVITY_PERSONAL
																	// = 1,
																	// SENSITIVITY_NONE =
																	// 0

		createField("haspicture", DistinguishedPropertySetType.Address, 0x8015, "haspicture", PropertyType.Boolean); // True/False

		createField(URN_SCHEMAS_CALENDAR, "fburl"); // freeBusyLocation

		// OWA settings
		createField("messageclass", 0x001a, PropertyType.String);
		createField("roamingxmlstream", 0x7c08, PropertyType.Binary);
		createField("roamingdictionary", 0x7c07, PropertyType.Binary);

		createField(DAV, "ishidden");

		// attachment content
		createField("attachDataBinary", 0x3701, PropertyType.Binary);

		createField("attachmentContactPhoto", 0x7FFF, PropertyType.Boolean); // PR_ATTACHMENT_CONTACTPHOTO
		createField("renderingPosition", 0x370B, PropertyType.Integer);// PR_RENDERING_POSITION

		// PR_ATTACH_FILENAME
		createField("attachExtension", 0x3703, PropertyType.String); // PR_ATTACH_EXTENSION

		createField("xmozlastack", DistinguishedPropertySetType.PublicStrings);
		createField("xmozsnoozetime", DistinguishedPropertySetType.PublicStrings);
		createField("xmozsendinvitations", DistinguishedPropertySetType.PublicStrings);

		// task
		createField(URN_SCHEMAS_MAILHEADER, "importance");// PS_INTERNET_HEADERS/importance
		createField("percentcomplete", DistinguishedPropertySetType.Task, 0x8102, "percentcomplete",
				PropertyType.Double);
		createField("taskstatus", DistinguishedPropertySetType.Task, 0x8101, "taskstatus", PropertyType.Integer);
		createField("startdate", DistinguishedPropertySetType.Task, 0x8104, "startdate", PropertyType.SystemTime);
		createField("duedate", DistinguishedPropertySetType.Task, 0x8105, "duedate", PropertyType.SystemTime);
		createField("datecompleted", DistinguishedPropertySetType.Task, 0x810F, "datecompleted",
				PropertyType.SystemTime);
		createField("iscomplete", DistinguishedPropertySetType.Task, 0x811C, "iscomplete", PropertyType.Boolean);

		createField("commonstart", DistinguishedPropertySetType.Common, 0x8516, "commonstart", PropertyType.SystemTime);
		createField("commonend", DistinguishedPropertySetType.Common, 0x8517, "commonend", PropertyType.SystemTime);
	}

	protected static String toHexString(int propertyTag) {
		StringBuilder hexValue = new StringBuilder(Integer.toHexString(propertyTag));
		while (hexValue.length() < 4) {
			hexValue.insert(0, '0');
		}
		return hexValue.toString();
	}

	protected static void createField(String alias, int propertyTag, PropertyType propertyType) {
		String name = 'x' + toHexString(propertyTag) + propertyTypeMap.get(propertyType);
		Field field;
		if (propertyType == PropertyType.Binary) {
			field = new Field(alias, SCHEMAS_MAPI_PROPTAG, name, propertyType, null, "bin.base64", name);
		}
		else {
			field = new Field(alias, SCHEMAS_MAPI_PROPTAG, name, propertyType);
		}
		fieldMap.put(field.alias, field);
	}

	protected static void createField(String alias,
			@SuppressWarnings("SameParameterValue") DistinguishedPropertySetType propertySetType) {
		Field field = new Field(
				Namespace.getNamespace(
						SCHEMAS_MAPI_STRING.getURI() + '{' + distinguishedPropertySetMap.get(propertySetType) + "}/"),
				alias);
		fieldMap.put(field.alias, field);
	}

	protected static void createField(String alias,
			@SuppressWarnings("SameParameterValue") DistinguishedPropertySetType propertySetType, int propertyTag,
			String responseAlias) {
		createField(alias, propertySetType, propertyTag, responseAlias, null);
	}

	protected static void createField(String alias, DistinguishedPropertySetType propertySetType, int propertyTag,
			String responseAlias, PropertyType propertyType) {
		String name;
		String updateAlias;
		if (propertySetType == DistinguishedPropertySetType.Address) {
			// Address namespace expects integer names
			name = String.valueOf(propertyTag);
			updateAlias = "_x0030_x" + toHexString(propertyTag);
		}
		else if (propertySetType == DistinguishedPropertySetType.Task) {
			name = "0x" + toHexString(propertyTag);
			updateAlias = "0x0000" + toHexString(propertyTag);
		}
		else {
			// Common namespace expects hex names
			name = "0x" + toHexString(propertyTag);
			updateAlias = "_x0030_x" + toHexString(propertyTag);
		}
		Field field = new Field(alias,
				Namespace.getNamespace(
						SCHEMAS_MAPI_ID.getURI() + '{' + distinguishedPropertySetMap.get(propertySetType) + "}/"),
				name, propertyType, responseAlias, null, updateAlias);
		fieldMap.put(field.alias, field);
	}

	protected static void createField(Namespace namespace, String name) {
		Field field = new Field(namespace, name);
		fieldMap.put(field.alias, field);
	}

	protected static void createField(String alias, Namespace namespace, String name) {
		Field field = new Field(alias, namespace, name, null);
		fieldMap.put(field.alias, field);
	}

	@SuppressWarnings("SameParameterValue")
	protected static void createField(String alias, Namespace namespace, String name, PropertyType propertyType) {
		Field field = new Field(alias, namespace, name, propertyType);
		fieldMap.put(field.alias, field);
	}

	private final DavPropertyName davPropertyName;

	protected final String alias;

	protected final String uri;

	protected final String requestPropertyString;

	protected final DavPropertyName responsePropertyName;

	protected final DavPropertyName updatePropertyName;

	protected final String cast;

	protected final boolean isIntValue;

	protected final boolean isMultivalued;

	protected final boolean isBooleanValue;

	protected final boolean isFloatValue;

	protected final boolean isDateValue;

	/**
	 * Создать поле для пространства имен и имени, использовать имя в качестве псевдонима.
	 * @param namespace Пространство имен обмена
	 * @param name Имя обмена
	 */
	protected Field(Namespace namespace, String name) {
		this(name, namespace, name, null);
	}

	/**
	 * Создать поле для пространства имен и имени типа propertyType.
	 * @param alias логическое имя в MT-EWS
	 * @param namespace пространство имен Exchange
	 * @param name имя Exchange
	 * @param propertyType тип свойства
	 */
	protected Field(String alias, Namespace namespace, String name, PropertyType propertyType) {
		this(alias, namespace, name, propertyType, null, null, name);
	}

	/**
	 * Создать поле для пространства имен и имени типа propertyType.
	 * @param alias логическое имя в MT-EWS
	 * @param namespace пространство имен Exchange
	 * @param name имя Exchange
	 * @param propertyType тип свойства
	 * @param responseAlias имя свойства в ответе SEARCH (как responsealias в запросе)
	 * @param cast тип приведения ответа (например, bin.base64)
	 * @param updateAlias некоторые свойства используют другой алиас в запросах PROPPATCH
	 */
	protected Field(String alias, Namespace namespace, String name, PropertyType propertyType, String responseAlias,
			String cast, String updateAlias) {
		this.alias = alias;

		// property name in PROPFIND requests
		davPropertyName = DavPropertyName.create(name, namespace);
		// property name in PROPPATCH requests
		updatePropertyName = DavPropertyName.create(updateAlias, namespace);

		// a few type based flags
		isMultivalued = propertyType != null && propertyType.toString().endsWith("Array");
		isIntValue = propertyType == PropertyType.Long || propertyType == PropertyType.Integer
				|| propertyType == PropertyType.Short;
		isBooleanValue = propertyType == PropertyType.Boolean;
		isFloatValue = propertyType == PropertyType.Float || propertyType == PropertyType.Double;
		isDateValue = propertyType == PropertyType.SystemTime;

		this.uri = namespace.getURI() + name;
		if (responseAlias == null) {
			this.requestPropertyString = '"' + uri + '"';
			this.responsePropertyName = davPropertyName;
		}
		else {
			this.requestPropertyString = '"' + uri + "\" as " + responseAlias;
			this.responsePropertyName = DavPropertyName.create(responseAlias, EMPTY);
		}
		this.cast = cast;
	}

	/**
	 * Свойство uri.
	 * @return uri
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * Тип свойства значение целое число.
	 * @return true, если значение поля является целым числом
	 */
	public boolean isIntValue() {
		return isIntValue;
	}

	/**
	 * Получить поле по псевдониму.
	 * @param alias псевдоним поля
	 * @return поле
	 */
	public static Field get(String alias) {
		Field field = fieldMap.get(alias);
		if (field == null) {
			throw new IllegalArgumentException("Unknown field: " + alias);
		}
		return field;
	}

	/**
	 * Получить поле заголовка Mime.
	 * @param headerName имя заголовка
	 * @return объект поля
	 */
	public static Field getHeader(String headerName) {
		return new Field(SCHEMAS_MAPI_STRING_INTERNET_HEADERS, headerName);
	}

	/**
	 * Создать объект DavProperty для поля alias и value.
	 * @param alias Псевдоним поля MT-EWS
	 * @param value Значение поля
	 * @return DavProperty со значением или DavPropertyName для нулевых значений
	 */
	public static PropEntry createDavProperty(String alias, String value) {
		Field field = Field.get(alias);
		if (value == null) {
			// return DavPropertyName to remove property
			return field.updatePropertyName;
		}
		else if (field.isMultivalued) {
			// multivalued field, split values separated by \n
			List<XmlSerializable> valueList = new ArrayList<>();
			String[] values = value.split(",");
			for (final String singleValue : values) {
				valueList.add(document -> DomUtil.createElement(document, "v", XML, singleValue));
			}

			return new DefaultDavProperty<>(field.updatePropertyName, valueList);
		}
		else if (field.isBooleanValue && !"haspicture".equals(alias)) {
			if ("true".equals(value)) {
				return new DefaultDavProperty<>(field.updatePropertyName, "1");
			}
			else if ("false".equals(value)) {
				return new DefaultDavProperty<>(field.updatePropertyName, "0");
			}
			else {
				throw new RuntimeException("Invalid value for " + field.alias + ": " + value);
			}
		}
		else {
			return new DefaultDavProperty<>(field.updatePropertyName, value);
		}
	}

	/**
	 * Создаёт объект значения свойства для поля и значения.
	 * @param alias псевдоним поля
	 * @param value значение поля
	 * @return объект значения свойства
	 * @see ExchangePropPatchRequest
	 */
	public static PropertyValue createPropertyValue(String alias, String value) {
		Field field = Field.get(alias);
		DavPropertyName davPropertyName = field.davPropertyName;
		if (value == null) {
			// return DavPropertyName to remove property
			return new PropertyValue(davPropertyName.getNamespace().getURI(), davPropertyName.getName());
		}
		else if (field.isMultivalued) {
			StringBuilder buffer = new StringBuilder();
			// multivalued field, split values separated by \n
			String[] values = value.split("\n");
			for (final String singleValue : values) {
				buffer.append("<v>");
				buffer.append(StringUtil.xmlEncode(singleValue));
				buffer.append("</v>");
			}
			return new PropertyValue(davPropertyName.getNamespace().getURI(), davPropertyName.getName(),
					buffer.toString());
		}
		else if (field.isBooleanValue) {
			if ("true".equals(value)) {
				return new PropertyValue(davPropertyName.getNamespace().getURI(), davPropertyName.getName(), "1",
						"boolean");
			}
			else if ("false".equals(value)) {
				return new PropertyValue(davPropertyName.getNamespace().getURI(), davPropertyName.getName(), "0",
						"boolean");
			}
			else {
				throw new RuntimeException("Invalid value for " + field.alias + ": " + value);
			}
		}
		else if (field.isFloatValue) {
			return new PropertyValue(davPropertyName.getNamespace().getURI(), davPropertyName.getName(),
					StringUtil.xmlEncode(value), "float");
		}
		else if (field.isIntValue) {
			return new PropertyValue(field.updatePropertyName.getNamespace().getURI(),
					field.updatePropertyName.getName(), StringUtil.xmlEncode(value), "int");
		}
		else if (field.isDateValue) {
			return new PropertyValue(field.updatePropertyName.getNamespace().getURI(),
					field.updatePropertyName.getName(), StringUtil.xmlEncode(value), "dateTime.tz");
		}
		else {
			return new PropertyValue(davPropertyName.getNamespace().getURI(), davPropertyName.getName(),
					StringUtil.xmlEncode(value));
		}
	}

	/**
	 * Имя свойства запроса SEARCH для алиаса
	 * @param alias алиас поля
	 * @return строка свойства запроса
	 */
	public static String getRequestPropertyString(String alias) {
		return Field.get(alias).requestPropertyString;
	}

	/**
	 * Имя свойства запроса PROPFIND
	 * @param alias псевдоним поля
	 * @return имя свойства запроса
	 */
	public static DavPropertyName getPropertyName(String alias) {
		return Field.get(alias).davPropertyName;
	}

	/**
	 * Имя свойства ответа для ПОИСКА
	 * @param alias псевдоним поля
	 * @return имя свойства ответа
	 */
	public static DavPropertyName getResponsePropertyName(String alias) {
		return Field.get(alias).responsePropertyName;
	}

}
