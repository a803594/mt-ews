/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Item delete option.
 */
@SuppressWarnings({"UnusedDeclaration"})
public final class AffectedTaskOccurrences extends AttributeOption {
    private AffectedTaskOccurrences(String value) {
        super("AffectedTaskOccurrences", value);
    }

    public static final AffectedTaskOccurrences AllOccurrences = new AffectedTaskOccurrences("AllOccurrences");
    public static final AffectedTaskOccurrences SpecifiedOccurrenceOnly = new AffectedTaskOccurrences("SpecifiedOccurrenceOnly");
}
