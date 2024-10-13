/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * ResolveNames search scope.
 */
@SuppressWarnings({"UnusedDeclaration"})
public final class SearchScope extends AttributeOption {
    private SearchScope(String value) {
        super("SearchScope", value);
    }

    public static final SearchScope ActiveDirectory = new SearchScope("ActiveDirectory");
    public static final SearchScope ActiveDirectoryContacts = new SearchScope("ActiveDirectoryContacts");
    public static final SearchScope Contacts = new SearchScope("Contacts");
    public static final SearchScope ContactsActiveDirectory = new SearchScope("ContactsActiveDirectory");
}
