/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import ru.mos.mostech.ews.util.StringUtil;

/**
 * Индексированный FieldURI
 */
public class IndexedFieldURI implements FieldURI {

	protected final String fieldURI;

	protected final String fieldIndex;

	protected final String fieldItemType;

	protected final String collectionName;

	/**
	 * Создать индексированное поле uri.
	 * @param fieldURI базовое поле uri
	 * @param fieldIndex имя поля
	 * @param fieldItemType тип элемента поля
	 * @param collectionName имя коллекции
	 */
	public IndexedFieldURI(String fieldURI, String fieldIndex, String fieldItemType, String collectionName) {
		this.fieldURI = fieldURI;
		this.fieldIndex = fieldIndex;
		this.fieldItemType = fieldItemType;
		this.collectionName = collectionName;
	}

	public void appendTo(StringBuilder buffer) {
		buffer.append("<t:IndexedFieldURI FieldURI=\"").append(fieldURI);
		buffer.append("\" FieldIndex=\"").append(fieldIndex);
		buffer.append("\"/>");
	}

	public void appendValue(StringBuilder buffer, String itemType, String value) {
		if (itemType != null) {
			// append IndexedFieldURI
			appendTo(buffer);
			buffer.append("<t:").append(fieldItemType).append('>');
			buffer.append("<t:").append(collectionName).append('>');
		}
		if (value != null && value.length() > 0) {
			buffer.append("<t:Entry Key=\"").append(fieldIndex).append("\">");
			buffer.append(StringUtil.xmlEncodeAttribute(value));
			buffer.append("</t:Entry>");
		}
		if (itemType != null) {
			buffer.append("</t:").append(collectionName).append('>');
			buffer.append("</t:").append(fieldItemType).append('>');
		}
	}

	public String getResponseName() {
		return fieldIndex;
	}

	@Override
	public String getGraphId() {
		throw new UnsupportedOperationException();
	}

}
