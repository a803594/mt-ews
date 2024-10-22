package ru.mos.mostech.ews.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.mos.mostech.ews.util.MdcUserPathUtils;

import java.io.IOException;

public class MdcHttpHandler implements HttpHandler {

	private final HttpHandler delegate;

	public MdcHttpHandler(HttpHandler delegate) {
		this.delegate = delegate;
	}

	@Override
	public void handle(HttpExchange httpExchange) throws IOException {
		try {
			MdcUserPathUtils.init(httpExchange.getRemoteAddress().getPort());
			delegate.handle(httpExchange);
		}
		finally {
			MdcUserPathUtils.clear();
		}
	}

}
