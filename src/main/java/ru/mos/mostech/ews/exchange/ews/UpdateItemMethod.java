/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Update Item method.
 */

@Slf4j
public class UpdateItemMethod extends EWSMethod {
    /**
     * Update exchange item.
     *
     * @param messageDisposition save or send option
     * @param conflictResolution overwrite option
     * @param sendMeetingInvitationsOrCancellations
     *                           send invitations option
     * @param itemId             item id with change key
     * @param updates            field updates
     */
    public UpdateItemMethod(MessageDisposition messageDisposition, ConflictResolution conflictResolution,
                            SendMeetingInvitationsOrCancellations sendMeetingInvitationsOrCancellations,
                            ItemId itemId, List<FieldUpdate> updates) {
        super("Item", "UpdateItem");
        this.itemId = itemId;
        this.updates = updates;
        addMethodOption(messageDisposition);
        addMethodOption(conflictResolution);
        addMethodOption(sendMeetingInvitationsOrCancellations);
    }
}