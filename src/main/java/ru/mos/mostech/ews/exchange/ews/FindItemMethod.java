/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Метод поиска элемента EWS.
 */
public class FindItemMethod extends EWSMethod {

	/**
	 * Метод поиска элемента.
	 * @param traversal режим обхода папки
	 * @param baseShape форма базового элемента
	 * @param parentFolderId идентификатор родительской папки
	 * @param offset начальный смещение
	 * @param maxCount максимальное количество результатов
	 */
	public FindItemMethod(FolderQueryTraversal traversal, BaseShape baseShape, FolderId parentFolderId, int offset,
			int maxCount) {
		super("Item", "FindItem");
		this.traversal = traversal;
		this.baseShape = baseShape;
		this.parentFolderId = parentFolderId;
		this.offset = offset;
		this.maxCount = maxCount;
	}

}
