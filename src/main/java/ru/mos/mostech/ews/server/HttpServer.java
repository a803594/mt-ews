package ru.mos.mostech.ews.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONObject;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.autodiscovery.AutoDiscoveryFacade;
import ru.mos.mostech.ews.autodiscovery.AutoDiscoveryFacade.ResolveEwsParams;
import ru.mos.mostech.ews.autodiscovery.AutoDiscoveryFacade.ResolveEwsResults;
import ru.mos.mostech.ews.util.IOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class HttpServer {

    private static volatile com.sun.net.httpserver.HttpServer server;

    private static final Map<ResolveEwsParams, ResolveEwsResults> cache = new ConcurrentHashMap<>();

    public static void start(int port) throws IOException {
        // Создаем сервер на указанном порту
        server = com.sun.net.httpserver.HttpServer.create(
                new InetSocketAddress("localhost", port), 0);

        // Определяем обработчик для корневого пути ("/")
        server.createContext("/pst", new PstHandler());
        server.createContext("/autodiscovery", new AutoDiscoveryHandler());

        // Запускаем сервер
        server.setExecutor(null); // Используем стандартный исполнитель
        server.start();

        log.info("HTTP-сервер для обработки запросов от mt-mail запущен на порту {}", port);
    }

    public static void stop() {
        server.stop(1000);
    }

    // Обработчик, который отвечает на HTTP-запросы
    static class PstHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Получаем метод запроса (GET, POST и т.д.)
            String requestMethod = exchange.getRequestMethod();

            // Формируем ответ
            String response = "Запрос получен! Метод: " + requestMethod;

            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }
    }

    static class AutoDiscoveryHandler implements HttpHandler {

        @SneakyThrows
        @Override
        public void handle(HttpExchange exchange) {
            log.info("Запрос на AutoDiscovery получен");
            try {
                String body = readBodyRequest(exchange);
                String query = Objects.requireNonNullElse(exchange.getRequestURI().getQuery(), "");

                JSONObject json = new JSONObject(body);
                String email = json.getString("email");
                log.info("Поиск EWS для {}", email);
                String password = json.getString("password");

                ResolveEwsParams resolveEwsParams = ResolveEwsParams.builder()
                        .email(email)
                        .password(password)
                        .build();

                ResolveEwsResults results = cache.get(resolveEwsParams);
                if (results == null
                        || results.getTime() < (System.currentTimeMillis() - Duration.ofMinutes(1).toMillis())) {
                    results = AutoDiscoveryFacade.resolveEws(resolveEwsParams).orElse(null);
                }

                if (results == null) {
                    log.info("EWS для {} не найден", email);
                    response404Xml(email, exchange);
                    return;
                }

                cache.put(resolveEwsParams, results);

                if (query.contains("responseType=json")) {
                    response200Json(results, exchange);
                } else {
                    response200Isp(results, exchange);
                }

            } catch (Exception e) {
                log.error("", e);
                response400Xml(e.getMessage(), exchange);
            }
        }


        private void response404Xml(String data, HttpExchange exchange) throws IOException {
            String response = """
                    <username>$data</username>
                    """.replace("$data", data);
            exchange.getResponseHeaders().add("Content-Type", "application/xml; charset=utf-8");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(404, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }

        private void response400Xml(String data, HttpExchange exchange) throws IOException {
            String response = """
                    <error>$data</error>
                    """.replace("$data", data);
            exchange.getResponseHeaders().add("Content-Type", "application/xml; charset=utf-8");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(405, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }

        private void response200Isp(ResolveEwsResults results, HttpExchange exchange) throws IOException {
            String response = IspResponse.build(
                    results.getDomain(),
                    results.getUser(),
                    Settings.getIntProperty("mt.ews.imapPort"),
                    Settings.getIntProperty("mt.ews.smtpPort"),
                    Settings.getIntProperty("mt.ews.caldavPort"),
                    Settings.isSecure()
            );
            exchange.getResponseHeaders().add("Content-Type", "text/xml");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }

        @SneakyThrows
        private void response200Json(ResolveEwsResults results, HttpExchange exchange) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("domain", results.getDomain());
            jsonObject.put("user", results.getUser());
            jsonObject.put("imapPort", Settings.getIntProperty("mt.ews.imapPort"));
            jsonObject.put("smtpPort", Settings.getIntProperty("mt.ews.smtpPort"));
            jsonObject.put("caldavPort", Settings.getIntProperty("mt.ews.caldavPort"));
            jsonObject.put("isSecure", Settings.isSecure());
            String response = jsonObject.toString();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }
    }

    private static String readBodyRequest(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(IOUtil.readFully(is), StandardCharsets.UTF_8);
        }
    }

}