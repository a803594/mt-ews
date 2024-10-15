/*
DIT
 */
package ru.mos.mostech.ews.ui.tray;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.exchange.NetworkDownException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;


/**
 * Tray icon handler
 */
@Slf4j
public final class MosTechEwsTray {
    private static final long ICON_SWITCH_MINIMUM_DELAY = 250;
    private static long lastIconSwitch;

    private MosTechEwsTray() {
    }

    static MosTechEwsTrayInterface mtEwsGatewayTray;

    /**
     * Return AWT Image icon for frame title.
     *
     * @return frame icon
     */
    public static java.util.List<Image> getFrameIcons() {
        java.util.List<Image> icons = null;
        if (mtEwsGatewayTray != null) {
            icons = mtEwsGatewayTray.getFrameIcons();
        }
        return icons;
    }

    /**
     * Switch tray icon between active and standby icon.
     */
    public static void switchIcon() {
        if (mtEwsGatewayTray != null && !Settings.getBooleanProperty("mt.ews.disableTrayActivitySwitch")) {
            if (System.currentTimeMillis() - lastIconSwitch > ICON_SWITCH_MINIMUM_DELAY) {
                mtEwsGatewayTray.switchIcon();
                lastIconSwitch = System.currentTimeMillis();
            }
        }
    }

    /**
     * Set tray icon to inactive (network down)
     */
    public static void resetIcon() {
        if (mtEwsGatewayTray != null && isActive()) {
            mtEwsGatewayTray.resetIcon();
        }
    }

    /**
     * Check if current tray status is inactive (network down).
     *
     * @return true if inactive
     */
    public static boolean isActive() {
        return mtEwsGatewayTray == null || mtEwsGatewayTray.isActive();
    }

    /**
     * Log and display balloon message according to log level.
     *
     * @param message text message
     * @param level   log level
     */
    private static void displayMessage(BundleMessage message, Level level) {
        log.info("{}, {}", level, message.formatLog());
        if (mtEwsGatewayTray != null && !Settings.getBooleanProperty("mt.ews.disableGuiNotifications")) {
            mtEwsGatewayTray.displayMessage(message.format(), level);
        }
    }

    /**
     * Log and display balloon message and exception according to log level.
     *
     * @param message text message
     * @param e       exception
     * @param level   log level
     */
    private static void displayMessage(BundleMessage message, Exception e, Level level) {
        if (e instanceof NetworkDownException) {
            log.info("{}, {}", level, BundleMessage.getExceptionLogMessage(message, e));
        } else {
            log.info("{}", BundleMessage.getExceptionLogMessage(message, e), e);
        }
        if (mtEwsGatewayTray != null && !Settings.getBooleanProperty("mt.ews.disableGuiNotifications")
                && (!(e instanceof NetworkDownException))) {
            mtEwsGatewayTray.displayMessage(BundleMessage.getExceptionMessage(message, e), level);
        }
        if (mtEwsGatewayTray != null && e instanceof NetworkDownException) {
            mtEwsGatewayTray.inactiveIcon();
        }
    }

    /**
     * Log message at level DEBUG.
     *
     * @param message bundle message
     */
    public static void debug(BundleMessage message) {
        displayMessage(message, Level.DEBUG);
    }

    /**
     * Log message at level INFO.
     *
     * @param message bundle message
     */
    public static void info(BundleMessage message) {
        displayMessage(message, Level.INFO);
    }

    /**
     * Log message at level WARN.
     *
     * @param message bundle message
     */
    public static void warn(BundleMessage message) {
        displayMessage(message, Level.WARN);
    }

    /**
     * Log exception at level WARN.
     *
     * @param e exception
     */
    public static void warn(Exception e) {
        displayMessage(null, e, Level.WARN);
    }

    /**
     * Log message at level ERROR.
     *
     * @param message bundle message
     */
    public static void error(BundleMessage message) {
        displayMessage(message, Level.ERROR);
    }

    /**
     * Log exception at level WARN for NetworkDownException,
     * ERROR for other exceptions.
     *
     * @param e exception
     */
    public static void log(Exception e) {
        // only warn on network down
        if (e instanceof NetworkDownException) {
            warn(e);
        } else {
            error(e);
        }
    }

    /**
     * Log exception at level ERROR.
     *
     * @param e exception
     */
    public static void error(Exception e) {
        displayMessage(null, e, Level.ERROR);
    }

    /**
     * Log message and exception at level DEBUG.
     *
     * @param message bundle message
     * @param e       exception
     */
    public static void debug(BundleMessage message, Exception e) {
        displayMessage(message, e, Level.DEBUG);
    }

    /**
     * Log message and exception at level WARN.
     *
     * @param message bundle message
     * @param e       exception
     */
    public static void warn(BundleMessage message, Exception e) {
        displayMessage(message, e, Level.WARN);
    }

    /**
     * Log message and exception at level ERROR.
     *
     * @param message bundle message
     * @param e       exception
     */
    public static void error(BundleMessage message, Exception e) {
        displayMessage(message, e, Level.ERROR);
    }

