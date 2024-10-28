/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import java.util.List;

/**
 * Метод перемещения элемента.
 */
public class MoveItemMethod extends EWSMethod {

	/**
	 * Переместить элемент в целевую папку.
	 * @param itemId идентификатор элемента
	 * @param toFolderId идентификатор целевой папки
	 */
	public MoveItemMethod(ItemId itemId, FolderId toFolderId) {
		super("Item", "MoveItem");
		this.itemId = itemId;
		this.toFolderId = toFolderId;
	}

	/**
	 * Переместить элементы в целевую папку.
	 * @param itemIds список идентификаторов элементов
	 * @param toFolderId идентификатор целевой папки
	 */
	public MoveItemMethod(List<ItemId> itemIds, FolderId toFolderId) {
		super("Item", "MoveItem");
		this.itemIds = itemIds;
		this.toFolderId = toFolderId;
	}

}
