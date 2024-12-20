/*
DIT
 */
package ru.mos.mostech.ews.ldap;

import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.AbstractConnection;
import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.exception.MosTechEwsException;
import ru.mos.mostech.ews.exchange.ExchangeSession;
import ru.mos.mostech.ews.exchange.ExchangeSessionFactory;
import ru.mos.mostech.ews.exchange.dav.WebdavExchangeSession;

import javax.naming.InvalidNameException;
import javax.naming.ldap.Rdn;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslServer;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Обработать соединение caldav.
 */
@Slf4j
public class LdapConnection extends AbstractConnection {

	/**
	 * MT-EWS базовый контекст
	 */
	static final String BASE_CONTEXT = "ou=people";

	/**
	 * Базовый контекст сервера OSX (OpenDirectory)
	 */
	static final String OD_BASE_CONTEXT = "o=od";
	static final String OD_USER_CONTEXT = "cn=users, o=od";
	static final String OD_CONFIG_CONTEXT = "cn=config, o=od";
	static final String COMPUTER_CONTEXT = "cn=computers, o=od";
	static final String OD_GROUP_CONTEXT = "cn=groups, o=od";

	// TODO: adjust Directory Utility settings
	static final String COMPUTER_CONTEXT_LION = "cn=computers,o=od";
	static final String OD_USER_CONTEXT_LION = "cn=users, ou=people";

	/**
	 * Корневые контексты именования DSE (по умолчанию и OpenDirectory)
	 */
	static final List<String> NAMING_CONTEXTS = new ArrayList<>();

	static {
		NAMING_CONTEXTS.add(BASE_CONTEXT);
		NAMING_CONTEXTS.add(OD_BASE_CONTEXT);
	}

	static final List<String> PERSON_OBJECT_CLASSES = new ArrayList<>();

	static {
		PERSON_OBJECT_CLASSES.add("top");
		PERSON_OBJECT_CLASSES.add("person");
		PERSON_OBJECT_CLASSES.add("organizationalPerson");
		PERSON_OBJECT_CLASSES.add("inetOrgPerson");
		// OpenDirectory class for iCal
		PERSON_OBJECT_CLASSES.add("apple-user");
	}

	/**
	 * Сопоставление имен атрибутов контактов Exchange с атрибутами LDAP. Используется
	 * только когда возвращаемые атрибуты пусты в запросе LDAP (возвращает все доступные
	 * атрибуты)
	 */
	static final HashMap<String, String> CONTACT_TO_LDAP_ATTRIBUTE_MAP = new HashMap<>();

	static {
		CONTACT_TO_LDAP_ATTRIBUTE_MAP.put("imapUid", "uid");
		CONTACT_TO_LDAP_ATTRIBUTE_MAP.put("co", "countryname");
		CONTACT_TO_LDAP_ATTRIBUTE_MAP.put("extensionattribute1", "custom1");
		CONTACT_TO_LDAP_ATTRIBUTE_MAP.put("extensionattribute2", "custom2");
		CONTACT_TO_LDAP_ATTRIBUTE_MAP.put("extensionattribute3", "custom3");
		CONTACT_TO_LDAP_ATTRIBUTE_MAP.put("extensionattribute4", "custom4");
		CONTACT_TO_LDAP_ATTRIBUTE_MAP.put("smtpemail1", "mail");
		CONTACT_TO_LDAP_ATTRIBUTE_MAP.put("smtpemail2", "xmozillasecondemail");
		CONTACT_TO_LDAP_ATTRIBUTE_MAP.put("homeCountry", "mozillahomecountryname");
		CONTACT_TO_LDAP_ATTRIBUTE_MAP.put("homeCity", "mozillahomelocalityname");
		CONTACT_TO_LDAP_ATTRIBUTE_MAP.put("homePostalCode", "mozillahomepostalcode");
		CONTACT_TO_LDAP_ATTRIBUTE_MAP.put("homeState", "mozillahomestate");
		CONTACT_TO_LDAP_ATTRIBUTE_MAP.put("homeStreet", "mozillahomestreet");
		CONTACT_TO_LDAP_ATTRIBUTE_MAP.put("businesshomepage", "mozillaworkurl");
		CONTACT_TO_LDAP_ATTRIBUTE_MAP.put("nickname", "mozillanickname");
	}

	/**
	 * Константа GUID компьютера OSX (используется для завершения участников в iCal)
	 */
	static final String COMPUTER_GUID = "52486C30-F0AB-48E3-9C37-37E9B28CDD7B";

	/**
	 * Константа виртуального хоста OSX guid (используется для завершения участников в
	 * iCal)
	 */
	static final String VIRTUALHOST_GUID = "D6DD8A10-1098-11DE-8C30-0800200C9A66";

	/**
	 * Константное значение OSX для атрибута apple-serviceslocator
	 */
	static final HashMap<String, String> STATIC_ATTRIBUTE_MAP = new HashMap<>();

	static {
		STATIC_ATTRIBUTE_MAP.put("apple-serviceslocator", COMPUTER_GUID + ':' + VIRTUALHOST_GUID + ":calendar");
	}

	/**
	 * Карта критериев LDAP в Exchange
	 */
	// TODO: remove
	static final HashMap<String, String> CRITERIA_MAP = new HashMap<>();

	static {
		// assume mail starts with firstname
		CRITERIA_MAP.put("uid", "AN");
		CRITERIA_MAP.put("mail", "FN");
		CRITERIA_MAP.put("displayname", "DN");
		CRITERIA_MAP.put("cn", "DN");
		CRITERIA_MAP.put("givenname", "FN");
		CRITERIA_MAP.put("sn", "LN");
		CRITERIA_MAP.put("title", "TL");
		CRITERIA_MAP.put("company", "CP");
		CRITERIA_MAP.put("o", "CP");
		CRITERIA_MAP.put("l", "OF");
		CRITERIA_MAP.put("department", "DP");
		CRITERIA_MAP.put("apple-group-realname", "DP");
	}

	/**
	 * Сопоставление атрибутов контактов LDAP и Exchange.
	 */
	static final HashMap<String, String> LDAP_TO_CONTACT_ATTRIBUTE_MAP = new HashMap<>();

