/*
DIT
 */
package ru.mos.mostech.ews.exchange;

import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.Settings;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;

/**
 * Загружать сообщения папки в отдельном потоке.
 */
@Slf4j
public class FolderLoadThread extends Thread {

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
		}
		catch (IOException e) {
			exception = e;
		}
		catch (Exception e) {
			log.error(e + " " + e.getMessage(), e);
			exception = new IOException(e.getMessage(), e);
		}
		finally {
			isComplete = true;
		}
	}

	/**
	 * Загрузить папку в отдельном потоке.
	 * @param folder текущая папка
	 * @param outputStream соединение с клиентом
	 * @throws IOException при ошибке
	 */
	public static void loadFolder(ExchangeSession.Folder folder, OutputStream outputStream) throws IOException {
		FolderLoadThread folderLoadThread = new FolderLoadThread(currentThread().getName(), folder);
		folderLoadThread.start();
		while (!folderLoadThread.isComplete) {
			try {
				folderLoadThread.join(20000);
			}
			catch (InterruptedException e) {
				log.warn("Thread interrupted", e);
				Thread.currentThread().interrupt();
			}
			log.debug("Still loading {} ({} messages)", folder.getFolderPath(), folder.getMessagesCount());
			if (Settings.getBooleanProperty("mt.ews.enableKeepAlive", false)) {
				try {
					outputStream.write(' ');
					outputStream.flush();
				}
				catch (SocketException e) {
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