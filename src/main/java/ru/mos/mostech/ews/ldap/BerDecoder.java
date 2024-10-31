/*
  DIT
 */

package ru.mos.mostech.ews.ldap;

import java.nio.charset.StandardCharsets;

/**
 * Декодер BER. Содержит методы для разбора буфера BER.
 *
 */
public final class BerDecoder extends Ber {

	private int origOffset; // The start point in buf to decode

	/**
	 * Создает декодер BER, который считывает байты из указанного буфера.
	 */
	public BerDecoder(byte[] buf, int offset, int bufsize) {

		this.buf = buf; // shared buffer, be careful to use this class
		this.bufsize = bufsize;
		this.origOffset = offset;

		reset();
	}

	/**
	 * Сбрасывает этот декодер, чтобы начать парсинг с начального смещения (т.е. в том же
	 * состоянии, что и после вызова конструктора).
	 */
	public void reset() {
		offset = origOffset;
	}

	/**
	 * Возвращает текущую позицию разбора. Она указывает на байт, который будет разобран
	 * следующим. Полезно для разбора последовательностей.
	 */
	public int getParsePosition() {
		return offset;
	}

	/**
	 * Парсит поле возможной переменной длины.
	 */
	public int parseLength() throws DecodeException {

		int lengthbyte = parseByte();

		if ((lengthbyte & 0x80) == 0x80) {

			lengthbyte &= 0x7f;

			if (lengthbyte == 0) {
				throw new DecodeException("Indefinite length not supported");
			}

			if (lengthbyte > 4) {
				throw new DecodeException("encoding too long");
			}

			if (bufsize - offset < lengthbyte) {
				throw new DecodeException("Insufficient data");
			}

			int retval = 0;

			for (int i = 0; i < lengthbyte; i++) {
				retval = (retval << 8) + (buf[offset++] & 0xff);
			}
			if (retval < 0) {
				throw new DecodeException("Invalid length bytes");
			}
			return retval;
		}
		else {
			return lengthbyte;
		}
	}

	/**
	 * Парсит следующую последовательность в этом BER буфере.
	 * @param rlen Массив для возвращения размера последовательности в байтах. Если null,
	 * размер не возвращается.
	 * @return Тег последовательности.
	 */
	public int parseSeq(int[] rlen) throws DecodeException {

		int seq = parseByte();
		int len = parseLength();
		if (rlen != null) {
			rlen[0] = len;
		}
		return seq;
	}

	/**
	 * Используется для пропуска байтов. Обычно применяется при попытке восстановиться от
	 * ошибки парсинга. Необходимо ли это делать публичным сейчас?
	 * @param i Количество байтов, которые нужно пропустить
	 */
	@SuppressWarnings("unused")
	void seek(int i) throws DecodeException {
		if (offset + i > bufsize || offset + i < 0) {
			throw new DecodeException("array index out of bounds");
		}
		offset += i;
	}

	/**
	 * Парсит следующий байт в этом BER буфере.
	 * @return Парсируемый байт.
	 */
	public int parseByte() throws DecodeException {
		if (bufsize - offset < 1) {
			throw new DecodeException("Insufficient data");
		}
		return buf[offset++] & 0xff;
	}

	/**
	 * Возвращает следующий байт в этом BER буфере без его потребления.
	 * @return Следующий байт.
	 */
	public int peekByte() throws DecodeException {
		if (bufsize - offset < 1) {
			throw new DecodeException("Insufficient data");
		}
		return buf[offset] & 0xff;
	}

	/**
	 * Разбирает целое число с тегом ASN_BOOLEAN из этого буфера BER.
	 * @return true, если тегированное целое число равно 0; false в противном случае.
	 */
	@SuppressWarnings("UnusedReturnValue")
	public boolean parseBoolean() throws DecodeException {
		return (parseIntWithTag(ASN_BOOLEAN) != 0x00);
	}

	/**
	 * Парсит целое число с тегом ASN_ENUMERATED из этого BER буфера.
	 * @return Тег перечисления.
	 */
	public int parseEnumeration() throws DecodeException {
		return parseIntWithTag(ASN_ENUMERATED);
	}

	/**
	 * Парсит целое число с тегом ASN_INTEGER из этого BER буфера.
	 * @return Значение целого числа.
	 */
	public int parseInt() throws DecodeException {
		return parseIntWithTag(ASN_INTEGER);
	}

