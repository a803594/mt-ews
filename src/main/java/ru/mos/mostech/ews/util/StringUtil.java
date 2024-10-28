/*
DIT
 */
package ru.mos.mostech.ews.util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Различные методы обработки строк
 */
public final class StringUtil {

	private StringUtil() {
	}

	/**
	 * Возвращает подстроку между startDelimiter и endDelimiter или null.
	 * @param value Строковое значение
	 * @param startDelimiter начальный разделитель
	 * @param endDelimiter конечный разделитель
	 * @return значение токена
	 */
	public static String getToken(String value, String startDelimiter, String endDelimiter) {
		String token = null;
		if (value != null) {
			int startIndex = value.indexOf(startDelimiter);
			if (startIndex >= 0) {
				startIndex += startDelimiter.length();
				int endIndex = value.indexOf(endDelimiter, startIndex);
				if (endIndex >= 0) {
					token = value.substring(startIndex, endIndex);
				}
			}
		}
		return token;
	}

	/**
	 * Вернуть подстроку между стартовым и конечным разделителем или null, искать
	 * последний токен в строке.
	 * @param value строковое значение
	 * @param startDelimiter стартовый разделитель
	 * @param endDelimiter конечный разделитель
	 * @return значение токена
	 */
	public static String getLastToken(String value, String startDelimiter, String endDelimiter) {
		String token = null;
		if (value != null) {
			int startIndex = value.lastIndexOf(startDelimiter);
			if (startIndex >= 0) {
				startIndex += startDelimiter.length();
				int endIndex = value.indexOf(endDelimiter, startIndex);
				if (endIndex >= 0) {
					token = value.substring(startIndex, endIndex);
				}
			}
		}
		return token;
	}

	/**
	 * Вернуть подстроку между начальным разделителем и конечным разделителем с новым
	 * токеном.
	 * @param value Строковое значение
	 * @param startDelimiter начальный разделитель
	 * @param endDelimiter конечный разделитель
	 * @param newToken новое значение токена
	 * @return значение токена
	 */
	public static String replaceToken(String value, String startDelimiter, String endDelimiter, String newToken) {
		String result = null;
		if (value != null) {
			int startIndex = value.indexOf(startDelimiter);
			if (startIndex >= 0) {
				startIndex += startDelimiter.length();
				int endIndex = value.indexOf(endDelimiter, startIndex);
				if (endIndex >= 0) {
					result = value.substring(0, startIndex) + newToken + value.substring(endIndex);
				}
			}
		}
		return result;
	}

	/**
	 * Объединить значения с указанным разделителем.
	 * @param values набор значений
	 * @param separator разделитель
	 * @return объединенные значения
	 */
	public static String join(Set<String> values, String separator) {
		if (values != null && !values.isEmpty()) {
			StringBuilder result = new StringBuilder();
			for (String value : values) {
				if (result.length() > 0) {
					result.append(separator);
				}
				result.append(value);
			}
			return result.toString();
		}
		else {
			return null;
		}
	}

	static class PatternMap {

		protected String match;

		protected String value;

		protected Pattern pattern;

		protected PatternMap(String match, String value) {
			this.match = match;
			this.value = value;
			pattern = Pattern.compile(match);
		}

		protected PatternMap(String match, String escapedMatch, String value) {
			this.match = match;
			this.value = value;
			pattern = Pattern.compile(escapedMatch);
		}

		protected PatternMap(String match, Pattern pattern, String value) {
			this.match = match;
			this.value = value;
			this.pattern = pattern;
		}

		protected String replaceAll(String string) {
			if (string != null && string.contains(match)) {
				return pattern.matcher(string).replaceAll(value);
			}
			else {
				return string;
			}
		}

	}

	private static final Pattern AMP_PATTERN = Pattern.compile("&");

	private static final Pattern PLUS_PATTERN = Pattern.compile("\\+");

	private static final Pattern QUOTE_PATTERN = Pattern.compile("\"");

	private static final Pattern CR_PATTERN = Pattern.compile("\r");

	private static final Pattern LF_PATTERN = Pattern.compile("\n");

	private static final List<PatternMap> URLENCODED_PATTERNS = new ArrayList<>();
	static {
		URLENCODED_PATTERNS.add(new PatternMap(String.valueOf((char) 0xF8FF), "_xF8FF_"));
		URLENCODED_PATTERNS.add(new PatternMap("%26", "&"));
		URLENCODED_PATTERNS.add(new PatternMap("%2B", "+"));
		URLENCODED_PATTERNS.add(new PatternMap("%3A", ":"));
		URLENCODED_PATTERNS.add(new PatternMap("%3B", ";"));
		URLENCODED_PATTERNS.add(new PatternMap("%3C", "<"));
		URLENCODED_PATTERNS.add(new PatternMap("%3E", ">"));
		URLENCODED_PATTERNS.add(new PatternMap("%22", "\""));
		URLENCODED_PATTERNS.add(new PatternMap("%23", "#"));
		URLENCODED_PATTERNS.add(new PatternMap("%2A", "*"));
		URLENCODED_PATTERNS.add(new PatternMap("%7C", "|"));
		URLENCODED_PATTERNS.add(new PatternMap("%3F", "?"));
		URLENCODED_PATTERNS.add(new PatternMap("%7E", "~"));

		// CRLF is replaced with LF in response
		URLENCODED_PATTERNS.add(new PatternMap("\n", "_x000D__x000A_"));

		// last replace %
		URLENCODED_PATTERNS.add(new PatternMap("%25", "%"));
	}

