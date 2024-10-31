/*
 DIT
 */

package ru.mos.mostech.ews.ldap;

import java.nio.charset.StandardCharsets;

/**
 * Кодировщик BER.
 */
public final class BerEncoder extends Ber {

	private int curSeqIndex;

	private int[] seqOffset;

	private static final int INITIAL_SEQUENCES = 16;

	private static final int DEFAULT_BUFSIZE = 1024;

	// When buf is full, expand its size by the following factor.
	private static final int BUF_GROWTH_FACTOR = 8;

	/**
	 * Создает BER-буфер для кодирования.
	 */
	public BerEncoder() {
		this(DEFAULT_BUFSIZE);
	}

	/**
	 * Создает BER-буфер заданного размера для кодирования. Укажите начальный размер
	 * буфера. Буфер будет расширен по мере необходимости.
	 * @param bufsize Количество байт для буфера.
	 */
	public BerEncoder(int bufsize) {
		buf = new byte[bufsize];
		this.bufsize = bufsize;
		offset = 0;

		seqOffset = new int[INITIAL_SEQUENCES];
		curSeqIndex = 0;
	}

	/**
	 * Сбрасывает кодировщик в состояние, когда он был вновь сконструирован. Обнуляет
	 * внутренние структуры данных.
	 */
	public void reset() {
		while (offset > 0) {
			buf[--offset] = 0;
		}
		while (curSeqIndex > 0) {
			seqOffset[--curSeqIndex] = 0;
		}
	}

	// ------------------ Accessor methods ------------

	/**
	 * Получает количество закодированных байт в этом BER буфере.
	 */
	public int getDataLen() {
		return offset;
	}

	/**
	 * Получает буфер, содержащий кодировку BER. Выбрасывает исключение, если встречены
	 * несоответствующие пары beginSeq() и endSeq(). Весь буфер не содержит закодированные
	 * байты. Используйте getDataLen(), чтобы определить количество закодированных байтов.
	 * Используйте getBuffer(true), чтобы избавиться от лишних байтов в массиве.
	 * @throws IllegalStateException Если буфер содержит несбалансированную
	 * последовательность.
	 */
	public byte[] getBuf() {
		if (curSeqIndex != 0) {
			throw new IllegalStateException("BER encode error: Unbalanced SEQUENCEs.");
		}
		return buf; // shared buffer, be careful to use this method.
	}

	/**
	 * Получает буфер, содержащий кодирование BER, обрезая неиспользуемые байты.
	 * @throws IllegalStateException Если буфер содержит несбалансированную
	 * последовательность.
	 */
	@SuppressWarnings("unused")
	public byte[] getTrimmedBuf() {
		int len = getDataLen();
		byte[] trimBuf = new byte[len];

		System.arraycopy(getBuf(), 0, trimBuf, 0, len);
		return trimBuf;
	}

	// -------------- encoding methods -------------

	/**
	 * Начало кодирования последовательности с тегом.
	 */
	public void beginSeq(int tag) {

		// Double the size of the SEQUENCE array if it overflows
		if (curSeqIndex >= seqOffset.length) {
			int[] seqOffsetTmp = new int[seqOffset.length * 2];

			System.arraycopy(seqOffset, 0, seqOffsetTmp, 0, seqOffset.length);
			seqOffset = seqOffsetTmp;
		}

		encodeByte(tag);
		seqOffset[curSeqIndex] = offset;

		// Save space for sequence length.
		// %%% Currently we save enough space for sequences up to 64k.
		// For larger sequences we'll need to shift the data to the right
		// in endSeq(). If we could instead pad the length field with
		// zeros, it would be a big win.
		ensureFreeBytes(3);
		offset += 3;

		curSeqIndex++;
	}

