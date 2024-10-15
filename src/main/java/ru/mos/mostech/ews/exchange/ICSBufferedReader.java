/*
DIT
 */
package ru.mos.mostech.ews.exchange;

import java.io.BufferedReader;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import java.io.Reader;
import lombok.extern.slf4j.Slf4j;

/**
 * ICS Buffered Reader.
 * Read events by line, handle multiple line elements
 */

@Slf4j
public class ICSBufferedReader extends BufferedReader {
    protected String nextLine;
    protected final StringBuilder currentLine = new StringBuilder(75);

    /**
     * Create an ICS reader on the provided reader
     *
     * @param in input reader
     * @throws IOException on error
     */
    public ICSBufferedReader(Reader in) throws IOException {
        super(in);
        nextLine = super.readLine();
    }

    /**
     * Read a line from input reader, unwrap long lines.
     */
    @Override
    public String readLine() throws IOException {
        if (nextLine == null) {
            return null;
        } else {
            currentLine.setLength(0);
            currentLine.append(nextLine);
            nextLine = super.readLine();
            while (nextLine != null && !(nextLine.length() == 0) &&
                    (nextLine.charAt(0) == ' ' || nextLine.charAt(0) == '\t'
                            // workaround for broken items with \n as first line character
                            || nextLine.charAt(0) == '\\'
                            // workaround for Exchange 2010 bug
                            || nextLine.charAt(0) == ':')) {
                // Timezone ends with \n => next line starts with :
                if (nextLine.charAt(0) == ':') {
                    currentLine.append(nextLine);
                } else {
                    currentLine.append(nextLine.substring(1));
                }
                nextLine = super.readLine();
            }
            return currentLine.toString();
        }
    }
}
