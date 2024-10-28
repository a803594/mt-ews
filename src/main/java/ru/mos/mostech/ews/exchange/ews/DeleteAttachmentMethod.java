/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Метод удаления вложения.
 */
public class DeleteAttachmentMethod extends EWSMethod {

	/**
	 * Метод удаления вложения.
	 * @param attachmentId идентификатор вложения
	 */
	public DeleteAttachmentMethod(String attachmentId) {
		super("Item", "DeleteAttachment");
		this.attachmentId = attachmentId;
	}

}
