/*
DIT
 */
package ru.mos.mostech.ews.exchange;

import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.util.StringUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Объект VCalendar.
 */
@Slf4j
public class VCalendar extends VObject {

	protected VObject firstVevent;

	protected VObject vTimezone;

	protected String email;

	/**
	 * Создать объект VCalendar из ридера;
	 * @param reader потоковый ридер
	 * @param email электронная почта текущего пользователя
	 * @param vTimezone часовой пояс пользователя OWA
	 * @throws IOException в случае ошибки
	 */
	public VCalendar(BufferedReader reader, String email, VObject vTimezone) throws IOException {
		super(reader);
		if (!"VCALENDAR".equals(type)) {
			throw new IOException("Invalid type: " + type);
		}
		this.email = email;
		// set OWA timezone information
		if (this.vTimezone == null && vTimezone != null) {
			setTimezone(vTimezone);
		}
	}

	/**
	 * Создание объекта VCalendar из строки;
	 * @param vCalendarBody тело элемента
	 * @param email электронная почта текущего пользователя
	 * @param vTimezone часовой пояс пользователя OWA
	 * @throws IOException в случае ошибки
	 */
	public VCalendar(String vCalendarBody, String email, VObject vTimezone) throws IOException {
		this(new ICSBufferedReader(new StringReader(vCalendarBody)), email, vTimezone);
	}

	/**
	 * Создать объект VCalendar из строки;
	 * @param vCalendarContent содержимое элемента
	 * @param email электронная почта текущего пользователя
	 * @param vTimezone часовой пояс пользователя OWA
	 * @throws IOException при ошибке
	 */
	public VCalendar(byte[] vCalendarContent, String email, VObject vTimezone) throws IOException {
		this(new ICSBufferedReader(
				new InputStreamReader(new ByteArrayInputStream(vCalendarContent), StandardCharsets.UTF_8)), email,
				vTimezone);
	}

	/**
	 * Пустой конструктор
	 */
	public VCalendar() {
		type = "VCALENDAR";
	}

	/**
	 * Установить часовой пояс в vObject
	 * @param vTimezone объект часового пояса
	 */
	public void setTimezone(VObject vTimezone) {
		if (vObjects == null) {
			addVObject(vTimezone);
		}
		else {
			vObjects.add(0, vTimezone);
		}
		this.vTimezone = vTimezone;
	}

	@Override
	public void addVObject(VObject vObject) {
		if (firstVevent == null && ("VEVENT".equals(vObject.type) || "VTODO".equals(vObject.type))) {
			firstVevent = vObject;
		}
		if ("VTIMEZONE".equals(vObject.type)) {
			if (vTimezone == null) {
				vTimezone = vObject;
			}
			else if (vTimezone.getPropertyValue("TZID").equals(vObject.getPropertyValue("TZID"))) {
				// drop duplicate TZID definition (Korganizer bug)
				vObject = null;
			}
		}
		if (vObject != null) {
			super.addVObject(vObject);
		}
	}

	protected boolean isAllDay(VObject vObject) {
		VProperty dtstart = vObject.getProperty("DTSTART");
		return dtstart != null && dtstart.hasParam("VALUE", "DATE");
	}

	protected boolean isCdoAllDay(VObject vObject) {
		return "TRUE".equals(vObject.getPropertyValue("X-MICROSOFT-CDO-ALLDAYEVENT"));
	}

	/**
	 * Проверить, является ли vCalendar событием на весь день CDO.
	 * @return true, если у vCalendar установлен свойство X-MICROSOFT-CDO-ALLDAYEVENT в
	 * TRUE
	 */
	public boolean isCdoAllDay() {
		return firstVevent != null && isCdoAllDay(firstVevent);
	}

	/**
	 * Получить email из значения свойства.
	 * @param property свойство
	 * @return значение email
	 */
	public String getEmailValue(VProperty property) {
		if (property == null) {
			return null;
		}
		String propertyValue = property.getValue();
		if (propertyValue != null && (propertyValue.startsWith("MAILTO:") || propertyValue.startsWith("mailto:"))) {
			return propertyValue.substring(7);
		}
		else {
			return propertyValue;
		}
	}

