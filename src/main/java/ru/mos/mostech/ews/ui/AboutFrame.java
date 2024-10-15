/*
DIT
 */
package ru.mos.mostech.ews.ui;

import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.MosTechEws;
import ru.mos.mostech.ews.ui.browser.DesktopBrowser;
import ru.mos.mostech.ews.ui.tray.MosTechEwsTray;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * About frame
 */

@Slf4j
public class AboutFrame extends JFrame {
    private final JEditorPane jEditorPane;

    /**
     * About frame.
     */
    public AboutFrame() {
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setTitle(BundleMessage.format("UI_ABOUT_MT_EWS"));
        try {
            setIconImages(MosTechEwsTray.getFrameIcons());
        } catch (NoSuchMethodError error) {
            MosTechEwsTray.debug(new BundleMessage("LOG_UNABLE_TO_SET_ICON_IMAGE"));
        }
        try {
            JLabel imageLabel = new JLabel();
            ClassLoader classloader = this.getClass().getClassLoader();
            URL imageUrl = classloader.getResource("tray32.png");
            if (imageUrl != null) {
                Image iconImage = ImageIO.read(imageUrl);
                ImageIcon icon = new ImageIcon(iconImage);
                imageLabel.setIcon(icon);

                JPanel imagePanel = new JPanel();
                imagePanel.add(imageLabel);
                add(BorderLayout.WEST, imagePanel);
            }
        } catch (IOException e) {
            MosTechEwsTray.error(new BundleMessage("LOG_UNABLE_TO_CREATE_ICON"), e);
        }

        jEditorPane = new JEditorPane();
        HTMLEditorKit htmlEditorKit = new HTMLEditorKit();
        StyleSheet stylesheet = htmlEditorKit.getStyleSheet();
        Font font = jEditorPane.getFont();
        stylesheet.addRule("body { font-size:small;font-family: " + ((font == null) ? "Arial" : font.getFamily()) + '}');
        jEditorPane.setEditorKit(htmlEditorKit);
        jEditorPane.setContentType("text/html");
        jEditorPane.setText(getContent(null));

        jEditorPane.setEditable(false);
        jEditorPane.setOpaque(false);
        jEditorPane.addHyperlinkListener(hle -> {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
                try {
                    DesktopBrowser.browse(hle.getURL().toURI());
                } catch (URISyntaxException e) {
                    MosTechEwsTray.error(new BundleMessage("LOG_UNABLE_TO_OPEN_LINK"), e);
                }
                setVisible(false);
            }
        });


        JPanel mainPanel = new JPanel();
        mainPanel.add(jEditorPane);
        add(BorderLayout.CENTER, mainPanel);

        JPanel buttonPanel = new JPanel();
        JButton ok = new JButton(BundleMessage.format("UI_BUTTON_OK"));
        ActionListener close = evt -> setVisible(false);
        ok.addActionListener(close);

        buttonPanel.add(ok);

        add(BorderLayout.SOUTH, buttonPanel);

        pack();
        setResizable(false);
        // center frame
        setLocation(getToolkit().getScreenSize().width / 2 -
                        getSize().width / 2,
                getToolkit().getScreenSize().height / 2 -
                        getSize().height / 2);
    }

    String getContent(String releasedVersion) {
        Package mtEwsPackage = MosTechEws.class.getPackage();
        StringBuilder buffer = new StringBuilder();
        buffer.append(BundleMessage.format("UI_ABOUT_MT_EWS_AUTHOR"));
        String currentVersion = mtEwsPackage.getImplementationVersion();
        if (currentVersion != null) {
            buffer.append(BundleMessage.format("UI_CURRENT_VERSION", currentVersion));
        }
        if ((currentVersion != null && releasedVersion != null && currentVersion.compareTo(releasedVersion) != 0)
                || (currentVersion == null && releasedVersion != null)) {
            buffer.append(BundleMessage.format("UI_LATEST_VERSION", releasedVersion));
        }
        buffer.append(BundleMessage.format("UI_HELP_INSTRUCTIONS"));
        return buffer.toString();
    }


    /**
     * Update about frame content with current released version.
     */
    public void update() {
        jEditorPane.setText(getContent(MosTechEws.getReleasedVersion()));
        pack();
    }

}
