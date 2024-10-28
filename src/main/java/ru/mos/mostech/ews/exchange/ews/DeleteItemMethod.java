/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Метод удаления элемента.
 */
public class DeleteItemMethod extends EWSMethod {

	/**
	 * Метод удаления элемента.
	 * @param itemId идентификатор элемента
	 * @param deleteType режим удаления
	 * @param sendMeetingCancellations отправить уведомления об отмене встреч
	 */
	public DeleteItemMethod(ItemId itemId, DeleteType deleteType, SendMeetingCancellations sendMeetingCancellations) {
		super("Item", "DeleteItem");
		addMethodOption(deleteType);
		addMethodOption(sendMeetingCancellations);
		addMethodOption(AffectedTaskOccurrences.AllOccurrences);
		this.itemId = itemId;
	}

}
