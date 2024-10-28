/*
DIT
 */

package ru.mos.mostech.ews.http.request;

import ru.mos.mostech.ews.util.StringUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Пользовательский метод поиска Exchange. Не загружает полный DOM в память.
 */
public class ExchangeSearchRequest extends ExchangeDavRequest {

	protected final String searchRequest;

	/**
	 * Создайте метод поиска.
	 * @param uri URI метода
	 * @param searchRequest Запрос поиска обмена
	 */
	public ExchangeSearchRequest(String uri, String searchRequest) {
		super(uri);
		this.searchRequest = searchRequest;
	}

	protected byte[] generateRequestContent() {
		try {

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
				writer.write("<?xml version=\"1.0\"?>\n");
				writer.write("<d:searchrequest xmlns:d=\"DAV:\">\n");
				writer.write("        <d:sql>");
				writer.write(StringUtil.xmlEncode(searchRequest));
				writer.write("</d:sql>\n");
				writer.write("</d:searchrequest>");
			}
			return baos.toByteArray();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public String getMethod() {
		return "SEARCH";
	}

}