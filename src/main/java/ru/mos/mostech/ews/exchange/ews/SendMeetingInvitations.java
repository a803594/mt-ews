/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Item update option.
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
