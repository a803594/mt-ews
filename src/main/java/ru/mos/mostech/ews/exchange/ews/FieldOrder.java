/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Sort order.
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
     * Append sort order to buffer.
     *
     * @param buffer search buffer
     */
    void appendTo(StringBuilder buffer) {
        buffer.append("<t:FieldOrder Order=\"").append(order).append("\">");
        fieldURI.appendTo(buffer);
        buffer.append("</t:FieldOrder>");
    }
}
