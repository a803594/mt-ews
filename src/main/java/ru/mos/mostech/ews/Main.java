package ru.mos.mostech.ews;

import lombok.SneakyThrows;
import org.codehaus.jettison.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;

public class Main {

	@SneakyThrows
	public static void main(String[] args) {

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

		List<File> javaFiles = JavaFileFinder.findJavaFiles("./");

		javaFiles.parallelStream().forEach(javaFile -> {
			try {
				String code = Files.readString(javaFile.toPath(), StandardCharsets.UTF_8);
				List<String> comments = CommentExtractor.extractComments(code);
				for (String comment : comments) {
					try {
						if (!comment.replaceAll("[А-я]", "").equals(comment)) {
							continue;
						}
						if (comment.contains("DIT")) {
							continue;
						}
						System.out.println(comment);
						String newComment = ChatGptClient.getResponse(
								"Переведи, сохрани структуру комментария, так чтобы можно было заменить прям в код, и верни только переведенный комментрий больше никаких других предложения и просто текст: "
										+ comment);
						System.out.println(newComment);
						code = code.replace(comment, newComment);
						Files.write(javaFile.toPath(), code.getBytes(StandardCharsets.UTF_8));
					}
					catch (Exception e) {
						System.out.println(e.getMessage());
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		});

	}

	public class CommentExtractor {

		public static List<String> extractComments(String javaCode) {
			List<String> comments = new ArrayList<>();

			// Регулярные выражения для поиска однострочных и многострочных комментариев
			String singleLineCommentPattern = "//.*";
			String multiLineCommentPattern = "/\\*.*?\\*/";

			// Создаем шаблоны
			Pattern singleLinePattern = Pattern.compile(singleLineCommentPattern);
			Pattern multiLinePattern = Pattern.compile(multiLineCommentPattern, Pattern.DOTALL);

			// Ищем однострочные комментарии
			Matcher singleLineMatcher = singleLinePattern.matcher(javaCode);
			while (singleLineMatcher.find()) {
				// comments.add(singleLineMatcher.group());
			}

			// Ищем многострочные комментарии
			Matcher multiLineMatcher = multiLinePattern.matcher(javaCode);
			while (multiLineMatcher.find()) {
				comments.add(multiLineMatcher.group());
			}

			return comments;
		}

	}

	public static class JavaFileFinder {

		public static List<File> findJavaFiles(String directoryPath) {
			List<File> javaFiles = new ArrayList<>();
			File directory = new File(directoryPath);

			if (!directory.exists() || !directory.isDirectory()) {
				throw new IllegalArgumentException("Invalid directory path: " + directoryPath);
			}

			findJavaFilesRecursive(directory, javaFiles);
			return javaFiles;
		}

		private static void findJavaFilesRecursive(File directory, List<File> javaFiles) {
			File[] files = directory.listFiles();

			if (files != null) {
				for (File file : files) {
					if (file.isDirectory()) {
						// Если это директория, рекурсивно ищем дальше
						findJavaFilesRecursive(file, javaFiles);
					}
					else if (file.getName().endsWith(".java")) {
						// Если файл .java, добавляем в список
						javaFiles.add(file);
					}
				}
			}
		}

	}

	public class ChatGptClient {

		private static final String CHAD_API_KEY = "chad-440e876282634eab9585583e71a7ee60n5u89d4q";

		private static final String API_URL = "https://ask.chadgpt.ru/api/public/gpt-4o-mini";

		public static String getResponse(String message) {
			try {
				// Создаем клиент
				HttpClient client = HttpClient.newHttpClient();

				JSONObject jsonObject = new JSONObject();
				jsonObject.put("message", message);
				jsonObject.put("api_key", CHAD_API_KEY);
				// Формируем JSON-объект
				String jsonRequest = jsonObject.toString();

				// Создаем запрос
				HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(API_URL))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
					.build();

				// Отправляем запрос и получаем ответ
				HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

				// Проверяем код статуса
				if (response.statusCode() != 200) {
					System.out.println("Ошибка! Код HTTP-ответа: " + response.statusCode());
					return null;
				}

				JSONObject rest = new JSONObject(response.body());
				return rest.getString("response");
			}
			catch (HttpTimeoutException e) {
				System.out.println("Превышено время ожидания запроса: " + e.getMessage());
				return null;
			}
			catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

	}

	public class JsonResponseParser {

		public static String extractResponse(String json) {
			// Проверяем, что строка не пустая
			if (json == null || json.isEmpty()) {
				return null;
			}

			// Ищем начальный индекс поля "response"
			int startIndex = json.indexOf("\"response\"");
			if (startIndex == -1) {
				return null; // Поле не найдено
			}

			// Сдвигаем индекс до начала значения
			startIndex = json.indexOf(":", startIndex) + 1;
			if (startIndex == 0) {
				return null; // Ошибка при определении индекса
			}

			// Находим конец значения
			int endIndex = json.indexOf(",", startIndex);
			if (endIndex == -1) {
				// Если запятая не найдена, ищем конец JSON-объекта
				endIndex = json.indexOf("}", startIndex);
			}

			// Извлекаем значение
			String responseValue;
			if (endIndex == -1) {
				responseValue = json.substring(startIndex).trim();
			}
			else {
				responseValue = json.substring(startIndex, endIndex).trim();
			}

			// Убираем лишние кавычки и пробелы
			responseValue = responseValue.replaceAll("^\"|\"$", "").trim();

			return responseValue.isEmpty() ? null : responseValue;
		}

	}

}
