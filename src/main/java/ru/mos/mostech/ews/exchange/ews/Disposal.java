/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Утилизация.
 */
@SuppressWarnings({ "UnusedDeclaration" })
public final class Disposal extends AttributeOption {

	private Disposal(String value) {
		super("DeleteType", value);
	}

	public static final Disposal HardDelete = new Disposal("HardDelete");

	public static final Disposal SoftDelete = new Disposal("SoftDelete");

	public static final Disposal MoveToDeletedItems = new Disposal("MoveToDeletedItems");

}
