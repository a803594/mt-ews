/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Тип утилизации элемента DeleteItem.
 */
@SuppressWarnings({ "UnusedDeclaration" })
public final class DeleteType extends AttributeOption {

	private DeleteType(String value) {
		super("DeleteType", value);
	}

	public static final DeleteType HardDelete = new DeleteType("HardDelete");

	public static final DeleteType SoftDelete = new DeleteType("SoftDelete");

	public static final DeleteType MoveToDeletedItems = new DeleteType("MoveToDeletedItems");

}
