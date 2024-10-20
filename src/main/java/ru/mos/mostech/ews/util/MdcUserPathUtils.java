package ru.mos.mostech.ews.util;

import org.slf4j.MDC;

public class MdcUserPathUtils {

    public static final String USER_LOG_PATH = "userLogPath";

    public static void init(int port) {
        MDC.put(USER_LOG_PATH, "/dev/null");
        String user = UserFinder.find(port);
        if (user == null) {
            return;
        }
        String path = "/home/" + user + "/.mt-ews/logging.log";
        MDC.put(USER_LOG_PATH, path);
    }

    public static void clear() {
        MDC.remove(USER_LOG_PATH);
    }
}
