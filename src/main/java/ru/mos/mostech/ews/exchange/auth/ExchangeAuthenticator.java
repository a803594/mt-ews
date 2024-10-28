/*
DIT
 */

package ru.mos.mostech.ews.exchange.auth;

import ru.mos.mostech.ews.http.HttpClientAdapter;

import java.io.IOException;
import java.net.URI;

/**
 * Общий интерфейс для всех аутентификаторов Exchange и O365. Реализуйте этот интерфейс,
 * чтобы создать пользовательские аутентификаторы для неподдерживаемой архитектуры
 * Exchange
 */
public interface ExchangeAuthenticator {

	void setUsername(String username);

	void setPassword(String password);

	/**
	 * Аутентификация против Exchange или O365
	 * @throws IOException в случае ошибки
	 */
	void authenticate() throws IOException;

	O365Token getToken() throws IOException;

	/**
	 * Вернуть URL по умолчанию или вычисленный URL для Exchange или O365
	 * @return целевой URL
	 */
	URI getExchangeUri();

	/**
	 * Возвращает новый экземпляр HttpClientAdapter с включенным пулом для ExchangeSession
	 * @return экземпляр HttpClientAdapter
	 */
	HttpClientAdapter getHttpClientAdapter();

}
