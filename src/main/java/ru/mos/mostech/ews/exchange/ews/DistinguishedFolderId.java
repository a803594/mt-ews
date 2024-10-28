/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import java.util.HashMap;
import java.util.Map;

/**
 * Отличительный идентификатор папки.
 */
public final class DistinguishedFolderId extends FolderId {

	private DistinguishedFolderId(String value) {
		super("t:DistinguishedFolderId", value, null);
	}

	private DistinguishedFolderId(String value, String mailbox) {
		super("t:DistinguishedFolderId", value, null, mailbox);
	}

	/**
	 * Имена DistinguishedFolderId
	 */
	@SuppressWarnings({ "UnusedDeclaration" })
	public enum Name {

		calendar, contacts, deleteditems, drafts, inbox, journal, notes, outbox, sentitems, tasks, msgfolderroot,
		publicfoldersroot, root, junkemail, searchfolders, voicemail, archivemsgfolderroot

	}

	private static final Map<Name, DistinguishedFolderId> folderIdMap = new HashMap<>();

	static {
		for (Name name : Name.values()) {
			folderIdMap.put(name, new DistinguishedFolderId(name.toString()));
		}
	}

	/**
	 * Получить объект DistinguishedFolderId для почтового ящика и имени.
	 * @param mailbox имя почтового ящика
	 * @param name имя идентификатора папки
	 * @return объект DistinguishedFolderId
	 */
	public static DistinguishedFolderId getInstance(String mailbox, Name name) {
		if (mailbox == null) {
			return folderIdMap.get(name);
		}
		else {
			return new DistinguishedFolderId(name.toString(), mailbox);
		}
	}

}