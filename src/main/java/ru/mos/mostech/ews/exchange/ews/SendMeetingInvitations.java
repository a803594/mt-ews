/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Опция обновления элемента.
 */
@SuppressWarnings({ "UnusedDeclaration" })
public final class SendMeetingInvitations extends AttributeOption {

	private SendMeetingInvitations(String value) {
		super("SendMeetingInvitations", value);
	}

	public static final SendMeetingInvitations SendToNone = new SendMeetingInvitations("SendToNone");

	public static final SendMeetingInvitations SendOnlyToAll = new SendMeetingInvitations("SendOnlyToAll");

	public static final SendMeetingInvitations SendToAllAndSaveCopy = new SendMeetingInvitations(
			"SendToAllAndSaveCopy");

}
