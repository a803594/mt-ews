/*
DIT
 */

package ru.mos.mostech.ews.exchange.auth;

import javafx.scene.web.WebEngine;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings({ "rawtypes", "unchecked" })
@Slf4j
public class O365InteractiveJSLogger {

	public void log(String message) {
		log.info(message);
	}

	public static void register(WebEngine webEngine) {

		try {
			Class jsObjectClass = Class.forName("netscape.javascript.JSObject");
			Method setMemberMethod = jsObjectClass.getDeclaredMethod("setMember", String.class, Object.class);

			Object window = webEngine.executeScript("window");
			setMemberMethod.invoke(window, "ru/mos/mostech/ews", new O365InteractiveJSLogger());

			webEngine.executeScript("console.log = function(message) { mt-ews.log(message); }");
		}
		catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			log.info("netscape.javascript.JSObject not available");
		}

	}

}
