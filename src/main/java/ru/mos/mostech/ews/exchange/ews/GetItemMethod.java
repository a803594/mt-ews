/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Метод получения элемента.
 */
public class GetItemMethod extends EWSMethod {

	/**
	 * Метод получения элемента.
	 * @param baseShape запрашиваемая базовая форма
	 * @param itemId идентификатор элемента
	 * @param includeMimeContent возвращать mime содержимое
	 */
	public GetItemMethod(BaseShape baseShape, ItemId itemId, boolean includeMimeContent) {
		super("Item", "GetItem");
		this.baseShape = baseShape;
		this.itemId = itemId;
		this.includeMimeContent = includeMimeContent;
	}

}
