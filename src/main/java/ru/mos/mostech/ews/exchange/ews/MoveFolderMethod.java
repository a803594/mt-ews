/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Метод создания папки.
 */
public class MoveFolderMethod extends EWSMethod {

	/**
	 * Переместить папку в целевую папку.
	 * @param folderId идентификатор папки
	 * @param toFolderId идентификатор целевой папки
	 */
	public MoveFolderMethod(FolderId folderId, FolderId toFolderId) {
		super("Folder", "MoveFolder");
		this.folderId = folderId;
		this.toFolderId = toFolderId;
	}

}
