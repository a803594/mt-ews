package ru.mos.mostech.ews.autodiscovery;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class HttpAutoDiscoverClient {

	public static void main(String[] args) {
		AutoDiscoveryRequestParams params = AutoDiscoveryRequestParams.builder()
			.email("***REMOVED***")
			.user("***REMOVED***")
			.password("***REMOVED***")
			.host("owa.mos.ru")
			.port(443)
			.isSecure(true)
			.build();

		String url = sendRequest(params);
		log.info("Результат: {}", url);
	}

	public static String sendRequest(AutoDiscoveryRequestParams params) {
		log.info("{}", params);
		try {
			String protocol = params.isSecure() ? "https" : "http";
			String url = String.format("%s://%s:%d/autodiscover/autodiscover.xml", protocol, params.getHost(),
					params.getPort());

			String requestBody = """
					<?xml version="1.0" encoding="utf-8"?>
					<Autodiscover xmlns="http://schemas.microsoft.com/exchange/autodiscover/outlook/requestschema/2006">
					    <Request>
					        <EMailAddress>%s</EMailAddress>
					        <AcceptableResponseSchema>http://schemas.microsoft.com/exchange/autodiscover/outlook/responseschema/2006a</AcceptableResponseSchema>
					    </Request>
					</Autodiscover>
					"""
				.formatted(params.getEmail());

			String auth = params.getUser() + ":" + params.getPassword();
			String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

			HttpResponse<String> response;
			HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.timeout(Duration.ofSeconds(5))
				.header("Content-Type", "text/xml; charset=UTF-8")
				.header("Authorization", "Basic " + encodedAuth)
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();

			response = client.send(request, HttpResponse.BodyHandlers.ofString());
			log.info("Код ответа: {}", response.statusCode());

			String result = response.body();
			log.info("Тело ответа: {}", result);

			String resultUrl = Optional.ofNullable(result)
				.map(res -> getElementValue(res, "EwssUrl"))
				.orElseGet(() -> getElementValue(Objects.requireNonNullElse(result, ""), "EwsUrl"));

			log.info("Найден EwsURL: {}", resultUrl);

			return resultUrl;
		}
		catch (InterruptedException e) {
			log.error("Соединение было прервано во время autoDiscovery: ", e);
			/* Clean up whatever needs to be handled before interrupting */
			Thread.currentThread().interrupt();
		}
		catch (Exception e) {
			log.error("Ошибка во время запроса на autoDiscovery: ", e);
		}

		return null;
	}

	private static String getElementValue(String xml, String elementName) {
		String openingTag = "<" + elementName + ">";
		String closingTag = "</" + elementName + ">";

		int startIndex = xml.indexOf(openingTag);
		int endIndex = xml.indexOf(closingTag);

		if (startIndex != -1 && endIndex != -1) {
			// Извлекаем текст между открывающим и закрывающим тегами
			startIndex += openingTag.length();
			return xml.substring(startIndex, endIndex).trim();
		}

		return null; // Возвращаем null, если элемент не найден
	}

	@Data
	@Builder
	@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
	public static class AutoDiscoveryRequestParams {

		String host;

		int port;

		String user;

		String email;

		String password;

		boolean isSecure;

	}

}