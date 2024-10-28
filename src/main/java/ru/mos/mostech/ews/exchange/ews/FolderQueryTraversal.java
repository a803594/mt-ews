/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Режим поиска folderQueryTraversalType.
 */
public final class FolderQueryTraversal extends AttributeOption {

	private FolderQueryTraversal(String value) {
		super("Traversal", value);
	}

	/**
	 * Искать только в текущей папке.
	 */
	public static final FolderQueryTraversal SHALLOW = new FolderQueryTraversal("Shallow");

	/**
	 * Рекурсивный поиск.
	 */
	@SuppressWarnings({ "UnusedDeclaration" })
	public static final FolderQueryTraversal DEEP = new FolderQueryTraversal("Deep");

}