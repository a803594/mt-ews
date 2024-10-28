/*
DIT
 */
package ru.mos.mostech.ews.exchange.dav;

/**
 * Значение свойства.
 */
public class PropertyValue {

	protected final String namespaceUri;

	protected final String name;

	protected final String xmlEncodedValue;

	protected final String typeString;

	/**
	 * Создать значение свойства Dav.
	 * @param namespaceUri пространство имен свойства
	 * @param name имя свойства
	 */
	public PropertyValue(String namespaceUri, String name) {
		this(namespaceUri, name, null, null);
	}

	/**
	 * Создание значения свойства Dav.
	 * @param namespaceUri пространство имен свойства
	 * @param name имя свойства
	 * @param xmlEncodedValue xml закодированное значение
	 */
	public PropertyValue(String namespaceUri, String name, String xmlEncodedValue) {
		this(namespaceUri, name, xmlEncodedValue, null);
	}

	/**
	 * Создать значение свойства Dav.
	 * @param namespaceUri пространство имен свойства
	 * @param name имя свойства
	 * @param xmlEncodedValue xml закодированное значение
	 * @param typeString тип свойства
	 */
	public PropertyValue(String namespaceUri, String name, String xmlEncodedValue, String typeString) {
		this.namespaceUri = namespaceUri;
		this.name = name;
		this.xmlEncodedValue = xmlEncodedValue;
		this.typeString = typeString;
	}

	/**
	 * Получить пространство имен свойства.
	 * @return пространство имен свойства
	 */
	public String getNamespaceUri() {
		return namespaceUri;
	}

	/**
	 * Получить значение в формате xml.
	 * @return Значение в формате xml
	 */
	public String getXmlEncodedValue() {
		return xmlEncodedValue;
	}

	/**
	 * Получить тип свойства.
	 * @return тип свойства
	 */
	public String getTypeString() {
		return typeString;
	}

	/**
	 * Получить имя свойства.
	 * @return имя свойства
	 */
	public String getName() {
		return name;
	}

}
