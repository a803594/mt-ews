/*
DIT
 */
package ru.mos.mostech.ews.exchange;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * RFC 1939: 3 Основные операции [...] Если любая строка начинается с октета завершения,
 * то строка "байт-упакована", добавляя октет завершения перед этой строкой ответа.
 */
public class DoubleDotOutputStream extends FilterOutputStream {

	// remember last 2 bytes written
	final int[] buf = { 0, 0 };

	public DoubleDotOutputStream(OutputStream out) {
		super(out);
	}

	@Override
	public void write(int b) throws IOException {
		if (b == '.' && (buf[0] == '\r' || buf[0] == '\n' || buf[0] == 0)) {
			// line starts with '.', prepend it with an additional '.'
			out.write('.');
		}
		out.write(b);

		buf[1] = buf[0];
		buf[0] = b;
	}

	/**
	 * RFC 1939: 3 Основные операции [...] Таким образом, многострочный ответ завершается
	 * пятью октетами "CRLF.CRLF"
	 * <p/>
	 * Не закрывайте фактический выходной поток
	 * @throws IOException в случае ошибки
	 */
	@Override
	public void close() throws IOException {
		if (buf[1] != '\r' || buf[0] != '\n') {
			out.write('\r');
			out.write('\n');
		}
		out.write('.');
		out.write('\r');
		out.write('\n');
	}

}
