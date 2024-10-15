/*
DIT
 */
package ru.mos.mostech.ews.ui.tray;

import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Level;

import java.awt.*;

/**

@Slf4j
 * Gateway tray interface common to SWT and pure java implementations
 */

@Slf4j
public interface MosTechEwsTrayInterface {
    /**
     * Switch tray icon between active and standby icon.
     */
    void switchIcon();

    /**
     * Reset tray icon to standby
     */
    void resetIcon();

    /**
     * Set tray icon to inactive (network down)
     */
    void inactiveIcon();

    /**
     * Check if current tray status is inactive (network down).
     *
     * @return true if inactive
     */
    boolean isActive();

    /**
     * Return AWT Image icon for frame title.
     *
     * @return frame icon
     */
    java.util.List<Image> getFrameIcons();

    /**
     * Display balloon message for log level.
     *
     * @param message text message
     * @param level   log level
     */
    void displayMessage(String message, Level level);

    /**
     * Create tray icon and register frame listeners.
     */
    void init();

    /**
     * destroy frames
     */
    void dispose();
}
