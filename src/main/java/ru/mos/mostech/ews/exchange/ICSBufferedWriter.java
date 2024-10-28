/*
DIT
 */
package ru.mos.mostech.ews.exchange;

/**
 * Писатель строк ICS. Делит строки длиннее 75 символов
 */
public class ICSBufferedWriter {

	final StringBuilder buffer = new StringBuilder();

	/**
	 * Записать содержимое в буфер, не разбивая строки.
	 * @param content содержимое ics
	 */
	public void write(String content) {
		if (content != null) {
			buffer.append(content);
		}
	}

	/**
	 * Записать строку в буфер, разбивая строки на 75 символов.
	 * @param line строка события ics
	 */
	public void writeLine(String line) {
		writeLine(line, false);
	}

	/**
	 * Записать строку с или без префикса продолжения.
	 * @param line содержимое строки
	 * @param prefix флаг продолжения
	 */
	public void writeLine(String line, boolean prefix) {
		int maxLength = 77;
		if (prefix) {
			maxLength--;
			buffer.append(' ');
		}
		if (line.length() > maxLength) {
			buffer.append(line, 0, maxLength);
			newLine();
			writeLine(line.substring(maxLength), true);
		}
		else {
			buffer.append(line);
			newLine();
		}
	}

	/**
	 * Добавить CRLF.
	 */
	public void newLine() {
		buffer.append((char) 13).append((char) 10);
	}

	/**
	 * Получить буфер как строку
	 * @return Содержимое ICS как строка
	 */
	@Override
	public String toString() {
		return buffer.toString();
	}

	/**
	 * Добавить свойство с одним значением
	 * @param propertyName имя свойства
	 * @param propertyValue значение свойства
	 */
	public void appendProperty(String propertyName, String propertyValue) {
		if ((propertyValue != null) && (propertyValue.length() > 0)) {
			StringBuilder lineBuffer = new StringBuilder();
			lineBuffer.append(propertyName);
			lineBuffer.append(':');
			appendMultilineEncodedValue(lineBuffer, propertyValue);
			writeLine(lineBuffer.toString());
		}

	}

	/**
	 * Добавить и закодировать \n в \\n в значении.
	 * @param buffer буфер строки
	 * @param value значение
	 */
	protected void appendMultilineEncodedValue(StringBuilder buffer, String value) {
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c == '\n') {
				buffer.append("\\n");
				// skip carriage return
			}
			else if (c != '\r') {
				buffer.append(value.charAt(i));
			}
		}
	}

}
