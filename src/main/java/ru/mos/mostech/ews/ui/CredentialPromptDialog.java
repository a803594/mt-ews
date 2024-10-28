package ru.mos.mostech.ews.ui;

import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.ui.tray.MosTechEwsTray;

import javax.swing.*;
import java.awt.*;

/**
 * Запрос учетных данных и пароля для Exchange.
 */
public class CredentialPromptDialog extends JDialog {

	final JTextField principalField = new JTextField(15);

	final JPasswordField passwordField = new JPasswordField(15);

	protected String principal;

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
	 * Получить основной объект пользователя.
	 * @return основной объект пользователя
	 */
	public String getPrincipal() {
		return principal;
	}

	/**
	 * Получить учетные данные.
	 * @param prompt Запрос Kerberos от обработчика обратных вызовов
	 */
	public CredentialPromptDialog(String prompt) {
		setAlwaysOnTop(true);

		setTitle(BundleMessage.format("UI_KERBEROS_CREDENTIAL_PROMPT"));

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
		questionPanel.add(imageLabel);

		passwordField.setMaximumSize(passwordField.getPreferredSize());
		passwordField.addActionListener(e -> {
			principal = principalField.getText();
			password = passwordField.getPassword();
			setVisible(false);
		});
		JPanel credentialPanel = new JPanel(new GridLayout(2, 2));

		JLabel promptLabel = new JLabel(' ' + prompt.trim());
		promptLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		promptLabel.setVerticalAlignment(SwingConstants.CENTER);

		credentialPanel.add(promptLabel);

		principalField.setMaximumSize(principalField.getPreferredSize());
		credentialPanel.add(principalField);

		JLabel passwordLabel = new JLabel(BundleMessage.format("UI_KERBEROS_PASSWORD_PROMPT"));
		passwordLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		passwordLabel.setVerticalAlignment(SwingConstants.CENTER);
		credentialPanel.add(passwordLabel);

		passwordField.setMaximumSize(passwordField.getPreferredSize());
		credentialPanel.add(passwordField);

		add(questionPanel, BorderLayout.WEST);
		add(credentialPanel, BorderLayout.CENTER);
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
			principal = principalField.getText();
			password = passwordField.getPassword();
			setVisible(false);
		});
		cancelButton.addActionListener(evt -> {
			principal = null;
			password = null;
			setVisible(false);
		});

		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		return buttonPanel;
	}

}