	static {
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("uid", "imapUid");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("mail", "smtpemail1");

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("displayname", "cn");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("commonname", "cn");

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("givenname", "givenName");

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("surname", "sn");

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("company", "o");

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("apple-group-realname", "department");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("mozillahomelocalityname", "homeCity");

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("c", "co");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("countryname", "co");

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("custom1", "extensionattribute1");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("custom2", "extensionattribute2");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("custom3", "extensionattribute3");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("custom4", "extensionattribute4");

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("mozillacustom1", "extensionattribute1");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("mozillacustom2", "extensionattribute2");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("mozillacustom3", "extensionattribute3");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("mozillacustom4", "extensionattribute4");

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("telephonenumber", "telephoneNumber");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("orgunit", "department");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("departmentnumber", "department");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("ou", "department");

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("mozillaworkstreet2", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("mozillahomestreet", "homeStreet");

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("xmozillanickname", "nickname");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("mozillanickname", "nickname");

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("cellphone", "mobile");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("homeurl", "personalHomePage");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("mozillahomeurl", "personalHomePage");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("apple-user-homeurl", "personalHomePage");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("mozillahomepostalcode", "homePostalCode");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("fax", "facsimiletelephonenumber");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("mozillahomecountryname", "homeCountry");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("streetaddress", "street");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("mozillaworkurl", "businesshomepage");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("workurl", "businesshomepage");

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("region", "st");

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("birthmonth", "bday");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("birthday", "bday");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("birthyear", "bday");

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("carphone", "othermobile");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("nsaimid", "im");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("nscpaimscreenname", "im");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("apple-imhandle", "im");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("imhandle", "im");

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("xmozillasecondemail", "smtpemail2");

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("notes", "description");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("pagerphone", "pager");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("pager", "pager");

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("locality", "l");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("homephone", "homePhone");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("mozillasecondemail", "smtpemail2");

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("zip", "postalcode");
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("mozillahomestate", "homeState");

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("modifytimestamp", "lastmodified");

		// ignore attribute
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("objectclass", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("mozillausehtmlmail", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("xmozillausehtmlmail", null);

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("mozillahomestreet2", null);

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("labeleduri", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("apple-generateduid", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("uidnumber", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("gidnumber", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("jpegphoto", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("apple-emailcontacts", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("apple-user-picture", null);

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("_writers_usercertificate", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("_writers_realname", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("_writers_jpegphoto", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("_guest", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("_writers_linkedidentity", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("_defaultlanguage", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("_writers_hint", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("_writers__defaultlanguage", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("_writers_picture", null);

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("apple-user-authenticationhint", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("external", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("userpassword", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("linkedidentity", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("homedirectory", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("authauthority", null);

		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("applefloor", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("buildingname", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("destinationindicator", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("postaladdress", null);
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("homepostaladdress", null);

		// iCal search attribute
		LDAP_TO_CONTACT_ATTRIBUTE_MAP.put("apple-serviceslocator", "apple-serviceslocator");
	}

	/**
	 * Карта игнорируемых атрибутов фильтра LDAP
	 */
	// TODO remove
	static final HashSet<String> IGNORE_MAP = new HashSet<>();

	static {
		IGNORE_MAP.add("objectclass");
		IGNORE_MAP.add("apple-generateduid");
		IGNORE_MAP.add("augmentconfiguration");
		IGNORE_MAP.add("ou");
		IGNORE_MAP.add("apple-realname");
		IGNORE_MAP.add("apple-group-nestedgroup");
		IGNORE_MAP.add("apple-group-memberguid");
		IGNORE_MAP.add("macaddress");
		IGNORE_MAP.add("memberuid");
	}

	// LDAP version
	static final int LDAP_VERSION3 = 0x03;

	// LDAP request operations
	static final int LDAP_REQ_BIND = 0x60;
	static final int LDAP_REQ_SEARCH = 0x63;
	static final int LDAP_REQ_UNBIND = 0x42;
	static final int LDAP_REQ_ABANDON = 0x50;

	// LDAP response operations
	static final int LDAP_REP_BIND = 0x61;
	static final int LDAP_REP_SEARCH = 0x64;
	static final int LDAP_REP_RESULT = 0x65;

	static final int LDAP_SASL_BIND_IN_PROGRESS = 0x0E;

	// LDAP return codes
	static final int LDAP_OTHER = 80;
	static final int LDAP_SUCCESS = 0;
	static final int LDAP_SIZE_LIMIT_EXCEEDED = 4;
	static final int LDAP_INVALID_CREDENTIALS = 49;

	// LDAP filter code
	static final int LDAP_FILTER_AND = 0xa0;
	static final int LDAP_FILTER_OR = 0xa1;
	static final int LDAP_FILTER_NOT = 0xa2;

	// LDAP filter operators
	static final int LDAP_FILTER_SUBSTRINGS = 0xa4;

	static final int LDAP_FILTER_PRESENT = 0x87;

	static final int LDAP_FILTER_EQUALITY = 0xa3;

	// LDAP filter mode
	static final int LDAP_SUBSTRING_INITIAL = 0x80;
	static final int LDAP_SUBSTRING_ANY = 0x81;
	static final int LDAP_SUBSTRING_FINAL = 0x82;

	// BER data types
	static final int LBER_ENUMERATED = 0x0a;
	static final int LBER_SET = 0x31;
	static final int LBER_SEQUENCE = 0x30;

	// LDAP search scope
	static final int SCOPE_BASE_OBJECT = 0;

	/**
	 * Сервис SASL для аутентификации DIGEST-MD5
	 */
	protected SaslServer saslServer;

	/**
	 * сырой ввод подключения inputStream
	 */
	protected BufferedInputStream is;

	/**
	 * многоразовый кодировщик BER
	 */
	protected final BerEncoder responseBer = new BerEncoder();

	/**
	 * Текущая версия LDAP (используется для кодирования строк)
	 */
	int ldapVersion = LDAP_VERSION3;

	/**
	 * Карта потоков поиска
	 */
	protected final HashMap<Integer, SearchRunnable> searchThreadMap = new HashMap<>();

	/**
	 * Инициализировать потоки и запустить поток.
	 * @param clientSocket Сокет LDAP клиента
	 */
	public LdapConnection(Socket clientSocket) {
		super(LdapConnection.class.getSimpleName(), clientSocket);
		try {
			is = new BufferedInputStream(client.getInputStream());
			os = new BufferedOutputStream(client.getOutputStream());
		}
		catch (IOException e) {
			close();
			log.error("{}", new BundleMessage("LOG_EXCEPTION_GETTING_SOCKET_STREAMS"), e);
		}
	}

	protected boolean isLdapV3() {
		return ldapVersion == LDAP_VERSION3;
	}

	@SuppressWarnings({ "java:S3776", "java:S6541", "java:S135" })
	@Override
	public void doRun() {
		byte[] inbuf = new byte[2048]; // Buffer for reading incoming bytes
		int bytesread; // Number of bytes in inbuf
		int bytesleft; // Number of bytes that need to read for completing resp
		int br; // Temp; number of bytes read from stream
		int offset; // Offset of where to store bytes in inbuf
		boolean eos; // End of stream

		try {
			ExchangeSessionFactory.checkConfig();
			while (true) {
				offset = 0;

				// check that it is the beginning of a sequence
				bytesread = is.read(inbuf, offset, 1);
				if (bytesread < 0) {
					break; // EOF
				}

				if (inbuf[offset++] != (Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR)) {
					continue;
				}

				// get length of sequence
				bytesread = is.read(inbuf, offset, 1);
				if (bytesread < 0) {
					break; // EOF
				}
				int seqlen = inbuf[offset++]; // Length of ASN sequence

				// if high bit is on, length is encoded in the
				// subsequent length bytes and the number of length bytes
				// is equal to & 0x80 (i.e. length byte with high bit off).
				if ((seqlen & 0x80) == 0x80) {
					int seqlenlen = seqlen & 0x7f; // number of length bytes

					bytesread = 0;
					eos = false;

					// Read all length bytes
					while (bytesread < seqlenlen) {
						br = is.read(inbuf, offset + bytesread, seqlenlen - bytesread);
						if (br < 0) {
							eos = true;
							break; // EOF
						}
						bytesread += br;
					}

					// end-of-stream reached before length bytes are read
					if (eos) {
						break; // EOF
					}

					// Add contents of length bytes to determine length
					seqlen = 0;
					for (int i = 0; i < seqlenlen; i++) {
						seqlen = (seqlen << 8) + (inbuf[offset + i] & 0xff);
					}
					offset += bytesread;
				}

				// read in seqlen bytes
				bytesleft = seqlen;
				if ((offset + bytesleft) > inbuf.length) {
					byte[] nbuf = new byte[offset + bytesleft];
					System.arraycopy(inbuf, 0, nbuf, 0, offset);
					inbuf = nbuf;
				}
				while (bytesleft > 0) {
					bytesread = is.read(inbuf, offset, bytesleft);
					if (bytesread < 0) {
						break; // EOF
					}
					offset += bytesread;
					bytesleft -= bytesread;
				}
				handleRequest(inbuf, offset);
			}

		}
		catch (SocketException e) {
			log.debug("{}", new BundleMessage("LOG_CONNECTION_CLOSED"));
		}
		catch (SocketTimeoutException e) {
			log.debug("{}", new BundleMessage("LOG_CLOSE_CONNECTION_ON_TIMEOUT"));
		}
		catch (Exception e) {
			log.error("", e);
			try {
				sendErr(0, LDAP_REP_BIND, e);
			}
			catch (IOException e2) {
				log.warn("{}", new BundleMessage("LOG_EXCEPTION_SENDING_ERROR_TO_CLIENT"), e2);
			}
		}
		finally {
			// cancel all search threads
			synchronized (searchThreadMap) {
				for (SearchRunnable searchRunnable : searchThreadMap.values()) {
					searchRunnable.abandon();
				}
			}
			close();
		}
	}

	protected static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

	protected void handleRequest(byte[] inbuf, int offset) throws IOException {
		BerDecoder reqBer = new BerDecoder(inbuf, 0, offset);
		int currentMessageId = 0;
		try {
			reqBer.parseSeq(null);
			currentMessageId = reqBer.parseInt();
			int requestOperation = reqBer.peekByte();

			if (requestOperation == LDAP_REQ_BIND) {
				reqBer.parseSeq(null);
				ldapVersion = reqBer.parseInt();
				// check for dn authentication
				userName = extractRdnValue(reqBer.parseString(isLdapV3()));
				if (reqBer.peekByte() == (Ber.ASN_CONTEXT | Ber.ASN_CONSTRUCTOR | 3)) {
					// SASL authentication
					reqBer.parseSeq(null);
					// Get mechanism, usually DIGEST-MD5
					String mechanism = reqBer.parseString(isLdapV3());

					byte[] serverResponse;
					CallbackHandler callbackHandler = callbacks -> {
						// look for username in callbacks
						for (Callback callback : callbacks) {
							if (callback instanceof NameCallback nameCallback) {
								userName = extractRdnValue(nameCallback.getDefaultName());
								// get password from session pool
								password = ExchangeSessionFactory.getUserPassword(userName);
							}
						}
						// handle other callbacks
						for (Callback callback : callbacks) {
							if (callback instanceof AuthorizeCallback authorizeCallback) {
								authorizeCallback.setAuthorized(true);
							}
							else if (callback instanceof PasswordCallback passwordCallback && password != null) {
								passwordCallback.setPassword(password.toCharArray());
							}
						}
					};
					int status;
					if (reqBer.bytesLeft() > 0 && saslServer != null) {
						byte[] clientResponse = reqBer.parseOctetString(Ber.ASN_OCTET_STR, null);
						serverResponse = saslServer.evaluateResponse(clientResponse);
						status = LDAP_SUCCESS;

						log.debug("{}", new BundleMessage("LOG_LDAP_REQ_BIND_USER", currentMessageId, userName));
						try {
							session = ExchangeSessionFactory.getInstance(userName, password);
							logConnection("LOGON", userName);
							log.debug("{}", new BundleMessage("LOG_LDAP_REQ_BIND_SUCCESS"));
						}
						catch (IOException e) {
							logConnection("FAILED", userName);
							serverResponse = EMPTY_BYTE_ARRAY;
							status = LDAP_INVALID_CREDENTIALS;
							log.debug("{}", new BundleMessage("LOG_LDAP_REQ_BIND_INVALID_CREDENTIALS"));
						}

					}
					else {
						Map<String, String> properties = new HashMap<>();
						properties.put("javax.security.sasl.qop", "auth,auth-int");
						saslServer = Sasl.createSaslServer(mechanism, "ldap", client.getLocalAddress().getHostAddress(),
								properties, callbackHandler);
						if (saslServer == null) {
							throw new IOException("Unable to create SASL server for mechanism " + mechanism);
						}
						serverResponse = saslServer.evaluateResponse(EMPTY_BYTE_ARRAY);
						status = LDAP_SASL_BIND_IN_PROGRESS;
					}

					responseBer.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
					responseBer.encodeInt(currentMessageId);
					responseBer.beginSeq(LDAP_REP_BIND);
					responseBer.encodeInt(status, LBER_ENUMERATED);
					// server credentials
					responseBer.encodeString("", isLdapV3());
					responseBer.encodeString("", isLdapV3());
					// challenge or response
					if (serverResponse != null) {
						responseBer.encodeOctetString(serverResponse, 0x87);
					}
					responseBer.endSeq();
					responseBer.endSeq();
					sendResponse();

				}
				else {
					password = reqBer.parseStringWithTag(Ber.ASN_CONTEXT, isLdapV3(), null);

					if (userName.length() > 0 && password.length() > 0) {
						log.debug("{}", new BundleMessage("LOG_LDAP_REQ_BIND_USER", currentMessageId, userName));
						try {
							session = ExchangeSessionFactory.getInstance(userName, password);
							logConnection("LOGON", userName);
							log.debug("{}", new BundleMessage("LOG_LDAP_REQ_BIND_SUCCESS"));
							sendClient(currentMessageId, LDAP_REP_BIND, LDAP_SUCCESS, "");
						}
						catch (IOException e) {
							logConnection("FAILED", userName);
							log.debug("{}", new BundleMessage("LOG_LDAP_REQ_BIND_INVALID_CREDENTIALS"));
							sendClient(currentMessageId, LDAP_REP_BIND, LDAP_INVALID_CREDENTIALS, "");
						}
					}
					else {
						log.debug("{}", new BundleMessage("LOG_LDAP_REQ_BIND_ANONYMOUS", currentMessageId));
						// anonymous bind
						sendClient(currentMessageId, LDAP_REP_BIND, LDAP_SUCCESS, "");
					}
				}

			}
			else if (requestOperation == LDAP_REQ_UNBIND) {
				log.debug("{}", new BundleMessage("LOG_LDAP_REQ_UNBIND", currentMessageId));
				if (session != null) {
					session = null;
				}
			}
			else if (requestOperation == LDAP_REQ_SEARCH) {
				reqBer.parseSeq(null);
				String dn = reqBer.parseString(isLdapV3());
				int scope = reqBer.parseEnumeration();
				/* int разыменованныеПсевдонимы = */
				reqBer.parseEnumeration();
				int sizeLimit = reqBer.parseInt();
				if (sizeLimit > 100 || sizeLimit == 0) {
					sizeLimit = 100;
				}
				int timelimit = reqBer.parseInt();
				/* типы только для */
				reqBer.parseBoolean();
				LdapFilter ldapFilter = parseFilter(reqBer);
				Set<String> returningAttributes = parseReturningAttributes(reqBer);
				SearchRunnable searchRunnable = new SearchRunnable(currentMessageId, dn, scope, sizeLimit, timelimit,
						ldapFilter, returningAttributes);
				if (BASE_CONTEXT.equalsIgnoreCase(dn) || OD_USER_CONTEXT.equalsIgnoreCase(dn)
						|| OD_USER_CONTEXT_LION.equalsIgnoreCase(dn)) {
					// launch search in a separate thread
					synchronized (searchThreadMap) {
						searchThreadMap.put(currentMessageId, searchRunnable);
					}
					Thread searchThread = new Thread(searchRunnable);
					searchThread.setName(getName() + "-Search-" + currentMessageId);
					searchThread.start();
				}
				else {
					// no need to create a separate thread, just run
					searchRunnable.run();
				}

			}
			else if (requestOperation == LDAP_REQ_ABANDON) {
				int abandonMessageId;
				abandonMessageId = reqBer.parseIntWithTag(LDAP_REQ_ABANDON);
				synchronized (searchThreadMap) {
					SearchRunnable searchRunnable = searchThreadMap.get(abandonMessageId);
					if (searchRunnable != null) {
						searchRunnable.abandon();
						searchThreadMap.remove(currentMessageId);
					}
				}
				log.debug("{}", new BundleMessage("LOG_LDAP_REQ_ABANDON_SEARCH", currentMessageId, abandonMessageId));
			}
			else {
				log.debug("{}", new BundleMessage("LOG_LDAP_UNSUPPORTED_OPERATION", requestOperation));
				sendClient(currentMessageId, LDAP_REP_RESULT, LDAP_OTHER, "Unsupported operation");
			}
		}
		catch (IOException e) {
			dumpBer(inbuf, offset);
			try {
				sendErr(currentMessageId, LDAP_REP_RESULT, e);
			}
			catch (IOException e2) {
				log.debug("{}", new BundleMessage("LOG_EXCEPTION_SENDING_ERROR_TO_CLIENT"), e2);
			}
			throw e;
		}
	}

	/**
	 * Извлечь значение rdn из имени пользователя
	 * @param dn отличительное имя или имя пользователя
	 * @return имя пользователя
	 */
	private String extractRdnValue(String dn) throws IOException {
		if (dn.startsWith("uid=")) {
			String rdn = dn;
			if (rdn.indexOf(',') > 0) {
				rdn = rdn.substring(0, rdn.indexOf(','));
			}
			try {
				return (String) new Rdn(rdn).getValue();
			}
			catch (InvalidNameException e) {
				throw new IOException(e);
			}
		}
		else {
			return dn;
		}
	}

	protected void dumpBer(byte[] inbuf, int offset) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Ber.dumpBER(baos, "LDAP request buffer\n", inbuf, 0, offset);
		log.debug(new String(baos.toByteArray(), StandardCharsets.UTF_8));
	}

	protected LdapFilter parseFilter(BerDecoder reqBer) throws IOException {
		LdapFilter ldapFilter;
		if (reqBer.peekByte() == LDAP_FILTER_PRESENT) {
			String attributeName = reqBer.parseStringWithTag(LDAP_FILTER_PRESENT, isLdapV3(), null).toLowerCase();
			ldapFilter = new SimpleFilter(attributeName);
		}
		else {
			int[] seqSize = new int[1];
			int ldapFilterType = reqBer.parseSeq(seqSize);
			int end = reqBer.getParsePosition() + seqSize[0];

			ldapFilter = parseNestedFilter(reqBer, ldapFilterType, end);
		}

		return ldapFilter;
	}

	protected LdapFilter parseNestedFilter(BerDecoder reqBer, int ldapFilterType, int end) throws IOException {
		LdapFilter nestedFilter;

		if ((ldapFilterType == LDAP_FILTER_OR) || (ldapFilterType == LDAP_FILTER_AND)
				|| ldapFilterType == LDAP_FILTER_NOT) {
			nestedFilter = new CompoundFilter(ldapFilterType);

			while (reqBer.getParsePosition() < end && reqBer.bytesLeft() > 0) {
				if (reqBer.peekByte() == LDAP_FILTER_PRESENT) {
					String attributeName = reqBer.parseStringWithTag(LDAP_FILTER_PRESENT, isLdapV3(), null)
						.toLowerCase();
					nestedFilter.add(new SimpleFilter(attributeName));
				}
				else {
					int[] seqSize = new int[1];
					int ldapFilterOperator = reqBer.parseSeq(seqSize);
					int subEnd = reqBer.getParsePosition() + seqSize[0];
					nestedFilter.add(parseNestedFilter(reqBer, ldapFilterOperator, subEnd));
				}
			}
		}
		else {
			// simple filter
			nestedFilter = parseSimpleFilter(reqBer, ldapFilterType);
		}

		return nestedFilter;
	}

	protected LdapFilter parseSimpleFilter(BerDecoder reqBer, int ldapFilterOperator) throws IOException {
		String attributeName = reqBer.parseString(isLdapV3()).toLowerCase();
		int ldapFilterMode = 0;

		StringBuilder value = new StringBuilder();
		if (ldapFilterOperator == LDAP_FILTER_SUBSTRINGS) {
			// Thunderbird sends values with space as separate strings, rebuild value
			int[] seqSize = new int[1];
			/* ПОДОБИЕ_ЛБЕР */
			reqBer.parseSeq(seqSize);
			int end = reqBer.getParsePosition() + seqSize[0];
			while (reqBer.getParsePosition() < end && reqBer.bytesLeft() > 0) {
				ldapFilterMode = reqBer.peekByte();
				if (value.length() > 0) {
					value.append(' ');
				}
				value.append(reqBer.parseStringWithTag(ldapFilterMode, isLdapV3(), null));
			}
		}
		else if (ldapFilterOperator == LDAP_FILTER_EQUALITY) {
			value.append(reqBer.parseString(isLdapV3()));
		}
		else {
			log.warn("{}", new BundleMessage("LOG_LDAP_UNSUPPORTED_FILTER_VALUE"));
		}

		String sValue = value.toString();

		if ("uid".equalsIgnoreCase(attributeName) && sValue.equals(userName)) {
			// replace with actual alias instead of login name search, only in Dav mode
			if (session instanceof WebdavExchangeSession) {
				sValue = session.getAlias();
				log.debug("{}", new BundleMessage("LOG_LDAP_REPLACED_UID_FILTER", userName, sValue));
			}
		}

		return new SimpleFilter(attributeName, sValue, ldapFilterOperator, ldapFilterMode);
	}

	protected Set<String> parseReturningAttributes(BerDecoder reqBer) throws IOException {
		Set<String> returningAttributes = new HashSet<>();
		int[] seqSize = new int[1];
		reqBer.parseSeq(seqSize);
		int end = reqBer.getParsePosition() + seqSize[0];
		while (reqBer.getParsePosition() < end && reqBer.bytesLeft() > 0) {
			returningAttributes.add(reqBer.parseString(isLdapV3()).toLowerCase());
		}
		return returningAttributes;
	}

	/**
	 * Отправить корневую DSE
	 * @param currentMessageId текущий идентификатор сообщения
	 * @throws IOException в случае ошибки
	 */
	protected void sendRootDSE(int currentMessageId) throws IOException {
		log.debug("{}", new BundleMessage("LOG_LDAP_SEND_ROOT_DSE"));

		Map<String, Object> attributes = new HashMap<>();
		attributes.put("objectClass", "top");
		attributes.put("namingContexts", NAMING_CONTEXTS);

		sendEntry(currentMessageId, "Root DSE", attributes);
	}

	protected void addIf(Map<String, Object> attributes, Set<String> returningAttributes, String name, Object value) {
		if ((returningAttributes.isEmpty()) || returningAttributes.contains(name)) {
			attributes.put(name, value);
		}
	}

	protected String currentHostName;

	protected String getCurrentHostName() throws UnknownHostException {
		if (currentHostName == null) {
			InetAddress clientInetAddress = client.getInetAddress();
			if (clientInetAddress != null && clientInetAddress.isLoopbackAddress()) {
				// local address, probably using localhost in iCal URL
				currentHostName = "localhost";
			}
			else {
				// remote address, send fully qualified domain name
				currentHostName = InetAddress.getLocalHost().getCanonicalHostName();
			}
		}
		return currentHostName;
	}

	/**
	 * Кэшировать строковое значение serviceInfo
	 */
	protected String serviceInfo;

	protected String getServiceInfo() throws UnknownHostException {
		if (serviceInfo == null) {
			serviceInfo = ("<?xml version='1.0' encoding='UTF-8'?>"
					+ "<!DOCTYPE plist PUBLIC '-//Apple//DTD PLIST 1.0//EN' 'http://www.apple.com/DTDs/PropertyList-1.0.dtd'>"
					+ "<plist version='1.0'>" + "<dict>" + "<key>com.apple.macosxserver.host</key>" + "<array>"
					+ "<string>localhost</string>" + // NOTE: Will be replaced by real
														// hostname
					"</array>" + "<key>com.apple.macosxserver.virtualhosts</key>" + "<dict>" + "<key>"
					+ VIRTUALHOST_GUID + "</key>" + "<dict>" + "<key>hostDetails</key>" + "<dict>" + "<key>http</key>"
					+ "<dict>" + "<key>enabled</key>" + "<true/>" + "<key>port</key>" + "<integer>")
					+ Settings.getProperty("mt.ews.caldavPort") + "</integer>" + "</dict>" + "<key>https</key>"
					+ "<dict>" + "<key>disabled</key>" + "<false/>" + "<key>port</key>" + "<integer>0</integer>"
					+ "</dict>" + "</dict>" + "<key>hostname</key>" + "<string>" + getCurrentHostName() + "</string>"
					+ "<key>serviceInfo</key>" + "<dict>" + "<key>calendar</key>" + "<dict>" + "<key>enabled</key>"
					+ "<true/>" + "<key>templates</key>" + "<dict>" + "<key>calendarUserAddresses</key>" + "<array>"
					+ "<string>%(principaluri)s</string>" + "<string>mailto:%(email)s</string>"
					+ "<string>urn:uuid:%(guid)s</string>" + "</array>" + "<key>principalPath</key>"
					+ "<string>/principals/__uuids__/%(guid)s/</string>" + "</dict>" + "</dict>" + "</dict>"
					+ "<key>serviceType</key>" + "<array>" + "<string>calendar</string>" + "</array>" + "</dict>"
					+ "</dict>" + "</dict>" + "</plist>";
		}
		return serviceInfo;
	}

	/**
	 * Отправить ComputerContext
	 * @param currentMessageId текущий идентификатор сообщения
	 * @param returningAttributes атрибуты для возврата
	 * @throws IOException при ошибке
	 */
	protected void sendComputerContext(int currentMessageId, Set<String> returningAttributes) throws IOException {
		List<String> objectClasses = new ArrayList<>();
		objectClasses.add("top");
		objectClasses.add("apple-computer");
		Map<String, Object> attributes = new HashMap<>();
		addIf(attributes, returningAttributes, "objectClass", objectClasses);
		addIf(attributes, returningAttributes, "apple-generateduid", COMPUTER_GUID);
		addIf(attributes, returningAttributes, "apple-serviceinfo", getServiceInfo());
		// TODO: remove ?
		addIf(attributes, returningAttributes, "apple-xmlplist", getServiceInfo());
		addIf(attributes, returningAttributes, "apple-serviceslocator", "::anyService");
		addIf(attributes, returningAttributes, "cn", getCurrentHostName());

		String dn = "cn=" + getCurrentHostName() + ", " + COMPUTER_CONTEXT;
		log.debug("{}", new BundleMessage("LOG_LDAP_SEND_COMPUTER_CONTEXT", dn, attributes));

		sendEntry(currentMessageId, dn, attributes);
	}

	/**
	 * Отправить базовый контекст
	 * @param currentMessageId текущий идентификатор сообщения
	 * @throws IOException при ошибке
	 */
	protected void sendBaseContext(int currentMessageId) throws IOException {
		List<String> objectClasses = new ArrayList<>();
		objectClasses.add("top");
		objectClasses.add("organizationalUnit");
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("objectClass", objectClasses);
		attributes.put("description", "MT-EWS Gateway LDAP for " + Settings.getProperty("mt.ews.url"));
		sendEntry(currentMessageId, BASE_CONTEXT, attributes);
	}

	protected void sendEntry(int currentMessageId, String dn, Map<String, Object> attributes) throws IOException {
		// synchronize on responseBer
		synchronized (responseBer) {
			responseBer.reset();
			responseBer.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
			responseBer.encodeInt(currentMessageId);
			responseBer.beginSeq(LDAP_REP_SEARCH);
			responseBer.encodeString(dn, isLdapV3());
			responseBer.beginSeq(LBER_SEQUENCE);
			for (Map.Entry<String, Object> entry : attributes.entrySet()) {
				responseBer.beginSeq(LBER_SEQUENCE);
				responseBer.encodeString(entry.getKey(), isLdapV3());
				responseBer.beginSeq(LBER_SET);
				Object values = entry.getValue();
				if (values instanceof String string) {
					responseBer.encodeString(string, isLdapV3());
				}
				else if (values instanceof List) {
					for (Object value : (Iterable) values) {
						responseBer.encodeString((String) value, isLdapV3());
					}
				}
				else {
					throw new MosTechEwsException("EXCEPTION_UNSUPPORTED_VALUE", values);
				}
				responseBer.endSeq();
				responseBer.endSeq();
			}
			responseBer.endSeq();
			responseBer.endSeq();
			responseBer.endSeq();
			sendResponse();
		}
	}

	protected void sendErr(int currentMessageId, int responseOperation, Exception e) throws IOException {
		String message = e.getMessage();
		if (message == null) {
			message = e.toString();
		}
		sendClient(currentMessageId, responseOperation, LDAP_OTHER, message);
	}

	protected void sendClient(int currentMessageId, int responseOperation, int status, String message)
			throws IOException {
		responseBer.reset();

		responseBer.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
		responseBer.encodeInt(currentMessageId);
		responseBer.beginSeq(responseOperation);
		responseBer.encodeInt(status, LBER_ENUMERATED);
		// dn
		responseBer.encodeString("", isLdapV3());
		// error message
		responseBer.encodeString(message, isLdapV3());
		responseBer.endSeq();
		responseBer.endSeq();
		sendResponse();
	}

	protected void sendResponse() throws IOException {
		os.write(responseBer.getBuf(), 0, responseBer.getDataLen());
		os.flush();
	}

	interface LdapFilter {

		ExchangeSession.Condition getContactSearchFilter();

		Map<String, ExchangeSession.Contact> findInGAL(ExchangeSession session, Set<String> returningAttributes,
				int sizeLimit) throws IOException;

		void add(LdapFilter filter);

		boolean isFullSearch();

		boolean isMatch(Map<String, String> person);

	}

	class CompoundFilter implements LdapFilter {

		final Set<LdapFilter> criteria = new HashSet<>();

		final int type;

		CompoundFilter(int filterType) {
			type = filterType;
		}

		@Override
		public String toString() {
			StringBuilder buffer = new StringBuilder();

			if (type == LDAP_FILTER_OR) {
				buffer.append("(|");
			}
			else if (type == LDAP_FILTER_AND) {
				buffer.append("(&");
			}
			else {
				buffer.append("(!");
			}

			for (LdapFilter child : criteria) {
				buffer.append(child.toString());
			}

			buffer.append(')');

			return buffer.toString();
		}

		/**
		 * Добавить дочерний фильтр
		 * @param filter внутренний фильтр
		 */
		public void add(LdapFilter filter) {
			criteria.add(filter);
		}

		/**
		 * Это полный поиск только в том случае, если каждый дочерний элемент также
		 * является полным поиском
		 * @return true, если фильтр полного поиска
		 */
		public boolean isFullSearch() {
			for (LdapFilter child : criteria) {
				if (!child.isFullSearch()) {
					return false;
				}
			}

			return true;
		}

		/**
		 * Постройте фильтр поиска для поиска в папке Контакты. Используйте синтаксис
		 * Exchange SEARCH
		 * @return фильтр поиска контактов
		 */
		public ExchangeSession.Condition getContactSearchFilter() {
			ExchangeSession.MultiCondition condition;

			if (type == LDAP_FILTER_OR) {
				condition = session.or();
			}
			else {
				condition = session.and();
			}

			for (LdapFilter child : criteria) {
				condition.add(child.getContactSearchFilter());
			}

			return condition;
		}

		/**
		 * Проверка соответствия человека текущему фильтру.
		 * @param person карта атрибутов человека
		 * @return true, если фильтр совпадает
		 */
		public boolean isMatch(Map<String, String> person) {
			if (type == LDAP_FILTER_OR) {
				for (LdapFilter child : criteria) {
					if (!child.isFullSearch()) {
						if (child.isMatch(person)) {
							// We've found a match
							return true;
						}
					}
				}

				// No subconditions are met
				return false;
			}
			else if (type == LDAP_FILTER_AND) {
				for (LdapFilter child : criteria) {
					if (!child.isFullSearch()) {
						if (!child.isMatch(person)) {
							// We've found a miss
							return false;
						}
					}
				}

				// All subconditions are met
				return true;
			}

			return false;
		}

		/**
		 * Найти лиц в глобальной адресной книге Exchange, соответствующих фильтру.
		 * Итерация по дочерним фильтрам для построения результатов.
		 * @param session Сессия Exchange
		 * @return карта лиц
		 * @throws IOException в случае ошибки
		 */
		public Map<String, ExchangeSession.Contact> findInGAL(ExchangeSession session, Set<String> returningAttributes,
				int sizeLimit) throws IOException {
			Map<String, ExchangeSession.Contact> persons = null;

			for (LdapFilter child : criteria) {
				int currentSizeLimit = sizeLimit;
				if (persons != null) {
					currentSizeLimit -= persons.size();
				}
				Map<String, ExchangeSession.Contact> childFind = child.findInGAL(session, returningAttributes,
						currentSizeLimit);

				if (childFind != null) {
					if (persons == null) {
						persons = childFind;
					}
					else if (type == LDAP_FILTER_OR) {
						// Create the union of the existing results and the child found
						// results
						persons.putAll(childFind);
					}
					else if (type == LDAP_FILTER_AND) {
						// Append current child filter results that match all child
						// filters to persons.
						// The hard part is that, due to the 100-item-returned galFind
						// limit
						// we may catch new items that match all child filters in each
						// child search.
						// Thus, instead of building the intersection, we check each
						// result against
						// all filters.

						for (ExchangeSession.Contact result : childFind.values()) {
							if (isMatch(result)) {
								// This item from the child result set matches all
								// sub-criteria, add it
								persons.put(result.get("uid"), result);
							}
						}
					}
				}
			}

			if ((persons == null) && !isFullSearch()) {
				// return an empty map (indicating no results were found)
				return new HashMap<>();
			}

			return persons;
		}

	}

	class SimpleFilter implements LdapFilter {

		static final String STAR = "*";

		final String attributeName;

		final String value;

		final int mode;

		final int operator;

		final boolean canIgnore;

		SimpleFilter(String attributeName) {
			this.attributeName = attributeName;
			this.value = SimpleFilter.STAR;
			this.operator = LDAP_FILTER_SUBSTRINGS;
			this.mode = 0;
			this.canIgnore = checkIgnore();
		}

		SimpleFilter(String attributeName, String value, int ldapFilterOperator, int ldapFilterMode) {
			this.attributeName = attributeName;
			this.value = value;
			this.operator = ldapFilterOperator;
			this.mode = ldapFilterMode;
			this.canIgnore = checkIgnore();
		}

		private boolean checkIgnore() {
			if ("objectclass".equals(attributeName) && STAR.equals(value)) {
				// ignore cases where any object class can match
				return true;
			}
			else if (IGNORE_MAP.contains(attributeName)) {
				// Ignore this specific attribute
				return true;
			}
			else if (CRITERIA_MAP.get(attributeName) == null && getContactAttributeName(attributeName) == null) {
				log.debug("{}", new BundleMessage("LOG_LDAP_UNSUPPORTED_FILTER_ATTRIBUTE", attributeName, value));

				return true;
			}

			return false;
		}

		public boolean isFullSearch() {
			// only (objectclass=*) is a full search
			return "objectclass".equals(attributeName) && STAR.equals(value);
		}

		@Override
		public String toString() {
			StringBuilder buffer = new StringBuilder();
			buffer.append('(');
			buffer.append(attributeName);
			buffer.append('=');
			if (SimpleFilter.STAR.equals(value)) {
				buffer.append(SimpleFilter.STAR);
			}
			else if (operator == LDAP_FILTER_SUBSTRINGS) {
				if (mode == LDAP_SUBSTRING_FINAL || mode == LDAP_SUBSTRING_ANY) {
					buffer.append(SimpleFilter.STAR);
				}
				buffer.append(value);
				if (mode == LDAP_SUBSTRING_INITIAL || mode == LDAP_SUBSTRING_ANY) {
					buffer.append(SimpleFilter.STAR);
				}
			}
			else {
				buffer.append(value);
			}

			buffer.append(')');
			return buffer.toString();
		}

		public ExchangeSession.Condition getContactSearchFilter() {
			String contactAttributeName = getContactAttributeName(attributeName);

			if (canIgnore || (contactAttributeName == null)) {
				return null;
			}

			ExchangeSession.Condition condition = null;

			if (operator == LDAP_FILTER_EQUALITY) {
				condition = session.isEqualTo(contactAttributeName, value);
			}
			else if ("*".equals(value)) {
				condition = session.not(session.isNull(contactAttributeName));
				// do not allow substring search on integer field imapUid
			}
			else if (!"imapUid".equals(contactAttributeName)) {
				// endsWith not supported by exchange, convert to contains
				if (mode == LDAP_SUBSTRING_FINAL || mode == LDAP_SUBSTRING_ANY) {
					condition = session.contains(contactAttributeName, value);
				}
				else {
					condition = session.startsWith(contactAttributeName, value);
				}
			}
			return condition;
		}

		public boolean isMatch(Map<String, String> person) {
			if (canIgnore) {
				// Ignore this filter
				return true;
			}

			String personAttributeValue = person.get(attributeName);

			if (personAttributeValue == null) {
				// No value to allow for filter match
				return false;
			}
			else if (value == null) {
				// This is a presence filter: found
				return true;
			}
			else if ((operator == LDAP_FILTER_EQUALITY) && personAttributeValue.equalsIgnoreCase(value)) {
				// Found an exact match
				return true;
			}
			else // noinspection RedundantIfStatement
			if ((operator == LDAP_FILTER_SUBSTRINGS)
					&& (personAttributeValue.toLowerCase().contains(value.toLowerCase()))) {
				// Found a substring match
				return true;
			}

			return false;
		}

		public Map<String, ExchangeSession.Contact> findInGAL(ExchangeSession session, Set<String> returningAttributes,
				int sizeLimit) throws IOException {
			if (canIgnore) {
				return null;
			}

			String contactAttributeName = getContactAttributeName(attributeName);

			if (contactAttributeName != null) {
				// quick fix for cn=* filter
				Map<String, ExchangeSession.Contact> galPersons = session.galFind(
						session.startsWith(contactAttributeName, "*".equals(value) ? "A" : value),
						convertLdapToContactReturningAttributes(returningAttributes), sizeLimit);

				if (operator == LDAP_FILTER_EQUALITY) {
					// Make sure only exact matches are returned

					Map<String, ExchangeSession.Contact> results = new HashMap<>();

					for (ExchangeSession.Contact person : galPersons.values()) {
						if (isMatch(person)) {
							// Found an exact match
							results.put(person.get("uid"), person);
						}
					}

					return results;
				}
				else {
					return galPersons;
				}
			}

			return null;
		}

		public void add(LdapFilter filter) {
			// Should never be called
			log.debug("{}", new BundleMessage("LOG_LDAP_UNSUPPORTED_FILTER", "nested simple filters"));
		}

	}

	/**
	 * Преобразовать имя атрибута контакта в имя атрибута LDAP.
	 * @param ldapAttributeName имя атрибута LDAP
	 * @return имя атрибута контакта
	 */
	protected static String getContactAttributeName(String ldapAttributeName) {
		String contactAttributeName = null;
		// first look in contact attributes
		if (ExchangeSession.CONTACT_ATTRIBUTES.contains(ldapAttributeName)) {
			contactAttributeName = ldapAttributeName;
		}
		else if (LDAP_TO_CONTACT_ATTRIBUTE_MAP.containsKey(ldapAttributeName)) {
			String mappedAttribute = LDAP_TO_CONTACT_ATTRIBUTE_MAP.get(ldapAttributeName);
			if (mappedAttribute != null) {
				contactAttributeName = mappedAttribute;
			}
		}
		else if (!"hassubordinates".equals(ldapAttributeName)) {
			log.debug("{}", new BundleMessage("UNKNOWN_ATTRIBUTE", ldapAttributeName));
		}
		return contactAttributeName;
	}

	/**
	 * Преобразовать имя атрибута LDAP в имя атрибута контакта.
	 * @param contactAttributeName имя атрибута ldap
	 * @return имя атрибута контакта
	 */
	protected static String getLdapAttributeName(String contactAttributeName) {
		String mappedAttributeName = CONTACT_TO_LDAP_ATTRIBUTE_MAP.get(contactAttributeName);
		if (mappedAttributeName != null) {
			return mappedAttributeName;
		}
		else {
			return contactAttributeName;
		}
	}

	protected Set<String> convertLdapToContactReturningAttributes(Set<String> returningAttributes) {
		Set<String> contactReturningAttributes;
		if (returningAttributes != null && !returningAttributes.isEmpty()) {
			contactReturningAttributes = new HashSet<>();
			// always return uid
			contactReturningAttributes.add("imapUid");
			for (String attribute : returningAttributes) {
				String contactAttributeName = getContactAttributeName(attribute);
				if (contactAttributeName != null) {
					contactReturningAttributes.add(contactAttributeName);
				}
			}
		}
		else {
			contactReturningAttributes = ExchangeSession.CONTACT_ATTRIBUTES;
		}
		return contactReturningAttributes;
	}

	protected class SearchRunnable implements Runnable {

		private final int currentMessageId;

		private final String dn;

		private final int scope;

		private final int sizeLimit;

		private final int timelimit;

		private final LdapFilter ldapFilter;

		private final Set<String> returningAttributes;

		private boolean abandon;

		protected SearchRunnable(int currentMessageId, String dn, int scope, int sizeLimit, int timelimit,
				LdapFilter ldapFilter, Set<String> returningAttributes) {
			this.currentMessageId = currentMessageId;
			this.dn = dn;
			this.scope = scope;
			this.sizeLimit = sizeLimit;
			this.timelimit = timelimit;
			this.ldapFilter = ldapFilter;
			this.returningAttributes = returningAttributes;
		}

		/**
		 * Отменить поиск.
		 */
		protected void abandon() {
			abandon = true;
		}

		public void run() {
			try {
				int size = 0;
				log.debug("{}", new BundleMessage("LOG_LDAP_REQ_SEARCH", currentMessageId, dn, scope, sizeLimit,
						timelimit, ldapFilter.toString(), returningAttributes));

				if (scope == SCOPE_BASE_OBJECT) {
					if (dn != null && dn.length() == 0) {
						size = 1;
						sendRootDSE(currentMessageId);
					}
					else if (BASE_CONTEXT.equals(dn)) {
						size = 1;
						// root
						sendBaseContext(currentMessageId);
					}
					else if (dn != null && dn.startsWith("uid=") && dn.indexOf(',') > 0) {
						if (session != null) {
							// single user request
							String uid = dn.substring("uid=".length(), dn.indexOf(','));
							Map<String, ExchangeSession.Contact> persons = null;

							// first search in contact
							try {
								// check if this is a contact uid
								Integer.parseInt(uid);
								persons = contactFind(session.isEqualTo("imapUid", uid), returningAttributes,
										sizeLimit);
							}
							catch (NumberFormatException e) {
								// ignore, this is not a contact uid
							}

							// then in GAL
							if (persons == null || persons.isEmpty()) {
								persons = session.galFind(session.isEqualTo("imapUid", uid),
										convertLdapToContactReturningAttributes(returningAttributes), sizeLimit);

								ExchangeSession.Contact person = persons.get(uid.toLowerCase());
								// filter out non exact results
								if (persons.size() > 1 || person == null) {
									persons = new HashMap<>();
									if (person != null) {
										persons.put(uid.toLowerCase(), person);
									}
								}
							}
							size = persons.size();
							sendPersons(currentMessageId, dn.substring(dn.indexOf(',')), persons, returningAttributes);
						}
						else {
							log.debug("{}", new BundleMessage("LOG_LDAP_REQ_SEARCH_ANONYMOUS_ACCESS_FORBIDDEN",
									currentMessageId, dn));
						}
					}
					else {
						log.debug("{}", new BundleMessage("LOG_LDAP_REQ_SEARCH_INVALID_DN", currentMessageId, dn));
					}
				}
				else if (COMPUTER_CONTEXT.equals(dn) || COMPUTER_CONTEXT_LION.equals(dn)) {
					size = 1;
					// computer context for iCal
					sendComputerContext(currentMessageId, returningAttributes);
				}
				else if ((BASE_CONTEXT.equalsIgnoreCase(dn) || OD_USER_CONTEXT.equalsIgnoreCase(dn))
						|| OD_USER_CONTEXT_LION.equalsIgnoreCase(dn)) {
					if (session != null) {
						Map<String, ExchangeSession.Contact> persons = new HashMap<>();
						if (ldapFilter.isFullSearch()) {
							// append personal contacts first
							for (ExchangeSession.Contact person : contactFind(null, returningAttributes, sizeLimit)
								.values()) {
								persons.put(person.get("imapUid"), person);
								if (persons.size() == sizeLimit) {
									break;
								}
							}
							// full search
							for (char c = 'A'; c <= 'Z'; c++) {
								if (!abandon && persons.size() < sizeLimit) {
									for (ExchangeSession.Contact person : session
										.galFind(session.startsWith("cn", String.valueOf(c)),
												convertLdapToContactReturningAttributes(returningAttributes), sizeLimit)
										.values()) {
										persons.put(person.get("uid"), person);
										if (persons.size() == sizeLimit) {
											break;
										}
									}
								}
								if (persons.size() == sizeLimit) {
									break;
								}
							}
						}
						else {
							// append personal contacts first
							ExchangeSession.Condition filter = ldapFilter.getContactSearchFilter();

							// if ldapfilter is not a full search and filter is null,
							// ignored all attribute filters => return empty results
							if (ldapFilter.isFullSearch() || filter != null) {
								for (ExchangeSession.Contact person : contactFind(filter, returningAttributes,
										sizeLimit)
									.values()) {
									persons.put(person.get("imapUid"), person);

									if (persons.size() == sizeLimit) {
										break;
									}
								}
								if (!abandon && persons.size() < sizeLimit) {
									for (ExchangeSession.Contact person : ldapFilter
										.findInGAL(session, returningAttributes, sizeLimit - persons.size())
										.values()) {
										if (persons.size() == sizeLimit) {
											break;
										}

										persons.put(person.get("uid"), person);
									}
								}
							}
						}

						size = persons.size();
						log.debug("{}", new BundleMessage("LOG_LDAP_REQ_SEARCH_FOUND_RESULTS", currentMessageId, size));
						sendPersons(currentMessageId, ", " + dn, persons, returningAttributes);
						log.debug("{}", new BundleMessage("LOG_LDAP_REQ_SEARCH_END", currentMessageId));
					}
					else {
						log.debug("{}", new BundleMessage("LOG_LDAP_REQ_SEARCH_ANONYMOUS_ACCESS_FORBIDDEN",
								currentMessageId, dn));
					}
				}
				else if (dn != null && dn.length() > 0 && !OD_CONFIG_CONTEXT.equals(dn)
						&& !OD_GROUP_CONTEXT.equals(dn)) {
					log.debug("{}", new BundleMessage("LOG_LDAP_REQ_SEARCH_INVALID_DN", currentMessageId, dn));
				}

				// iCal: do not send LDAP_SIZE_LIMIT_EXCEEDED on apple-computer search by
				// cn with sizelimit 1
				if (size > 1 && size == sizeLimit) {
					log.debug("{}", new BundleMessage("LOG_LDAP_REQ_SEARCH_SIZE_LIMIT_EXCEEDED", currentMessageId));
					sendClient(currentMessageId, LDAP_REP_RESULT, LDAP_SIZE_LIMIT_EXCEEDED, "");
				}
				else {
					log.debug("{}", new BundleMessage("LOG_LDAP_REQ_SEARCH_SUCCESS", currentMessageId));
					sendClient(currentMessageId, LDAP_REP_RESULT, LDAP_SUCCESS, "");
				}
			}
			catch (SocketException e) {
				// client closed connection
				log.warn(BundleMessage.formatLog("LOG_CLIENT_CLOSED_CONNECTION"));
			}
			catch (IOException e) {
				log.error("", e);
				try {
					sendErr(currentMessageId, LDAP_REP_RESULT, e);
				}
				catch (IOException e2) {
					log.debug("{}", new BundleMessage("LOG_EXCEPTION_SENDING_ERROR_TO_CLIENT"), e2);
				}
			}
			finally {
				synchronized (searchThreadMap) {
					searchThreadMap.remove(currentMessageId);
				}
			}
		}

		/**
		 * Поиск пользователей в папке контактов
		 * @param condition фильтр поиска
		 * @param returningAttributes запрашиваемые атрибуты
		 * @param maxCount максимальное количество элементов
		 * @return Список пользователей
		 * @throws IOException при ошибке
		 */
		public Map<String, ExchangeSession.Contact> contactFind(ExchangeSession.Condition condition,
				Set<String> returningAttributes, int maxCount) throws IOException {
			Map<String, ExchangeSession.Contact> results = new HashMap<>();

			Set<String> contactReturningAttributes = convertLdapToContactReturningAttributes(returningAttributes);
			contactReturningAttributes.remove("apple-serviceslocator");
			List<ExchangeSession.Contact> contacts = session.searchContacts(ExchangeSession.CONTACTS,
					contactReturningAttributes, condition, maxCount);

			for (ExchangeSession.Contact contact : contacts) {
				// use imapUid as uid
				String imapUid = contact.get("imapUid");
				if (imapUid != null) {
					results.put(imapUid, contact);
				}
			}

			return results;
		}

		/**
		 * Преобразовать в атрибуты LDAP и отправить запись
		 * @param currentMessageId текущий идентификатор сообщения
		 * @param baseContext базовый контекст запроса (BASE_CONTEXT или OD_BASE_CONTEXT)
		 * @param persons карта персон
		 * @param returningAttributes возвращаемые атрибуты
		 * @throws IOException в случае ошибки
		 */
		protected void sendPersons(int currentMessageId, String baseContext,
				Map<String, ExchangeSession.Contact> persons, Set<String> returningAttributes) throws IOException {
			boolean needObjectClasses = returningAttributes.contains("objectclass") || returningAttributes.isEmpty();
			boolean returnAllAttributes = returningAttributes.isEmpty();

			for (ExchangeSession.Contact person : persons.values()) {
				if (abandon) {
					break;
				}

				Map<String, Object> ldapPerson = new HashMap<>();

				// convert Contact entries
				if (returnAllAttributes) {
					// just convert contact attributes to default ldap names
					for (Map.Entry<String, String> entry : person.entrySet()) {
						String ldapAttribute = getLdapAttributeName(entry.getKey());
						String value = entry.getValue();
						if (value != null) {
							ldapPerson.put(ldapAttribute, value);
						}
					}
				}
				else {
					// always map uid
					ldapPerson.put("uid", person.get("imapUid"));
					// iterate over requested attributes
					for (String ldapAttribute : returningAttributes) {
						String contactAttribute = getContactAttributeName(ldapAttribute);
						String value = person.get(contactAttribute);
						if (value != null) {
							if (ldapAttribute.startsWith("birth")) {
								SimpleDateFormat parser = ExchangeSession.getZuluDateFormat();
								Calendar calendar = Calendar.getInstance();
								try {
									calendar.setTime(parser.parse(value));
								}
								catch (ParseException e) {
									throw new IOException(e + " " + e.getMessage());
								}
								switch (ldapAttribute) {
									case "birthday":
										value = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
										break;
									case "birthmonth":
										value = String.valueOf(calendar.get(Calendar.MONTH) + 1);
										break;
									case "birthyear":
										value = String.valueOf(calendar.get(Calendar.YEAR));
										break;
								}
							}
							ldapPerson.put(ldapAttribute, value);
						}
						else if ("hassubordinates".equals(ldapAttribute)) {
							ldapPerson.put(ldapAttribute, "false");
						}
					}
				}

				// Process all attributes which have static mappings
				for (Map.Entry<String, String> entry : STATIC_ATTRIBUTE_MAP.entrySet()) {
					String ldapAttribute = entry.getKey();
					String value = entry.getValue();

					if (value != null && (returnAllAttributes || returningAttributes.contains(ldapAttribute))) {
						ldapPerson.put(ldapAttribute, value);
					}
				}

				if (needObjectClasses) {
					ldapPerson.put("objectClass", PERSON_OBJECT_CLASSES);
				}

				// iCal: copy email to apple-generateduid, encode @
				if (returnAllAttributes || returningAttributes.contains("apple-generateduid")) {
					String mail = (String) ldapPerson.get("mail");
					if (mail != null) {
						ldapPerson.put("apple-generateduid", mail.replaceAll("@", "__AT__"));
					}
					else {
						// failover, should not happen
						ldapPerson.put("apple-generateduid", ldapPerson.get("uid"));
					}
				}

				// iCal: replace current user alias with login name
				if (session.getAlias().equals(ldapPerson.get("uid"))) {
					if (returningAttributes.contains("uidnumber")) {
						ldapPerson.put("uidnumber", userName);
					}
				}
				log.debug("{}", new BundleMessage("LOG_LDAP_REQ_SEARCH_SEND_PERSON", currentMessageId,
						ldapPerson.get("uid"), baseContext, ldapPerson));

				try {
					sendEntry(currentMessageId, new Rdn("uid", ldapPerson.get("uid")) + baseContext, ldapPerson);
				}
				catch (InvalidNameException e) {
					throw new IOException(e);
				}
			}

		}

	}

}
