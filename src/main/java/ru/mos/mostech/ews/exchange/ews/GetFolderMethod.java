/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import java.util.Set;

/**
 * Метод EWS GetFolder.
 */
public class GetFolderMethod extends EWSMethod {

	/**
	 * Получить метод папки.
	 * @param baseShape запрашиваемая базовая форма
	 * @param folderId id папки
	 * @param additionalProperties дополнительные запрашиваемые свойства
	 */
	public GetFolderMethod(BaseShape baseShape, FolderId folderId, Set<FieldURI> additionalProperties) {
		super("Folder", "GetFolder");
		this.baseShape = baseShape;
		this.folderId = folderId;
		this.additionalProperties = additionalProperties;
	}

}
