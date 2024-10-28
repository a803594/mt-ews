/*
DIT
 */
package ru.mos.mostech.ews.ui.tray;

import org.slf4j.event.Level;

import java.awt.*;

/**
 * Интерфейс трейлера шлюза, общий для реализаций SWT и чистого Java
 */
public interface MosTechEwsTrayInterface {

	/**
	 * Переключить значок в трее между активным и резервным значком.
	 */
	void switchIcon();

	/**
	 * Сбросить значок в трее в режим ожидания
	 */
	void resetIcon();

	/**
	 * Установить значок в Tray как неактивный (сеть отключена)
	 */
	void inactiveIcon();

	/**
	 * Проверьте, является ли текущее состояние подноса неактивным (неисправность сети).
	 * @return true, если неактивно
	 */
	boolean isActive();

	/**
	 * Вернуть иконку AWT изображения для заголовка окна.
	 * @return иконка окна
	 */
	java.util.List<Image> getFrameIcons();

	/**
	 * Отобразить сообщение в виде баллона для уровня журнала.
	 * @param message текст сообщения
	 * @param level уровень журнала
	 */
	void displayMessage(String message, Level level);

	/**
	 * Создать иконку в трее и зарегистрировать слушатели фрейма.
	 */
	void init();

	/**
	 * уничтожить фреймы
	 */
	void dispose();

}
