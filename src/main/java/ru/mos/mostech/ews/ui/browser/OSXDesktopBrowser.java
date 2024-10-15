/*
DIT
 */
package ru.mos.mostech.ews.ui.browser;

import java.io.IOException;
import java.net.URI;

/**
 * Failover: Runtime.exec open URL
 */
public final class OSXDesktopBrowser {
    private OSXDesktopBrowser() {
    }

    /**
     * Open default browser at location URI.
     * User OSX open command
     *
     * @param location location URI
     * @throws IOException on error
     */
    public static void browse(URI location) throws IOException {
        Runtime.getRuntime().exec(new String[]{"open", location.toString()});
    }
}