	/**
	 * Завершить последовательность BER.
	 */
	public void endSeq() throws EncodeException {
		curSeqIndex--;
		if (curSeqIndex < 0) {
			throw new IllegalStateException("BER encode error: Unbalanced SEQUENCEs.");
		}

		int start = seqOffset[curSeqIndex] + 3; // index beyond length field
		int len = offset - start;

		if (len <= 0x7f) {
			shiftSeqData(start, len, -2);
			buf[seqOffset[curSeqIndex]] = (byte) len;
		}
		else if (len <= 0xff) {
			shiftSeqData(start, len, -1);
			buf[seqOffset[curSeqIndex]] = (byte) 0x81;
			buf[seqOffset[curSeqIndex] + 1] = (byte) len;
		}
		else if (len <= 0xffff) {
			buf[seqOffset[curSeqIndex]] = (byte) 0x82;
			buf[seqOffset[curSeqIndex] + 1] = (byte) (len >> 8);
			buf[seqOffset[curSeqIndex] + 2] = (byte) len;
		}
		else if (len <= 0xffffff) {
			shiftSeqData(start, len, 1);
			buf[seqOffset[curSeqIndex]] = (byte) 0x83;
			buf[seqOffset[curSeqIndex] + 1] = (byte) (len >> 16);
			buf[seqOffset[curSeqIndex] + 2] = (byte) (len >> 8);
			buf[seqOffset[curSeqIndex] + 3] = (byte) len;
		}
		else {
			throw new EncodeException("SEQUENCE too long");
		}
	}

	/**
	 * Сдвигает содержимое buf в диапазоне [start,start+len) на заданное значение.
	 * Положительное значение сдвига означает смещение вправо.
	 */
	private void shiftSeqData(int start, int len, int shift) {
		if (shift > 0) {
			ensureFreeBytes(shift);
		}
		System.arraycopy(buf, start, buf, start + shift, len);
		offset += shift;
	}

	/**
	 * Закодировать один байт.
	 */
	public void encodeByte(int b) {
		ensureFreeBytes(1);
		buf[offset++] = (byte) b;
	}

	/*
	 * private void deleteByte() { offset--; } Удалить байт
	 */

	/*
	 * Кодирует целое число. <blockquote><pre> BER целое число ::= 0x02 berlength байт
	 * {байт}* </pre></blockquote>
	 */
	public void encodeInt(int i) {
		encodeInt(i, 0x02);
	}

	/**
	 * Кодирует целое число и тег. <blockquote><pre>
	 * Целое число BER с тегом ::= тег длина ber байт {байт}*
	 *</pre></blockquote>
	 */
	public void encodeInt(int i, int tag) {
		int mask = 0xff800000;
		int intsize = 4;

		while ((((i & mask) == 0) || ((i & mask) == mask)) && (intsize > 1)) {
			intsize--;
			i <<= 8;
		}

		encodeInt(i, tag, intsize);
	}

	//
	// encodes an int using numbytes for the actual encoding.
	//
	private void encodeInt(int i, int tag, int intsize) {
		if (intsize > 4) {
			throw new IllegalArgumentException("BER encode error: INTEGER too long.");
		}

		ensureFreeBytes(2 + intsize);

		buf[offset++] = (byte) tag;
		buf[offset++] = (byte) intsize;

		int mask = 0xff000000;

		while (intsize-- > 0) {
			buf[offset++] = (byte) ((i & mask) >> 24);
			i <<= 8;
		}
	}

	/**
	 * Кодирует булевое значение. <blockquote><pre>
	 * BER булевое значение ::= 0x01 0x01 {0xff|0x00}
	 *</pre></blockquote>
	 */
	@SuppressWarnings("unused")
	public void encodeBoolean(boolean b) {
		encodeBoolean(b, ASN_BOOLEAN);
	}

	/**
	 * Кодирует булево значение и тег <blockquote><pre>
	 * BER булево w ТЕГ ::= тег 0x01 {0xff|0x00}
	 *</pre></blockquote>
	 */
	public void encodeBoolean(boolean b, int tag) {
		ensureFreeBytes(3);

		buf[offset++] = (byte) tag;
		buf[offset++] = 0x01;
		buf[offset++] = b ? (byte) 0xff : (byte) 0x00;
	}