	protected String getMethod() {
		return getPropertyValue("METHOD");
	}

	protected void fixVCalendar(boolean fromServer) {
		// set iCal 4 global X-CALENDARSERVER-ACCESS from CLASS
		if (fromServer) {
			setPropertyValue("X-CALENDARSERVER-ACCESS", getCalendarServerAccess());
		}

		if (fromServer && "PUBLISH".equals(getPropertyValue("METHOD"))) {
			removeProperty("METHOD");
		}

		// iCal 4 global X-CALENDARSERVER-ACCESS
		String calendarServerAccess = getPropertyValue("X-CALENDARSERVER-ACCESS");
		String now = ExchangeSession.getZuluDateFormat().format(new Date());

		// fix method from iPhone
		if (!fromServer && getPropertyValue("METHOD") == null) {
			setPropertyValue("METHOD", "PUBLISH");
		}

		// rename TZID for maximum iCal/iPhone compatibility
		String tzid = null;
		if (fromServer) {
			// get current tzid
			VObject vObject = vTimezone;
			if (vObject != null) {
				String currentTzid = vObject.getPropertyValue("TZID");
				// fix TZID with \n (Exchange 2010 bug)
				if (currentTzid != null && currentTzid.endsWith("\n")) {
					currentTzid = currentTzid.substring(0, currentTzid.length() - 1);
					vObject.setPropertyValue("TZID", currentTzid);
				}
				if (currentTzid != null && currentTzid.indexOf(' ') >= 0) {
					try {
						tzid = ResourceBundle.getBundle("timezones").getString(currentTzid);
						vObject.setPropertyValue("TZID", tzid);
					}
					catch (MissingResourceException e) {
						log.debug("Timezone " + currentTzid + " not found in rename table");
					}
				}
			}
		}

		if (!fromServer) {
			fixTimezoneToServer();
		}

		// iterate over vObjects
		for (VObject vObject : vObjects) {
			if ("VEVENT".equals(vObject.type)) {
				if (calendarServerAccess != null) {
					vObject.setPropertyValue("CLASS", getEventClass(calendarServerAccess));
					// iCal 3, get X-CALENDARSERVER-ACCESS from local VEVENT
				}
				else if (vObject.getPropertyValue("X-CALENDARSERVER-ACCESS") != null) {
					vObject.setPropertyValue("CLASS",
							getEventClass(vObject.getPropertyValue("X-CALENDARSERVER-ACCESS")));
				}
				if (fromServer) {
					// remove organizer line for event without attendees for iPhone
					if (vObject.getProperty("ATTENDEE") == null) {
						vObject.setPropertyValue("ORGANIZER", null);
					}
					// detect allday and update date properties
					if (isCdoAllDay(vObject)) {
						setClientAllday(vObject.getProperty("DTSTART"));
						setClientAllday(vObject.getProperty("DTEND"));
						setClientAllday(vObject.getProperty("RECURRENCE-ID"));
					}
					String cdoBusyStatus = vObject.getPropertyValue("X-MICROSOFT-CDO-BUSYSTATUS");
					if (cdoBusyStatus != null) {
						// we set status only if it's tentative
						if ("TENTATIVE".equals(cdoBusyStatus)) {
							vObject.setPropertyValue("STATUS", "TENTATIVE");
						}
						// in all cases, we set the transparency (also called "show time
						// as" in UI)
						vObject.setPropertyValue("TRANSP", !"FREE".equals(cdoBusyStatus) ? "OPAQUE" : "TRANSPARENT");
					}

					// Apple iCal doesn't understand this key, and it's entourage
					// specific (i.e. not needed by any caldav client): strip it out
					vObject.removeProperty("X-ENTOURAGE_UUID");

					splitExDate(vObject);

					// remove empty properties
					if ("".equals(vObject.getPropertyValue("LOCATION"))) {
						vObject.removeProperty("LOCATION");
					}
					if ("".equals(vObject.getPropertyValue("DESCRIPTION"))) {
						vObject.removeProperty("DESCRIPTION");
					}
					if ("".equals(vObject.getPropertyValue("CLASS"))) {
						vObject.removeProperty("CLASS");
					}
					// rename TZID
					if (tzid != null) {
						VProperty dtStart = vObject.getProperty("DTSTART");
						if (dtStart != null && dtStart.getParam("TZID") != null) {
							dtStart.setParam("TZID", tzid);
						}
						VProperty dtEnd = vObject.getProperty("DTEND");
						if (dtEnd != null && dtEnd.getParam("TZID") != null) {
							dtEnd.setParam("TZID", tzid);
						}
						VProperty recurrenceId = vObject.getProperty("RECURRENCE-ID");
						if (recurrenceId != null && recurrenceId.getParam("TZID") != null) {
							recurrenceId.setParam("TZID", tzid);
						}
						VProperty exDate = vObject.getProperty("EXDATE");
						if (exDate != null && exDate.getParam("TZID") != null) {
							exDate.setParam("TZID", tzid);
						}
					}
					// remove unsupported attachment reference
					if (vObject.getProperty("ATTACH") != null) {
						List<String> toRemoveValues = null;
						List<String> values = vObject.getProperty("ATTACH").getValues();
						for (String value : values) {
							if (value.contains("CID:")) {
								if (toRemoveValues == null) {
									toRemoveValues = new ArrayList<>();
								}
								toRemoveValues.add(value);
							}
						}
						if (toRemoveValues != null) {
							values.removeAll(toRemoveValues);
							if (values.size() == 0) {
								vObject.removeProperty("ATTACH");
							}
						}
					}
				}
				else {
					// add organizer line to all events created in Exchange for active
					// sync
					String organizer = getEmailValue(vObject.getProperty("ORGANIZER"));
					if (organizer == null) {
						vObject.setPropertyValue("ORGANIZER", "MAILTO:" + email);
					}
					else if (!email.equalsIgnoreCase(organizer)
							&& vObject.getProperty("X-MICROSOFT-CDO-REPLYTIME") == null) {
						vObject.setPropertyValue("X-MICROSOFT-CDO-REPLYTIME", now);
					}
					// set OWA allday flag
					vObject.setPropertyValue("X-MICROSOFT-CDO-ALLDAYEVENT", isAllDay(vObject) ? "TRUE" : "FALSE");
					if (vObject.getPropertyValue("TRANSP") != null) {
						vObject.setPropertyValue("X-MICROSOFT-CDO-BUSYSTATUS",
								!"TRANSPARENT".equals(vObject.getPropertyValue("TRANSP")) ? "BUSY" : "FREE");
					}

					if (isAllDay(vObject)) {
						// convert date values to outlook compatible values
						setServerAllday(vObject.getProperty("DTSTART"));
						setServerAllday(vObject.getProperty("DTEND"));
					}
					else {
						fixTzid(vObject.getProperty("DTSTART"));
						fixTzid(vObject.getProperty("DTEND"));
					}
				}

				fixAttendees(vObject, fromServer);

				fixAlarm(vObject, fromServer);
			}
		}

	}

