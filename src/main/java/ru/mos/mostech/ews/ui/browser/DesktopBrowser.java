/*
DIT
 */
package ru.mos.mostech.ews.ui.browser;

import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.ui.AboutFrame;
import ru.mos.mostech.ews.ui.tray.MosTechEwsTray;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Open default browser.
 */
public final class DesktopBrowser {
    private DesktopBrowser() {
    }

    /**
     * Open default browser at location URI.
     * User Java 6 Desktop class, OSX open command or SWT program launch
     *
     * @param location location URI
     */
    public static void browse(URI location) {
        try {
            // trigger ClassNotFoundException
            ClassLoader classloader = AboutFrame.class.getClassLoader();
            classloader.loadClass("java.awt.Desktop");

            // Open link in default browser
            AwtDesktopBrowser.browse(location);
        } catch (ClassNotFoundException e) {
            MosTechEwsTray.debug(new BundleMessage("LOG_JAVA6_DESKTOP_UNAVAILABLE"));
            // failover for MacOSX
            if (System.getProperty("os.name").toLowerCase().startsWith("mac os x")) {
                try {
                    OSXDesktopBrowser.browse(location);
                } catch (Exception e2) {
                    MosTechEwsTray.error(new BundleMessage("LOG_UNABLE_TO_OPEN_LINK"), e2);
                }
            }
        } catch (java.lang.UnsupportedOperationException e) {
            if (Settings.isUnix()) {
                try {
                    XdgDesktopBrowser.browse(location);
                } catch (Exception e2) {
                    MosTechEwsTray.error(new BundleMessage("LOG_UNABLE_TO_OPEN_LINK"), e2);
                }
            } else {
                MosTechEwsTray.error(new BundleMessage("LOG_UNABLE_TO_OPEN_LINK"), e);
            }
        } catch (Exception e) {
            MosTechEwsTray.error(new BundleMessage("LOG_UNABLE_TO_OPEN_LINK"), e);
        }
    }

    /**
     * Open default browser at location.
     * User Java 6 Desktop class, OSX open command or SWT program launch
     *
     * @param location target location
     */
    public static void browse(String location) {
        try {
            DesktopBrowser.browse(new URI(location));
        } catch (URISyntaxException e) {
            MosTechEwsTray.error(new BundleMessage("LOG_UNABLE_TO_OPEN_LINK"), e);
        }
    }

}
