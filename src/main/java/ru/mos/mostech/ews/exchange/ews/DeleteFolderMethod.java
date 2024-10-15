/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Delete Folder method.
 */

@Slf4j
public class DeleteFolderMethod extends EWSMethod {
    /**
     * Delete folder method.
     *
     * @param folderId folder id
     */
    public DeleteFolderMethod(FolderId folderId) {
        super("Folder", "DeleteFolder");
        this.folderId = folderId;
        this.deleteType = Disposal.HardDelete;
    }
}
