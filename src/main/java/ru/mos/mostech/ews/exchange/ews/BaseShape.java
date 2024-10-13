/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

/**
 * Item or folder base shape.
 */
@SuppressWarnings({"UnusedDeclaration"})
public final class BaseShape extends ElementOption {
    private BaseShape(String value) {
        super("t:BaseShape", value);
    }

    /**
     * Return id only.
     */
    public static final BaseShape ID_ONLY = new BaseShape("IdOnly");
    /**
     * Return default properties.
     */
    public static final BaseShape DEFAULT = new BaseShape("Default");
    /**
     * Return all properties, except MAPI extended properties.
     */
    public static final BaseShape ALL_PROPERTIES = new BaseShape("AllProperties");
}