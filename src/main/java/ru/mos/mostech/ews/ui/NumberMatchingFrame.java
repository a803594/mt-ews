/*
DIT
 */

package ru.mos.mostech.ews.ui;

import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.ui.tray.MosTechEwsTray;

import javax.swing.*;
import java.awt.*;

/**
 * Display number matching value during O365 MFA process.
 */
public class NumberMatchingFrame extends JFrame {

	/**
	 * Number matching dialog.
	 * @param entropy number matching value from Azure AD
	 */
	public NumberMatchingFrame(String entropy) {
		setAlwaysOnTop(true);

		setTitle(BundleMessage.format("UI_O365_MFA_NUMBER_MATCHING"));
		try {
			setIconImages(MosTechEwsTray.getFrameIcons());
		}
		catch (NoSuchMethodError error) {
			MosTechEwsTray.debug(new BundleMessage("LOG_UNABLE_TO_SET_ICON_IMAGE"));
		}

		JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel imageLabel = new JLabel();
		imageLabel.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
		imageLabel.setText(BundleMessage.format("UI_O365_MFA_NUMBER_MATCHING_PROMPT", entropy));
		infoPanel.add(imageLabel);
		add(infoPanel, BorderLayout.NORTH);
		add(getButtonPanel(), BorderLayout.SOUTH);

		pack();
		// center frame
		setLocation(getToolkit().getScreenSize().width / 2 - getSize().width / 2,
				getToolkit().getScreenSize().height / 2 - getSize().height / 2);
		setAlwaysOnTop(true);

		// auto close after 1 minute
		Timer timer = new Timer(60000, evt -> {
			NumberMatchingFrame.this.setVisible(false);
			NumberMatchingFrame.this.dispose();
		});
		timer.start();
		setVisible(true);
	}

	protected JPanel getButtonPanel() {
		JPanel buttonPanel = new JPanel();
		JButton okButton = new JButton(BundleMessage.format("UI_BUTTON_OK"));
		okButton.addActionListener(evt -> {
			setVisible(false);
			dispose();
		});

		buttonPanel.add(okButton);
		return buttonPanel;
	}

}
