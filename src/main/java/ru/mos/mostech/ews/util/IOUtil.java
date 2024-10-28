/*
DIT
 */
package ru.mos.mostech.ews.util;

import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 * Функции ввода-вывода.
 */
public final class IOUtil {

	private IOUtil() {
	}

	/**
	 * Записать всё содержимое входного потока в выходной поток.
	 * @param inputStream входной поток
	 * @param outputStream выходной поток
	 * @throws IOException в случае ошибки
	 */
	public static void write(InputStream inputStream, OutputStream outputStream) throws IOException {
		byte[] bytes = new byte[8192];
		int length;
		while ((length = inputStream.read(bytes)) > 0) {
			outputStream.write(bytes, 0, length);
		}
	}

	/**
	 * Декодировать входную строку в формате base64, вернуть массив байтов.
	 * @param encoded Строка в формате Base64
	 * @return декодированный контент в виде массива байтов
	 */
	public static byte[] decodeBase64(String encoded) {
		return Base64.decodeBase64(encoded.getBytes(StandardCharsets.US_ASCII));
	}

	/**
	 * Декодировать строку ввода в формате base64, вернуть содержимое в виде UTF-8 строки.
	 * @param encoded Строка в формате Base64
	 * @return декодированное содержимое в виде массива байт
	 */
	public static String decodeBase64AsString(String encoded) {
		return new String(decodeBase64(encoded), StandardCharsets.UTF_8);
	}

	/**
	 * Кодирует значение в формате Base64.
	 * @param value входное значение
	 * @return значение в формате base64
	 */
	public static String encodeBase64AsString(String value) {
		return new String(Base64.encodeBase64(value.getBytes(StandardCharsets.UTF_8)), StandardCharsets.US_ASCII);
	}

	/**
	 * Кодирует значение в формате Base64.
	 * @param value входное значение
	 * @return значение в формате base64
	 */
	public static String encodeBase64AsString(byte[] value) {
		return new String(Base64.encodeBase64(value), StandardCharsets.US_ASCII);
	}

	/**
	 * Кодирует значение в формате Base64.
	 * @param value входное значение
	 * @return значение в формате base64
	 */
	public static byte[] encodeBase64(String value) {
		return Base64.encodeBase64(value.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Кодирует значение в формате Base64.
	 * @param value входное значение
	 * @return значение в формате base64
	 */
	public static byte[] encodeBase64(byte[] value) {
		return Base64.encodeBase64(value);
	}

	/**
	 * Изменяет размер байтов изображения до максимальной ширины или высоты.
	 * @param inputBytes байты входного изображения
	 * @param max максимальный размер
	 * @return масштабированные байты изображения
	 * @throws IOException в случае ошибки
	 */
	public static byte[] resizeImage(byte[] inputBytes, int max) throws IOException {
		BufferedImage inputImage = ImageIO.read(new ByteArrayInputStream(inputBytes));
		if (inputImage == null) {
			throw new IOException("Unable to decode image data");
		}
		BufferedImage outputImage = resizeImage(inputImage, max);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(outputImage, "jpg", baos);
		return baos.toByteArray();
	}

	/**
	 * Изменить размер изображения до максимальной ширины или высоты.
	 * @param inputImage входное изображение
	 * @param max максимальный размер
	 * @return масштабированное изображение
	 */
	public static BufferedImage resizeImage(BufferedImage inputImage, int max) {
		int width = inputImage.getWidth();
		int height = inputImage.getHeight();
		int targetWidth;
		int targetHeight;
		if (width <= max && height <= max) {
			return inputImage;
		}
		else if (width > height) {
			targetWidth = max;
			targetHeight = targetWidth * height / width;
		}
		else {
			targetHeight = max;
			targetWidth = targetHeight * width / height;
		}
		Image scaledImage = inputImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
		BufferedImage targetImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
		targetImage.getGraphics().drawImage(scaledImage, 0, 0, null);
		return targetImage;
	}

	/**
	 * Прочитать все содержимое inputStream в массив байтов.
	 * @param inputStream входной поток
	 * @return содержимое в виде массива байтов
	 * @throws IOException при ошибке
	 */
	public static byte[] readFully(InputStream inputStream) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		write(inputStream, baos);
		return baos.toByteArray();
	}

	@SneakyThrows
	public static String readToString(Supplier<InputStream> ou) {
		try (InputStream out = ou.get()) {
			return new String(readFully(out), StandardCharsets.UTF_8);
		}
	}

}
