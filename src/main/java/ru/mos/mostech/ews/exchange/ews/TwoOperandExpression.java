/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import ru.mos.mostech.ews.util.StringUtil;

/**
 * Two operand expression.
 */
public class TwoOperandExpression implements SearchExpression {
    @SuppressWarnings({"UnusedDeclaration"})
    protected enum Operator {
        IsEqualTo, IsNotEqualTo, IsGreaterThan, IsGreaterThanOrEqualTo, IsLessThan, IsLessThanOrEqualTo
    }

    protected final Operator operator;
    protected final FieldURI fieldURI;
    protected final String value;

    /**
     * Create two operand expression.
     *
     * @param operator operator
     * @param fieldURI field operand
     * @param value    value operand
     */
    public TwoOperandExpression(Operator operator, FieldURI fieldURI, String value) {
        this.operator = operator;
        this.fieldURI = fieldURI;
        this.value = value;
    }

    public void appendTo(StringBuilder buffer) {
        buffer.append("<t:").append(operator.toString()).append('>');
        fieldURI.appendTo(buffer);

        buffer.append("<t:FieldURIOrConstant><t:Constant Value=\"");
        // encode urlcompname
        if (fieldURI instanceof ExtendedFieldURI && "0x10f3".equals(((ExtendedFieldURI) fieldURI).propertyTag)) {
            buffer.append(StringUtil.xmlEncodeAttribute(StringUtil.encodeUrlcompname(value)));
        } else {
            buffer.append(StringUtil.xmlEncodeAttribute(value));
        }
        buffer.append("\"/></t:FieldURIOrConstant>");

        buffer.append("</t:").append(operator.toString()).append('>');
    }

}