	private void fixTimezoneToServer() {
		if (vTimezone != null && vTimezone.vObjects != null && vTimezone.vObjects.size() > 2) {
			VObject standard = null;
			VObject daylight = null;
			for (VObject vObject : vTimezone.vObjects) {
				if ("STANDARD".equals(vObject.type)) {
					if (standard == null || (vObject.getPropertyValue("DTSTART")
						.compareTo(standard.getPropertyValue("DTSTART")) > 0)) {
						standard = vObject;
					}
				}
				if ("DAYLIGHT".equals(vObject.type)) {
					if (daylight == null || (vObject.getPropertyValue("DTSTART")
						.compareTo(daylight.getPropertyValue("DTSTART")) > 0)) {
						daylight = vObject;
					}
				}
			}
			vTimezone.vObjects.clear();
			vTimezone.vObjects.add(standard);
			vTimezone.vObjects.add(daylight);
		}
		// fix 3569922: quick workaround for broken Israeli Timezone issue
		if (vTimezone != null && vTimezone.vObjects != null) {
			for (VObject vObject : vTimezone.vObjects) {
				VProperty rrule = vObject.getProperty("RRULE");
				if (rrule != null && rrule.getValues().size() == 3 && "BYDAY=-2SU".equals(rrule.getValues().get(1))) {
					rrule.getValues().set(1, "BYDAY=4SU");
				}
				// Fix 555 another broken Israeli timezone
				if (rrule != null && rrule.getValues().size() == 4 && "BYDAY=FR".equals(rrule.getValues().get(1))
						&& "BYMONTHDAY=23,24,25,26,27,28,29".equals(rrule.getValues().get(2))) {
					rrule.getValues().set(1, "BYDAY=-1FR");
					rrule.getValues().remove(2);
				}
			}
		}

		// validate RRULE - COUNT and UNTIL may not occur at once
		if (vTimezone != null && vTimezone.vObjects != null) {
			for (VObject vObject : vTimezone.vObjects) {
				VProperty rrule = vObject.getProperty("RRULE");
				if (rrule != null) {
					Map<String, String> rruleValueMap = rrule.getValuesAsMap();
					if (rruleValueMap.containsKey("UNTIL") && rruleValueMap.containsKey("COUNT")) {
						rrule.removeValue("UNTIL=" + rruleValueMap.get("UNTIL"));
					}
				}
			}
		}
		// end validate RRULE

		// convert TZID to Exchange time zone id
		ResourceBundle tzBundle = ResourceBundle.getBundle("exchtimezones");
		ResourceBundle tzidsBundle = ResourceBundle.getBundle("stdtimezones");
		for (VObject vObject : vObjects) {
			if (vObject.isVTimezone()) {
				String tzid = vObject.getPropertyValue("TZID");
				// check if tzid is avalid Exchange timezone id
				if (!tzidsBundle.containsKey(tzid)) {
					String exchangeTzid = null;
					// try to convert standard timezone id to Exchange timezone id
					if (tzBundle.containsKey(tzid)) {
						exchangeTzid = tzBundle.getString(tzid);
					}
					else {
						// failover, map to a close timezone
						for (VObject tzDefinition : vObject.vObjects) {
							if ("STANDARD".equals(tzDefinition.type)) {
								exchangeTzid = getTzidFromOffset(tzDefinition.getPropertyValue("TZOFFSETTO"));
							}
						}
					}
					if (exchangeTzid != null) {
						vObject.setPropertyValue("TZID", exchangeTzid);
						// also replace TZID in properties
						updateTzid(tzid, exchangeTzid);
					}
				}
			}
		}
	}

