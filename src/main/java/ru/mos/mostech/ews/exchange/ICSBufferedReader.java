/*
DIT
 */
package ru.mos.mostech.ews.exchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Буферизированный читатель ICS. Читает события построчно, обрабатывает несколько строк
 * элементов
 */
public class ICSBufferedReader extends BufferedReader {

	protected String nextLine;

	protected final StringBuilder currentLine = new StringBuilder(75);

	/**
	 * Создает ICS-ридер на основеProvided reader
	 * @param in входной ридер
	 * @throws IOException в случае ошибки
	 */
	public ICSBufferedReader(Reader in) throws IOException {
		super(in);
		nextLine = super.readLine();
	}

	/**
	 * Прочитать строку из входного считывателя, распаковать длинные строки.
	 */
	@Override
	public String readLine() throws IOException {
		if (nextLine == null) {
			return null;
		}
		else {
			currentLine.setLength(0);
			currentLine.append(nextLine);
			nextLine = super.readLine();
			while (nextLine != null && !(nextLine.length() == 0)
					&& (nextLine.charAt(0) == ' ' || nextLine.charAt(0) == '\t'
					// workaround for broken items with \n as first line character
							|| nextLine.charAt(0) == '\\'
							// workaround for Exchange 2010 bug
							|| nextLine.charAt(0) == ':')) {
				// Timezone ends with \n => next line starts with :
				if (nextLine.charAt(0) == ':') {
					currentLine.append(nextLine);
				}
				else {
					currentLine.append(nextLine.substring(1));
				}
				nextLine = super.readLine();
			}
			return currentLine.toString();
		}
	}

}
