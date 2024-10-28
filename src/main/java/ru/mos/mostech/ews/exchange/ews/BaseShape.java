/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Основная форма элемента или папки.
 */
@SuppressWarnings({ "UnusedDeclaration" })
public final class BaseShape extends ElementOption {

	private BaseShape(String value) {
		super("t:BaseShape", value);
	}

	/**
	 * Возврат только id.
	 */
	public static final BaseShape ID_ONLY = new BaseShape("IdOnly");

	/**
	 * Возврат свойств по умолчанию.
	 */
	public static final BaseShape DEFAULT = new BaseShape("Default");

	/**
	 * Вернуть все свойства, за исключением расширенных свойств MAPI.
	 */
	public static final BaseShape ALL_PROPERTIES = new BaseShape("AllProperties");

}