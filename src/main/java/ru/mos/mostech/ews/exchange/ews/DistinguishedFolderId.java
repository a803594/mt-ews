/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Distinguished Folder Id.
 */

@Slf4j
public final class DistinguishedFolderId extends FolderId {

    private DistinguishedFolderId(String value) {
        super("t:DistinguishedFolderId", value, null);
    }

    private DistinguishedFolderId(String value, String mailbox) {
        super("t:DistinguishedFolderId", value, null, mailbox);
    }

    /**
     * DistinguishedFolderId names
     */
    @SuppressWarnings({"UnusedDeclaration"})

@Slf4j
    public enum Name {
        calendar, contacts, deleteditems, drafts, inbox, journal, notes, outbox, sentitems, tasks, msgfolderroot,
        publicfoldersroot, root, junkemail, searchfolders, voicemail,
        archivemsgfolderroot
    }

    private static final Map<Name, DistinguishedFolderId> folderIdMap = new HashMap<>();

    static {
        for (Name name : Name.values()) {
            folderIdMap.put(name, new DistinguishedFolderId(name.toString()));
        }
    }

    /**
     * Get DistinguishedFolderId object for mailbox and name.
     *
     * @param mailbox mailbox name
     * @param name    folder id name
     * @return DistinguishedFolderId object
     */
    public static DistinguishedFolderId getInstance(String mailbox, Name name) {
        if (mailbox == null) {
            return folderIdMap.get(name);
        } else {
            return new DistinguishedFolderId(name.toString(), mailbox);
        }
    }

}