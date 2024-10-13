/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Contains comparison mode.
 */
@SuppressWarnings({"UnusedDeclaration"})
public final class ContainmentComparison extends AttributeOption {
    private ContainmentComparison(String value) {
        super("ContainmentComparison", value);
    }

    public static final ContainmentComparison Exact = new ContainmentComparison("Exact");
    public static final ContainmentComparison IgnoreCase = new ContainmentComparison("IgnoreCase");
    public static final ContainmentComparison IgnoreNonSpacingCharacters = new ContainmentComparison("IgnoreNonSpacingCharacters");
    public static final ContainmentComparison Loose = new ContainmentComparison("Loose");
    public static final ContainmentComparison IgnoreCaseAndNonSpacingCharacters = new ContainmentComparison("IgnoreCaseAndNonSpacingCharacters");
    public static final ContainmentComparison LooseAndIgnoreCase = new ContainmentComparison("LooseAndIgnoreCase");
    public static final ContainmentComparison LooseAndIgnoreNonSpace = new ContainmentComparison("LooseAndIgnoreNonSpace");
    public static final ContainmentComparison LooseAndIgnoreCaseAndIgnoreNonSpace = new ContainmentComparison("LooseAndIgnoreCaseAndIgnoreNonSpace");

}
