/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

/**
 * Обновление поля с несколькими значениями.
 */
public class IndexedFieldUpdate extends FieldUpdate {

	final Set<FieldUpdate> updates = new HashSet<>();

	protected final String collectionName;

	/**
	 * Создать объект обновления индексируемого поля.
	 * @param collectionName имя коллекции значений, напр. EmailAddresses
	 */
	public IndexedFieldUpdate(String collectionName) {
		this.collectionName = collectionName;
	}

	/**
	 * Добавить значение индексированного поля.
	 * @param fieldUpdate объект обновления поля
	 */
	public void addFieldValue(FieldUpdate fieldUpdate) {
		updates.add(fieldUpdate);
	}

	/**
	 * Записать поле в писатель запроса.
	 * @param itemType тип элемента
	 * @param writer писатель запроса
	 * @throws IOException в случае ошибки
	 */
	@Override
	public void write(String itemType, Writer writer) throws IOException {
		if (itemType == null) {
			// check if at least one non null value
			boolean hasValue = false;
			for (FieldUpdate fieldUpdate : updates) {
				if (fieldUpdate.value != null) {
					hasValue = true;
					break;
				}
			}
			if (hasValue) {
				// use collection name on create
				writer.write("<t:");
				writer.write(collectionName);
				writer.write(">");

				StringBuilder buffer = new StringBuilder();
				for (FieldUpdate fieldUpdate : updates) {
					fieldUpdate.fieldURI.appendValue(buffer, null, fieldUpdate.value);
				}
				writer.write(buffer.toString());

				writer.write("</t:");
				writer.write(collectionName);
				writer.write(">");
			}
		}
		else {
			// on update, write each fieldupdate
			for (FieldUpdate fieldUpdate : updates) {
				fieldUpdate.write(itemType, writer);
			}
		}
	}

}
