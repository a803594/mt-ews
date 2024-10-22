/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Delete attachment method.
 */
public class DeleteAttachmentMethod extends EWSMethod {

	/**
	 * Delete attachment method.
	 * @param attachmentId attachment id
	 */
	public DeleteAttachmentMethod(String attachmentId) {
		super("Item", "DeleteAttachment");
		this.attachmentId = attachmentId;
	}

}
