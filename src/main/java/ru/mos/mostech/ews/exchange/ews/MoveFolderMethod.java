/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Create Folder method.
 */

@Slf4j
public class MoveFolderMethod extends EWSMethod {
    /**
     * Move folder to target folder.
     *
     * @param folderId   folder id
     * @param toFolderId target folder id
     */
    public MoveFolderMethod(FolderId folderId, FolderId toFolderId) {
        super("Folder", "MoveFolder");
        this.folderId = folderId;
        this.toFolderId = toFolderId;
    }
}
