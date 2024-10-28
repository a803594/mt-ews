/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import java.util.Set;

/**
 * Найти папку EWS.
 */
public class FindFolderMethod extends EWSMethod {

	/**
	 * Найти папку обмена.
	 * @param traversal тип обхода
	 * @param baseShape исходная форма
	 * @param parentFolderId идентификатор родительской папки
	 * @param additionalProperties свойства папки
	 * @param offset начальный смещение
	 * @param maxCount максимальное количество результатов
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
	 * Найти папку обмена.
	 * @param traversal тип обхода
	 * @param baseShape базовая форма
	 * @param parentFolderId идентификатор родительской папки
	 * @param additionalProperties свойства папки
	 * @param searchExpression выражение поиска
	 * @param offset начальный смещение
	 * @param maxCount максимальное количество результатов
	 */
	public FindFolderMethod(FolderQueryTraversal traversal, BaseShape baseShape, FolderId parentFolderId,
			Set<FieldURI> additionalProperties, SearchExpression searchExpression, int offset, int maxCount) {
		this(traversal, baseShape, parentFolderId, additionalProperties, offset, maxCount);
		this.searchExpression = searchExpression;
	}

}
