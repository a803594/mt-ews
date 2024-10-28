/*
DIT
 */
package ru.mos.mostech.ews.ui;

import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.ui.tray.MosTechEwsTray;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Получить пароль смарт-карты
 */
public class PasswordPromptDialog extends JDialog {

	final JPasswordField passwordField = new JPasswordField(20);

	protected char[] password;

	/**
	 * Получить пароль пользователя.
	 * @return пароль пользователя в виде массива символов
	 */
	public char[] getPassword() {
		if (password != null) {
			return password.clone();
		}
		else {
			return "".toCharArray();
		}
	}

	/**
	 * Получить пароль смарт-карты.
	 * @param prompt запрос пароля от модуля PKCS11
	 */
	public PasswordPromptDialog(String prompt) {
		this(prompt, null);
	}

	/**
	 * Получить пароль смарт-карты.
	 * @param prompt подсказка пароля от модуля PKCS11
	 * @param captchaImage изображение фильтра ISA pinsafe
	 */
	public PasswordPromptDialog(String prompt, Image captchaImage) {
		setAlwaysOnTop(true);

		setTitle(BundleMessage.format("UI_PASSWORD_PROMPT"));
		try {
			setIconImages(MosTechEwsTray.getFrameIcons());
		}
		catch (NoSuchMethodError error) {
			MosTechEwsTray.debug(new BundleMessage("LOG_UNABLE_TO_SET_ICON_IMAGE"));
		}

		JPanel questionPanel = new JPanel();
		questionPanel.setLayout(new BoxLayout(questionPanel, BoxLayout.Y_AXIS));
		JLabel imageLabel = new JLabel();
		imageLabel.setIcon(UIManager.getIcon("OptionPane.questionIcon"));
		imageLabel.setText(prompt);
		questionPanel.add(imageLabel);

		passwordField.setMaximumSize(passwordField.getPreferredSize());
		passwordField.addActionListener(e -> {
			password = passwordField.getPassword();
			setVisible(false);
		});
		JPanel passwordPanel = new JPanel();
		passwordPanel.setLayout(new BoxLayout(passwordPanel, BoxLayout.Y_AXIS));
		if (captchaImage != null) {
			JLabel captchaLabel = new JLabel(new ImageIcon(captchaImage));
			captchaLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			captchaLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
			passwordPanel.add(captchaLabel);
		}
		passwordPanel.add(passwordField);

		add(questionPanel, BorderLayout.NORTH);
		add(passwordPanel, BorderLayout.CENTER);
		add(getButtonPanel(), BorderLayout.SOUTH);
		setModal(true);

		pack();
		// center frame
		setLocation(getToolkit().getScreenSize().width / 2 - getSize().width / 2,
				getToolkit().getScreenSize().height / 2 - getSize().height / 2);
		setAlwaysOnTop(true);
		setVisible(true);
	}

	protected JPanel getButtonPanel() {
		JPanel buttonPanel = new JPanel();
		JButton okButton = new JButton(BundleMessage.format("UI_BUTTON_OK"));
		JButton cancelButton = new JButton(BundleMessage.format("UI_BUTTON_CANCEL"));
		okButton.addActionListener(evt -> {
			password = passwordField.getPassword();
			setVisible(false);
		});
		cancelButton.addActionListener(evt -> {
			password = null;
			setVisible(false);
		});

		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		return buttonPanel;
	}

}
