/*
DIT
 */
package ru.mos.mostech.ews.exchange;

import javax.mail.internet.MimeUtility;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Mime OutputStreamWriter для создания Mime-сообщения в памяти.
 */
public class MimeOutputStreamWriter extends OutputStreamWriter {

	/**
	 * Создать MIME outputStreamWriter
	 * @param out выходной поток
	 * @throws UnsupportedEncodingException в случае ошибки
	 */
	public MimeOutputStreamWriter(OutputStream out) throws UnsupportedEncodingException {
		super(out, "ASCII");
	}

	/**
	 * Записать MIME заголовок
	 * @param header имя заголовка
	 * @param value значение заголовка
	 * @throws IOException при ошибке
	 */
	public void writeHeader(String header, String value) throws IOException {
		// do not write empty headers
		if (value != null && value.length() > 0) {
			write(header);
			write(": ");
			write(MimeUtility.encodeText(value, "UTF-8", null));
			writeLn();
		}
	}

	/**
	 * Записать MIME заголовок
	 * @param header имя заголовка
	 * @param value значение заголовка
	 * @throws IOException при ошибке
	 */
	public void writeHeader(String header, Date value) throws IOException {
		SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss Z", Locale.ENGLISH);
		writeHeader(header, formatter.format(value));
	}

	/**
	 * Записать строку.
	 * @param line содержимое строки
	 * @throws IOException в случае ошибки
	 */
	public void writeLn(String line) throws IOException {
		write(line);
		write("\r\n");
	}

	/**
	 * Запись CRLF.
	 * @throws IOException в случае ошибки
	 */
	public void writeLn() throws IOException {
		write("\r\n");
	}

}
