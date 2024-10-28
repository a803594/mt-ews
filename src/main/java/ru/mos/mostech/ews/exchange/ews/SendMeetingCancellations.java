/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Опция обновления элемента.
 */
@SuppressWarnings({ "UnusedDeclaration" })
public final class SendMeetingCancellations extends AttributeOption {

	private SendMeetingCancellations(String value) {
		super("SendMeetingCancellations", value);
	}

	public static final SendMeetingCancellations SendToNone = new SendMeetingCancellations("SendToNone");

	public static final SendMeetingCancellations SendOnlyToAll = new SendMeetingCancellations("SendOnlyToAll");

	public static final SendMeetingCancellations SendToAllAndSaveCopy = new SendMeetingCancellations(
			"SendToAllAndSaveCopy");

}
