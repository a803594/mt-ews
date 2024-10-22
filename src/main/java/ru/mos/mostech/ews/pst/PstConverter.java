package ru.mos.mostech.ews.pst;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import ru.mos.mostech.ews.util.IOUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@UtilityClass
public class PstConverter {

	private volatile ExecutorService executor;

	public static void main(String[] args) {
		start();
		convert("./backup.pst");
		// stop();
	}

	public void start() {
		executor = Executors.newSingleThreadExecutor();
	}

	public void stop() {
		try {
			executor.awaitTermination(1, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			log.error("", e);
			Thread.currentThread().interrupt();
		}
		executor.shutdownNow();
	}

	@SneakyThrows
	public Path convert(String inputPath) {
		File jar = findJar();
		Objects.requireNonNull(jar, "pst library not found");
		String userDirectory = getUserDirectory(inputPath);
		Objects.requireNonNull(userDirectory, "user directory not found");

		String fileName = Path.of(inputPath).getFileName().toString();
		Path pstPathDir = Paths.get(userDirectory, ".mt-ews", "pst");
		Files.createDirectories(pstPathDir);
		Path tempDirectory = Files.createTempDirectory(pstPathDir, fileName);
		doConvert(jar.getAbsolutePath(), inputPath, tempDirectory.toString());
		return tempDirectory;
	}

	private void doConvert(String jarPath, String inputPath, String outputDir) {
		executor.submit(() -> {
			try {
				ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", jarPath, "-i", inputPath, "-o",
						outputDir, "-f", "EML", "-e", "windows-1251");

				processBuilder.redirectErrorStream(true);
				Process process = processBuilder.start();
				String processOut = IOUtil.readToString(process::getInputStream);
				boolean success = process.waitFor() == 0; // Успех, если код завершения 0
				createResultJson(outputDir, success, processOut);
			}
			catch (InterruptedException e) {
				log.error("", e);
				Thread.currentThread().interrupt();
			}
			catch (Exception e) {
				log.error("", e);
			}
		});
	}

	private void createResultJson(String outputDir, boolean success, String processOut) {
		JSONObject resultJson = new JSONObject();
		try {
			resultJson.put("result", success);
			resultJson.put("status", "DONE");
			resultJson.put("output", processOut);

			JSONArray filesArray = getAllEmlFiles(outputDir);
			resultJson.put("files", filesArray);

			// Запись JSON в файл
			try (FileWriter fileWriter = new FileWriter(new File(outputDir, "results.json"))) {
				fileWriter.write(resultJson.toString(4)); // Форматирование с отступами
			}
		}
		catch (JSONException | IOException e) {
			log.error("", e);
		}
	}

	public JSONObject getStatus(String outputDir) throws JSONException {
		File file = new File(outputDir, "results.json");
		if (!file.exists()) {
			JSONObject resultJson = new JSONObject();
			resultJson.put("status", "PENDING");
			resultJson.put("files", getAllEmlFiles(outputDir));
			return resultJson;
		}
		String json = IOUtil.readToString(() -> {
			try {
				return new FileInputStream(file);
			}
			catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		});
		return new JSONObject(json);
	}

	private JSONArray getAllEmlFiles(String outputDir) throws JSONException {
		JSONArray filesArray = new JSONArray();
		File outputDirectory = new File(outputDir);
		collectEmlFiles(outputDirectory, filesArray);
		return filesArray;
	}

	private void collectEmlFiles(File directory, JSONArray emlFiles) throws JSONException {
		if (!directory.exists() || !directory.isDirectory()) {
			return;
		}

		File[] files = directory.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					collectEmlFiles(file, emlFiles);
				}
				else if (file.isFile() && file.getName().endsWith(".eml")) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("name", file.getName());
					jsonObject.put("path", file.getAbsolutePath());
					emlFiles.put(jsonObject);
				}
			}
		}
	}

	private File findJar() {
		String directoryPath = "./"; // Путь к папке для поиска
		String fileNamePattern1 = "mt-ews-pst.*\\with-dependencies.jar"; // Шаблон имени
																			// файла
		String fileNamePattern2 = "mt-ews-pst.*\\.jar"; // Шаблон имени файла

		File foundFile = findFile(directoryPath, fileNamePattern1);

		if (foundFile == null) {
			foundFile = findFile(directoryPath, fileNamePattern2);
		}

		if (foundFile != null) {
			log.info("Найден файл: {}", foundFile.getAbsolutePath());
		}
		else {
			log.info("Файл не найден. {} или {}", fileNamePattern1, fileNamePattern2);
		}

		return foundFile;
	}

	private File findFile(String directoryPath, String fileNamePattern) {
		File directory = new File(directoryPath);
		File[] files = directory.listFiles();

		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					// Рекурсивный вызов для подпапок
					File foundFile = findFile(file.getAbsolutePath(), fileNamePattern);
					if (foundFile != null) {
						return foundFile; // Возвращаем найденный файл
					}
				}
				else if (file.getName().matches(fileNamePattern)) {
					return file; // Возвращаем найденный файл
				}
			}
		}
		return null; // Если файл не найден
	}

	public String getUserDirectory(String filePath) {
		Path path = Path.of(filePath).toAbsolutePath();
		Path homePath = Paths.get("/home");

		// Проверяем, содержит ли путь директорию /home
		if (path.startsWith(homePath) && path.getNameCount() > 1) {
			// Извлекаем первую часть после /home, которая будет именем пользователя
			Path userPath = homePath.resolve(path.getName(1));
			return userPath.toString();
		}

		return null; // Если путь не содержит /home или структура некорректна
	}

}
