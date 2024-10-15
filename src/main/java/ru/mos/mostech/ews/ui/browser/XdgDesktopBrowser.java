/*
DIT
 */
package ru.mos.mostech.ews.ui.browser;

import java.io.IOException;
import java.net.URI;

/**
 * Failover: Runtime.exec open URL
 */
public final class XdgDesktopBrowser {
    private XdgDesktopBrowser() {
    }

    /**
     * Open default browser at location URI.
     * Use xdg-open to open browser url
     *
     * @param location location URI
     * @throws IOException on error
     */
    public static void browse(URI location) throws IOException {
        Runtime.getRuntime().exec(new String[]{"xdg-open", location.toString()});
    }
}
