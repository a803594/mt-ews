/*
DIT
 */
package ru.mos.mostech.ews.ui.tray;

import ru.mos.mostech.ews.BundleMessage;

import javax.swing.*;

/**
 * MacOSX specific frame to handle menu
 */
public class OSXFrameGatewayTray extends FrameGatewayTray implements OSXTrayInterface {

	@Override
	protected void buildMenu() {
		// create a popup menu
		JMenu menu = new JMenu(BundleMessage.format("UI_LOGS"));
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(menu);
		mainFrame.setJMenuBar(menuBar);

		JMenuItem logItem = new JMenuItem(BundleMessage.format("UI_SHOW_LOGS"));
		logItem.addActionListener(e -> MosTechEwsTray.showLogs());
		menu.add(logItem);
	}

	@Override
	protected void createAndShowGUI() {
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		super.createAndShowGUI();
		try {
			new OSXHandler(this);
		}
		catch (Exception e) {
			MosTechEwsTray.error(new BundleMessage("LOG_ERROR_LOADING_OSXADAPTER"), e);
		}
	}

}
