/*
DIT
 */
package ru.mos.mostech.ews.exchange;

/**
 * Запись VCard
 */
public class VCardWriter extends ICSBufferedWriter {

	/**
	 * Начало VCard и версия
	 */
	public void startCard() {
		startCard(null);
	}

	/**
	 * Начало VCard и версия
	 */
	public void startCard(String version) {
		writeLine("BEGIN:VCARD");
		writeLine("VERSION:" + ((version == null) ? "4.0" : version));
	}

	/**
	 * Добавить составное значение
	 * @param propertyName имя свойства
	 * @param propertyValue значения свойства
	 */
	public void appendProperty(String propertyName, String... propertyValue) {
		boolean hasValue = false;
		for (String value : propertyValue) {
			if ((value != null) && (value.length() > 0)) {
				hasValue = true;
				break;
			}
		}
		if (hasValue) {
			boolean first = true;
			StringBuilder lineBuffer = new StringBuilder();
			lineBuffer.append(propertyName);
			lineBuffer.append(':');
			for (String value : propertyValue) {
				if (first) {
					first = false;
				}
				else {
					lineBuffer.append(';');
				}
				appendEncodedValue(lineBuffer, value);
			}
			writeLine(lineBuffer.toString());
		}
	}

	/**
	 * Кодирует и добавляет значение в буфер
	 * @param buffer текущий буфер
	 * @param value значение свойства
	 */
	private void appendEncodedValue(StringBuilder buffer, String value) {
		if (value != null) {
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				if (c == ',' || c == ';') {
					buffer.append('\\');
				}
				if (c == '\n') {
					buffer.append("\\n");
				}
				else if (c != '\r') {
					buffer.append(value.charAt(i));
				}
			}
		}
	}

	/**
	 * Конец VCard
	 */
	public void endCard() {
		writeLine("END:VCARD");
	}

}
