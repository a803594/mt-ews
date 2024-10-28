/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import java.io.IOException;
import java.io.Writer;

/**
 * Генеральный параметр.
 */
public abstract class Option {

	protected final String name;

	protected final String value;

	protected Option(String name, String value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Записать содержимое XML в писатель.
	 * @param writer писатель
	 * @throws IOException в случае ошибки
	 */
	public abstract void write(Writer writer) throws IOException;

}
