/*
DIT
 */

package ru.mos.mostech.ews.exchange.ews;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


@Slf4j
public class AutoDiscoverMethod extends HttpPost implements ResponseHandler {

    protected static final Logger LOGGER = Logger.getLogger(AutoDiscoverMethod.class);

    public AutoDiscoverMethod(String url, String userEmail) {
        super(url);
        setRequestEntity(userEmail);
    }

    private void setRequestEntity(String userEmail) {
        String body = "<Autodiscover xmlns=\"http://schemas.microsoft.com/exchange/autodiscover/outlook/requestschema/2006\">" +
                "<Request>" +
                "<EMailAddress>" + userEmail + "</EMailAddress>" +
                "<AcceptableResponseSchema>http://schemas.microsoft.com/exchange/autodiscover/outlook/responseschema/2006a</AcceptableResponseSchema>" +
                "</Request>" +
                "</Autodiscover>";
        setEntity(new StringEntity(body, ContentType.create("text/xml", "UTF-8")));
    }

    @Override
    public Object handleResponse(HttpResponse response) throws IOException {
        String ewsUrl = null;
        try {
            Header contentTypeHeader = response.getFirstHeader("Content-Type");
            if (contentTypeHeader != null &&
                    ("text/xml; charset=utf-8".equals(contentTypeHeader.getValue())
                            || "text/html; charset=utf-8".equals(contentTypeHeader.getValue())
                    )) {
                BufferedReader autodiscoverReader = null;
                try {
                    autodiscoverReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
                    String line;
                    // find ews url
                    //noinspection StatementWithEmptyBody
                    while ((line = autodiscoverReader.readLine()) != null
                            && (!line.contains("<EwsUrl>"))
                            && (!line.contains("</EwsUrl>"))) {
                    }
                    if (line != null) {
                        ewsUrl = line.substring(line.indexOf("<EwsUrl>") + 8, line.indexOf("</EwsUrl>"));
                    }
                } catch (IOException e) {
                    LOGGER.debug(e);
                } finally {
                    if (autodiscoverReader != null) {
                        try {
                            autodiscoverReader.close();
                        } catch (IOException e) {
                            LOGGER.debug(e);
                        }
                    }
                }
            }

        } finally {
            ((CloseableHttpResponse) response).close();
        }
        return ewsUrl;
    }
}
