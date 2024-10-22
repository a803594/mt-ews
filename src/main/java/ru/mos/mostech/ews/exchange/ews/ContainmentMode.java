/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Contains search mode.
 */
@SuppressWarnings({ "UnusedDeclaration" })
public final class ContainmentMode extends AttributeOption {

	private ContainmentMode(String value) {
		super("ContainmentMode", value);
	}

	@Override
	public String toString() {
		return value;
	}

	/**
	 * Full String.
	 */
	public static final ContainmentMode FullString = new ContainmentMode("FullString");

	/**
	 * Starts with.
	 */
	public static final ContainmentMode Prefixed = new ContainmentMode("Prefixed");

	/**
	 * Contains
	 */
	public static final ContainmentMode Substring = new ContainmentMode("Substring");

	public static final ContainmentMode PrefixOnWords = new ContainmentMode("PrefixOnWords");

	public static final ContainmentMode ExactPhrase = new ContainmentMode("ExactPhrase");

}
