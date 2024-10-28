/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import ru.mos.mostech.ews.util.StringUtil;

import java.io.IOException;
import java.io.Writer;

/**
 * Общий вариант элемента.
 */
public class ElementOption extends Option {

	ElementOption option;

	/**
	 * Создать элемент опции.
	 * @param name имя тега элемента
	 * @param value значение элемента
	 */
	protected ElementOption(String name, String value) {
		super(name, value);
	}

	protected ElementOption(String name, ElementOption option) {
		super(name, null);
		this.option = option;
	}

	@Override
	public void write(Writer writer) throws IOException {
		writer.write('<');
		writer.write(name);
		writer.write('>');
		if (option != null) {
			option.write(writer);
		}
		else {
			writer.write(StringUtil.xmlEncode(value));
		}
		writer.write("</");
		writer.write(name);
		writer.write('>');
	}

}
