/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * EWS Find Folder.
 */

@Slf4j
public class FindFolderMethod extends EWSMethod {

    /**
     * Find Exchange Folder.
     *
     * @param traversal            traversal type
     * @param baseShape            base shape
     * @param parentFolderId       parent folder id
     * @param additionalProperties folder properties
     * @param offset         start offset
     * @param maxCount       maximum result count
     */
    public FindFolderMethod(FolderQueryTraversal traversal, BaseShape baseShape, FolderId parentFolderId,
                            Set<FieldURI> additionalProperties, int offset, int maxCount) {
        super("Folder", "FindFolder");
        this.traversal = traversal;
        this.baseShape = baseShape;
        this.parentFolderId = parentFolderId;
        this.additionalProperties = additionalProperties;
        this.offset = offset;
        this.maxCount = maxCount;
    }

    /**
     * Find Exchange Folder.
     *
     * @param traversal            traversal type
     * @param baseShape            base shape
     * @param parentFolderId       parent folder id
     * @param additionalProperties folder properties
     * @param searchExpression     search expression
     * @param offset         start offset
     * @param maxCount       maximum result count
     */
    public FindFolderMethod(FolderQueryTraversal traversal, BaseShape baseShape, FolderId parentFolderId,
                            Set<FieldURI> additionalProperties, SearchExpression searchExpression, int offset, int maxCount) {
        this(traversal, baseShape, parentFolderId, additionalProperties, offset, maxCount);
        this.searchExpression = searchExpression;
    }
}
