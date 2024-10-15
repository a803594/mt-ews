/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Create Attachment Method.
 */

@Slf4j
public class CreateAttachmentMethod extends EWSMethod {
    /**
     * Create attachment method.
     *
     * @param parentItemId parent item id
     * @param attachment   attachment object
     */
    public CreateAttachmentMethod(ItemId parentItemId, FileAttachment attachment) {
        super("Item", "CreateAttachment");
        this.parentItemId = parentItemId;
        this.attachment = attachment;
    }
}
