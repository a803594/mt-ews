/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import ru.mos.mostech.ews.util.StringUtil;

import java.util.List;

/**
 * Неиндексированное поле URI
 */
public class UnindexedFieldURI implements FieldURI {

	protected final String fieldURI;

	protected final String fieldName;

	/**
	 * Создать неиндексируемое поле URI.
	 * @param fieldURI имя поля
	 */
	public UnindexedFieldURI(String fieldURI) {
		this.fieldURI = fieldURI;
		int colonIndex = fieldURI.indexOf(':');
		if (colonIndex < 0) {
			fieldName = fieldURI;
		}
		else {
			fieldName = fieldURI.substring(colonIndex + 1);
		}
	}

	public void appendTo(StringBuilder buffer) {
		buffer.append("<t:FieldURI FieldURI=\"").append(fieldURI).append("\"/>");
	}

	public void appendValue(StringBuilder buffer, String itemType, String value) {
		if (fieldURI.startsWith("message") && itemType != null) {
			itemType = "Message";
		}
		else if (fieldURI.startsWith("calendar") && itemType != null) {
			itemType = "CalendarItem";
		}
		else if (fieldURI.startsWith("task") && itemType != null) {
			itemType = "Task";
		}
		else if (fieldURI.startsWith("contacts") && itemType != null) {
			itemType = "Contact";
		}
		if (itemType != null) {
			appendTo(buffer);
			buffer.append("<t:");
			buffer.append(itemType);
			buffer.append('>');
		}
		if ("MeetingTimeZone".equals(fieldName)) {
			buffer.append("<t:MeetingTimeZone TimeZoneName=\"");
			buffer.append(StringUtil.xmlEncodeAttribute(value));
			buffer.append("\"></t:MeetingTimeZone>");
		}
		else if ("StartTimeZone".equals(fieldName)) {
			buffer.append("<t:StartTimeZone Id=\"");
			buffer.append(StringUtil.xmlEncodeAttribute(value));
			buffer.append("\"></t:StartTimeZone>");
		}
		else if ("EndTimeZone".equals(fieldName)) {
			buffer.append("<t:EndTimeZone Id=\"");
			buffer.append(StringUtil.xmlEncodeAttribute(value));
			buffer.append("\"></t:EndTimeZone>");
		}
		else {
			buffer.append("<t:");
			buffer.append(fieldName);
			buffer.append('>');
			buffer.append(StringUtil.xmlEncodeAttribute(value));
			buffer.append("</t:");
			buffer.append(fieldName);
			buffer.append('>');
		}
		if (itemType != null) {
			buffer.append("</t:");
			buffer.append(itemType);
			buffer.append('>');
		}
	}

	public void appendValues(StringBuilder buffer, String itemType, List<String> values) {
		if (fieldURI.startsWith("message") && itemType != null) {
			itemType = "Message";
		}
		else if (fieldURI.startsWith("calendar") && itemType != null) {
			itemType = "CalendarItem";
		}
		else if (fieldURI.startsWith("task") && itemType != null) {
			itemType = "Task";
		}
		else if (fieldURI.startsWith("contacts") && itemType != null) {
			itemType = "Contact";
		}
		else if (fieldURI.startsWith("distributionlist") && itemType != null) {
			itemType = "DistributionList";
		}
		if (!values.isEmpty()) {
			if (itemType != null) {
				appendTo(buffer);
				buffer.append("<t:");
				buffer.append(itemType);
				buffer.append('>');
			}
			buffer.append("<t:");
			buffer.append(fieldName);
			buffer.append('>');
			for (String value : values) {
				if ("RequiredAttendees".equals(fieldName) || "OptionalAttendees".equals(fieldName)) {
					buffer.append("<t:Attendee><t:Mailbox><t:EmailAddress>");
					buffer.append(StringUtil.xmlEncodeAttribute(value));
					buffer.append("</t:EmailAddress></t:Mailbox></t:Attendee>");
				}
				else if ("Members".equals(fieldName)) {
					if (value.toLowerCase().startsWith("mailto:")) {
						buffer.append("<t:Member><t:Mailbox><t:EmailAddress>");
						buffer.append(StringUtil.xmlEncodeAttribute(value.substring(7)));
						buffer.append("</t:EmailAddress></t:Mailbox></t:Member>");
					}
					else if (value.startsWith("urn:uuid:")) {
						buffer.append("<t:Member><t:Mailbox><t:MailboxType>PrivateDL</t:MailboxType><t:ItemId Id=\"");
						buffer.append(StringUtil.xmlEncodeAttribute(value.substring(9)));
						buffer.append("\"/></t:Mailbox></t:Member>");
					}
				}
				else {
					buffer.append(StringUtil.xmlEncodeAttribute(value));
				}
			}

			buffer.append("</t:");
			buffer.append(fieldName);
			buffer.append('>');

			if (itemType != null) {
				buffer.append("</t:");
				buffer.append(itemType);
				buffer.append('>');
			}
		}
		else if (itemType != null) {
			// append field name only to remove values
			appendTo(buffer);
		}

	}

	public String getResponseName() {
		return fieldName;
	}

	@Override
	public String getGraphId() {
		throw new UnsupportedOperationException();
	}

}
