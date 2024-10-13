/*
DIT
 */

package ru.mos.mostech.ews.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * Encrypt string with user password.
 * Simple implementation based on AES
 */
public class StringEncryptor {
    static final String ALGO = "PBEWithHmacSHA256AndAES_128";
    static String fingerprint;

    static {
        try {
            fingerprint = InetAddress.getLocalHost().getCanonicalHostName().substring(0, 16);
        } catch (Throwable t) {
            fingerprint = "mtewsgateway!&";
        }
    }

    private final String password;

    public StringEncryptor(String password) {
        this.password = password;
    }

    public String encryptString(String value) throws IOException {
        try {
            byte[] plaintext = value.getBytes(StandardCharsets.UTF_8);

            // Encrypt
            Cipher enc = Cipher.getInstance(ALGO);
            enc.init(Cipher.ENCRYPT_MODE, getSecretKey(), getPBEParameterSpec());
            byte[] encrypted = enc.doFinal(plaintext);
            return "{AES}" + IOUtil.encodeBase64AsString(encrypted);

        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public String decryptString(String value) throws IOException {
        if (value != null && value.startsWith("{AES}")) {
            try {
                byte[] encrypted = IOUtil.decodeBase64(value.substring(5));

                Cipher dec = Cipher.getInstance(ALGO);
                dec.init(Cipher.DECRYPT_MODE, getSecretKey(), getPBEParameterSpec());
                byte[] decrypted = dec.doFinal(encrypted);
                return new String(decrypted, StandardCharsets.UTF_8);

            } catch (Exception e) {
                throw new IOException(e);
            }
        } else {
            return value;
        }
    }

    private SecretKey getSecretKey() throws InvalidKeySpecException, NoSuchAlgorithmException {
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray());

        SecretKeyFactory kf = SecretKeyFactory.getInstance(ALGO);
        return kf.generateSecret(keySpec);
    }

    private PBEParameterSpec getPBEParameterSpec() {
        byte[] bytes = fingerprint.getBytes(StandardCharsets.UTF_8);
        return new PBEParameterSpec(bytes, 10000, new IvParameterSpec(bytes));
    }
}
