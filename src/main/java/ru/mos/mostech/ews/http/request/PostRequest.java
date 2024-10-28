/*
DIT
 */

package ru.mos.mostech.ews.http.request;

import org.apache.http.*;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.message.BasicNameValuePair;
import ru.mos.mostech.ews.http.HttpClientAdapter;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

/**
 * HTTP POST запрос с обработчиком строкового ответа.
 */
public class PostRequest extends HttpPost implements ResponseHandler<String>, ResponseWrapper {

	private ArrayList<NameValuePair> parameters = new ArrayList<>();

	private String responseBodyAsString = null;

	private HttpResponse response;

	public PostRequest(final URI uri) {
		super(uri);
	}

	public PostRequest(final String url) {
		super(URI.create(url));
	}

	public void setRequestHeader(String name, String value) {
		setHeader(name, value);
	}

	@Override
	public HttpEntity getEntity() {
		return new UrlEncodedFormEntity(parameters, Consts.UTF_8);
	}

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

	public void setParameter(final String name, final String value) {
		parameters.add(new BasicNameValuePair(name, value));
	}

	public void removeParameter(final String name) {
		ArrayList<NameValuePair> toDelete = new ArrayList<>();
		for (NameValuePair param : parameters) {
			if (param.getName().equals(name)) {
				toDelete.add(param);
			}
		}
		parameters.removeAll(toDelete);
	}

	public ArrayList<NameValuePair> getParameters() {
		return parameters;
	}

	public String getResponseBodyAsString() throws IOException {
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
	 * Получить код состояния из ответа.
	 * @return Код состояния HTTP
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
