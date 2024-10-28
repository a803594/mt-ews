/*
DIT
 */

package ru.mos.mostech.ews.http.request;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import ru.mos.mostech.ews.http.HttpClientAdapter;

import java.io.IOException;
import java.net.URI;

/**
 * HTTP GET запрос с обработчиком строкового ответа.
 */
public class GetRequest extends HttpGet implements ResponseHandler<String>, ResponseWrapper {

	private HttpResponse response;

	private String responseBodyAsString;

	public GetRequest(final URI uri) {
		super(uri);
	}

	/**
	 * @throws IllegalArgumentException если uri недействителен.
	 */
	public GetRequest(final String uri) {
		super(uri);
	}

	/**
	 * Обработать ответ на запрос и вернуть ответ в виде строки. Тело ответа равно null
	 * при редиректе
	 * @param response объект ответа
	 * @return тело ответа в виде строки
	 * @throws IOException при ошибке
	 */
	@Override
	public String handleResponse(HttpResponse response) throws IOException {
		this.response = response;
		if (HttpClientAdapter.isRedirect(response)) {
			return null;
		}
		else {
			responseBodyAsString = new BasicResponseHandler().handleResponse(response);
			return responseBodyAsString;
		}
	}

	public String getResponseBodyAsString() throws IOException {
		checkResponse();
		if (responseBodyAsString == null) {
			throw new IOException("No response body available");
		}
		return responseBodyAsString;
	}

	public Header getResponseHeader(String name) {
		checkResponse();
		return response.getFirstHeader(name);
	}

	/**
	 * Получить код статуса из ответа.
	 * @return Код статуса HTTP
	 */
	public int getStatusCode() {
		checkResponse();
		return response.getStatusLine().getStatusCode();
	}

	/**
	 * Получить текст причины из ответа.
	 * @return текст причины
	 */
	public String getReasonPhrase() {
		checkResponse();
		return response.getStatusLine().getReasonPhrase();
	}

	public URI getRedirectLocation() {
		checkResponse();
		return HttpClientAdapter.getRedirectLocation(response);
	}

	public HttpResponse getHttpResponse() {
		return response;
	}

	/**
	 * Проверьте, доступен ли ответ.
	 */
	private void checkResponse() {
		if (response == null) {
			throw new IllegalStateException("Should execute request first");
		}
	}

}
