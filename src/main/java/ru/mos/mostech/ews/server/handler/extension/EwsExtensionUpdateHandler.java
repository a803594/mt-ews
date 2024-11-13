package ru.mos.mostech.ews.server.handler.extension;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONObject;
import ru.mos.mostech.ews.server.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Slf4j
public class EwsExtensionUpdateHandler implements HttpHandler {

	@SneakyThrows
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try (InputStream is = EwsExtensionHelper.getUpdateJson()) {
			byte[] bytes = Objects.requireNonNull(is).readAllBytes();
			String s = new String(bytes, StandardCharsets.UTF_8);
			HttpServer.sendResponse(exchange, 200, s, HttpServer.APPLICATION_JSON_CHARSET_UTF_8);
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
			JSONObject jsonObject = new JSONObject();
			jsonObject.put(HttpServer.ERROR_FIELD, e.getMessage());
			String response = jsonObject.toString();
			HttpServer.sendResponse(exchange, 500, response, HttpServer.APPLICATION_JSON_CHARSET_UTF_8);
		}
	}

}
