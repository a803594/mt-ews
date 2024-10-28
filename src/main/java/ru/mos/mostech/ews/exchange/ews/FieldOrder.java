/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Порядок сортировки.
 */
public class FieldOrder {

	protected enum Order {

		Descending, Ascending

	}

	protected Order order;

	protected FieldURI fieldURI;

	public FieldOrder(FieldURI fieldURI, Order order) {
		this.fieldURI = fieldURI;
		this.order = order;
	}

	/**
	 * Добавить порядок сортировки в буфер.
	 * @param buffer буфер поиска
	 */
	void appendTo(StringBuilder buffer) {
		buffer.append("<t:FieldOrder Order=\"").append(order).append("\">");
		fieldURI.appendTo(buffer);
		buffer.append("</t:FieldOrder>");
	}

}
