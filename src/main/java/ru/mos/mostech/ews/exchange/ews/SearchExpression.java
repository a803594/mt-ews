/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Выражение поиска EWS.
 */
public interface SearchExpression {

	/**
	 * Добавить поисковое выражение в буфер.
	 * @param buffer буфер поиска
	 */
	void appendTo(StringBuilder buffer);

}
