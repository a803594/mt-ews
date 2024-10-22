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
 * Mime OutputStreamWriter to build in memory Mime message.
 */
public class MimeOutputStreamWriter extends OutputStreamWriter {

	/**
	 * Build MIME outputStreamWriter
	 * @param out outputstream
	 * @throws UnsupportedEncodingException on error
	 */
	public MimeOutputStreamWriter(OutputStream out) throws UnsupportedEncodingException {
		super(out, "ASCII");
	}

	/**
	 * Write MIME header
	 * @param header header name
	 * @param value header value
	 * @throws IOException on error
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
	 * Write MIME header
	 * @param header header name
	 * @param value header value
	 * @throws IOException on error
	 */
	public void writeHeader(String header, Date value) throws IOException {
		SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss Z", Locale.ENGLISH);
		writeHeader(header, formatter.format(value));
	}

	/**
	 * Write line.
	 * @param line line content
	 * @throws IOException on error
	 */
	public void writeLn(String line) throws IOException {
		write(line);
		write("\r\n");
	}

	/**
	 * Write CRLF.
	 * @throws IOException on error
	 */
	public void writeLn() throws IOException {
		write("\r\n");
	}

}