    /**
     * Create tray icon and register frame listeners.
     */
    public static void init(boolean notray) {
        String currentDesktop = System.getenv("XDG_CURRENT_DESKTOP");
        String javaVersion = System.getProperty("java.version");
        String arch = System.getProperty("sun.arch.data.model");
        log.debug("OS Name: " + System.getProperty("os.name") +
                " Java version: " + javaVersion + ((arch != null) ? ' ' + arch : "") +
                " System tray " + (SystemTray.isSupported() ? "" : "not ") + "supported " +
                ((currentDesktop == null) ? "" : "Current Desktop: " + currentDesktop)
        );

        if (Settings.isLinux()) {
            // enable anti aliasing on linux
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
        }

        if (!Settings.getBooleanProperty("mt.ews.server")) {
            if (!notray) {
                if ("Unity".equals(currentDesktop)) {
                    log.info("Detected Unity desktop, please follow instructions to restore normal systray " +
                            "or run MT-EWS in server mode");
                } else if (currentDesktop != null && currentDesktop.contains("GNOME")) {
                    log.info("Detected Gnome desktop, please follow instructions to restore normal systray or run MT-EWS in server mode");
                }
                if (Settings.O365_INTERACTIVE.equals(Settings.getProperty("mt.ews.mode"))) {
                    log.info("O365Interactive is not compatible with SWT, do not try to create SWT tray");
                }
                // try java6 tray support, except on Linux
                if (mtEwsGatewayTray == null /*&& !isLinux()*/) {
                    try {
                        if (SystemTray.isSupported()) {
                            if (isOSX()) {
                                mtEwsGatewayTray = new OSXAwtGatewayTray();
                            } else {
                                mtEwsGatewayTray = new AwtGatewayTray();
                            }
                            mtEwsGatewayTray.init();
                        }
                    } catch (NoClassDefFoundError e) {
                        MosTechEwsTray.info(new BundleMessage("LOG_SYSTEM_TRAY_NOT_AVAILABLE"));
                    }
                }
            }

            if (mtEwsGatewayTray == null) {
                if (isOSX()) {
                    // MacOS
                    mtEwsGatewayTray = new OSXFrameGatewayTray();
                } else {
                    mtEwsGatewayTray = new FrameGatewayTray();
                }
                mtEwsGatewayTray.init();
            }
        }
    }

    /**
     * Test if running on OSX
     *
     * @return true on Mac OS X
     */
    public static boolean isOSX() {
        return System.getProperty("os.name").toLowerCase().startsWith("mac os x");
    }

    /**
     * Test if running on Windows
     *
     * @return true on Windows
     */
    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    /**
     * Test if running on Linux
     *
     * @return true on Linux
     */
    public static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().startsWith("linux");
    }

    /**
     * Load image with current class loader.
     *
     * @param fileName image resource file name
     * @return image
     */
    public static BufferedImage loadImage(String fileName) {
        BufferedImage result = null;
        try {
            ClassLoader classloader = MosTechEwsTray.class.getClassLoader();
            URL imageUrl = classloader.getResource(fileName);
            if (imageUrl == null) {
                throw new IOException("Missing resource: " + fileName);
            }
            result = ImageIO.read(imageUrl);
        } catch (IOException e) {
            MosTechEwsTray.warn(new BundleMessage("LOG_UNABLE_TO_LOAD_IMAGE"), e);
        }
        return result;
    }

    public static BufferedImage adjustTrayIcon(BufferedImage image) {
        Color backgroundColor = null;
        String backgroundColorString = Settings.getProperty("mt.ews.trayBackgroundColor");

        String xdgCurrentDesktop = System.getenv("XDG_CURRENT_DESKTOP");

        boolean isKDE = "KDE".equals(xdgCurrentDesktop);
        boolean isXFCE = "XFCE".equals(xdgCurrentDesktop);
        boolean isUnity = "Unity".equals(xdgCurrentDesktop);
        boolean isCinnamon = "X-Cinnamon".equals(xdgCurrentDesktop);
        boolean isGnome = xdgCurrentDesktop != null && xdgCurrentDesktop.contains("GNOME");

        if (backgroundColorString == null || backgroundColorString.isEmpty()) {
            // define color for default theme
            if (isKDE) {
                backgroundColorString = "#DDF6E8";
            }
            if (isUnity) {
                backgroundColorString = "#4D4B45";
            }
            if (isXFCE) {
                backgroundColorString = "#E8E8E7";
            }
            if (isCinnamon) {
                backgroundColorString = "#2E2E2E";
            }
            if (isGnome) {
                backgroundColorString = "#000000";
            }
        }

        int imageType = BufferedImage.TYPE_INT_ARGB;
        if (backgroundColorString != null && backgroundColorString.length() == 7
                && backgroundColorString.startsWith("#")) {
            int red = Integer.parseInt(backgroundColorString.substring(1, 3), 16);
            int green = Integer.parseInt(backgroundColorString.substring(3, 5), 16);
            int blue = Integer.parseInt(backgroundColorString.substring(5, 7), 16);
            backgroundColor = new Color(red, green, blue);
            imageType = BufferedImage.TYPE_INT_RGB;
        }

        if (backgroundColor != null || isKDE || isUnity || isXFCE || isGnome) {
            int width = image.getWidth();
            int height = image.getHeight();
            int x = 0;
            int y = 0;
            if (isKDE || isXFCE) {
                width = 22;
                height = 22;
                x = 3;
                y = 3;
            } else if (isUnity) {
                width = 22;
                height = 24;
                x = 4;
                y = 4;
            } else if (isCinnamon || isGnome) {
                width = 24;
                height = 24;
                x = 4;
                y = 4;
            }
            BufferedImage bufferedImage = new BufferedImage(width, height, imageType);
            Graphics2D graphics = bufferedImage.createGraphics();
            graphics.setColor(backgroundColor);
            graphics.fillRect(0, 0, width, height);
            graphics.drawImage(image, x, y, null);
            graphics.dispose();
            return bufferedImage;
        } else {
            return image;
        }
    }


    /**
     * Dispose application tray icon
     */
    public static void dispose() {
        if (mtEwsGatewayTray != null) {
            mtEwsGatewayTray.dispose();
        }
    }

    /**
     * Open logging window.
     */
    public static void showLogs() {
        throw new UnsupportedOperationException();
    }
}
