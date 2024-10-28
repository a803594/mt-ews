/*
DIT
 */
package ru.mos.mostech.ews.exchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Базовый класс для VCalendar, VTimezone, VEvent.
 */
public class VObject {

	/**
	 * Свойства VObject
	 */
	ArrayList<VProperty> properties;

	/**
	 * Внутренние VObjects (например, VEVENT, VALARM, ...)
	 */
	ArrayList<VObject> vObjects;

	/**
	 * Базовое имя объекта (VCALENDAR, VEVENT, VCARD...).
	 */
	public String type;

	/**
	 * Создает VObject с заданным типом
	 * @param beginProperty первая строка свойства
	 * @param reader потоковый считыватель сразу после строки BEGIN:TYPE
	 * @throws IOException в случае ошибки
	 */
	public VObject(VProperty beginProperty, BufferedReader reader) throws IOException {
		if (!"BEGIN".equals(beginProperty.getKey())) {
			throw new IOException("Invalid first line: " + beginProperty);
		}
		type = beginProperty.getValue();
		String beginLine = "BEGIN:" + type;
		String endLine = "END:" + type;
		String line = reader.readLine();
		while (line != null && !line.startsWith(endLine)) {
			// ignore invalid BEGIN line inside object (Sogo Carddav issue)
			if (!beginLine.equals(line)) {
				handleLine(line, reader);
			}
			line = reader.readLine();
		}
		// ignore missing END:VCALENDAR line on modified occurrences
	}

	/**
	 * Создать VObject из ридера.
	 * @param reader потоковый ридер сразу после строки BEGIN:TYPE
	 * @throws IOException в случае ошибки
	 */
	public VObject(BufferedReader reader) throws IOException {
		this(new VProperty(reader.readLine()), reader);
	}

	/**
	 * Создать объект VCalendar из строки;
	 * @param itemBody тело элемента
	 * @throws IOException в случае ошибки
	 */
	public VObject(String itemBody) throws IOException {
		this(new ICSBufferedReader(new StringReader(itemBody)));
	}

	/**
	 * Создать пустой объект VCalendar;
	 */
	public VObject() {
	}

	public boolean isVTimezone() {
		return "VTIMEZONE".equals(type);
	}

	public boolean isVEvent() {
		return "VEVENT".equals(type);
	}

	public boolean isVAlarm() {
		return "VALARM".equals(type);
	}

	protected void handleLine(String line, BufferedReader reader) throws IOException {
		// skip empty lines
		if (line.length() > 0) {
			VProperty property = new VProperty(line);
			// inner object
			if ("BEGIN".equals(property.getKey())) {
				addVObject(new VObject(property, reader));
			}
			else if (property.getKey() != null) {
				addProperty(property);
			}
		}
	}

	/**
	 * Добавить vObject.
	 * @param vObject внутренний объект
	 */
	public void addVObject(VObject vObject) {
		if (vObjects == null) {
			vObjects = new ArrayList<>();
		}
		vObjects.add(vObject);
	}

	/**
	 * Добавить vProperty.
	 * @param property vProperty
	 */
	public void addProperty(VProperty property) {
		if (property.getValue() != null) {
			if (properties == null) {
				properties = new ArrayList<>();
			}
			properties.add(property);
		}
	}

	/**
	 * Записать VObject в писатель.
	 * @param writer буферизованный писатель
	 */
	public void writeTo(ICSBufferedWriter writer) {
		writer.write("BEGIN:");
		writer.writeLine(type);
		if (properties != null) {
			for (VProperty property : properties) {
				writer.writeLine(property.toString());
			}
		}
		if (vObjects != null) {
			for (VObject object : vObjects) {
				object.writeTo(writer);
			}
		}
		writer.write("END:");
		writer.writeLine(type);
	}

	public String toString() {
		ICSBufferedWriter writer = new ICSBufferedWriter();
		writeTo(writer);
		return writer.toString();
	}

	/**
	 * Получить свойства VObject
	 * @return свойства
	 */
	public List<VProperty> getProperties() {
		return properties;
	}

	/**
	 * Получить vProperty по имени.
	 * @param name имя свойства
	 * @return объект свойства
	 */
	public VProperty getProperty(String name) {
		if (properties != null) {
			for (VProperty property : properties) {
				if (property.getKey() != null && property.getKey().equalsIgnoreCase(name)) {
					return property;
				}
			}

		}
		return null;
	}

	/**
	 * Получить многозначное свойство vProperty по имени.
	 * @param name имя свойства
	 * @return список свойств
	 */
	public List<VProperty> getProperties(String name) {
		List<VProperty> result = null;
		if (properties != null) {
			for (VProperty property : properties) {
				if (property.getKey() != null && property.getKey().equalsIgnoreCase(name)) {
					if (result == null) {
						result = new ArrayList<>();
					}
					result.add(property);
				}
			}

		}
		return result;
	}

	/**
	 * Получить значение vProperty по имени.
	 * @param name имя свойства
	 * @return значение свойства
	 */
	public String getPropertyValue(String name) {
		VProperty property = getProperty(name);
		if (property != null) {
			return property.getValue();
		}
		else {
			return null;
		}
	}

	/**
	 * Установить значение vProperty на vObject, удалить свойство, если значение равно
	 * null.
	 * @param name имя свойства
	 * @param value значение свойства
	 */
	public void setPropertyValue(String name, String value) {
		if (value == null) {
			removeProperty(name);
		}
		else {
			VProperty property = getProperty(name);
			if (property == null) {
				property = new VProperty(name, value);
				addProperty(property);
			}
			else {
				property.setValue(value);
			}
		}
	}

	/**
	 * Добавить значение vProperty на vObject.
	 * @param name имя свойства
	 * @param value значение свойства
	 */
	public void addPropertyValue(String name, String value) {
		if (value != null) {
			VProperty property = new VProperty(name, value);
			addProperty(property);
		}
	}

	/**
	 * Удалить vProperty из vObject.
	 * @param name имя свойства
	 */
	public void removeProperty(String name) {
		if (properties != null) {
			VProperty property = getProperty(name);
			if (property != null) {
				properties.remove(property);
			}
		}
	}

	/**
	 * Удалить объект vProperty из vObject.
	 * @param property объект
	 */
	public void removeProperty(VProperty property) {
		if (properties != null) {
			properties.remove(property);
		}
	}

	public void setType(String type) {
		this.type = type;
	}

}