	/**
	 * Кодирует строку. <blockquote><pre>
	 * BER строка ::= 0x04 длина_строки байт1 байт2...
	 *</pre></blockquote> Строка переводится в байты, используя UTF-8 или ISO-Latin-1.
	 */
	public void encodeString(String str, boolean encodeUTF8) throws EncodeException {
		encodeString(str, ASN_OCTET_STR, encodeUTF8);
	}

	/**
	 * Кодирует строку и тег. <blockquote><pre>
	 * Строка BER с ТЕГОМ ::= тег длина_строки байт1 байт2...
	 *</pre></blockquote>
	 */
	public void encodeString(String str, int tag, boolean encodeUTF8) throws EncodeException {

		encodeByte(tag);

		int i = 0;
		int count;
		byte[] bytes = null;

		if (str == null) {
			count = 0;
		}
		else if (encodeUTF8) {
			bytes = str.getBytes(StandardCharsets.UTF_8);
			count = bytes.length;
		}
		else {
			bytes = str.getBytes(StandardCharsets.ISO_8859_1);
			count = bytes.length;
		}

		encodeLength(count);

		ensureFreeBytes(count);
		while (i < count) {
			buf[offset++] = bytes[i++];
		}
	}

	/**
	 * Кодирует часть октетной строки и тег.
	 */
	public void encodeOctetString(byte[] tb, int tag, int tboffset, int length) throws EncodeException {

		encodeByte(tag);
		encodeLength(length);

		if (length > 0) {
			ensureFreeBytes(length);
			System.arraycopy(tb, tboffset, buf, offset, length);
			offset += length;
		}
	}

	/**
	 * Кодирует строку октетов и тег.
	 */
	public void encodeOctetString(byte[] tb, int tag) throws EncodeException {
		encodeOctetString(tb, tag, 0, tb.length);
	}

	private void encodeLength(int len) throws EncodeException {
		ensureFreeBytes(4); // worst case

		if (len < 128) {
			buf[offset++] = (byte) len;
		}
		else if (len <= 0xff) {
			buf[offset++] = (byte) 0x81;
			buf[offset++] = (byte) len;
		}
		else if (len <= 0xffff) {
			buf[offset++] = (byte) 0x82;
			buf[offset++] = (byte) (len >> 8);
			buf[offset++] = (byte) (len & 0xff);
		}
		else if (len <= 0xffffff) {
			buf[offset++] = (byte) 0x83;
			buf[offset++] = (byte) (len >> 16);
			buf[offset++] = (byte) (len >> 8);
			buf[offset++] = (byte) (len & 0xff);
		}
		else {
			throw new EncodeException("string too long");
		}
	}

	/**
	 * Кодирует массив строк.
	 */
	@SuppressWarnings("unused")
	public void encodeStringArray(String[] strs, boolean encodeUTF8) throws EncodeException {
		if (strs == null)
			return;
		for (String str : strs) {
			encodeString(str, encodeUTF8);
		}
	}

	/**
	 * Обеспечивает наличие как минимум "len" неиспользуемых байтов в "buf". Если
	 * требуется больше места, "buf" увеличивается в размере на коэффициент
	 * BUF_GROWTH_FACTOR, после чего добавляется "len" байт, если "buf" всё еще не
	 * достаточно велик.
	 */
	private void ensureFreeBytes(int len) {
		if (bufsize - offset < len) {
			int newsize = bufsize * BUF_GROWTH_FACTOR;
			if (newsize - offset < len) {
				newsize += len;
			}
			byte[] newbuf = new byte[newsize];
			// Only copy bytes in the range [0, offset)
			System.arraycopy(buf, 0, newbuf, 0, offset);

			buf = newbuf;
			bufsize = newsize;
		}
	}

}
