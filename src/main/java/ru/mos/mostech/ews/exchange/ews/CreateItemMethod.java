/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import org.apache.http.entity.AbstractHttpEntity;
import ru.mos.mostech.ews.Settings;

/**
 * Create Item method.
 */
public class CreateItemMethod extends EWSMethod {

	/**
	 * Create exchange item.
	 * @param messageDisposition save or send option
	 * @param savedItemFolderId saved item folder id
	 * @param item item content
	 */
	public CreateItemMethod(MessageDisposition messageDisposition, FolderId savedItemFolderId, EWSMethod.Item item) {
		super("Item", "CreateItem");
		this.savedItemFolderId = savedItemFolderId;
		this.item = item;
		addMethodOption(messageDisposition);
		((AbstractHttpEntity) getEntity())
			.setChunked(Settings.getBooleanProperty("mt.ews.enableChunkedRequest", false));
	}

	/**
	 * Create exchange item.
	 * @param messageDisposition save or send option
	 * @param sendMeetingInvitations send invitation option
	 * @param savedItemFolderId saved item folder id
	 * @param item item content
	 */
	public CreateItemMethod(MessageDisposition messageDisposition, SendMeetingInvitations sendMeetingInvitations,
			FolderId savedItemFolderId, EWSMethod.Item item) {
		super("Item", "CreateItem");
		this.savedItemFolderId = savedItemFolderId;
		this.item = item;
		addMethodOption(messageDisposition);
		addMethodOption(sendMeetingInvitations);
	}

}
