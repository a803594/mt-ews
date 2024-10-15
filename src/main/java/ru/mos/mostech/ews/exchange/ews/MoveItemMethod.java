/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Move Item method.
 */

@Slf4j
public class MoveItemMethod extends EWSMethod {
    /**
     * Move item to target folder.
     *
     * @param itemId     item id
     * @param toFolderId target folder id
     */
    public MoveItemMethod(ItemId itemId, FolderId toFolderId) {
        super("Item", "MoveItem");
        this.itemId = itemId;
        this.toFolderId = toFolderId;
    }

    /**
     * Move items to target folder.
     *
     * @param itemIds    item id list
     * @param toFolderId target folder id
     */
    public MoveItemMethod(List<ItemId> itemIds, FolderId toFolderId) {
        super("Item", "MoveItem");
        this.itemIds = itemIds;
        this.toFolderId = toFolderId;
    }
}
