/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * MessageDisposition flag.
 */
@SuppressWarnings({ "UnusedDeclaration" })
public final class MessageDisposition extends AttributeOption {

	private MessageDisposition(String value) {
		super("MessageDisposition", value);
	}

	public static final MessageDisposition SaveOnly = new MessageDisposition("SaveOnly");

	public static final MessageDisposition SendOnly = new MessageDisposition("SendOnly");

	public static final MessageDisposition SendAndSaveCopy = new MessageDisposition("SendAndSaveCopy");

}
