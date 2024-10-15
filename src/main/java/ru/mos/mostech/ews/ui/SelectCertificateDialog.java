/*
DIT
 */
package ru.mos.mostech.ews.ui;

import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.ui.tray.MosTechEwsTray;

import javax.swing.*;
import java.awt.*;

/**
 * Let user select a client certificate
 */

@Slf4j
public class SelectCertificateDialog extends JDialog {
    protected final JList<String> aliasListBox;
    protected final String[] aliases;
    protected String selectedAlias;

    /**
     * Gets user selected alias.
     *
     * @return user selected alias
     */
    public String getSelectedAlias() {
        return this.selectedAlias;
    }

    /**
     * Select a client certificate
     *
     * @param aliases An array of certificate aliases for the user to pick from
     */
    public SelectCertificateDialog(String[] aliases, String[] descriptions) {
        setAlwaysOnTop(true);
        this.aliases = aliases;

        setTitle(BundleMessage.format("UI_CERTIFICATE_ALIAS_PROMPT"));
        try {
            setIconImages(MosTechEwsTray.getFrameIcons());
        } catch (NoSuchMethodError error) {
            MosTechEwsTray.debug(new BundleMessage("LOG_UNABLE_TO_SET_ICON_IMAGE"));
        }

        JPanel questionPanel = new JPanel();
        questionPanel.setLayout(new BoxLayout(questionPanel, BoxLayout.Y_AXIS));
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(UIManager.getIcon("OptionPane.questionIcon"));
        imageLabel.setText(BundleMessage.format("UI_CERTIFICATE_ALIAS_PROMPT"));
        questionPanel.add(imageLabel);

        aliasListBox = new JList<>(descriptions);
        aliasListBox.setMaximumSize(aliasListBox.getPreferredSize());

        JPanel aliasPanel = new JPanel();
        aliasPanel.setLayout(new BoxLayout(aliasPanel, BoxLayout.Y_AXIS));
        aliasPanel.add(aliasListBox);

        add(questionPanel, BorderLayout.NORTH);
        add(aliasPanel, BorderLayout.CENTER);
        add(getButtonPanel(), BorderLayout.SOUTH);
        setModal(true);

        pack();
        // center frame
        setLocation(getToolkit().getScreenSize().width / 2 -
                getSize().width / 2,
                getToolkit().getScreenSize().height / 2 -
                        getSize().height / 2);
	setAlwaysOnTop(true);
        setVisible(true);
    }

    protected JPanel getButtonPanel() {
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton(BundleMessage.format("UI_BUTTON_OK"));
        JButton cancelButton = new JButton(BundleMessage.format("UI_BUTTON_CANCEL"));
        okButton.addActionListener(evt -> {
            selectedAlias = aliases[aliasListBox.getSelectedIndex()];
            setVisible(false);
        });
        cancelButton.addActionListener(evt -> {
            selectedAlias = null;
            setVisible(false);
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        return buttonPanel;
    }

}
