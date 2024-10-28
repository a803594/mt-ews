/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Метод создания папки.
 */
public class CreateFolderMethod extends EWSMethod {

	/**
	 * Метод обновления папки.
	 * @param parentFolderId идентификатор родительской папки
	 * @param item элемент папки
	 */
	public CreateFolderMethod(FolderId parentFolderId, Item item) {
		super("Folder", "CreateFolder");
		this.parentFolderId = parentFolderId;
		this.item = item;
	}

}
