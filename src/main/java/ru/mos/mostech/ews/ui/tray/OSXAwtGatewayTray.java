/*
DIT
 */
package ru.mos.mostech.ews.ui.tray;

import org.apache.log4j.Level;
import ru.mos.mostech.ews.BundleMessage;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Extended Awt tray with OSX extensions.
 */
public class OSXAwtGatewayTray extends AwtGatewayTray implements OSXTrayInterface {
    protected static final String OSX_TRAY_ACTIVE_PNG = "osxtray2.png";
    protected static final String OSX_TRAY_PNG = "osxtray.png";
    protected static final String OSX_TRAY_INACTIVE_PNG = "osxtrayinactive.png";

    @Override
    protected void loadIcons() {
        image = MosTechEwsTray.adjustTrayIcon(MosTechEwsTray.loadImage(OSX_TRAY_PNG));
        activeImage = MosTechEwsTray.adjustTrayIcon(MosTechEwsTray.loadImage(OSX_TRAY_ACTIVE_PNG));
        inactiveImage = MosTechEwsTray.adjustTrayIcon(MosTechEwsTray.loadImage(OSX_TRAY_INACTIVE_PNG));

        frameIcons = new ArrayList<>();
        frameIcons.add(MosTechEwsTray.loadImage(AwtGatewayTray.TRAY128_PNG));
        frameIcons.add(MosTechEwsTray.loadImage(AwtGatewayTray.TRAY_PNG));
    }


    @Override
    protected void createAndShowGUI() {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        super.createAndShowGUI();
        trayIcon.removeActionListener(settingsListener);
        try {
            new OSXHandler(this);
        } catch (Exception e) {
            MosTechEwsTray.error(new BundleMessage("LOG_ERROR_LOADING_OSXADAPTER"), e);
        }
    }

    @Override
    public void displayMessage(final String message, final Level level) {
        super.displayMessage(message, level);
    }

    protected Image getImageForIcon(Icon icon) {
        BufferedImage bufferedimage = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = bufferedimage.getGraphics();
        icon.paintIcon(null, g, 0, 0);
        g.dispose();
        return bufferedimage;
    }
}
