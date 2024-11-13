package ru.mos.mostech.ews.server.handler.extension;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONObject;
import ru.mos.mostech.ews.server.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

@Slf4j
public class EwsExtensionDownloadHandler implements HttpHandler {

	@SneakyThrows
	@Override
	public void handle(HttpExchange exchange) throws IOException {

		try (InputStream is = EwsExtensionHelper.getExtensionFile()) {
			byte[] bytes = Objects.requireNonNull(is).readAllBytes();

			Headers headers = exchange.getResponseHeaders();
			headers.add(HttpServer.CONTENT_TYPE, HttpServer.APPLICATION_XPINSTALL);
			headers.add(HttpServer.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			String fileName = EwsExtensionHelper.getExtensionFileName();
			headers.add(HttpServer.CONTENT_DISPOSITION, String.format("attachment; filename=%s", fileName));
			exchange.sendResponseHeaders(200, bytes.length);
			OutputStream os = exchange.getResponseBody();
			os.write(bytes);
			os.close();
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
