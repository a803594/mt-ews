/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Item update option.
 */
@SuppressWarnings({"UnusedDeclaration"})

@Slf4j
public final class SendMeetingCancellations extends AttributeOption {
    private SendMeetingCancellations(String value) {
        super("SendMeetingCancellations", value);
    }

    public static final SendMeetingCancellations SendToNone = new SendMeetingCancellations("SendToNone");
    public static final SendMeetingCancellations SendOnlyToAll = new SendMeetingCancellations("SendOnlyToAll");
    public static final SendMeetingCancellations SendToAllAndSaveCopy = new SendMeetingCancellations("SendToAllAndSaveCopy");
}
