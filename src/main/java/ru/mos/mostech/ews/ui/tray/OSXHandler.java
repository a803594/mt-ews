/*
DIT
 */

package ru.mos.mostech.ews.ui.tray;

import java.awt.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class OSXHandler implements InvocationHandler {

	private final OSXTrayInterface davGatewayTray;

	public OSXHandler(OSXTrayInterface davGatewayTray)
			throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		this.davGatewayTray = davGatewayTray;
		addEventHandlers();
	}

	public static final boolean IS_JAVA9 = Double.parseDouble(System.getProperty("java.specification.version")) >= 1.9;

	public void addEventHandlers()
			throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		Class<?> applicationClass;
		Class<?> aboutHandlerClass;
		Class<?> preferencesHandlerClass;

		Object application;
		if (IS_JAVA9) {
			applicationClass = Class.forName("java.awt.Desktop");
			application = Desktop.getDesktop();
			aboutHandlerClass = Class.forName("java.awt.desktop.AboutHandler");
			preferencesHandlerClass = Class.forName("java.awt.desktop.PreferencesHandler");
		}
		else {
			applicationClass = Class.forName("com.apple.eawt.Application");
			application = applicationClass.getMethod("getApplication").invoke(null);
			aboutHandlerClass = Class.forName("com.apple.eawt.AboutHandler");
			preferencesHandlerClass = Class.forName("com.apple.eawt.PreferencesHandler");
		}

		Object proxy = Proxy.newProxyInstance(OSXHandler.class.getClassLoader(),
				new Class<?>[] { aboutHandlerClass, preferencesHandlerClass }, this);

		applicationClass.getDeclaredMethod("setAboutHandler", aboutHandlerClass).invoke(application, proxy);
		applicationClass.getDeclaredMethod("setPreferencesHandler", preferencesHandlerClass).invoke(application, proxy);
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) {
		if ("handleAbout".equals(method.getName())) {
			davGatewayTray.about();
		}
		else if ("handlePreferences".equals(method.getName())) {
			davGatewayTray.preferences();
		}
		return null;
	}

}
