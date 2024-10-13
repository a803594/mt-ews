/*
DIT
 */
package ru.mos.mostech.ews.exchange;

import org.apache.log4j.Logger;
import ru.mos.mostech.ews.Settings;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;

/**
 * Load folder messages in a separate thread.
 */
public class FolderLoadThread extends Thread {
    private static final Logger LOGGER = Logger.getLogger(FolderLoadThread.class);

    boolean isComplete = false;
    ExchangeSession.Folder folder;
    IOException exception;

    FolderLoadThread(String threadName, ExchangeSession.Folder folder) {
        super(threadName + "-LoadFolder");
        setDaemon(true);
        this.folder = folder;
    }

    @Override
    public void run() {
        try {
            folder.loadMessages();
        } catch (IOException e) {
            exception = e;
        } catch (Exception e) {
            LOGGER.error(e+" "+e.getMessage(), e);
            exception = new IOException(e.getMessage(), e);
        } finally {
            isComplete = true;
        }
    }

    /**
     * Load folder in a separate thread.
     *
     * @param folder       current folder
     * @param outputStream client connection
     * @throws IOException          on error
     */
    public static void loadFolder(ExchangeSession.Folder folder, OutputStream outputStream) throws IOException {
        FolderLoadThread folderLoadThread = new FolderLoadThread(currentThread().getName(), folder);
        folderLoadThread.start();
        while (!folderLoadThread.isComplete) {
            try {
                folderLoadThread.join(20000);
            } catch (InterruptedException e) {
                LOGGER.warn("Thread interrupted", e);
                Thread.currentThread().interrupt();
            }
            LOGGER.debug("Still loading " + folder.folderPath + " (" + folder.count() + " messages)");
            if (Settings.getBooleanProperty("mt.ews.enableKeepAlive", false)) {
                try {
                    outputStream.write(' ');
                    outputStream.flush();
                } catch (SocketException e) {
                    folderLoadThread.interrupt();
                    throw e;
                }
            }
        }
        if (folderLoadThread.exception != null) {
            throw folderLoadThread.exception;
        }

    }
}