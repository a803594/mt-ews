/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

/**
 * Item id.
 */
public class ItemId implements Serializable {

	protected final String name;

	protected final String id;

	protected final String changeKey;

	/**
	 * Build Item id from response item.
	 * @param item response item
	 */
	public ItemId(EWSMethod.Item item) {
		this("ItemId", item);
	}

	/**
	 * Build Item id object from item id.
	 * @param itemId item id
	 */
	public ItemId(String itemId) {
		this("ItemId", itemId);
	}

	/**
	 * Build Item id from response item.
	 * @param name item name
	 * @param item response item
	 */
	public ItemId(String name, EWSMethod.Item item) {
		this.name = name;
		this.id = item.get("ItemId");
		this.changeKey = item.get("ChangeKey");
	}

	/**
	 * Build Item id object from item id.
	 * @param name item name
	 * @param itemId item id
	 */
	public ItemId(String name, String itemId) {
		this.name = name;
		this.id = itemId;
		this.changeKey = null;
	}

	/**
	 * Build Item id object from item id and change key.
	 * @param name item name
	 * @param itemId item id
	 * @param changeKey change key
	 */
	public ItemId(String name, String itemId, String changeKey) {
		this.name = name;
		this.id = itemId;
		this.changeKey = changeKey;
	}

	/**
	 * Write item id as XML.
	 * @param writer request writer
	 * @throws IOException on error
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
