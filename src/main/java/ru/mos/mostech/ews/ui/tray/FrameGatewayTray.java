/*
DIT
 */
package ru.mos.mostech.ews.ui.tray;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.MosTechEws;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.ui.AboutFrame;
import ru.mos.mostech.ews.ui.SettingsFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * Failover GUI for Java 1.5 without SWT
 */
@Slf4j
public class FrameGatewayTray implements MosTechEwsTrayInterface {
    protected FrameGatewayTray() {
    }

    static JFrame mainFrame;
    static AboutFrame aboutFrame;
    static SettingsFrame settingsFrame;
    private static JEditorPane errorArea;
    private static JLabel errorLabel;
    private static JEditorPane messageArea;
    private static ArrayList<Image> frameIcons;
    private static Image image;
    private static Image activeImage;
    private static Image inactiveImage;
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
            Image currentImage = mainFrame.getIconImage();
            if (currentImage != null && currentImage.equals(image)) {
                mainFrame.setIconImage(activeImage);
            } else {
                mainFrame.setIconImage(image);
            }
        });
    }

    /**
     * Set tray icon to inactive (network down)
     */
    public void resetIcon() {
        SwingUtilities.invokeLater(() -> mainFrame.setIconImage(image));
    }

    /**
     * Set tray icon to inactive (network down)
     */
    public void inactiveIcon() {
        isActive = false;
        SwingUtilities.invokeLater(() -> mainFrame.setIconImage(inactiveImage));
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
     * Log and display balloon message according to log level.
     *
     * @param message text message
     * @param level   log level
     */
    public void displayMessage(final String message, final Level level) {
        SwingUtilities.invokeLater(() -> {
            if (errorArea != null && messageArea != null) {
                if (level.equals(Level.INFO)) {
                    errorLabel.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
                    errorArea.setText(message);
                } else if (level.equals(Level.WARN)) {
                    errorLabel.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
                    errorArea.setText(message);
                } else if (level.equals(Level.ERROR)) {
                    errorLabel.setIcon(UIManager.getIcon("OptionPane.errorIcon"));
                    errorArea.setText(message);
                }
                messageArea.setText(message);
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
        // dispose frames
        settingsFrame.dispose();
        aboutFrame.dispose();
    }

    protected void buildMenu() {
        // create a popup menu
        JMenu menu = new JMenu(BundleMessage.format("UI_MT_EWS_GATEWAY"));
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(menu);
        mainFrame.setJMenuBar(menuBar);

        // create an action settingsListener to listen for settings action executed on the tray icon
        ActionListener aboutListener = e -> about();
        // create menu item for the default action
        JMenuItem aboutItem = new JMenuItem(BundleMessage.format("UI_ABOUT"));
        aboutItem.addActionListener(aboutListener);
        menu.add(aboutItem);


        // create an action settingsListener to listen for settings action executed on the tray icon
        ActionListener settingsListener = e -> preferences();
        // create menu item for the default action
        JMenuItem defaultItem = new JMenuItem(BundleMessage.format("UI_SETTINGS"));
        defaultItem.addActionListener(settingsListener);
        menu.add(defaultItem);

        JMenuItem logItem = new JMenuItem(BundleMessage.format("UI_SHOW_LOGS"));
        logItem.addActionListener(e -> MosTechEwsTray.showLogs());
        menu.add(logItem);

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
        JMenuItem exitItem = new JMenuItem(BundleMessage.format("UI_EXIT"));
        exitItem.addActionListener(exitListener);
        menu.add(exitItem);
    }

    protected void createAndShowGUI() {
        // set cross platform look and feel on Linux, except is swing.defaultlaf is set
        if (Settings.isLinux() && System.getProperty("swing.defaultlaf") == null) {
            System.setProperty("swing.defaultlaf", UIManager.getCrossPlatformLookAndFeelClassName());
        } else {
            System.setProperty("swing.defaultlaf", UIManager.getSystemLookAndFeelClassName());
        }
        String imageName = AwtGatewayTray.TRAY_PNG;
        String activeImageName = AwtGatewayTray.TRAY_ACTIVE_PNG;
        String inactiveImageName = AwtGatewayTray.TRAY_INACTIVE_PNG;
        // use hi res icons on Linux
        if (Settings.isLinux()) {
            imageName = AwtGatewayTray.TRAY128_PNG;
            activeImageName = AwtGatewayTray.TRAY128_ACTIVE_PNG;
            inactiveImageName = AwtGatewayTray.TRAY128_INACTIVE_PNG;
        }
        image = MosTechEwsTray.loadImage(imageName);
        activeImage = MosTechEwsTray.loadImage(activeImageName);
        inactiveImage = MosTechEwsTray.loadImage(inactiveImageName);

        frameIcons = new ArrayList<>();
        frameIcons.add(image);
        frameIcons.add(MosTechEwsTray.loadImage(AwtGatewayTray.TRAY128_PNG));

        mainFrame = new JFrame();
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.setTitle(BundleMessage.format("UI_MT_EWS_GATEWAY"));
        mainFrame.setIconImages(frameIcons);

        JPanel errorPanel = new JPanel();
        errorPanel.setBorder(BorderFactory.createTitledBorder(BundleMessage.format("UI_LAST_MESSAGE")));
        errorPanel.setLayout(new BoxLayout(errorPanel, BoxLayout.X_AXIS));
        errorArea = new JTextPane();
        errorArea.setEditable(false);
        errorArea.setBackground(mainFrame.getBackground());
        errorLabel = new JLabel();
        errorPanel.add(errorLabel);
        errorPanel.add(errorArea);

        JPanel messagePanel = new JPanel();
        messagePanel.setBorder(BorderFactory.createTitledBorder(BundleMessage.format("UI_LAST_LOG")));
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.X_AXIS));

        messageArea = new JTextPane();
        messageArea.setText(BundleMessage.format("LOG_STARTING_MT_EWS"));
        messageArea.setEditable(false);
        messageArea.setBackground(mainFrame.getBackground());
        messagePanel.add(messageArea);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(errorPanel);
        mainPanel.add(messagePanel);
        mainFrame.add(mainPanel);

        aboutFrame = new AboutFrame();
        settingsFrame = new SettingsFrame();
        buildMenu();

        mainFrame.setMinimumSize(new Dimension(400, 250));
        mainFrame.pack();
        // workaround MacOSX
        if (mainFrame.getSize().width < 400 || mainFrame.getSize().height < 180) {
            mainFrame.setSize(Math.max(mainFrame.getSize().width, 400),
                    Math.max(mainFrame.getSize().height, 180));
        }
        // center frame
        mainFrame.setLocation(mainFrame.getToolkit().getScreenSize().width / 2 -
                        mainFrame.getSize().width / 2,
                mainFrame.getToolkit().getScreenSize().height / 2 -
                        mainFrame.getSize().height / 2);
        mainFrame.setVisible(true);

        // display settings frame on first start
        if (Settings.isFirstStart()) {
            settingsFrame.setVisible(true);
            settingsFrame.toFront();
            settingsFrame.repaint();
            settingsFrame.requestFocus();
        }
    }
}