	protected void updateTzid(String tzid, String newTzid) {
		for (VObject vObject : vObjects) {
			if (vObject.isVEvent()) {
				for (VProperty vProperty : vObject.properties) {
					if (tzid.equalsIgnoreCase(vProperty.getParamValue("TZID"))) {
						vProperty.setParam("TZID", newTzid);
					}
				}
			}
		}
	}

	private void fixTzid(VProperty property) {
		if (property != null && !property.hasParam("TZID")) {
			property.addParam("TZID", vTimezone.getPropertyValue("TZID"));
		}
	}

	protected void splitExDate(VObject vObject) {
		List<VProperty> exDateProperties = vObject.getProperties("EXDATE");
		if (exDateProperties != null) {
			for (VProperty property : exDateProperties) {
				String value = property.getValue();
				if (value.indexOf(',') >= 0) {
					// split property
					vObject.removeProperty(property);
					for (String singleValue : value.split(",")) {
						VProperty singleProperty = new VProperty("EXDATE", singleValue);
						singleProperty.setParams(property.getParams());
						vObject.addProperty(singleProperty);
					}
				}
			}
		}
	}

	protected void setServerAllday(VProperty property) {
		if (vTimezone != null) {
			// set TZID param
			if (!property.hasParam("TZID")) {
				property.addParam("TZID", vTimezone.getPropertyValue("TZID"));
			}
			// remove VALUE
			property.removeParam("VALUE");
			String value = property.getValue();
			if (value.length() != 8) {
				log.warn("Invalid date value in allday event: " + value);
			}
			property.setValue(property.getValue() + "T000000");
		}
	}

