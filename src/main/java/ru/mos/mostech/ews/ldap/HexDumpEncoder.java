/*
 DIT
 */

package ru.mos.mostech.ews.ldap;

import java.io.*;
import java.nio.ByteBuffer;

/**
 * Этот класс кодирует буфер в классический формат: "Шестнадцатеричный дамп" прошлого. Это
 * полезно для анализа содержимого бинарных буферов. Получаемый формат таков: <pre>
 * xxxx: 00 11 22 33 44 55 66 77   88 99 aa bb cc dd ee ff ................
 * </pre> Где xxxx - это смещение в буфере вChunks по 16 байт, за которым следуют
 * кодированные шестнадцатеричные байты ASCII, за которыми следует ASCII-представление
 * байтов или '.' , если они не являются допустимыми байтами.
 *
 * @author Chuck McManis
 */
public class HexDumpEncoder {

	private int offset;

	private int thisLineLength;

	private int currentByte;

	private byte[] thisLine = new byte[16];

	static void hexDigit(PrintStream p, byte x) {
		char c;

		c = (char) ((x >> 4) & 0xf);
		if (c > 9)
			c = (char) ((c - 10) + 'A');
		else
			c = (char) (c + '0');
		p.write(c);
		c = (char) (x & 0xf);
		if (c > 9)
			c = (char) ((c - 10) + 'A');
		else
			c = (char) (c + '0');
		p.write(c);
	}

	protected int bytesPerAtom() {
		return (1);
	}

	protected int bytesPerLine() {
		return (16);
	}

	protected void encodeBufferPrefix(OutputStream o) {
		offset = 0;
		pStream = new PrintStream(o);
	}

	protected void encodeLinePrefix(int len) {
		hexDigit(pStream, (byte) ((offset >>> 8) & 0xff));
		hexDigit(pStream, (byte) (offset & 0xff));
		pStream.print(": ");
		currentByte = 0;
		thisLineLength = len;
	}

	protected void encodeAtom(byte[] buf, int off) {
		thisLine[currentByte] = buf[off];
		hexDigit(pStream, buf[off]);
		pStream.print(" ");
		currentByte++;
		if (currentByte == 8)
			pStream.print("  ");
	}

	protected void encodeLineSuffix() {
		if (thisLineLength < 16) {
			for (int i = thisLineLength; i < 16; i++) {
				pStream.print("   ");
				if (i == 7)
					pStream.print("  ");
			}
		}
		pStream.print(" ");
		for (int i = 0; i < thisLineLength; i++) {
			if ((thisLine[i] < ' ') || (thisLine[i] > 'z')) {
				pStream.print(".");
			}
			else {
				pStream.write(thisLine[i]);
			}
		}
		pStream.println();
		offset += thisLineLength;
	}

	/** Поток, который понимает "печать" */
	protected PrintStream pStream;

	/**
	 * Этот метод обходит странную семантику метода read класса BufferedInputStream.
	 */
	protected int readFully(InputStream in, byte[] buffer) throws java.io.IOException {
		for (int i = 0; i < buffer.length; i++) {
			int q = in.read();
			if (q == -1)
				return i;
			buffer[i] = (byte) q;
		}
		return buffer.length;
	}

	/**
	 * Кодирует байты из входного потока и записывает их в виде текстовых символов в
	 * выходной поток. Этот метод будет выполняться до тех пор, пока не исчерпает входной
	 * поток, но не добавляет суффикс строки для последней строки, которая короче, чем
	 * bytesPerLine().
	 */
	public void encode(InputStream inStream, OutputStream outStream) throws IOException {
		int j;
		int numBytes;
		byte[] tmpbuffer = new byte[bytesPerLine()];

		encodeBufferPrefix(outStream);

		while (true) {
			numBytes = readFully(inStream, tmpbuffer);
			if (numBytes == 0) {
				break;
			}
			encodeLinePrefix(numBytes);
			for (j = 0; j < numBytes; j += bytesPerAtom()) {
				encodeAtom(tmpbuffer, j);
			}
			if (numBytes < bytesPerLine()) {
				break;
			}
			else {
				encodeLineSuffix();
			}
		}
	}

	/**
	 * 'Безпоточный' вариант encode, который просто принимает буфер байтов и возвращает
	 * строку, содержащую закодированный буфер.
	 */
	public String encode(byte[] aBuffer) {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		ByteArrayInputStream inStream = new ByteArrayInputStream(aBuffer);
		String retVal;
		try {
			encode(inStream, outStream);
			// explicit ascii->unicode conversion
			retVal = outStream.toString("ISO-8859-1");
		}
		catch (Exception IOException) {
			// This should never happen.
			throw new Error("CharacterEncoder.encode internal error");
		}
		return (retVal);
	}

