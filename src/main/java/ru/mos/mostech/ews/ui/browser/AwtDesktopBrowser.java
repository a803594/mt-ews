/*
DIT
 */
package ru.mos.mostech.ews.ui.browser;

import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

/**

@Slf4j
 * Wrapper class to call Java6 Desktop class to launch default browser.
 */

@Slf4j
public final class AwtDesktopBrowser {
    private AwtDesktopBrowser() {
    }

    /**
     * Open default browser at location URI.
     * User Java 6 Desktop class
     *
     * @param location location URI
     * @throws IOException on error
     */
    public static void browse(URI location) throws IOException {
        Desktop desktop = Desktop.getDesktop();
        desktop.browse(location);
    }

}