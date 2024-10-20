package ru.mos.mostech.ews.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class UserFinder {

    public static void main(String[] args) {
        MDC.put("user", "aaa");
        find(1222);
    }
    public static String find(int port) {
        try {
            // Шаг 1: Запустить netstat и получить список подключений
            Process netstatProcess = new ProcessBuilder("netstat", "-tnp").start();
            BufferedReader netstatReader = new BufferedReader(new InputStreamReader(netstatProcess.getInputStream()));

            String line;
            String targetPort = String.valueOf(port); // Здесь укажите номер порта
            String pid = null;

            while ((line = netstatReader.readLine()) != null) {
                if (line.contains(":" + targetPort)) {
                    Matcher matcher = Pattern.compile("\\b([0-9]+)\\/").matcher(line);
                    if (matcher.find()) {
                        pid = matcher.group(1);
                        break;
                    }
                }
            }

            if (pid == null) {
                log.debug("Не удалось найти процесс для указанного порта.");
                return null;
            }

            // Шаг 2: Использовать ps, чтобы найти имя пользователя по PID
            Process psProcess = new ProcessBuilder("ps", "-p", pid, "-o", "user=").start();
            BufferedReader psReader = new BufferedReader(new InputStreamReader(psProcess.getInputStream()));

            if ((line = psReader.readLine()) != null) {
                log.debug("Имя пользователя: {}", line.trim());
                return line.trim();
            } else {
                log.debug("Не удалось найти имя пользователя для PID: {}", pid);
            }


        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }
}
