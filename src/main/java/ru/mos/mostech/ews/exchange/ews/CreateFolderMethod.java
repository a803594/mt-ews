/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Create Folder method.
 */
public class CreateFolderMethod extends EWSMethod {
    /**
     * Update folder method.
     *
     * @param parentFolderId parent folder id
     * @param item           folder item
     */
    public CreateFolderMethod(FolderId parentFolderId, Item item) {
        super("Folder", "CreateFolder");
        this.parentFolderId = parentFolderId;
        this.item = item;
    }
}
