package ru.mos.mostech.ews.exchange;

import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.Settings;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;

/**
 * Поток загрузки сообщений. Используется для предотвращения тайм-аутов по POP и IMAP
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

	@Override
	public void run() {
		try {
			message.loadMimeMessage();
		}
		catch (IOException e) {
			ioException = e;
		}
		catch (MessagingException e) {
			messagingException = e;
		}
		finally {
			isComplete = true;
		}
	}

	/**
	 * Загружает MIME-сообщение в отдельном потоке, если объем превышает 1 МБ. Отправляет
	 * символ пробела каждые десять секунд, чтобы избежать таймаутов клиента
	 * @param message сообщение
	 * @param outputStream выходной поток
	 * @throws IOException при ошибке
	 * @throws MessagingException при ошибке
	 */
	public static void loadMimeMessage(ExchangeSession.Message message, OutputStream outputStream)
			throws IOException, MessagingException {
		if (message.size < 1024 * 1024) {
			message.loadMimeMessage();
		}
		else {
			log.debug("Load large message " + (message.size / 1024) + "KB uid " + message.getUid() + " imapUid "
					+ message.getImapUid() + " in a separate thread");
			MessageLoadThread messageLoadThread = new MessageLoadThread(currentThread().getName(), message);
			messageLoadThread.start();
			while (!messageLoadThread.isComplete) {
				try {
					messageLoadThread.join(10000);
				}
				catch (InterruptedException e) {
					log.warn("Thread interrupted", e);
					Thread.currentThread().interrupt();
				}
				log.debug("Still loading uid " + message.getUid() + " imapUid " + message.getImapUid());
				if (Settings.getBooleanProperty("mt.ews.enableKeepAlive", false)) {
					try {
						outputStream.write(' ');
						outputStream.flush();
					}
					catch (SocketException e) {
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