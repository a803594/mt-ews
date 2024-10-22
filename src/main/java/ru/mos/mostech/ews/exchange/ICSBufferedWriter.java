/*
DIT
 */
package ru.mos.mostech.ews.exchange;

/**
 * ICS String writer. split lines longer than 75 characters
 */
public class ICSBufferedWriter {

	final StringBuilder buffer = new StringBuilder();

	/**
	 * Write content to buffer, do not split lines.
	 * @param content ics content
	 */
	public void write(String content) {
		if (content != null) {
			buffer.append(content);
		}
	}

	/**
	 * Write line to buffer, split lines at 75 characters.
	 * @param line ics event line
	 */
	public void writeLine(String line) {
		writeLine(line, false);
	}

	/**
	 * Write line with or without continuation prefix.
	 * @param line line content
	 * @param prefix continuation flag
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
	 * Append CRLF.
	 */
	public void newLine() {
		buffer.append((char) 13).append((char) 10);
	}

	/**
	 * Get buffer as String
	 * @return ICS content as String
	 */
	@Override
	public String toString() {
		return buffer.toString();
	}

	/**
	 * Append single value property
	 * @param propertyName property name
	 * @param propertyValue property value
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
	 * Append and encode \n to \\n in value.
	 * @param buffer line buffer
	 * @param value value
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
