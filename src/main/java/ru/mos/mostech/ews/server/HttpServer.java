package ru.mos.mostech.ews.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONObject;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.autodiscovery.AutoDiscoveryFacade;
import ru.mos.mostech.ews.autodiscovery.AutoDiscoveryFacade.ResolveEwsParams;
import ru.mos.mostech.ews.autodiscovery.AutoDiscoveryFacade.ResolveEwsResults;
import ru.mos.mostech.ews.pst.PstConverter;
import ru.mos.mostech.ews.util.IOUtil;
import ru.mos.mostech.ews.util.MdcUserPathUtils;
import ru.mos.mostech.ews.util.ZipUtil;
import ru.mos.mostech.ews.util.KeysUtils;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class HttpServer {

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=utf-8";
    public static final String APPLICATION_XML_CHARSET_UTF_8 = "application/xml; charset=utf-8";
    public static final String APPLICATION_ZIP = "application/zip";

    private static volatile com.sun.net.httpserver.HttpServer server;

    private static final Map<ResolveEwsParams, ResolveEwsResults> cache = new ConcurrentHashMap<>();

    @SneakyThrows
    public static void start(int port) {
        server = createHttpServer(port);

        // Определяем обработчик для корневого пути ("/")
        server.createContext("/pst", new MdcHttpHandler(new PstHandler()));
        server.createContext("/pst-status", new MdcHttpHandler(new PstStatusHandler()));
        server.createContext("/autodiscovery", new MdcHttpHandler(new AutoDiscoveryHandler()));
        server.createContext("/ews-settings", new MdcHttpHandler(new EwsSettingsHandler()));
        server.createContext("/ews-logs", new MdcHttpHandler(new EwsLogsHandler()));

        // Запускаем сервер
        server.setExecutor(null); // Используем стандартный исполнитель
        server.start();
        PstConverter.start();

        log.info("HTTP-сервер для обработки запросов от mt-mail запущен на порту {}", port);
    }

    private static com.sun.net.httpserver.HttpServer createHttpServer(int port) throws NoSuchAlgorithmException, KeyManagementException, CertificateException, IOException, KeyStoreException, UnrecoverableKeyException {
        String keystoreFile = Settings.getProperty("mt.ews.ssl.keystoreFile");
        if (keystoreFile == null || keystoreFile.isEmpty()) {
            return com.sun.net.httpserver.HttpServer.create(
                    new InetSocketAddress("localhost", port), 0);
        }
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(KeysUtils.getKeyManagers(), null, null);
        // Создаем сервер на указанном порту
        HttpsServer server = HttpsServer.create(
                new InetSocketAddress("localhost", port), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        return server;
    }

    public static void stop() {
        server.stop(1000);
        PstConverter.stop();
    }

    // Обработчик, который отвечает на HTTP-запросы
    static class PstHandler implements HttpHandler {

        @SneakyThrows
        @Override
        public void handle(HttpExchange exchange) {
            try {

                String body = readBodyRequest(exchange);

                JSONObject json = new JSONObject(body);
                String path = json.optString("path");
                Path outputDir = PstConverter.convert(path);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("path", outputDir.toAbsolutePath().toString());
                String response = jsonObject.toString();
                sendResponse(exchange, 200, response, APPLICATION_JSON_CHARSET_UTF_8);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", e.getMessage());
                String response = jsonObject.toString();
                sendResponse(exchange, 500, response, APPLICATION_JSON_CHARSET_UTF_8);
            }

        }
    }

    static class PstStatusHandler implements HttpHandler {

        @SneakyThrows
        @Override
        public void handle(HttpExchange exchange) {
            try {
                String body = readBodyRequest(exchange);

                JSONObject json = new JSONObject(body);
                String path = json.optString("path");
                JSONObject status = PstConverter.getStatus(path);
                String response = status.toString();
                sendResponse(exchange, 200, response, APPLICATION_JSON_CHARSET_UTF_8);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", e.getMessage());
                String response = jsonObject.toString();
                sendResponse(exchange, 500, response, APPLICATION_JSON_CHARSET_UTF_8);
            }

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
            sendResponse(exchange, 404, response, APPLICATION_XML_CHARSET_UTF_8);
        }

        private void response400Xml(String data, HttpExchange exchange) throws IOException {
            String response = """
                    <error>$data</error>
                    """.replace("$data", data);
            sendResponse(exchange, 400, response, APPLICATION_XML_CHARSET_UTF_8);
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
            sendResponse(exchange, 200, response, APPLICATION_XML_CHARSET_UTF_8);
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
            sendResponse(exchange, 200, response, APPLICATION_JSON_CHARSET_UTF_8);
        }
    }

    static class EwsSettingsHandler implements HttpHandler {

        @SneakyThrows
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                JSONObject jsonObject = new JSONObject();
                for (String[] setting : Settings.getAll()) {
                    jsonObject.put(setting[0], setting[1]);
                }
                String response = jsonObject.toString();
                sendResponse(exchange, 200, response, APPLICATION_JSON_CHARSET_UTF_8);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", e.getMessage());
                String response = jsonObject.toString();
                sendResponse(exchange, 500, response, APPLICATION_JSON_CHARSET_UTF_8);
            }
        }
    }

    static class EwsLogsHandler implements HttpHandler {

        @SneakyThrows
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                int remotePort = exchange.getRemoteAddress().getPort();
                Optional<Path> userLogPath = MdcUserPathUtils.getUserLogPathByPort(remotePort);

                if (userLogPath.isPresent()) {
                    File file = new File(userLogPath.get().toString());
                    sendZipFile(exchange, 200, file);
                } else {
                    throw new Exception("Не удалось найти имя пользователя");
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", e.getMessage());
                String response = jsonObject.toString();
                sendResponse(exchange, 500, response, APPLICATION_JSON_CHARSET_UTF_8);
            }
        }
    }

    private static String readBodyRequest(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(IOUtil.readFully(is), StandardCharsets.UTF_8);
        }
    }

    private static void sendResponse(HttpExchange exchange, int status, String response, String contentType) throws IOException {
        exchange.getResponseHeaders().add(CONTENT_TYPE, contentType);
        exchange.getResponseHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        exchange.sendResponseHeaders(status, response.getBytes(StandardCharsets.UTF_8).length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes(StandardCharsets.UTF_8));
        os.close();
    }

    private static void sendZipFile(HttpExchange exchange, int status, File file) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.add(CONTENT_TYPE, APPLICATION_ZIP);
        headers.add(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        headers.add(CONTENT_DISPOSITION, String.format("attachment; filename=%s", file.getName()));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipUtil.zipFolder(file.toPath(), bos);
        exchange.sendResponseHeaders(status, bos.size());
        OutputStream os = exchange.getResponseBody();
        bos.writeTo(os);
        bos.close();
        os.close();
    }
}