	/**
	 * Парсит целое число, предшествующее тегу. <blockquote><pre>
	 * Целое число в формате BER ::= тег длина байт {байт}*
	 *</pre></blockquote>
	 */
	protected int parseIntWithTag(int tag) throws DecodeException {

		if (parseByte() != tag) {
			throw new DecodeException(
					"Encountered ASN.1 tag " + (buf[offset - 1] & 0xff) + " (expected tag " + tag + ")");
		}

		int len = parseLength();

		if (len > 4) {
			throw new DecodeException("INTEGER too long");
		}
		else if (len > bufsize - offset) {
			throw new DecodeException("Insufficient data");
		}

		byte fb = buf[offset++];
		int value;

		value = fb & 0x7F;
		for (int i = 1 /* первый байт уже прочитан */ ; i < len; i++) {
			value <<= 8;
			value |= (buf[offset++] & 0xff);
		}

		if ((fb & 0x80) == 0x80) {
			value = -value;
		}

		return value;
	}

	/**
	 * Разбирает строку.
	 */
	public String parseString(boolean decodeUTF8) throws DecodeException {
		return parseStringWithTag(ASN_SIMPLE_STRING, decodeUTF8, null);
	}

	/**
	 * Парсит строку заданного тега из этого BER буфера. <blockquote><pre>
	 *BER простая строка ::= тег длина {байт}*
	 *</pre></blockquote>
	 * @param rlen Массив для хранения относительного разобранного смещения; если null,
	 * смещение не устанавливается.
	 * @param decodeUTF8 Если true, используйте UTF-8 при декодировании строки; в
	 * противном случае используйте ISO-Latin-1 (8859_1). Используйте true для LDAPv3;
	 * false для LDAPv2.
	 * @param tag Тег, предшествующий строке.
	 * @return Не-null разобранная строка.
	 */
	public String parseStringWithTag(int tag, boolean decodeUTF8, int[] rlen) throws DecodeException {

		int st;
		int originOffset = offset;

		if ((st = parseByte()) != tag) {
			throw new DecodeException(
					"Encountered ASN.1 tag " + Integer.toString((byte) st) + " (expected tag " + tag + ")");
		}

		int len = parseLength();

		if (len > bufsize - offset) {
			throw new DecodeException("Insufficient data");
		}

		String retstr;
		if (len == 0) {
			retstr = "";
		}
		else {
			byte[] buf2 = new byte[len];

			System.arraycopy(buf, offset, buf2, 0, len);
			if (decodeUTF8) {
				retstr = new String(buf2, StandardCharsets.UTF_8);
			}
			else {
				retstr = new String(buf2, StandardCharsets.ISO_8859_1);
			}
			offset += len;
		}

		if (rlen != null) {
			rlen[0] = offset - originOffset;
		}

		return retstr;
	}

	/**
	 * Парсит октетную строку заданного типа (тег) из этого BER буфера. <blockquote><pre>
	 * Двоичные данные BER типа "тег" ::= тег длина {байт}*
	 *</pre></blockquote>
	 * @param tag Тег, который нужно найти.
	 * @param rlen Массив для возвращения относительнойParsed позиции. Если null, то
	 * относительнаяParsed позиция не возвращается.
	 * @return Не нулевой массив, содержащий октетную строку.
	 * @throws DecodeException Если следующий байт в BER буфере не {@code тег}, или если
	 * длина, указанная в BER буфере, превышает количество оставшихся байт в буфере.
	 */
	public byte[] parseOctetString(int tag, int[] rlen) throws DecodeException {

		int originOffset = offset;
		int st;
		if ((st = parseByte()) != tag) {

			throw new DecodeException("Encountered ASN.1 tag " + st + " (expected tag " + tag + ")");
		}

		int len = parseLength();

		if (len > bufsize - offset) {
			throw new DecodeException("Insufficient data");
		}

		byte[] retarr = new byte[len];
		if (len > 0) {
			System.arraycopy(buf, offset, retarr, 0, len);
			offset += len;
		}

		if (rlen != null) {
			rlen[0] = offset - originOffset;
		}

		return retarr;
	}

	/**
	 * Возвращает количество нераспарсенных байтов в этом BER-буфере.
	 */
	public int bytesLeft() {
		return bufsize - offset;
	}

}
