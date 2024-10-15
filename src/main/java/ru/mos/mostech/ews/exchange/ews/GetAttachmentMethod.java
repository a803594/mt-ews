/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Get Attachment Method.
 */

@Slf4j
public class GetAttachmentMethod extends EWSMethod {

    /**
     * Get Attachment Method.
     *
     * @param attachmentId attachment id
     */
    public GetAttachmentMethod(String attachmentId) {
        super("Attachment", "GetAttachment");
        this.attachmentId = attachmentId;
    }
}
