/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Get Item method.
 */

@Slf4j
public class GetItemMethod extends EWSMethod {
    /**
     * Get item method.
     *
     * @param baseShape          base requested shape
     * @param itemId             item id
     * @param includeMimeContent return mime content
     */
    public GetItemMethod(BaseShape baseShape, ItemId itemId, boolean includeMimeContent) {
        super("Item", "GetItem");
        this.baseShape = baseShape;
        this.itemId = itemId;
        this.includeMimeContent = includeMimeContent;
    }

}
