/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import ru.mos.mostech.ews.util.StringUtil;

/**
 * Extended MAPI property.
 */
public class ExtendedFieldURI implements FieldURI {

	@SuppressWarnings({ "UnusedDeclaration" })
	protected enum PropertyType {

		ApplicationTime, ApplicationTimeArray, Binary, BinaryArray, Boolean, CLSID, CLSIDArray, Currency, CurrencyArray,
		Double, DoubleArray, Error, Float, FloatArray, Integer, IntegerArray, Long, LongArray, Null, Object,
		ObjectArray, Short, ShortArray, SystemTime, SystemTimeArray, String, StringArray

	}

	@SuppressWarnings({ "UnusedDeclaration" })
	protected enum DistinguishedPropertySetType {

		Meeting, Appointment, Common, PublicStrings, Address, InternetHeaders, CalendarAssistant, UnifiedMessaging, Task

	}

	protected String propertyTag;

	protected DistinguishedPropertySetType distinguishedPropertySetId;

	protected String propertyName;

	protected int propertyId;

	protected final PropertyType propertyType;

	/**
	 * Create extended field uri.
	 * @param intPropertyTag property tag as int
	 * @param propertyType property type
	 */
	public ExtendedFieldURI(int intPropertyTag, PropertyType propertyType) {
		this.propertyTag = "0x" + Integer.toHexString(intPropertyTag);
		this.propertyType = propertyType;
	}

	/**
	 * Create extended field uri.
	 * @param distinguishedPropertySetId distinguished property set id
	 * @param propertyId property id
	 * @param propertyType property type
	 */
	public ExtendedFieldURI(DistinguishedPropertySetType distinguishedPropertySetId, int propertyId,
			PropertyType propertyType) {
		this.distinguishedPropertySetId = distinguishedPropertySetId;
		this.propertyId = propertyId;
		this.propertyType = propertyType;
	}

	/**
	 * Create extended field uri.
	 * @param distinguishedPropertySetId distinguished property set id
	 * @param propertyName property name
	 */
	public ExtendedFieldURI(DistinguishedPropertySetType distinguishedPropertySetId, String propertyName) {
		this.distinguishedPropertySetId = distinguishedPropertySetId;
		this.propertyName = propertyName;
		this.propertyType = PropertyType.String;
	}

	/**
	 * Create extended field uri.
	 * @param distinguishedPropertySetId distinguished property set id
	 * @param propertyName property name
	 * @param propertyType property type
	 */
	public ExtendedFieldURI(DistinguishedPropertySetType distinguishedPropertySetId, String propertyName,
			PropertyType propertyType) {
		this.distinguishedPropertySetId = distinguishedPropertySetId;
		this.propertyName = propertyName;
		this.propertyType = propertyType;
	}

	public void appendTo(StringBuilder buffer) {
		buffer.append("<t:ExtendedFieldURI");
		if (propertyTag != null) {
			buffer.append(" PropertyTag=\"").append(propertyTag).append('"');
		}
		if (distinguishedPropertySetId != null) {
			buffer.append(" DistinguishedPropertySetId=\"").append(distinguishedPropertySetId).append('"');
		}
		if (propertyName != null) {
			buffer.append(" PropertyName=\"").append(propertyName).append('"');
		}
		if (propertyId != 0) {
			buffer.append(" PropertyId=\"").append(propertyId).append('"');
		}
		if (propertyType != null) {
			buffer.append(" PropertyType=\"").append(propertyType.toString()).append('"');
		}
		buffer.append("/>");
	}

	public void appendValue(StringBuilder buffer, String itemType, String value) {
		if (itemType != null) {
			appendTo(buffer);
			buffer.append("<t:");
			buffer.append(itemType);
			buffer.append('>');
		}
		buffer.append("<t:ExtendedProperty>");
		appendTo(buffer);
		if (propertyType == PropertyType.StringArray) {
			buffer.append("<t:Values>");
			String[] values = value.split(",");
			for (final String singleValue : values) {
				buffer.append("<t:Value>");
				buffer.append(StringUtil.xmlEncode(singleValue));
				buffer.append("</t:Value>");

			}
			buffer.append("</t:Values>");
		}
		else {
			buffer.append("<t:Value>");
			if ("0x10f3".equals(propertyTag)) {
				buffer.append(StringUtil.xmlEncode(StringUtil.encodeUrlcompname(value)));
			}
			else {
				buffer.append(StringUtil.xmlEncode(value));
			}
			buffer.append("</t:Value>");
		}
		buffer.append("</t:ExtendedProperty>");
		if (itemType != null) {
			buffer.append("</t:");
			buffer.append(itemType);
			buffer.append('>');
		}
	}

	/**
	 * Field name in EWS response.
	 * @return field name in response
	 */
	public String getResponseName() {
		if (propertyTag != null) {
			return propertyTag;
		}
		else if (propertyName != null) {
			return propertyName;
		}
		else {
			return String.valueOf(propertyId);
		}
	}

	@Override
	public String getGraphId() {
		return propertyType.name() + " " + propertyTag;
	}

}
