/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Получить метод вложения.
 */
public class GetAttachmentMethod extends EWSMethod {

	/**
	 * Получить метод вложения.
	 * @param attachmentId идентификатор вложения
	 */
	public GetAttachmentMethod(String attachmentId) {
		super("Attachment", "GetAttachment");
		this.attachmentId = attachmentId;
	}

}
