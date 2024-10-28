/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * URI поля.
 */
public interface FieldURI {

	/**
	 * Добавить поле в буфер
	 * @param buffer текущий буфер
	 */
	void appendTo(StringBuilder buffer);

	/**
	 * Добавить обновленное значение поля в буфер
	 * @param buffer текущий буфер
	 * @param itemType тип элемента
	 * @param value значение поля
	 */
	void appendValue(StringBuilder buffer, String itemType, String value);

	/**
	 * Имя свойства в ответе EWS.
	 * @return имя свойства
	 */
	String getResponseName();

	/**
	 * Получить идентификатор поля для графических запросов
	 * @return идентификатор поля
	 */
	String getGraphId();

}
