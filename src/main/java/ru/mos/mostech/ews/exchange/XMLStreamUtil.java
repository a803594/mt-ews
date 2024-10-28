/*
DIT
 */
package ru.mos.mostech.ews.exchange;

import lombok.extern.slf4j.Slf4j;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Утилиты методов XmlStreamReader
 */
@Slf4j
public final class XMLStreamUtil {

	private XMLStreamUtil() {
	}

	/**
	 * Создать новый XMLInputFactory.
	 * @return XML-фаабрики ввода
	 */
	public static XMLInputFactory getXmlInputFactory() {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		inputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
		inputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
		inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
		inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
		// Woodstox 5.2.0 or later
		if (inputFactory.isPropertySupported("com.ctc.wstx.allowXml11EscapedCharsInXml10")) {
			inputFactory.setProperty("com.ctc.wstx.allowXml11EscapedCharsInXml10", Boolean.TRUE);
		}
		return inputFactory;
	}

	/**
	 * Преобразование XML-потока в карту записей. Запись также является картой
	 * ключ/значение
	 * @param inputStream xml входной поток
	 * @param rowName имя тега xml записей
	 * @param idName имя тега xml атрибута записи, используемого в качестве ключа в
	 * основной карте
	 * @return карта записей
	 * @throws IOException в случае ошибки
	 */
	public static Map<String, Map<String, String>> getElementContentsAsMap(InputStream inputStream, String rowName,
			String idName) throws IOException {
		Map<String, Map<String, String>> results = new HashMap<>();
		Map<String, String> item = null;
		String currentElement = null;
		XMLStreamReader reader = null;
		try {
			XMLInputFactory inputFactory = getXmlInputFactory();
			reader = inputFactory.createXMLStreamReader(inputStream);
			while (reader.hasNext()) {
				int event = reader.next();
				if (event == XMLStreamConstants.START_ELEMENT && rowName.equals(reader.getLocalName())) {
					item = new HashMap<>();
				}
				else if (event == XMLStreamConstants.END_ELEMENT && rowName.equals(reader.getLocalName())) {
					if (item != null && item.containsKey(idName)) {
						results.put(item.get(idName).toLowerCase(), item);
					}
					item = null;
				}
				else if (event == XMLStreamConstants.START_ELEMENT && item != null) {
					currentElement = reader.getLocalName();
				}
				else if (event == XMLStreamConstants.CHARACTERS && currentElement != null) {
					String text = reader.getText();
					if (item != null) {
						item.put(currentElement, text);
					}
					currentElement = null;
				}
			}
		}
		catch (XMLStreamException e) {
			throw new IOException(e.getMessage());
		}
		finally {
			try {
				if (reader != null) {
					reader.close();
				}
			}
			catch (XMLStreamException e) {
				log.error("", e);
			}
		}
		return results;
	}

	/**
	 * Проверяет, находится ли считыватель на начале тега с именем tagLocalName.
	 * @param reader xml потоковый считыватель
	 * @param tagLocalName локальное имя тега
	 * @return true, если считыватель находится на начале тега с именем tagLocalName
	 */
	public static boolean isStartTag(XMLStreamReader reader, String tagLocalName) {
		return (reader.getEventType() == XMLStreamConstants.START_ELEMENT)
				&& (reader.getLocalName().equals(tagLocalName));
	}

	/**
	 * Проверка, находится ли читалка на начальном теге.
	 * @param reader xml потоковый читатель
	 * @return true, если читалка находится на начальном теге
	 */
	public static boolean isStartTag(XMLStreamReader reader) {
		return (reader.getEventType() == XMLStreamConstants.START_ELEMENT);
	}

	/**
	 * Проверяет, находится ли reader на закрывающем теге с именем tagLocalName.
	 * @param reader xml потоковый чтение
	 * @param tagLocalName локальное имя тега
	 * @return true, если reader находится на закрывающем теге с именем tagLocalName
	 */
	public static boolean isEndTag(XMLStreamReader reader, String tagLocalName) {
		return (reader.getEventType() == XMLStreamConstants.END_ELEMENT)
				&& (reader.getLocalName().equals(tagLocalName));
	}

	/**
	 * Создает XML потоковый обозреватель для массива байтов
	 * @param xmlContent XML содержимое в виде массива байтов
	 * @return XML потоковый обозреватель
	 * @throws XMLStreamException в случае ошибки
	 */
	public static XMLStreamReader createXMLStreamReader(byte[] xmlContent) throws XMLStreamException {
		return createXMLStreamReader(new ByteArrayInputStream(xmlContent));
	}

	/**
	 * Создать XML потоковый ридер для строки
	 * @param xmlContent XML содержимое в виде строки
	 * @return XML потоковый ридер
	 * @throws XMLStreamException в случае ошибки
	 */
	public static XMLStreamReader createXMLStreamReader(String xmlContent) throws XMLStreamException {
		XMLInputFactory xmlInputFactory = XMLStreamUtil.getXmlInputFactory();
		return xmlInputFactory.createXMLStreamReader(new StringReader(xmlContent));
	}

	/**
	 * Создать читатель XML потока для inputStream
	 * @param inputStream xml содержимое inputStream
	 * @return читатель XML потока
	 * @throws XMLStreamException при ошибке
	 */
	public static XMLStreamReader createXMLStreamReader(InputStream inputStream) throws XMLStreamException {
		XMLInputFactory xmlInputFactory = XMLStreamUtil.getXmlInputFactory();
		return xmlInputFactory.createXMLStreamReader(inputStream);
	}

	/**
	 * Получить текст элемента.
	 * @param reader потоковый считыватель
	 * @return текст элемента
	 */
	public static String getElementText(XMLStreamReader reader) {
		String value = null;
		try {
			value = reader.getElementText();
		}
		catch (XMLStreamException | RuntimeException e) {
			// RuntimeException: probably com.ctc.wstx.exc.WstxLazyException on invalid
			// character sequence
			log.warn(e.getMessage());
		}

		return value;
	}

}
