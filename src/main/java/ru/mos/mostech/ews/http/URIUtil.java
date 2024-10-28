/*
DIT
 */

package ru.mos.mostech.ews.http;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.http.Consts;

import java.io.IOException;
import java.util.BitSet;

/**
 * Реализуйте логику кодирования/декодирования для замены HttpClient 3 URIUtil
 */
public class URIUtil {

	/**
	 * Символ процента "%" всегда имеет зарезервированное назначение индикатора
	 * экранирования, он должен быть экранирован как "%25", чтобы использоваться как
	 * данные в URI.
	 */
	protected static final BitSet percent = new BitSet(256);

	// Static initializer for percent
	static {
		percent.set('%');
	}

	/**
	 * Битовый набор для цифры.
	 * <p>
	 * <blockquote><pre>
	 * цифра    = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" |
	 *            "8" | "9"
	 * </pre></blockquote>
	 * <p>
	 */
	protected static final BitSet digit = new BitSet(256);

	// Static initializer for digit
	static {
		for (int i = '0'; i <= '9'; i++) {
			digit.set(i);
		}
	}

	/**
	 * Битовый набор для альфа.
	 * <p>
	 * <blockquote><pre>
	 * alpha         = lowalpha | upalpha
	 * </pre></blockquote>
	 * <p>
	 */
	protected static final BitSet alpha = new BitSet(256);

	// Static initializer for alpha
	static {
		for (int i = 'a'; i <= 'z'; i++) {
			alpha.set(i);
		}
		for (int i = 'A'; i <= 'Z'; i++) {
			alpha.set(i);
		}
	}

	/**
	 * BitSet для алфавитно-цифровых символов (объединение алфавита и цифр).
	 * <p>
	 * <blockquote><pre>
	 *  alphanum      = alpha | digit
	 * </pre></blockquote>
	 * <p>
	 */
	protected static final BitSet alphanum = new BitSet(256);

	// Static initializer for alphanum
	static {
		alphanum.or(alpha);
		alphanum.or(digit);
	}

	/**
	 * Множество битов для шестнадцатеричной системы.
	 * <p>
	 * <blockquote><pre>
	 * hex           = digit | "A" | "B" | "C" | "D" | "E" | "F" |
	 *                         "a" | "b" | "c" | "d" | "e" | "f"
	 * </pre></blockquote>
	 * <p>
	 */
	protected static final BitSet hex = new BitSet(256);

	// Static initializer for hex
	static {
		hex.or(digit);
		for (int i = 'a'; i <= 'f'; i++) {
			hex.set(i);
		}
		for (int i = 'A'; i <= 'F'; i++) {
			hex.set(i);
		}
	}

	/**
	 * Множество битов для экранированных символов.
	 * <p>
	 * <blockquote><pre>
	 * экранированный = "%" шестнадцатеричный шестнадцатеричный
	 * </pre></blockquote>
	 * <p>
	 */
	protected static final BitSet escaped = new BitSet(256);

	// Static initializer for escaped
	static {
		escaped.or(percent);
		escaped.or(hex);
	}

	/**
	 * Битовый набор для отметки.
	 * <p>
	 * <blockquote><pre>
	 * отметка       = "-" | "_" | "." | "!" | "~" | "*" | "'" |
	 *                 "(" | ")"
	 * </pre></blockquote>
	 * <p>
	 */
	protected static final BitSet mark = new BitSet(256);

	// Static initializer for mark
	static {
		mark.set('-');
		mark.set('_');
		mark.set('.');
		mark.set('!');
		mark.set('~');
		mark.set('*');
		mark.set('\'');
		mark.set('(');
		mark.set(')');
	}

	/**
	 * Символы данных, которые допустимы в URI, но не имеют зарезервированной цели,
	 * называются незарезервированными.
	 * <p>
	 * <blockquote><pre>
	 * незарезервированные    = алфавитно-цифровые | знаки
	 * </pre></blockquote>
	 * <p>
	 */
	protected static final BitSet unreserved = new BitSet(256);

	// Static initializer for unreserved
	static {
		unreserved.or(alphanum);
		unreserved.or(mark);
	}

	/**
	 * Битовый набор для зарезервированных.
	 * <p>
	 * <blockquote><pre>
	 * зарезервированные = ";" | "/" | "?" | ":" | "@" | "&amp;" | "=" | "+" |
	 *                     "$" | ","
	 * </pre></blockquote>
	 * <p>
	 */
	protected static final BitSet reserved = new BitSet(256);

	// Static initializer for reserved
	static {
		reserved.set(';');
		reserved.set('/');
		reserved.set('?');
		reserved.set(':');
		reserved.set('@');
		reserved.set('&');
		reserved.set('=');
		reserved.set('+');
		reserved.set('$');
		reserved.set(',');
	}

	/**
	 * Набор битов для uric.
	 * <p>
	 * <blockquote><pre>
	 * uric          = зарезервировано | незарезервировано | экранировано
	 * </pre></blockquote>
	 * <p>
	 */
	protected static final BitSet uric = new BitSet(256);

	// Static initializer for uric
	static {
		uric.or(reserved);
		uric.or(unreserved);
		uric.or(escaped);
	}

	/**
	 * Набор битов для pchar.
	 * <p>
	 * <blockquote><pre>
	 * pchar         = unreserved | escaped |
	 *                 ":" | "@" | "&" | "=" | "+" | "$" | ","
	 * </pre></blockquote>
	 * <p>
	 */
	protected static final BitSet pchar = new BitSet(256);

