/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import java.util.List;

/**
 * Copy item to another folder.
 */
public class CopyItemMethod extends EWSMethod {

	/**
	 * Copy item method.
	 * @param itemId item id
	 * @param toFolderId target folder id
	 */
	public CopyItemMethod(ItemId itemId, FolderId toFolderId) {
		super("Item", "CopyItem");
		this.itemId = itemId;
		this.toFolderId = toFolderId;
	}

	/**
	 * Copy items to target folder.
	 * @param itemIds item id list
	 * @param toFolderId target folder id
	 */
	public CopyItemMethod(List<ItemId> itemIds, FolderId toFolderId) {
		super("Item", "CopyItem");
		this.itemIds = itemIds;
		this.toFolderId = toFolderId;
	}

}
