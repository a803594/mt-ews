package ru.mos.mostech.ews.exchange;

import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.Settings;

import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * Create message in a separate thread.
 */
@Slf4j
public class MessageCreateThread extends Thread {
    

    boolean isComplete = false;
    ExchangeSession session;
    String folderPath;
    String messageName;
    HashMap<String, String> properties;
    MimeMessage mimeMessage;
    ExchangeSession.Message message;
    IOException exception;

    MessageCreateThread(String threadName, ExchangeSession session, String folderPath, String messageName, HashMap<String, String> properties, MimeMessage mimeMessage) {
        super(threadName + "-MessageCreate");
        setDaemon(true);
        this.session = session;
        this.folderPath = folderPath;
        this.messageName = messageName;
        this.properties = properties;
        this.mimeMessage = mimeMessage;
    }

    public void run() {
        try {
            this.message = session.createMessage(folderPath, messageName, properties, mimeMessage);
        } catch (IOException e) {
            exception = e;
        } finally {
            isComplete = true;
        }
    }

    /**
     * Create message in a separate thread.
     *
     * @param session      Exchange session
     * @param folderPath   folder path
     * @param messageName  message name
     * @param properties   message properties
     * @param mimeMessage  message content
     * @param outputStream output stream
     * @param capabilities IMAP capabilities
     * @throws InterruptedException on error
     * @throws IOException          on error
     */
    public static ExchangeSession.Message createMessage(ExchangeSession session, String folderPath, String messageName, HashMap<String, String> properties, MimeMessage mimeMessage, OutputStream outputStream, String capabilities) throws IOException {
        MessageCreateThread messageCreateThread = new MessageCreateThread(currentThread().getName(), session, folderPath, messageName, properties, mimeMessage);
        messageCreateThread.start();
        while (!messageCreateThread.isComplete) {
            try {
                messageCreateThread.join(20000);
            } catch (InterruptedException e) {
                log.warn("Thread interrupted", e);
                Thread.currentThread().interrupt();
            }
            if (!messageCreateThread.isComplete) {
                if (Settings.getBooleanProperty("mt.ews.enableKeepAlive", false)) {
                    log.debug("Still loading message, send capabilities untagged response to avoid timeout");
                    try {
                        log.debug("* " + capabilities);
                        outputStream.write(("* " + capabilities).getBytes(StandardCharsets.US_ASCII));
                        outputStream.write((char) 13);
                        outputStream.write((char) 10);
                        outputStream.flush();
                    } catch (SocketException e) {
                        messageCreateThread.interrupt();
                        throw e;
                    }
                }
            }
        }
        if (messageCreateThread.exception != null) {
            throw messageCreateThread.exception;
        }
        return messageCreateThread.message;
    }
}
