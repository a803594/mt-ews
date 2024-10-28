/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import java.io.IOException;
import java.io.Writer;

/**
 * Обновление поля
 */
public class FieldUpdate {

	FieldURI fieldURI;

	String value;

	/**
	 * Создать обновление поля с заданным значением.
	 * @param fieldURI целевое поле
	 * @param value значение поля
	 */
	public FieldUpdate(FieldURI fieldURI, String value) {
		this.fieldURI = fieldURI;
		this.value = value;
	}

	protected FieldUpdate() {
		// empty constructor for subclass
	}

	/**
	 * Записать поле в писатель запроса.
	 * @param itemType тип элемента
	 * @param writer писатель запроса
	 * @throws IOException при ошибке
	 */
	public void write(String itemType, Writer writer) throws IOException {
		String action;
		// noinspection VariableNotUsedInsideIf
		if (value == null || value.length() == 0) {
			action = "Delete";
		}
		else {
			action = "Set";
		}
		if (itemType != null) {
			writer.write("<t:");
			writer.write(action);
			writer.write(itemType);
			writer.write("Field>");
		}

		// do not try to set empty value on create
		if (itemType != null || (value != null && value.length() > 0)) {
			StringBuilder buffer = new StringBuilder();
			if (value == null || value.length() == 0) {
				fieldURI.appendTo(buffer);
			}
			else {
				fieldURI.appendValue(buffer, itemType, value);
			}
			writer.write(buffer.toString());
		}

		if (itemType != null) {
			writer.write("</t:");
			writer.write(action);
			writer.write(itemType);
			writer.write("Field>");
		}
	}

}
