/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

/**
 * Идентификатор элемента.
 */
public class ItemId implements Serializable {

	protected final String name;

	protected final String id;

	protected final String changeKey;

	/**
	 * Сформировать ID элемента из полученного элемента.
	 * @param item полученный элемент
	 */
	public ItemId(EWSMethod.Item item) {
		this("ItemId", item);
	}

	/**
	 * Построить объект идентификатора элемента из идентификатора элемента.
	 * @param itemId идентификатор элемента
	 */
	public ItemId(String itemId) {
		this("ItemId", itemId);
	}

	/**
	 * Сформировать id элемента из элемента ответа.
	 * @param name имя элемента
	 * @param item элемент ответа
	 */
	public ItemId(String name, EWSMethod.Item item) {
		this.name = name;
		this.id = item.get("ItemId");
		this.changeKey = item.get("ChangeKey");
	}

	/**
	 * Построить объект id элемента из id элемента.
	 * @param name имя элемента
	 * @param itemId id элемента
	 */
	public ItemId(String name, String itemId) {
		this.name = name;
		this.id = itemId;
		this.changeKey = null;
	}

	/**
	 * Построить объект идентификатора элемента из идентификатора элемента и ключа
	 * изменений.
	 * @param name имя элемента
	 * @param itemId идентификатор элемента
	 * @param changeKey ключ изменений
	 */
	public ItemId(String name, String itemId, String changeKey) {
		this.name = name;
		this.id = itemId;
		this.changeKey = changeKey;
	}

	/**
	 * Запишите идентификатор элемента в виде XML.
	 * @param writer писатель запроса
	 * @throws IOException в случае ошибки
	 */
	public void write(Writer writer) throws IOException {
		writer.write("<t:");
		writer.write(name);
		writer.write(" Id=\"");
		writer.write(id);
		if (changeKey != null) {
			writer.write("\" ChangeKey=\"");
			writer.write(changeKey);
		}
		writer.write("\"/>");
	}

}
