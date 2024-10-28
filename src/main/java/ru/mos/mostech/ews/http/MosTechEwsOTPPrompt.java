/*
DIT
 */
package ru.mos.mostech.ews.http;

import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.ui.PasswordPromptDialog;

import java.awt.*;

/**
 * Попросите пользователя один раз ввести пароль.
 */
public final class MosTechEwsOTPPrompt {

	private MosTechEwsOTPPrompt() {
	}

	/**
	 * Запросить у пользователя пароль токена
	 * @return одноразовый пароль, предоставленный пользователем
	 */
	public static String getOneTimePassword() {
		PasswordPromptDialog passwordPromptDialog = new PasswordPromptDialog(
				BundleMessage.format("UI_OTP_PASSWORD_PROMPT"));
		return String.valueOf(passwordPromptDialog.getPassword());
	}

	/**
	 * Запросите у пользователя значение капчи
	 * @param captchaImage изображение капчи
	 * @return одноразовый пароль, предоставленный пользователем
	 */
	public static String getCaptchaValue(Image captchaImage) {
		PasswordPromptDialog passwordPromptDialog = new PasswordPromptDialog(BundleMessage.format("UI_CAPTCHA_PROMPT"),
				captchaImage);
		return String.valueOf(passwordPromptDialog.getPassword());
	}

}
