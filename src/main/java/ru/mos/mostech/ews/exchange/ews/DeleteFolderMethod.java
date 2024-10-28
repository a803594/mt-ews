/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Метод удаления папки.
 */
public class DeleteFolderMethod extends EWSMethod {

	/**
	 * Метод удаления папки.
	 * @param folderId идентификатор папки
	 */
	public DeleteFolderMethod(FolderId folderId) {
		super("Folder", "DeleteFolder");
		this.folderId = folderId;
		this.deleteType = Disposal.HardDelete;
	}

}
