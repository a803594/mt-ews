/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Метод создания вложения.
 */
public class CreateAttachmentMethod extends EWSMethod {

	/**
	 * Метод создания вложения.
	 * @param parentItemId идентификатор родительского элемента
	 * @param attachment объект вложения
	 */
	public CreateAttachmentMethod(ItemId parentItemId, FileAttachment attachment) {
		super("Item", "CreateAttachment");
		this.parentItemId = parentItemId;
		this.attachment = attachment;
	}

}
