/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Field URI.
 */

@Slf4j
public interface FieldURI {

    /**
     * Append field to buffer
     *
     * @param buffer current buffer
     */
    void appendTo(StringBuilder buffer);

    /**
     * Append updated field value to buffer
     *
     * @param buffer   current buffer
     * @param itemType item type
     * @param value    field value
     */
    void appendValue(StringBuilder buffer, String itemType, String value);

    /**
     * Property name in EWS response.
     *
     * @return property name
     */
    String getResponseName();

    /**
     * Get field id for graph requests
     * @return field id
     */
    String getGraphId();
}
