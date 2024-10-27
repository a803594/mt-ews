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
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Slf4j
@UtilityClass
public class PstConverter {

	private static final AtomicReference<ExecutorService> EXECUTOR = new AtomicReference<>();

	public void start() {
		stop();
		EXECUTOR.set(new ThreadPoolExecutor(3, 3, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), (r, executor) -> {
			throw new RejectedExecutionException("Все потоки заняты, задание было отклонено!");
		}));
	}

	public void stop() {
		ExecutorService executorService = EXECUTOR.get();
		if (executorService == null || executorService.isShutdown()) {
			return;
		}
		try {
			boolean isStop = executorService.awaitTermination(1, TimeUnit.SECONDS);
			log.debug("Stopping PST ExecutorService: {}", isStop);
		}
		catch (InterruptedException e) {
			log.error("", e);
			Thread.currentThread().interrupt();
		}
		executorService.shutdownNow();
	}

	@SneakyThrows
	public Path convert(String originalFilePath, Supplier<InputStream> is) {
		File jar = findJar();
		Objects.requireNonNull(jar, "pst library not found");

		String user = getUser(originalFilePath);
		Path pstPathDir = Paths.get("./.mt-ews", "pst", Objects.requireNonNullElse(user, "unknown"));
		Files.createDirectories(pstPathDir);

		String fileName = Path.of(originalFilePath).getFileName().toString();
		Path inputFilePath = pstPathDir.resolve(fileName);
		try (InputStream inputStream = is.get()) {
			Files.copy(inputStream, inputFilePath, StandardCopyOption.REPLACE_EXISTING);
		}

		Path tempDirectory = Files.createTempDirectory(pstPathDir, fileName);
		doConvert(jar.getAbsolutePath(), inputFilePath.toString(), tempDirectory.toString());
		return tempDirectory;
	}

	private void doConvert(String jarPath, String inputPath, String outputDir) {
		EXECUTOR.get().submit(() -> {
			try {
				ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", jarPath, "-i", inputPath, "-o",
						outputDir, "-f", "EML", "-e", "windows-1251");

				processBuilder.redirectErrorStream(true);
				Process process = processBuilder.start();
				String processOut = IOUtil.readToString(process::getInputStream);
				boolean success = process.waitFor(5, TimeUnit.MINUTES);
				if (!success) {
					process.destroy();
				}
				createResultJson(outputDir, success, processOut);
			}
			catch (InterruptedException e) {
				log.error("Converting PST Interrupted", e);
				Thread.currentThread().interrupt();
			}
			catch (Exception e) {
				log.error("Converting PST Error", e);
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
				throw new UnsupportedOperationException("PST Status JSON is not found", e);
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

	public String getUser(String filePath) {
		Path path = Path.of(filePath).toAbsolutePath();
		Path homePath = Paths.get("/home");

		if (path.startsWith(homePath) && path.getNameCount() > 1) {
			Path userPath = path.getName(1);
			return userPath.toString();
		}

		return null;
	}

}