	protected void setClientAllday(VProperty property) {
		if (property != null) {
			// set VALUE=DATE param
			if (!property.hasParam("VALUE")) {
				property.addParam("VALUE", "DATE");
			}
			// remove TZID
			property.removeParam("TZID");
			String value = property.getValue();
			if (value.length() != 8) {
				// try to convert datetime value to date value
				try {
					Calendar calendar = Calendar.getInstance();
					SimpleDateFormat dateParser = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
					calendar.setTime(dateParser.parse(value));
					calendar.add(Calendar.HOUR_OF_DAY, 12);
					SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");
					value = dateFormatter.format(calendar.getTime());
				}
				catch (ParseException e) {
					log.warn("Invalid date value in allday event: " + value);
				}
			}
			property.setValue(value);
		}
	}

	protected void fixAlarm(VObject vObject, boolean fromServer) {
		if (vObject.vObjects != null) {
			if (Settings.getBooleanProperty("mt.ews.caldavDisableReminders", false)) {
				ArrayList<VObject> vAlarms = null;
				for (VObject vAlarm : vObject.vObjects) {
					if ("VALARM".equals(vAlarm.type)) {
						if (vAlarms == null) {
							vAlarms = new ArrayList<>();
						}
						vAlarms.add(vAlarm);
					}
				}
				// remove all vAlarms
				if (vAlarms != null) {
					for (VObject vAlarm : vAlarms) {
						vObject.vObjects.remove(vAlarm);
					}
				}

			}
			else {
				for (VObject vAlarm : vObject.vObjects) {
					if ("VALARM".equals(vAlarm.type)) {
						String action = vAlarm.getPropertyValue("ACTION");
						if (fromServer && "DISPLAY".equals(action)
						// convert DISPLAY to AUDIO only if user defined an alarm sound
								&& Settings.getProperty("mt.ews.caldavAlarmSound") != null) {
							// Convert alarm to audio for iCal
							vAlarm.setPropertyValue("ACTION", "AUDIO");

							if (vAlarm.getPropertyValue("ATTACH") == null) {
								// Add defined sound into the audio alarm
								VProperty vProperty = new VProperty("ATTACH",
										Settings.getProperty("mt.ews.caldavAlarmSound"));
								vProperty.addParam("VALUE", "URI");
								vAlarm.addProperty(vProperty);
							}

						}
						else if (!fromServer && "AUDIO".equals(action)) {
							// Use the alarm action that exchange (and blackberry)
							// understand
							// (exchange and blackberry don't understand audio actions)
							vAlarm.setPropertyValue("ACTION", "DISPLAY");
						}
					}
				}
			}
		}
	}

	/**
	 * Заменить пути главного пользователя iCal4 (Snow Leopard) на выражение mailto
	 * @param value значение участника или строка ics
	 * @return исправленное значение
	 */
	protected String replaceIcal4Principal(String value) {
		if (value.contains("/principals/__uuids__/")) {
			return value.replaceAll("/principals/__uuids__/([^/]*)__AT__([^/]*)/", "mailto:$1@$2");
		}
		else {
			return value;
		}
	}

	private void fixAttendees(VObject vObject, boolean fromServer) {
		if (vObject.properties != null) {
			for (VProperty property : vObject.properties) {
				if ("ATTENDEE".equalsIgnoreCase(property.getKey())) {
					if (fromServer) {
						// If this is coming from the server, strip out RSVP for this
						// user as an attendee where the partstat is something other
						// than PARTSTAT=NEEDS-ACTION since the RSVP confuses iCal4 into
						// thinking the attendee has not replied
						if (isCurrentUser(property) && property.hasParam("RSVP", "TRUE")) {
							if (!"NEEDS-ACTION".equals(property.getParamValue("PARTSTAT"))) {
								property.removeParam("RSVP");
							}
						}
					}
					else {
						property.setValue(replaceIcal4Principal(property.getValue()));
					}
				}

			}
		}

	}

