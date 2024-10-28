/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import java.util.List;

/**
 * Скопировать элемент в другую папку.
 */
public class CopyItemMethod extends EWSMethod {

	/**
	 * Метод копирования элемента.
	 * @param itemId идентификатор элемента
	 * @param toFolderId идентификатор целевой папки
	 */
	public CopyItemMethod(ItemId itemId, FolderId toFolderId) {
		super("Item", "CopyItem");
		this.itemId = itemId;
		this.toFolderId = toFolderId;
	}

	/**
	 * Копировать элементы в целевую папку.
	 * @param itemIds список идентификаторов элементов
	 * @param toFolderId идентификатор целевой папки
	 */
	public CopyItemMethod(List<ItemId> itemIds, FolderId toFolderId) {
		super("Item", "CopyItem");
		this.itemIds = itemIds;
		this.toFolderId = toFolderId;
	}

}
