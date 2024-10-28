/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import java.util.List;

/**
 * Метод обновления элемента.
 */
public class UpdateItemMethod extends EWSMethod {

	/**
	 * Обновить элемент обмена.
	 * @param messageDisposition опция сохранения или отправки
	 * @param conflictResolution опция перезаписи
	 * @param sendMeetingInvitationsOrCancellations опция отправки приглашений
	 * @param itemId идентификатор элемента с ключом изменения
	 * @param updates обновления полей
	 */
	public UpdateItemMethod(MessageDisposition messageDisposition, ConflictResolution conflictResolution,
			SendMeetingInvitationsOrCancellations sendMeetingInvitationsOrCancellations, ItemId itemId,
			List<FieldUpdate> updates) {
		super("Item", "UpdateItem");
		this.itemId = itemId;
		this.updates = updates;
		addMethodOption(messageDisposition);
		addMethodOption(conflictResolution);
		addMethodOption(sendMeetingInvitationsOrCancellations);
	}

}