	/**
	 * Вернуть массив байтов из оставшихся байтов в этом ByteBuffer.
	 * <P>
	 * Позиция ByteBuffer будет перемещена к лимиту ByteBuffer.
	 * <P>
	 * Чтобы избежать лишнего копирования, реализация попытается вернуть массив байтов,
	 * поддерживающий ByteBuffer. Если это невозможно, будет создан новый массив байтов.
	 */
	private byte[] getBytes(ByteBuffer bb) {
		/*
		 * Это никогда не должно возвращать BufferOverflowException, так как мы осторожны
		 * в том, чтобы выделить ровно столько, сколько нужно.
		 */
		byte[] buf = null;

		/*
		 * Если есть пригодный байтовый буфер, используйте его. Используйте только если
		 * массив точно представляет текущий ByteBuffer.
		 */
		if (bb.hasArray()) {
			byte[] tmp = bb.array();
			if ((tmp.length == bb.capacity()) && (tmp.length == bb.remaining())) {
				buf = tmp;
				bb.position(bb.limit());
			}
		}

		if (buf == null) {
			/*
			 * Этот класс не имеет концепции encode(buf, len, off), поэтому, если у нас
			 * есть частичный буфер, мы должны перераспределить память.
			 */
			buf = new byte[bb.remaining()];

			/*
			 * позиция() автоматически обновляется
			 */
			bb.get(buf);
		}

		return buf;
	}

	/**
	 * 'Безпоточный' вариант кодирования, который просто берет ByteBuffer и возвращает
	 * строку, содержащую закодированный буфер.
	 * <P>
	 * Позиция ByteBuffer будет продвинута до предела ByteBuffer.
	 */
	public String encode(ByteBuffer aBuffer) {
		byte[] buf = getBytes(aBuffer);
		return encode(buf);
	}

	/**
	 * Кодирует байты из входного потока и записывает их в виде текстовых символов в
	 * выходной поток. Этот метод будет работать до тех пор, пока не исчерпает входной
	 * поток. Он отличается от encode тем, что добавляет перевод строки в конце последней
	 * строки, которая короче bytesPerLine().
	 */
	public void encodeBuffer(InputStream inStream, OutputStream outStream) throws IOException {
		int j;
		int numBytes;
		byte[] tmpbuffer = new byte[bytesPerLine()];

		encodeBufferPrefix(outStream);

		while (true) {
			numBytes = readFully(inStream, tmpbuffer);
			if (numBytes == 0) {
				break;
			}
			encodeLinePrefix(numBytes);
			for (j = 0; j < numBytes; j += bytesPerAtom()) {
				encodeAtom(tmpbuffer, j);
			}
			encodeLineSuffix();
			if (numBytes < bytesPerLine()) {
				break;
			}
		}
	}

	/**
	 * Кодирует буфер в <i>aBuffer</i> и записывает закодированный результат в
	 * OutputStream <i>aStream</i>.
	 */
	public void encodeBuffer(byte[] aBuffer, OutputStream aStream) throws IOException {
		ByteArrayInputStream inStream = new ByteArrayInputStream(aBuffer);
		encodeBuffer(inStream, aStream);
	}

	/**
	 * 'Безпоточный' вариант encode, который просто принимает буфер байтов и возвращает
	 * строку, содержащую закодированный буфер.
	 */
	@SuppressWarnings("unused")
	public String encodeBuffer(byte[] aBuffer) {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		ByteArrayInputStream inStream = new ByteArrayInputStream(aBuffer);
		try {
			encodeBuffer(inStream, outStream);
		}
		catch (Exception IOException) {
			// This should never happen.
			throw new Error("CharacterEncoder.encodeBuffer internal error");
		}
		return (outStream.toString());
	}

	/**
	 * Кодирует <i>aBuffer</i> ByteBuffer и записывает закодированный результат в
	 * OutputStream <i>aStream</i>.
	 * <P>
	 * Позиция ByteBuffer будет продвинута до предела ByteBuffer.
	 */
	@SuppressWarnings("unused")
	public void encodeBuffer(ByteBuffer aBuffer, OutputStream aStream) throws IOException {
		byte[] buf = getBytes(aBuffer);
		encodeBuffer(buf, aStream);
	}

}
