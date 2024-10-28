/*
DIT
 */

package ru.mos.mostech.ews.exchange.ews;

import java.io.IOException;
import java.io.Writer;

public class OccurrenceItemId extends ItemId {

	protected final int instanceIndex;

	/**
	 * Создать объект идентификатора элемента из идентификатора элемента и ключа
	 * изменения.
	 * @param recurringMasterId идентификатор повторяющегося мастера
	 * @param instanceIndex индекс экземпляра
	 */
	public OccurrenceItemId(String recurringMasterId, int instanceIndex) {
		super("OccurrenceItemId", recurringMasterId);
		this.instanceIndex = instanceIndex;
	}

	/**
	 * Создать объект id элемента из id элемента и ключа изменения.
	 * @param recurringMasterId id основного элемента
	 * @param changeKey ключ изменения
	 * @param instanceIndex индекс вхождения
	 */
	public OccurrenceItemId(String recurringMasterId, String changeKey, int instanceIndex) {
		super("OccurrenceItemId", recurringMasterId, changeKey);
		this.instanceIndex = instanceIndex;
	}

	/**
	 * Записать идентификатор элемента в формате XML.
	 * @param writer писатель запроса
	 * @throws IOException в случае ошибки
	 */
	public void write(Writer writer) throws IOException {
		writer.write("<t:");
		writer.write(name);
		writer.write(" RecurringMasterId=\"");
		writer.write(id);
		if (changeKey != null) {
			writer.write("\" ChangeKey=\"");
			writer.write(changeKey);
		}
		writer.write("\" InstanceIndex=\"");
		writer.write(String.valueOf(instanceIndex));
		writer.write("\"/>");
	}

}
