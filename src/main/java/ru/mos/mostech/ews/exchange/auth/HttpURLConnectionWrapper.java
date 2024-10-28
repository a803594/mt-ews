/*
DIT
 */

package ru.mos.mostech.ews.exchange.auth;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Обертка для HttpURLConnection, чтобы исправить отсутствие типа содержимого и добавить
 * логирование.
 */
@Slf4j
public class HttpURLConnectionWrapper extends HttpURLConnection {

	HttpURLConnection httpURLConnection;

	HttpURLConnectionWrapper(HttpURLConnection httpURLConnection, URL url) {
		super(url);
		this.httpURLConnection = httpURLConnection;
	}

	@Override
	public void setRequestMethod(String method) throws ProtocolException {
		httpURLConnection.setRequestMethod(method);
	}

	@Override
	public void setInstanceFollowRedirects(boolean followRedirects) {
		httpURLConnection.setInstanceFollowRedirects(followRedirects);
	}

	@Override
	public boolean getInstanceFollowRedirects() {
		return httpURLConnection.getInstanceFollowRedirects();
	}

	@Override
	public String getRequestMethod() {
		return httpURLConnection.getRequestMethod();
	}

	@Override
	public int getResponseCode() throws IOException {
		return httpURLConnection.getResponseCode();
	}

	@Override
	public String getResponseMessage() throws IOException {
		return httpURLConnection.getResponseMessage();
	}

	@Override
	public Map<String, List<String>> getHeaderFields() {
		log.debug("{}", httpURLConnection.getHeaderFields());
		return httpURLConnection.getHeaderFields();
	}

	@Override
	public String getHeaderField(String name) {
		return httpURLConnection.getHeaderField(name);
	}

	@Override
	public String getHeaderField(int n) {
		return httpURLConnection.getHeaderField(n);
	}

	@Override
	public void disconnect() {
		httpURLConnection.disconnect();
	}

	@Override
	public void setDoOutput(boolean dooutput) {
		httpURLConnection.setDoOutput(dooutput);
	}

	@Override
	public boolean usingProxy() {
		return httpURLConnection.usingProxy();
	}

	@Override
	public void connect() throws IOException {
		try {
			httpURLConnection.connect();
		}
		catch (IOException e) {
			log.error(e.getMessage());
			throw e;
		}
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return httpURLConnection.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return httpURLConnection.getOutputStream();
	}

	@Override
	public InputStream getErrorStream() {
		return httpURLConnection.getErrorStream();
	}

	@Override
	public void setRequestProperty(String key, String value) {
		httpURLConnection.setRequestProperty(key, value);
	}

	@Override
	public void addRequestProperty(String key, String value) {
		httpURLConnection.setRequestProperty(key, value);
	}

	@Override
	public Map<String, List<String>> getRequestProperties() {
		return httpURLConnection.getRequestProperties();
	}

	@Override
	public String getRequestProperty(String key) {
		return httpURLConnection.getRequestProperty(key);
	}

	/**
	 * Исправить отсутствующий тип контента
	 * @return тип контента или text/html, если отсутствует
	 */
	@Override
	public String getContentType() {
		final String contentType = httpURLConnection.getContentType();
		// workaround for missing content type
		if (contentType == null && getContentLength() > 0) {
			log.debug("Fix missing content-type at " + url.toString());
			return "text/html";
		}
		return contentType;
	}

}
