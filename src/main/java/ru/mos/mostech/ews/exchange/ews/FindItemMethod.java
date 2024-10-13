/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * EWS Find Item Method.
 */
public class FindItemMethod extends EWSMethod {
    /**
     * Find item method.
     *
     * @param traversal      folder traversal mode
     * @param baseShape      base item shape
     * @param parentFolderId parent folder id
     * @param offset         start offset
     * @param maxCount       maximum result count
     */
    public FindItemMethod(FolderQueryTraversal traversal, BaseShape baseShape, FolderId parentFolderId, int offset, int maxCount) {
        super("Item", "FindItem");
        this.traversal = traversal;
        this.baseShape = baseShape;
        this.parentFolderId = parentFolderId;
        this.offset = offset;
        this.maxCount = maxCount;
    }
}