	private boolean isCurrentUser(VProperty property) {
		return property.getValue().equalsIgnoreCase("mailto:" + email);
	}

	/**
	 * Возвращает объект VTimezone
	 * @return VTimezone
	 */
	public VObject getVTimezone() {
		return vTimezone;
	}

	/**
	 * Преобразовать X-CALENDARSERVER-ACCESS в CLASS. см.
	 * http://svn.calendarserver.org/repository/calendarserver/CalendarServer/trunk/doc/Extensions/caldav-privateevents.txt
	 * @param calendarServerAccess значение X-CALENDARSERVER-ACCESS
	 * @return значение CLASS
	 */
	protected String getEventClass(String calendarServerAccess) {
		if ("PRIVATE".equalsIgnoreCase(calendarServerAccess)) {
			return "CONFIDENTIAL";
		}
		else if ("CONFIDENTIAL".equalsIgnoreCase(calendarServerAccess)
				|| "RESTRICTED".equalsIgnoreCase(calendarServerAccess)) {
			return "PRIVATE";
		}
		else {
			return null;
		}
	}

	/**
	 * Конвертировать CLASS в X-CALENDARSERVER-ACCESS. смотрите
	 * http://svn.calendarserver.org/repository/calendarserver/CalendarServer/trunk/doc/Extensions/caldav-privateevents.txt
	 * *
	 * @return значение X-CALENDARSERVER-ACCESS
	 */
	protected String getCalendarServerAccess() {
		String eventClass = getFirstVeventPropertyValue("CLASS");
		if ("PRIVATE".equalsIgnoreCase(eventClass)) {
			return "CONFIDENTIAL";
		}
		else if ("CONFIDENTIAL".equalsIgnoreCase(eventClass)) {
			return "PRIVATE";
		}
		else {
			return null;
		}
	}

	/**
	 * Получить значение свойства из первого VEVENT в VCALENDAR.
	 * @param name имя свойства
	 * @return значение свойства
	 */
	public String getFirstVeventPropertyValue(String name) {
		if (firstVevent == null) {
			return null;
		}
		else {
			return firstVevent.getPropertyValue(name);
		}
	}

	protected VProperty getFirstVeventProperty(String name) {
		if (firstVevent == null) {
			return null;
		}
		else {
			return firstVevent.getProperty(name);
		}
	}

	/**
	 * Получить свойства по имени из первого VEVENT.
	 * @param name имя свойства
	 * @return свойства
	 */
	public List<VProperty> getFirstVeventProperties(String name) {
		if (firstVevent == null) {
			return null;
		}
		else {
			return firstVevent.getProperties(name);
		}
	}

	/**
	 * Удалить VAlarm из VCalendar.
	 */
	public void removeVAlarm() {
		if (vObjects != null) {
			for (VObject vObject : vObjects) {
				if ("VEVENT".equals(vObject.type)) {
					// As VALARM is the only possible inner object, just drop all objects
					if (vObject.vObjects != null) {
						vObject.vObjects = null;
					}
				}
			}
		}
	}

	/**
	 * Проверить, имеет ли VCalendar элемент VALARM.
	 * @return true, если VCalendar имеет VALARM
	 */
	public boolean hasVAlarm() {
		if (vObjects != null) {
			for (VObject vObject : vObjects) {
				if ("VEVENT".equals(vObject.type)) {
					if (vObject.vObjects != null && !vObject.vObjects.isEmpty()) {
						return vObject.vObjects.get(0).isVAlarm();
					}
				}
			}
		}
		return false;
	}

