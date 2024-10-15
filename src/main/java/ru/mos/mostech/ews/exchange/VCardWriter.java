/*
DIT
 */
package ru.mos.mostech.ews.exchange;

/**
 * VCard Writer
 */

@Slf4j
public class VCardWriter extends ICSBufferedWriter {
    /**
     * Begin VCard and version
     */
    public void startCard() {
        startCard(null);
    }

    /**
     * Begin VCard and version
     */
    public void startCard(String version) {
        writeLine("BEGIN:VCARD");
        writeLine("VERSION:"+((version == null)?"4.0":version));
    }

    /**
     * Append compound value
     *
     * @param propertyName  property name
     * @param propertyValue property values
     */
    public void appendProperty(String propertyName, String... propertyValue) {
        boolean hasValue = false;
        for (String value : propertyValue) {
            if ((value != null) && (value.length() > 0)) {
                hasValue = true;
                break;
            }
        }
        if (hasValue) {
            boolean first = true;
            StringBuilder lineBuffer = new StringBuilder();
            lineBuffer.append(propertyName);
            lineBuffer.append(':');
            for (String value : propertyValue) {
                if (first) {
                    first = false;
                } else {
                    lineBuffer.append(';');
                }
                appendEncodedValue(lineBuffer, value);
            }
            writeLine(lineBuffer.toString());
        }
    }

    /**
     * Encode and append value to buffer
     *
     * @param buffer current buffer
     * @param value  property value
     */
    private void appendEncodedValue(StringBuilder buffer, String value) {
        if (value != null) {
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == ',' || c == ';') {
                    buffer.append('\\');
                }
                if (c == '\n') {
                    buffer.append("\\n");
                } else if (c != '\r') {
                    buffer.append(value.charAt(i));
                }
            }
        }
    }

    /**
     * End VCard
     */
    public void endCard() {
        writeLine("END:VCARD");
    }
}
