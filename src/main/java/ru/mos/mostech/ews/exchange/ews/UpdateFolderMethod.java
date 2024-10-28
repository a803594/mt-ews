/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import java.util.List;

/**
 * Метод обновления папки.
 */
public class UpdateFolderMethod extends EWSMethod {

	/**
	 * Обновить параметры папки.
	 * @param folderId идентификатор папки
	 * @param updates обновления свойств папки
	 */
	public UpdateFolderMethod(FolderId folderId, List<FieldUpdate> updates) {
		super("Folder", "UpdateFolder");
		this.folderId = folderId;
		this.updates = updates;
	}

}