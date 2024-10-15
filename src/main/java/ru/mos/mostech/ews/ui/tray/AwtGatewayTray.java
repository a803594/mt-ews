/*
DIT
 */
package ru.mos.mostech.ews.ui.tray;

import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.MosTechEws;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.ui.AboutFrame;
import ru.mos.mostech.ews.ui.SettingsFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Tray icon handler based on java 1.6
 */

@Slf4j
public class AwtGatewayTray implements MosTechEwsTrayInterface {
    protected static final String TRAY_PNG = "tray.png";

    protected static final String TRAY_ACTIVE_PNG = "tray2.png";
    protected static final String TRAY_INACTIVE_PNG = "trayinactive.png";

    protected static final String TRAY128_PNG = "tray128.png";
    protected static final String TRAY128_ACTIVE_PNG = "tray128active.png";
    protected static final String TRAY128_INACTIVE_PNG = "tray128inactive.png";

    protected AwtGatewayTray() {
    }

    static AboutFrame aboutFrame;
    static SettingsFrame settingsFrame;
    ActionListener settingsListener;

    static TrayIcon trayIcon;
    protected static ArrayList<Image> frameIcons;
    protected static BufferedImage image;
    protected static BufferedImage activeImage;
    protected static BufferedImage inactiveImage;

    private boolean isActive = true;

    /**
     * Return AWT Image icon for frame title.
     *
     * @return frame icon
     */
    @Override
    public java.util.List<Image> getFrameIcons() {
        return frameIcons;
    }

    /**
     * Switch tray icon between active and standby icon.
     */
    public void switchIcon() {
        isActive = true;
        SwingUtilities.invokeLater(() -> {
            if (trayIcon.getImage().equals(image)) {
                trayIcon.setImage(activeImage);
            } else {
                trayIcon.setImage(image);
            }
        });
    }

    /**
     * Set tray icon to inactive (network down)
     */
    public void resetIcon() {
        SwingUtilities.invokeLater(() -> trayIcon.setImage(image));
    }

    /**
     * Set tray icon to inactive (network down)
     */
    public void inactiveIcon() {
        isActive = false;
        SwingUtilities.invokeLater(() -> trayIcon.setImage(inactiveImage));
    }

    /**
     * Check if current tray status is inactive (network down).
     *
     * @return true if inactive
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Display balloon message for log level.
     *
     * @param message text message
     * @param level   log level
     */
    public void displayMessage(final String message, final Level level) {
        SwingUtilities.invokeLater(() -> {
            if (trayIcon != null) {
                TrayIcon.MessageType messageType = null;
                if (level.equals(Level.INFO)) {
                    messageType = TrayIcon.MessageType.INFO;
                } else if (level.equals(Level.WARN)) {
                    messageType = TrayIcon.MessageType.WARNING;
                } else if (level.equals(Level.ERROR)) {
                    messageType = TrayIcon.MessageType.ERROR;
                }
                if (messageType != null) {
                    trayIcon.displayMessage(BundleMessage.format("UI_MT_EWS_GATEWAY"), message, messageType);
                }
                trayIcon.setToolTip(BundleMessage.format("UI_MT_EWS_GATEWAY") + '\n' + message);
            }
        });
    }

    /**
     * Open about window
     */
    public void about() {
        SwingUtilities.invokeLater(() -> {
            aboutFrame.update();
            aboutFrame.setVisible(true);
            aboutFrame.toFront();
            aboutFrame.requestFocus();
        });
    }

    /**
     * Open settings window
     */
    public void preferences() {
        SwingUtilities.invokeLater(() -> {
            settingsFrame.reload();
            settingsFrame.setVisible(true);
            settingsFrame.toFront();
            settingsFrame.repaint();
            settingsFrame.requestFocus();
        });
    }

    /**
     * Create tray icon and register frame listeners.
     */
    public void init() {
        SwingUtilities.invokeLater(this::createAndShowGUI);
    }

    public void dispose() {
        SystemTray.getSystemTray().remove(trayIcon);

        // dispose frames
        settingsFrame.dispose();
        aboutFrame.dispose();
    }

    protected void loadIcons() {
        image = MosTechEwsTray.adjustTrayIcon(MosTechEwsTray.loadImage(AwtGatewayTray.TRAY_PNG));
        activeImage = MosTechEwsTray.adjustTrayIcon(MosTechEwsTray.loadImage(AwtGatewayTray.TRAY_ACTIVE_PNG));
        inactiveImage = MosTechEwsTray.adjustTrayIcon(MosTechEwsTray.loadImage(AwtGatewayTray.TRAY_INACTIVE_PNG));

        frameIcons = new ArrayList<>();
        frameIcons.add(MosTechEwsTray.loadImage(AwtGatewayTray.TRAY128_PNG));
        frameIcons.add(MosTechEwsTray.loadImage(AwtGatewayTray.TRAY_PNG));
    }

    protected void createAndShowGUI() {
        System.setProperty("swing.defaultlaf", UIManager.getSystemLookAndFeelClassName());

        // get the SystemTray instance
        SystemTray tray = SystemTray.getSystemTray();
        loadIcons();

        // create a popup menu
        PopupMenu popup = new PopupMenu();

        aboutFrame = new AboutFrame();
        // create an action settingsListener to listen for settings action executed on the tray icon
        ActionListener aboutListener = e -> about();
        // create menu item for the default action
        MenuItem aboutItem = new MenuItem(BundleMessage.format("UI_ABOUT"));
        aboutItem.addActionListener(aboutListener);
        popup.add(aboutItem);

        settingsFrame = new SettingsFrame();
        // create an action settingsListener to listen for settings action executed on the tray icon
        settingsListener = e -> preferences();
        // create menu item for the default action
        MenuItem defaultItem = new MenuItem(BundleMessage.format("UI_SETTINGS"));
        defaultItem.addActionListener(settingsListener);
        popup.add(defaultItem);

        MenuItem logItem = new MenuItem(BundleMessage.format("UI_SHOW_LOGS"));
        logItem.addActionListener(e -> MosTechEwsTray.showLogs());
        popup.add(logItem);

        // create an action exitListener to listen for exit action executed on the tray icon
        ActionListener exitListener = e -> {
            try {
                MosTechEws.stop();
            } catch (Exception exc) {
                MosTechEwsTray.error(exc);
            }
            // make sure we do exit
            System.exit(0);
        };
        // create menu item for the exit action
        MenuItem exitItem = new MenuItem(BundleMessage.format("UI_EXIT"));
        exitItem.addActionListener(exitListener);
        popup.add(exitItem);

        /// ... add other items
        // construct a TrayIcon
        trayIcon = new TrayIcon(image, BundleMessage.format("UI_MT_EWS_GATEWAY"), popup);
        // set the TrayIcon properties
        trayIcon.addActionListener(settingsListener);
        // ...
        // add the tray image
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            MosTechEwsTray.warn(new BundleMessage("LOG_UNABLE_TO_CREATE_TRAY"), e);
        }

        // display settings frame on first start
        if (Settings.isFirstStart()) {
            settingsFrame.setVisible(true);
            settingsFrame.toFront();
            settingsFrame.repaint();
            settingsFrame.requestFocus();
        }
    }

}
