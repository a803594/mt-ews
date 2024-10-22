/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Delete Item method.
 */
public class DeleteItemMethod extends EWSMethod {

	/**
	 * Delete item method.
	 * @param itemId item id
	 * @param deleteType delete mode
	 * @param sendMeetingCancellations send meeting cancellation notifications
	 */
	public DeleteItemMethod(ItemId itemId, DeleteType deleteType, SendMeetingCancellations sendMeetingCancellations) {
		super("Item", "DeleteItem");
		addMethodOption(deleteType);
		addMethodOption(sendMeetingCancellations);
		addMethodOption(AffectedTaskOccurrences.AllOccurrences);
		this.itemId = itemId;
	}

}
