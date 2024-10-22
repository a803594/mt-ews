/*
 DIT
 */

package ru.mos.mostech.ews.ldap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Base class that defines common fields, constants, and debug method.
 *
 * @author Jagane Sundar
 */
@SuppressWarnings("unused")
public abstract class Ber {

	protected byte[] buf;

	protected int offset;

	protected int bufsize;

	protected Ber() {
	}

	public static void dumpBER(OutputStream outStream, String tag, byte[] bytes, int from, int to) {

		try {
			outStream.write('\n');
			outStream.write(tag.getBytes(StandardCharsets.UTF_8));

			new HexDumpEncoder().encodeBuffer(new ByteArrayInputStream(bytes, from, to), outStream);

			outStream.write('\n');
		}
		catch (IOException e) {
			try {
				outStream.write("Ber.dumpBER(): error encountered\n".getBytes(StandardCharsets.UTF_8));
			}
			catch (IOException e2) {
				// ignore
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////
	//
	// some ASN defines
	//
	////////////////////////////////////////////////////////////////////////////

	public static final int ASN_BOOLEAN = 0x01;

	public static final int ASN_INTEGER = 0x02;

	public static final int ASN_BIT_STRING = 0x03;

	public static final int ASN_SIMPLE_STRING = 0x04;

	public static final int ASN_OCTET_STR = 0x04;

	public static final int ASN_NULL = 0x05;

	public static final int ASN_OBJECT_ID = 0x06;

	public static final int ASN_SEQUENCE = 0x10;

	public static final int ASN_SET = 0x11;

	public static final int ASN_PRIMITIVE = 0x00;

	public static final int ASN_UNIVERSAL = 0x00;

	public static final int ASN_CONSTRUCTOR = 0x20;

	public static final int ASN_APPLICATION = 0x40;

	public static final int ASN_CONTEXT = 0x80;

	public static final int ASN_PRIVATE = 0xC0;

	public static final int ASN_ENUMERATED = 0x0a;

	final static class EncodeException extends IOException {

		private static final long serialVersionUID = -5247359637775781768L;

		EncodeException(String msg) {
			super(msg);
		}

	}

	final static class DecodeException extends IOException {

		private static final long serialVersionUID = 8735036969244425583L;

		DecodeException(String msg) {
			super(msg);
		}

	}

}
