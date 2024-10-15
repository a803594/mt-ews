package ru.mos.mostech.ews.exchange;

import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.Settings;
import lombok.extern.slf4j.Slf4j;

import javax.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import java.io.OutputStream;
import lombok.extern.slf4j.Slf4j;
import java.net.SocketException;
import lombok.extern.slf4j.Slf4j;

/**
 * Message load thread.
 * Used to avoid timeouts over POP and IMAP
 */

@Slf4j
public class MessageLoadThread extends Thread {

    protected boolean isComplete = false;
    protected ExchangeSession.Message message;
    protected IOException ioException;
    protected MessagingException messagingException;

    protected MessageLoadThread(String threadName, ExchangeSession.Message message) {
        super(threadName + "-LoadMessage");
        setDaemon(true);
        this.message = message;
    }

    public void run() {
        try {
            message.loadMimeMessage();
        } catch (IOException e) {
            ioException = e;
        } catch (MessagingException e) {
            messagingException = e;
        } finally {
            isComplete = true;
        }
    }

    /**
     * Load mime message in a separate thread if over 1MB.
     * Send a space character every ten seconds to avoid client timeouts
     *
     * @param message      message
     * @param outputStream output stream
     * @throws IOException        on error
     * @throws MessagingException on error
     */
    public static void loadMimeMessage(ExchangeSession.Message message, OutputStream outputStream) throws IOException, MessagingException {
        if (message.size < 1024 * 1024) {
            message.loadMimeMessage();
        } else {
            LOGGER.debug("Load large message " + (message.size / 1024) + "KB uid " + message.getUid() + " imapUid " + message.getImapUid() + " in a separate thread");
                MessageLoadThread messageLoadThread = new MessageLoadThread(currentThread().getName(), message);
                messageLoadThread.start();
                while (!messageLoadThread.isComplete) {
                    try {
                        messageLoadThread.join(10000);
                    } catch (InterruptedException e) {
                        LOGGER.warn("Thread interrupted", e);
                        Thread.currentThread().interrupt();
                    }
                    LOGGER.debug("Still loading uid " + message.getUid() + " imapUid " + message.getImapUid());
                    if (Settings.getBooleanProperty("mt.ews.enableKeepAlive", false)) {
                        try {
                            outputStream.write(' ');
                            outputStream.flush();
                        } catch (SocketException e) {
                            // client closed connection, stop thread
                            message.dropMimeMessage();
                            messageLoadThread.interrupt();
                            throw e;
                        }
                    }
                }
                if (messageLoadThread.ioException != null) {
                    throw messageLoadThread.ioException;
                }
                if (messageLoadThread.messagingException != null) {
                    throw messageLoadThread.messagingException;
                }
        }
    }
}