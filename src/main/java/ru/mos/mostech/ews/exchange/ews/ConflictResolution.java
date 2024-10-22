/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Item update conflict resolution
 */
@SuppressWarnings({ "UnusedDeclaration" })
public final class ConflictResolution extends AttributeOption {

	private ConflictResolution(String value) {
		super("ConflictResolution", value);
	}

	public static final ConflictResolution NeverOverwrite = new ConflictResolution("NeverOverwrite");

	public static final ConflictResolution AutoResolve = new ConflictResolution("AutoResolve");

	public static final ConflictResolution AlwaysOverwrite = new ConflictResolution("AlwaysOverwrite");

}
