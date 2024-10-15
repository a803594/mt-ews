/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Item update option.
 */
@SuppressWarnings({"UnusedDeclaration"})
public final class SendMeetingInvitationsOrCancellations extends AttributeOption {
    private SendMeetingInvitationsOrCancellations(String value) {
        super("SendMeetingInvitationsOrCancellations", value);
    }

    public static final SendMeetingInvitationsOrCancellations SendToNone = new SendMeetingInvitationsOrCancellations("SendToNone");
    public static final SendMeetingInvitationsOrCancellations SendOnlyToAll = new SendMeetingInvitationsOrCancellations("SendOnlyToAll");
    public static final SendMeetingInvitationsOrCancellations SendToAllAndSaveCopy = new SendMeetingInvitationsOrCancellations("SendToAllAndSaveCopy");
}
