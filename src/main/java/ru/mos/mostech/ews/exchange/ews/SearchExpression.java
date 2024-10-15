/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * EWS Search Expression.
 */

@Slf4j
public interface SearchExpression {
    /**
     * Append search expression to buffer.
     *
     * @param buffer search buffer
     */
    void appendTo(StringBuilder buffer);
}
