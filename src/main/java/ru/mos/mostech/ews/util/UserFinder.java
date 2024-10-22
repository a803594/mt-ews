package ru.mos.mostech.ews.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@UtilityClass
public class UserFinder {

	public static void main(String[] args) {
		find(55676);
	}

	public synchronized String find(int port) {
		try {
			// Шаг 1: Запустить netstat и получить список подключений
			Process netstatProcess = new ProcessBuilder("netstat", "-tnp").start();
			String line;
			String pid;
			try (BufferedReader netstatReader = new BufferedReader(
					new InputStreamReader(netstatProcess.getInputStream()))) {

				String targetPort = String.valueOf(port); // Здесь укажите номер порта
				pid = null;

				boolean isCompleted = netstatProcess.waitFor(5, TimeUnit.SECONDS);
				if (!isCompleted) {
					throw new UnsupportedOperationException("Процесс netstat не завершился за указанное время");
				}

				while ((line = netstatReader.readLine()) != null) {
					if (line.contains(":" + targetPort)) {
						Matcher matcher = Pattern.compile("\\b(\\d+)/").matcher(line);
						if (matcher.find()) {
							pid = matcher.group(1);
							break;
						}
					}
				}
			}

			if (pid == null) {
				log.debug("Не удалось найти процесс для указанного порта.");
				return null;
			}

			// Шаг 2: Использовать ps, чтобы найти имя пользователя по PID
			Process psProcess = new ProcessBuilder("ps", "-p", pid, "-o", "user=").start();
			try (BufferedReader psReader = new BufferedReader(new InputStreamReader(psProcess.getInputStream()))) {

				boolean isCompleted = netstatProcess.waitFor(5, TimeUnit.SECONDS);
				if (!isCompleted) {
					throw new UnsupportedOperationException("Процесс ps не завершился за указанное время");
				}

				if ((line = psReader.readLine()) != null) {
					log.debug("Имя пользователя: {}", line.trim());
					return line.trim();
				}
				else {
					log.debug("Не удалось найти имя пользователя для PID: {}", pid);
				}
			}

		}
		catch (IOException | InterruptedException e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			log.error(e.getMessage(), e);
		}

		return null;
	}

}
