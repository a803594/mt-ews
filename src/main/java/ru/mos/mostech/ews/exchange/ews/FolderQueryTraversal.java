/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Folder folderQueryTraversalType search mode.
 */
public final class FolderQueryTraversal extends AttributeOption {

	private FolderQueryTraversal(String value) {
		super("Traversal", value);
	}

	/**
	 * Search only in current folder.
	 */
	public static final FolderQueryTraversal SHALLOW = new FolderQueryTraversal("Shallow");

	/**
	 * Recursive search.
	 */
	@SuppressWarnings({ "UnusedDeclaration" })
	public static final FolderQueryTraversal DEEP = new FolderQueryTraversal("Deep");

}