	// Static initializer for pchar
	static {
		pchar.or(unreserved);
		pchar.or(escaped);
		pchar.set(':');
		pchar.set('@');
		pchar.set('&');
		pchar.set('=');
		pchar.set('+');
		pchar.set('$');
		pchar.set(',');
	}

	/**
	 * Массив битов для параметра (псевдоним для pchar).
	 * <p>
	 * <blockquote><pre>
	 * param         = *pchar
	 * </pre></blockquote>
	 * <p>
	 */
	protected static final BitSet param = pchar;

	/**
	 * Набор битов для сегмента.
	 * <p>
	 * <blockquote><pre>
	 * сегмент       = *pchar *( ";" параметр )
	 * </pre></blockquote>
	 * <p>
	 */
	protected static final BitSet segment = new BitSet(256);

	// Static initializer for segment
	static {
		segment.or(pchar);
		segment.set(';');
		segment.or(param);
	}

	/**
	 * Набор битов для сегментов пути.
	 * <p>
	 * <blockquote><pre>
	 * path_segments = сегмент *( "/" сегмент )
	 * </pre></blockquote>
	 * <p>
	 */
	protected static final BitSet path_segments = new BitSet(256);

	// Static initializer for path_segments
	static {
		path_segments.set('/');
		path_segments.or(segment);
	}

	/**
	 * Абсолютный путь URI.
	 * <p>
	 * <blockquote><pre>
	 * abs_path      = "/"  path_segments
	 * </pre></blockquote>
	 * <p>
	 */
	protected static final BitSet abs_path = new BitSet(256);

	// Static initializer for abs_path
	static {
		abs_path.set('/');
		abs_path.or(path_segments);
	}

	/**
	 * Те символы, которые разрешены для abs_path.
	 */
	public static final BitSet allowed_abs_path = new BitSet(256);
	static {
		allowed_abs_path.or(abs_path);
		// allowed_abs_path.set('/'); // aleady included
		allowed_abs_path.andNot(percent);
		allowed_abs_path.clear('+');
	}

	/**
	 * Те символы, которые разрешены для компонента запроса.
	 */
	public static final BitSet allowed_query = new BitSet(256);

	// Static initializer for allowed_query
	static {
		allowed_query.or(uric);
		allowed_query.clear('%');
	}

	/**
	 * Те символы, которые разрешены в компоненте запроса.
	 */
	public static final BitSet allowed_within_query = new BitSet(256);

	// Static initializer for allowed_within_query
	static {
		allowed_within_query.or(allowed_query);
		allowed_within_query.andNot(reserved); // excluded 'reserved'
	}

	/**
	 * Декодировать строку с URL-кодировкой.
	 * @param escaped закодированная строка
	 * @return декодированная строка
	 * @throws IOException в случае ошибки
	 */
	public static String decode(String escaped) throws IOException {
		try {
			return getString(URLCodec.decodeUrl(getAsciiBytes(escaped)));
		}
		catch (DecoderException e) {
			throw new IOException(e.getMessage());
		}
	}

	/**
	 * Кодировать путь URL.
	 * @param unescaped не закодированный путь
	 * @return закодированный путь
	 */
	public static String encodePath(String unescaped) {
		return encode(unescaped, allowed_abs_path);
	}

	/**
	 * Кодирует строку в формате URL.
	 * @param unescaped не закодированная строка
	 * @param allowed набор разрешенных символов
	 * @return закодированная строка
	 */
	public static String encode(String unescaped, BitSet allowed) {
		return getAsciiString(URLCodec.encodeUrl(allowed, getBytes(unescaped)));
	}

	/**
	 * Кодировать строку запроса в URL.
	 * @param unescaped не закодированная строка запроса
	 * @return закодированная строка запроса
	 */
	public static String encodeWithinQuery(String unescaped) {
		return encode(unescaped, allowed_within_query);
	}

	/**
	 * Кодирование URL для пути и строки запроса.
	 * @param unescaped не закодированный путь и строка запроса
	 * @return закодированная строка пути и строки запроса
	 */
	public static String encodePathQuery(String unescaped) {
		int at = unescaped.indexOf('?');
		if (at < 0) {
			return encode(unescaped, allowed_abs_path);
		}
		else {
			return encode(unescaped.substring(0, at), allowed_abs_path) + '?'
					+ encode(unescaped.substring(at + 1), allowed_query);
		}
	}

	public static byte[] getBytes(final String value) {
		if (value == null) {
			throw new IllegalArgumentException("Parameter may not be null");
		}

		return value.getBytes(Consts.UTF_8);
	}

	public static byte[] getAsciiBytes(final String value) {
		if (value == null) {
			throw new IllegalArgumentException("Parameter may not be null");
		}

		return value.getBytes(Consts.ASCII);
	}

	/**
	 * Преобразовать массив байтов в строковое значение ASCII.
	 * @param bytes массив байтов
	 * @return строка ASCII
	 */
	public static String getAsciiString(final byte[] bytes) {
		if (bytes == null) {
			throw new IllegalArgumentException("Parameter may not be null");
		}

		return new String(bytes, Consts.ASCII);
	}

	/**
	 * Преобразует массив байтов в строковое значение UTF-8.
	 * @param bytes массив байтов
	 * @return строка ASCII
	 */
	public static String getString(final byte[] bytes) {
		if (bytes == null) {
			throw new IllegalArgumentException("Parameter may not be null");
		}

		return new String(bytes, Consts.UTF_8);
	}

}
