package ru.mos.mostech.ews.util;

import org.slf4j.MDC;

import java.nio.file.Path;
import java.util.Optional;

public class MdcUserPathUtils {

	private static final String USER_LOG_PATH_KEY = "userLogPath";

	private static final String LOG_FILE_NAME = "logging.log";

	public static void init(int port) {
		MDC.put(USER_LOG_PATH_KEY, "/dev/null");
		Optional<Path> path = getUserLogPathByPort(port);
		if (path.isEmpty()) {
			return;
		}
		String logfile = path.get().resolve(LOG_FILE_NAME).toString();
		MDC.put(USER_LOG_PATH_KEY, logfile);
	}

	public static void clear() {
		MDC.remove(USER_LOG_PATH_KEY);
	}

	public static Optional<Path> getUserLogPathByPort(int port) {
		String user = UserFinder.find(port);
		if (user == null) {
			return Optional.empty();
		}
		return Optional.of(Path.of("/home", user, ".mt-ews"));
	}

}
