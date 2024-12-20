/*
DIT
 */
package ru.mos.mostech.ews.exchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

/**
 * Замените двойные точки на одиночные в входном потоке. Строка с одной точкой означает
 * конец потока
 */
public class DoubleDotInputStream extends PushbackInputStream {

	final int[] buffer = new int[4];

	int index = -1;

	public DoubleDotInputStream(InputStream in) {
		super(in, 4);
	}

	/**
	 * Поместить текущий байт в буфер и считать следующий байт.
	 * @return следующий байт
	 * @throws IOException в случае ошибки
	 */
	protected int readNextByte() throws IOException {
		int b = super.read();
		buffer[++index] = b;
		return b;
	}

	@Override
	public int read() throws IOException {
		int b = super.read();
		if (b == '\r') {
			// \r\n
			if (readNextByte() == '\n') {
				// \r\n.
				if (readNextByte() == '.') {
					// \r\n.\r
					if (readNextByte() == '\r') {
						// \r\n.\r\n
						if (readNextByte() == '\n') {
							// end of stream
							index = -1;
							b = -1;
						}
						// \r\n..
					}
					else if (buffer[index] == '.') {
						// replace double dot
						index--;
					}
				}
			}
			// push back characters
			if (index >= 0) {
				while (index >= 0) {
					unread(buffer[index--]);
				}
			}
		}
		return b;
	}

}