	private static final List<PatternMap> URLENCODE_PATTERNS = new ArrayList<>();
	static {
		// first replace %
		URLENCODE_PATTERNS.add(new PatternMap("%", "%25"));

		URLENCODE_PATTERNS.add(new PatternMap("_xF8FF_", String.valueOf((char) 0xF8FF)));
		URLENCODE_PATTERNS.add(new PatternMap("&", AMP_PATTERN, "%26"));
		URLENCODE_PATTERNS.add(new PatternMap("+", PLUS_PATTERN, "%2B"));
		URLENCODE_PATTERNS.add(new PatternMap(":", "%3A"));
		URLENCODE_PATTERNS.add(new PatternMap(";", "%3B"));
		URLENCODE_PATTERNS.add(new PatternMap("<", "%3C"));
		URLENCODE_PATTERNS.add(new PatternMap(">", "%3E"));
		URLENCODE_PATTERNS.add(new PatternMap("\"", "%22"));
		URLENCODE_PATTERNS.add(new PatternMap("#", "%23"));
		URLENCODE_PATTERNS.add(new PatternMap("~", "%7E"));
		URLENCODE_PATTERNS.add(new PatternMap("*", "\\*", "%2A"));
		URLENCODE_PATTERNS.add(new PatternMap("|", "\\|", "%7C"));
		URLENCODE_PATTERNS.add(new PatternMap("?", "\\?", "%3F"));

		URLENCODE_PATTERNS.add(new PatternMap("_x000D__x000A_", "\r\n"));

	}

	private static final List<PatternMap> XML_DECODE_PATTERNS = new ArrayList<>();
	static {
		XML_DECODE_PATTERNS.add(new PatternMap("&amp;", "&"));
		XML_DECODE_PATTERNS.add(new PatternMap("&lt;", "<"));
		XML_DECODE_PATTERNS.add(new PatternMap("&gt;", ">"));
	}

	private static final List<PatternMap> XML_ENCODE_PATTERNS = new ArrayList<>();
	static {
		XML_ENCODE_PATTERNS.add(new PatternMap("&", AMP_PATTERN, "&amp;"));
		XML_ENCODE_PATTERNS.add(new PatternMap("<", "&lt;"));
		XML_ENCODE_PATTERNS.add(new PatternMap(">", "&gt;"));
	}

	private static final Pattern SLASH_PATTERN = Pattern.compile("/");

	private static final Pattern UNDERSCORE_PATTERN = Pattern.compile("_");

	private static final Pattern DASH_PATTERN = Pattern.compile("-");

	// WebDav search parameter encode
	private static final Pattern APOS_PATTERN = Pattern.compile("'");

	/**
	 * Кодирует содержимое в XML.
	 * @param name декодированное имя
	 * @return name закодированное имя
	 */
	public static String xmlEncode(String name) {
		String result = name;
		if (result != null) {
			for (PatternMap patternMap : XML_ENCODE_PATTERNS) {
				result = patternMap.replaceAll(result);
			}
		}
		return result;
	}

	/**
	 * Кодирование Xml внутри атрибута.
	 * @param name раскодированное имя
	 * @return name закодированное имя
	 */
	public static String xmlEncodeAttribute(String name) {
		String result = xmlEncode(name);
		if (result != null) {
			if (result.indexOf('"') >= 0) {
				result = QUOTE_PATTERN.matcher(result).replaceAll("&#x22;");
			}
			if (result.indexOf('\r') >= 0) {
				result = CR_PATTERN.matcher(result).replaceAll("&#x0D;");
			}
			if (result.indexOf('\n') >= 0) {
				result = LF_PATTERN.matcher(result).replaceAll("&#x0A;");
			}
		}
		return result;
	}

	/**
	 * Необходимо декодировать XML для iCal
	 * @param name закодированное имя
	 * @return name декодированное имя
	 */
	public static String xmlDecode(String name) {
		String result = name;
		if (result != null) {
			for (PatternMap patternMap : XML_DECODE_PATTERNS) {
				result = patternMap.replaceAll(result);
			}
		}
		return result;
	}

