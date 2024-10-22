/*
DIT
 */
package ru.mos.mostech.ews.http;

import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.ui.PasswordPromptDialog;

import java.awt.*;

/**
 * Ask user one time password.
 */
public final class MosTechEwsOTPPrompt {

	private MosTechEwsOTPPrompt() {
	}

	/**
	 * Ask user token password
	 * @return user provided one time password
	 */
	public static String getOneTimePassword() {
		PasswordPromptDialog passwordPromptDialog = new PasswordPromptDialog(
				BundleMessage.format("UI_OTP_PASSWORD_PROMPT"));
		return String.valueOf(passwordPromptDialog.getPassword());
	}

	/**
	 * Ask user captcha value
	 * @param captchaImage captcha image
	 * @return user provided one time password
	 */
	public static String getCaptchaValue(Image captchaImage) {
		PasswordPromptDialog passwordPromptDialog = new PasswordPromptDialog(BundleMessage.format("UI_CAPTCHA_PROMPT"),
				captchaImage);
		return String.valueOf(passwordPromptDialog.getPassword());
	}

}
