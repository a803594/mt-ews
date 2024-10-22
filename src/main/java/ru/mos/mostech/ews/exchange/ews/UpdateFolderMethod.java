/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import java.util.List;

/**
 * Update Folder method.
 */
public class UpdateFolderMethod extends EWSMethod {

	/**
	 * Update folder options.
	 * @param folderId folder id
	 * @param updates folder properties updates
	 */
	public UpdateFolderMethod(FolderId folderId, List<FieldUpdate> updates) {
		super("Folder", "UpdateFolder");
		this.folderId = folderId;
		this.updates = updates;
	}

}