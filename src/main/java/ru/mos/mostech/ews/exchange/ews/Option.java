/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import java.io.IOException;
import java.io.Writer;

/**
 * Generic option.
 */
public abstract class Option {

	protected final String name;

	protected final String value;

	protected Option(String name, String value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Write XML content to writer.
	 * @param writer writer
	 * @throws IOException on error
	 */
	public abstract void write(Writer writer) throws IOException;

}
