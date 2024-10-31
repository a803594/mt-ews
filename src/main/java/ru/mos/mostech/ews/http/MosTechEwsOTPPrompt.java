/*
DIT
 */
package ru.mos.mostech.ews.http;

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
		return "";
	}

	/**
	 * Запросите у пользователя значение капчи
	 * @param captchaImage изображение капчи
	 * @return одноразовый пароль, предоставленный пользователем
	 */
	public static String getCaptchaValue(Image captchaImage) {
		return "";
	}

}