	/**
	 * Преобразовать значение base64 в шестнадцатеричное.
	 * @param value значение base64
	 * @return шестнадцатеричное значение
	 */
	@SuppressWarnings("unused")
	public static String base64ToHex(String value) {
		String hexValue = null;
		if (value != null) {
			hexValue = new String(Hex.encodeHex(Base64.decodeBase64(value.getBytes(StandardCharsets.UTF_8))));
		}
		return hexValue;
	}

	/**
	 * Преобразовать шестнадцатеричное значение в base64.
	 * @param value шестнадцатеричное значение
	 * @return значение в base64
	 * @throws DecoderException в случае ошибки
	 */
	@SuppressWarnings("unused")
	public static String hexToBase64(String value) throws DecoderException {
		String base64Value = null;
		if (value != null) {
			base64Value = new String(Base64.encodeBase64(Hex.decodeHex(value.toCharArray())), StandardCharsets.UTF_8);
		}
		return base64Value;
	}

	/**
	 * Кодирует имя элемента для получения фактического значения, хранящегося в свойстве
	 * urlcompname MAPI.
	 * @param value декодированное значение
	 * @return urlcompname закодированное значение
	 */
	public static String encodeUrlcompname(String value) {
		String result = value;
		if (result != null) {
			for (PatternMap patternMap : URLENCODE_PATTERNS) {
				result = patternMap.replaceAll(result);
			}
		}
		return result;
	}

	/**
	 * Декодировать urlcompname для получения имени элемента.
	 * @param urlcompname закодированное значение
	 * @return декодированное значение
	 */
	public static String decodeUrlcompname(String urlcompname) {
		String result = urlcompname;
		if (result != null) {
			for (PatternMap patternMap : URLENCODED_PATTERNS) {
				result = patternMap.replaceAll(result);
			}
		}
		return result;
	}

	/**
	 * Кодирует знак плюс в закодированном href. '+' декодируется как ' ' с помощью
	 * URIUtil.decode, обходное решение - сначала принудительно закодировать в '%2B'
	 * @param value закодированный href
	 * @return закодированный href
	 */
	public static String encodePlusSign(String value) {
		String result = value;
		if (result.indexOf('+') >= 0) {
			result = PLUS_PATTERN.matcher(result).replaceAll("%2B");
		}
		return result;
	}

	/**
	 * Кодирует базовое значение itemId EWS в совместимое с URL значение.
	 * @param value базовое значение в формате base64
	 * @return совместимое с URL значение
	 */
	public static String base64ToUrl(String value) {
		String result = value;
		if (result != null) {
			if (result.indexOf('+') >= 0) {
				result = PLUS_PATTERN.matcher(result).replaceAll("-");
			}
			if (result.indexOf('/') >= 0) {
				result = SLASH_PATTERN.matcher(result).replaceAll("_");
			}
		}
		return result;
	}

	/**
	 * Кодирует совместимый с EWS itemId обратно в значение base64.
	 * @param value совместимое с URL значение
	 * @return значение base64
	 */
	public static String urlToBase64(String value) {
		String result = value;
		if (result.indexOf('-') >= 0) {
			result = DASH_PATTERN.matcher(result).replaceAll("+");
		}
		if (result.indexOf('_') >= 0) {
			result = UNDERSCORE_PATTERN.matcher(result).replaceAll("/");
		}
		return result;
	}

	/**
	 * Кодирует кавычки в параметре поиска Dav.
	 * @param value параметр поиска
	 * @return экранированное значение
	 */
	public static String davSearchEncode(String value) {
		String result = value;
		if (result.indexOf('\'') >= 0) {
			result = APOS_PATTERN.matcher(result).replaceAll("''");
		}
		return result;
	}

	/**
	 * Получить значение даты на весь день из зулус-метки времени.
	 * @param value зулус-дата и время
	 * @return значение даты на весь день в формате yyyyMMdd
	 */
	public static String convertZuluDateTimeToAllDay(String value) {
		String result = value;
		if (value != null && value.length() != 8) {
			// try to convert datetime value to date value
			try {
				Calendar calendar = Calendar.getInstance();
				SimpleDateFormat dateParser = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
				calendar.setTime(dateParser.parse(value));
				calendar.add(Calendar.HOUR_OF_DAY, 12);
				SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");
				result = dateFormatter.format(calendar.getTime());
			}
			catch (ParseException e) {
				// ignore
			}
		}
		return result;
	}

	/**
	 * Удалить кавычки, если они есть в значении.
	 * @param value входное значение
	 * @return строка без кавычек
	 */
	public static String removeQuotes(String value) {
		String result = value;
		if (result != null) {
			if (result.startsWith("\"") || result.startsWith("{") || result.startsWith("(")) {
				result = result.substring(1);
			}
			if (result.endsWith("\"") || result.endsWith("}") || result.endsWith(")")) {
				result = result.substring(0, result.length() - 1);
			}
		}
		return result;
	}

}