	public String getReminderMinutesBeforeStart() {
		String result = "0";
		if (vObjects != null) {
			for (VObject vObject : vObjects) {
				if (vObject.vObjects != null && !vObject.vObjects.isEmpty() && vObject.vObjects.get(0).isVAlarm()) {
					String trigger = vObject.vObjects.get(0).getPropertyValue("TRIGGER");
					if (trigger != null) {
						if (trigger.startsWith("-PT") && trigger.endsWith("M")) {
							result = trigger.substring(3, trigger.length() - 1);
						}
						else if (trigger.startsWith("-PT") && trigger.endsWith("H")) {
							result = trigger.substring(3, trigger.length() - 1);
							// convert to minutes
							result = String.valueOf(Integer.parseInt(result) * 60);
						}
						else if (trigger.startsWith("-P") && trigger.endsWith("D")) {
							result = trigger.substring(2, trigger.length() - 1);
							// convert to minutes
							result = String.valueOf(Integer.parseInt(result) * 60 * 24);
						}
						else if (trigger.startsWith("-P") && trigger.endsWith("W")) {
							result = trigger.substring(2, trigger.length() - 1);
							// convert to minutes
							result = String.valueOf(Integer.parseInt(result) * 60 * 24 * 7);
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Проверьте, является ли этот VCalendar встречей.
	 * @return true, если у этого VCalendar есть участники
	 */
	public boolean isMeeting() {
		return getFirstVeventProperty("ATTENDEE") != null;
	}

	/**
	 * Проверьте, является ли текущий пользователь организатором встречи.
	 * @return true, если Email пользователя совпадает с Email организатора
	 */
	public boolean isMeetingOrganizer() {
		return email.equalsIgnoreCase(getEmailValue(getFirstVeventProperty("ORGANIZER")));
	}

	/**
	 * Установить значение свойства для первого VEVENT.
	 * @param propertyName имя свойства
	 * @param propertyValue значение свойства
	 */
	public void setFirstVeventPropertyValue(String propertyName, String propertyValue) {
		firstVevent.setPropertyValue(propertyName, propertyValue);
	}

	/**
	 * Добавить свойство в первый VEVENT.
	 * @param vProperty объект свойства
	 */
	public void addFirstVeventProperty(VProperty vProperty) {
		firstVevent.addProperty(vProperty);
	}

	/**
	 * Проверьте, является ли этот элемент элементом VTODO
	 * @return true для элементов VTODO
	 */
	public boolean isTodo() {
		return firstVevent != null && "VTODO".equals(firstVevent.type);
	}

	/**
	 * VCalendar получатели уведомлений
	 */
	public static class Recipients {

		/**
		 * список участников
		 */
		public String attendees;

		/**
		 * список опциональных участников
		 */
		public String optionalAttendees;

		/**
		 * организатор vCalendar
		 */
		public String organizer;

	}

	/**
	 * Построить значение получателей для VCalendar.
	 * @param isNotification если true, отфильтровать получателей, которые должны получать
	 * уведомления о встречах
	 * @return получателей уведомлений/мероприятий
	 */
	public Recipients getRecipients(boolean isNotification) {

		HashSet<String> attendees = new HashSet<>();
		HashSet<String> optionalAttendees = new HashSet<>();

		// get recipients from first VEVENT
		List<VProperty> attendeeProperties = getFirstVeventProperties("ATTENDEE");
		if (attendeeProperties != null) {
			for (VProperty property : attendeeProperties) {
				// exclude current user and invalid values from recipients
				// also exclude no action attendees
				String attendeeEmail = getEmailValue(property);
				if (!email.equalsIgnoreCase(attendeeEmail) && attendeeEmail != null && attendeeEmail.indexOf('@') >= 0
				// return all attendees for user calendar folder, filter for notifications
						&& (!isNotification
								// notify attendee if reply explicitly requested
								|| (property.hasParam("RSVP", "TRUE")) || (
								// workaround for iCal bug: do not notify if reply
								// explicitly not requested
								!(property.hasParam("RSVP", "FALSE")) && ((property.hasParam("PARTSTAT", "NEEDS-ACTION")
										// need to include other PARTSTATs participants
										// for CANCEL notifications
										|| property.hasParam("PARTSTAT", "ACCEPTED")
										|| property.hasParam("PARTSTAT", "DECLINED")
										|| property.hasParam("PARTSTAT", "TENTATIVE")))))) {
					if (property.hasParam("ROLE", "OPT-PARTICIPANT")) {
						optionalAttendees.add(attendeeEmail);
					}
					else {
						attendees.add(attendeeEmail);
					}
				}
			}
		}
		Recipients recipients = new Recipients();
		recipients.organizer = getEmailValue(getFirstVeventProperty("ORGANIZER"));
		recipients.attendees = StringUtil.join(attendees, ", ");
		recipients.optionalAttendees = StringUtil.join(optionalAttendees, ", ");
		return recipients;
	}

	public String getAttendeeStatus() {
		String status = null;
		List<VProperty> attendeeProperties = getFirstVeventProperties("ATTENDEE");
		if (attendeeProperties != null) {
			for (VProperty property : attendeeProperties) {
				String attendeeEmail = getEmailValue(property);
				if (email.equalsIgnoreCase(attendeeEmail) && property.hasParam("PARTSTAT")) {
					// found current user attendee line
					status = property.getParamValue("PARTSTAT");
					break;
				}
			}
		}
		return status;
	}

	/**
	 * Получить первый VEvent
	 * @return первый VEvent
	 */
	public VObject getFirstVevent() {
		return firstVevent;
	}

	/**
	 * Получить исключения повторяющихся событий VCalendar.
	 * @return события повторений
	 */
	public List<VObject> getModifiedOccurrences() {
		boolean first = true;
		ArrayList<VObject> results = new ArrayList<>();
		for (VObject vObject : vObjects) {
			if ("VEVENT".equals(vObject.type)) {
				if (first) {
					first = false;
				}
				else {
					results.add(vObject);
				}
			}
		}
		return results;
	}

	public TimeZone getStandardTimezoneId(String tzid) {
		String convertedTzid;
		// convert Exchange TZID to standard timezone
		try {
			convertedTzid = ResourceBundle.getBundle("timezones").getString(tzid);
		}
		catch (MissingResourceException e) {
			convertedTzid = tzid;
			// failover: detect timezone from offset
			VObject vTimezone = getVTimezone();
			for (VObject tzDefinition : vTimezone.vObjects) {
				if ("STANDARD".equals(tzDefinition.type)) {
					convertedTzid = getTzidFromOffset(tzDefinition.getPropertyValue("TZOFFSETTO"));
				}
			}
			convertedTzid = ResourceBundle.getBundle("timezones").getString(convertedTzid);
		}
		return TimeZone.getTimeZone(convertedTzid);

	}

	private String getTzidFromOffset(String tzOffset) {
		if (tzOffset == null) {
			return null;
		}
		else if (tzOffset.length() == 7) {
			tzOffset = tzOffset.substring(0, 5);
		}
		return ResourceBundle.getBundle("tzoffsettimezones").getString(tzOffset);
	}

	public String convertCalendarDateToExchangeZulu(String vcalendarDateValue, String tzid) throws IOException {
		String zuluDateValue = null;
		TimeZone timeZone;
		if (tzid == null) {
			timeZone = ExchangeSession.GMT_TIMEZONE;
		}
		else {
			timeZone = getStandardTimezoneId(tzid);
		}
		if (vcalendarDateValue != null) {
			try {
				SimpleDateFormat dateParser;
				if (vcalendarDateValue.length() == 8) {
					dateParser = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
				}
				else {
					dateParser = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ENGLISH);
				}
				dateParser.setTimeZone(timeZone);
				SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
				dateFormatter.setTimeZone(ExchangeSession.GMT_TIMEZONE);
				zuluDateValue = dateFormatter.format(dateParser.parse(vcalendarDateValue));
			}
			catch (ParseException e) {
				throw new IOException("Invalid date " + vcalendarDateValue + " with tzid " + tzid);
			}
		}
		return zuluDateValue;
	}

}
