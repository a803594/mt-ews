/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import org.apache.http.entity.AbstractHttpEntity;
import ru.mos.mostech.ews.Settings;

/**
 * Метод создания элемента.
 */
public class CreateItemMethod extends EWSMethod {

	/**
	 * Создать элемент обмена.
	 * @param messageDisposition опция сохранения или отправки
	 * @param savedItemFolderId идентификатор папки сохранённых элементов
	 * @param item содержимое элемента
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
	 * Создать элемент обмена.
	 * @param messageDisposition опция сохранения или отправки
	 * @param sendMeetingInvitations опция отправки приглашений
	 * @param savedItemFolderId id папки сохраненных элементов
	 * @param item содержание элемента
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
