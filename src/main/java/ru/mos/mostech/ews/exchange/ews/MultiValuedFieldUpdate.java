/*
DIT
 */

package ru.mos.mostech.ews.exchange.ews;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

/**
 * Специальный класс обновления поля для обработки нескольких значений участников
 */
public class MultiValuedFieldUpdate extends FieldUpdate {

	ArrayList<String> values = new ArrayList<>();

	/**
	 * Создать обновление поля с значением.
	 * @param fieldURI целевое поле
	 */
	public MultiValuedFieldUpdate(FieldURI fieldURI) {
		this.fieldURI = fieldURI;
	}

	/**
	 * Добавить одно значение
	 * @param value значение
	 */
	public void addValue(String value) {
		values.add(value);
	}

	/**
	 * Записать поле в писатель запроса.
	 * @param itemType тип элемента
	 * @param writer писатель запроса
	 * @throws IOException при ошибке
	 */
	@Override
	public void write(String itemType, Writer writer) throws IOException {
		String action;
		// noinspection VariableNotUsedInsideIf
		if (values.isEmpty()) {
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
		if (itemType != null || (!values.isEmpty())) {
			StringBuilder buffer = new StringBuilder();
			((UnindexedFieldURI) fieldURI).appendValues(buffer, itemType, values);
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
