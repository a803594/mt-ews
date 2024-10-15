/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * EWS Search Expression.
 */
public interface SearchExpression {
    /**
     * Append search expression to buffer.
     *
     * @param buffer search buffer
     */
    void appendTo(StringBuilder buffer);
}
