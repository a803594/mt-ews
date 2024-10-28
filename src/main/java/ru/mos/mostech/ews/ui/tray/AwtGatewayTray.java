/*
DIT
 */
package ru.mos.mostech.ews.ui.tray;

import org.slf4j.event.Level;
import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.MosTechEws;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.ui.AboutFrame;
import ru.mos.mostech.ews.ui.SettingsFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Обработчик значка в трее, основанный на java 1.6
 */
public class AwtGatewayTray implements MosTechEwsTrayInterface {

	protected static final String TRAY_PNG = "logo.png";

	protected static final String TRAY_ACTIVE_PNG = "logo.png";

	protected static final String TRAY_INACTIVE_PNG = "logo.png";

	protected static final String TRAY128_PNG = "logo.png";

	protected static final String TRAY128_ACTIVE_PNG = "logo.png";

	protected static final String TRAY128_INACTIVE_PNG = "logo.png";

	protected AwtGatewayTray() {
	}

	static AboutFrame aboutFrame;
	static SettingsFrame settingsFrame;

	ActionListener settingsListener;

	static TrayIcon trayIcon;

	protected static ArrayList<Image> frameIcons;

	protected static BufferedImage image;

	protected static BufferedImage activeImage;

	protected static BufferedImage inactiveImage;

	private boolean isActive = true;

	/**
	 * Возвращает AWT изображение иконки для заголовка окна.
	 * @return иконка окна
	 */
	@Override
	public java.util.List<Image> getFrameIcons() {
		return frameIcons;
	}

	/**
	 * Переключить значок в трее между активным и резервным значком.
	 */
	public void switchIcon() {
		isActive = true;
		SwingUtilities.invokeLater(() -> {
			if (trayIcon.getImage().equals(image)) {
				trayIcon.setImage(activeImage);
			}
			else {
				trayIcon.setImage(image);
			}
		});
	}

	/**
	 * Установить значок в трее в неактивное состояние (сеть не работает)
	 */
	public void resetIcon() {
		SwingUtilities.invokeLater(() -> trayIcon.setImage(image));
	}

	/**
	 * Установить значок в трее в неактивное состояние (сеть не работает)
	 */
	public void inactiveIcon() {
		isActive = false;
		SwingUtilities.invokeLater(() -> trayIcon.setImage(inactiveImage));
	}

	/**
	 * Проверьте, неактивен ли текущий статус лотка (сеть отключена).
	 * @return true, если неактивен
	 */
	public boolean isActive() {
		return isActive;
	}

	/**
	 * Отобразить сообщение-воздушный шарик для уровня журнала.
	 * @param message текст сообщения
	 * @param level уровень журнала
	 */
	public void displayMessage(final String message, final Level level) {

	}

	/**
	 * Открыть окно "О программе"
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
	 * Открыть окно настроек
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
	 * Создать значок в области уведомлений и зарегистрировать слушатели фрейма.
	 */
	public void init() {
		SwingUtilities.invokeLater(this::createAndShowGUI);
	}

	public void dispose() {
		SystemTray.getSystemTray().remove(trayIcon);

		// dispose frames
		settingsFrame.dispose();
		aboutFrame.dispose();
	}

	protected void loadIcons() {
		image = MosTechEwsTray.adjustTrayIcon(MosTechEwsTray.loadImage(AwtGatewayTray.TRAY_PNG));
		activeImage = MosTechEwsTray.adjustTrayIcon(MosTechEwsTray.loadImage(AwtGatewayTray.TRAY_ACTIVE_PNG));
		inactiveImage = MosTechEwsTray.adjustTrayIcon(MosTechEwsTray.loadImage(AwtGatewayTray.TRAY_INACTIVE_PNG));

		frameIcons = new ArrayList<>();
		frameIcons.add(MosTechEwsTray.loadImage(AwtGatewayTray.TRAY128_PNG));
		frameIcons.add(MosTechEwsTray.loadImage(AwtGatewayTray.TRAY_PNG));
	}

	protected void createAndShowGUI() {
		System.setProperty("swing.defaultlaf", UIManager.getSystemLookAndFeelClassName());

		// get the SystemTray instance
		SystemTray tray = SystemTray.getSystemTray();
		loadIcons();

		// create a popup menu
		PopupMenu popup = new PopupMenu();

		aboutFrame = new AboutFrame();
		// create an action settingsListener to listen for settings action executed on the
		// tray icon
		ActionListener aboutListener = e -> about();
		// create menu item for the default action
		MenuItem aboutItem = new MenuItem(BundleMessage.format("UI_ABOUT"));
		aboutItem.addActionListener(aboutListener);
		popup.add(aboutItem);

		settingsFrame = new SettingsFrame();
		// create an action settingsListener to listen for settings action executed on the
		// tray icon
		settingsListener = e -> preferences();
		// create menu item for the default action
		MenuItem defaultItem = new MenuItem(BundleMessage.format("UI_SETTINGS"));
		defaultItem.addActionListener(settingsListener);
		popup.add(defaultItem);

		MenuItem logItem = new MenuItem(BundleMessage.format("UI_SHOW_LOGS"));
		logItem.addActionListener(e -> MosTechEwsTray.showLogs());
		popup.add(logItem);

		// create an action exitListener to listen for exit action executed on the tray
		// icon
		ActionListener exitListener = e -> {
			try {
				MosTechEws.stop();
			}
			catch (Exception exc) {
				MosTechEwsTray.error(exc);
			}
			// make sure we do exit
			System.exit(0);
		};
		// create menu item for the exit action
		MenuItem exitItem = new MenuItem(BundleMessage.format("UI_EXIT"));
		exitItem.addActionListener(exitListener);
		popup.add(exitItem);

		/// ... add other items
		// construct a TrayIcon
		trayIcon = new TrayIcon(image, BundleMessage.format("UI_MT_EWS_GATEWAY"), popup);
		// set the TrayIcon properties
		trayIcon.addActionListener(settingsListener);
		// ...
		// add the tray image
		try {
			tray.add(trayIcon);
		}
		catch (AWTException e) {
			MosTechEwsTray.warn(new BundleMessage("LOG_UNABLE_TO_CREATE_TRAY"), e);
		}

		// display settings frame on first start
		if (Settings.isFirstStart()) {
			settingsFrame.setVisible(true);
			settingsFrame.toFront();
			settingsFrame.repaint();
			settingsFrame.requestFocus();
		}
	}

}
