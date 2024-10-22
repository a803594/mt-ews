package ru.mos.mostech.ews.util;

import lombok.experimental.UtilityClass;
import ru.mos.mostech.ews.Settings;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

@UtilityClass
public class KeysUtils {

    /**
     * Build trust managers from truststore file.
     *
     * @return trust managers
     * @throws CertificateException     on error
     * @throws NoSuchAlgorithmException on error
     * @throws IOException              on error
     * @throws KeyStoreException        on error
     */
    public TrustManager[] getTrustManagers() throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException {
        String truststoreFile = Settings.getProperty("mt.ews.ssl.truststoreFile");
        if (truststoreFile == null || truststoreFile.isEmpty()) {
            return new TrustManager[]{};
        }
        try (InputStream trustStoreInputStream = getKeyStoreStream(truststoreFile)) {
            KeyStore trustStore = KeyStore.getInstance(Settings.getProperty("mt.ews.ssl.truststoreType"));
            trustStore.load(trustStoreInputStream, Settings.getCharArrayProperty("mt.ews.ssl.truststorePass"));

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            return tmf.getTrustManagers();
        }
    }

    /**
     * Build key managers from keystore file.
     *
     * @return key managers
     * @throws CertificateException     on error
     * @throws NoSuchAlgorithmException on error
     * @throws IOException              on error
     * @throws KeyStoreException        on error
     */
    public KeyManager[] getKeyManagers() throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException, UnrecoverableKeyException {
        String keystoreFile = Settings.getProperty("mt.ews.ssl.keystoreFile");
        if (keystoreFile == null || keystoreFile.isEmpty()) {
            return new KeyManager[]{};
        }
        try (InputStream keyStoreInputStream = getKeyStoreStream(keystoreFile)) {
            KeyStore keystore = KeyStore.getInstance(Settings.getProperty("mt.ews.ssl.keystoreType"));
            keystore.load(keyStoreInputStream, Settings.getCharArrayProperty("mt.ews.ssl.keystorePass"));

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keystore, Settings.getCharArrayProperty("mt.ews.ssl.keyPass"));
            return kmf.getKeyManagers();
        }
    }

    private InputStream getKeyStoreStream(String keystoreFile) throws IOException {
        if (keystoreFile == null || keystoreFile.isEmpty()) {
            throw new RuntimeException("Неправильная конфигурация. keystoreFile - пустой");
        }
        if (keystoreFile.startsWith("classpath:")) {
            keystoreFile = keystoreFile.replace("classpath:", "");
            return Thread.currentThread().getContextClassLoader().getResourceAsStream(keystoreFile);
        }
        return Files.newInputStream(Paths.get(keystoreFile));
    }


}
