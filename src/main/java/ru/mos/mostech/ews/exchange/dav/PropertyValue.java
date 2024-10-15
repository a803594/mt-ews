/*
DIT
 */
package ru.mos.mostech.ews.exchange.dav;

/**
 * Property value.
 */

@Slf4j
public class PropertyValue {
    protected final String namespaceUri;
    protected final String name;
    protected final String xmlEncodedValue;
    protected final String typeString;

    /**
     * Create Dav property value.
     *
     * @param namespaceUri property namespace
     * @param name         property name
     */
    public PropertyValue(String namespaceUri, String name) {
        this(namespaceUri, name, null, null);
    }

    /**
     * Create Dav property value.
     *
     * @param namespaceUri    property namespace
     * @param name            property name
     * @param xmlEncodedValue xml encoded value
     */
    public PropertyValue(String namespaceUri, String name, String xmlEncodedValue) {
        this(namespaceUri, name, xmlEncodedValue, null);
    }

    /**
     * Create Dav property value.
     *
     * @param namespaceUri    property namespace
     * @param name            property name
     * @param xmlEncodedValue xml encoded value
     * @param typeString            property type
     */
    public PropertyValue(String namespaceUri, String name, String xmlEncodedValue, String typeString) {
        this.namespaceUri = namespaceUri;
        this.name = name;
        this.xmlEncodedValue = xmlEncodedValue;
        this.typeString = typeString;
    }

    /**
     * Get property namespace.
     *
     * @return property namespace
     */
    public String getNamespaceUri() {
        return namespaceUri;
    }

    /**
     * Get xml encoded value.
     *
     * @return Xml encoded value
     */
    public String getXmlEncodedValue() {
        return xmlEncodedValue;
    }

    /**
     * Get property type.
     *
     * @return property type
     */
    public String getTypeString() {
        return typeString;
    }

    /**
     * Get property name.
     *
     * @return property name
     */
    public String getName() {
        return name;
    }
}
