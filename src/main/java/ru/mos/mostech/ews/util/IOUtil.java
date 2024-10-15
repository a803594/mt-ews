/*
DIT
 */
package ru.mos.mostech.ews.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Input output functions.
 */

@Slf4j
public final class IOUtil {
    private IOUtil() {
    }

    /**
     * Write all inputstream content to outputstream.
     *
     * @param inputStream  input stream
     * @param outputStream output stream
     * @throws IOException on error
     */
    public static void write(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] bytes = new byte[8192];
        int length;
        while ((length = inputStream.read(bytes)) > 0) {
            outputStream.write(bytes, 0, length);
        }
    }


    /**
     * Decode base64 input string, return byte array.
     *
     * @param encoded Base64 encoded string
     * @return decoded content as byte array
     */
    public static byte[] decodeBase64(String encoded) {
        return Base64.decodeBase64(encoded.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Decode base64 input string, return content as UTF-8 String.
     *
     * @param encoded Base64 encoded string
     * @return decoded content as byte array
     */
    public static String decodeBase64AsString(String encoded) {
        return new String(decodeBase64(encoded), StandardCharsets.UTF_8);
    }

    /**
     * Base64 encode value.
     *
     * @param value input value
     * @return base64  value
     */
    public static String encodeBase64AsString(String value) {
        return new String(Base64.encodeBase64(value.getBytes(StandardCharsets.UTF_8)), StandardCharsets.US_ASCII);
    }

    /**
     * Base64 encode value.
     *
     * @param value input value
     * @return base64  value
     */
    public static String encodeBase64AsString(byte[] value) {
        return new String(Base64.encodeBase64(value), StandardCharsets.US_ASCII);
    }

    /**
     * Base64 encode value.
     *
     * @param value input value
     * @return base64  value
     */
    public static byte[] encodeBase64(String value) {
        return Base64.encodeBase64(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Base64 encode value.
     *
     * @param value input value
     * @return base64  value
     */
    public static byte[] encodeBase64(byte[] value) {
        return Base64.encodeBase64(value);
    }

    /**
     * Resize image bytes to a max width or height image size.
     *
     * @param inputBytes input image bytes
     * @param max        max size
     * @return scaled image bytes
     * @throws IOException on error
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
     * Resize image to a max width or height image size.
     *
     * @param inputImage input image
     * @param max        max size
     * @return scaled image
     */
    public static BufferedImage resizeImage(BufferedImage inputImage, int max) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        int targetWidth;
        int targetHeight;
        if (width <= max && height <= max) {
            return inputImage;
        } else if (width > height) {
            targetWidth = max;
            targetHeight = targetWidth * height / width;
        } else {
            targetHeight = max;
            targetWidth = targetHeight * width / height;
        }
        Image scaledImage = inputImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage targetImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        targetImage.getGraphics().drawImage(scaledImage, 0, 0, null);
        return targetImage;
    }

    /**
     * Read all inputStream content to a byte array.
     *
     * @param inputStream input stream
     * @return content as byte array
     * @throws IOException on error
     */
    public static byte[] readFully(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        write(inputStream, baos);
        return baos.toByteArray();
    }

}
