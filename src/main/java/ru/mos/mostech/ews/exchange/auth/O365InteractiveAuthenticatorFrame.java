/*
DIT
 */

package ru.mos.mostech.ews.exchange.auth;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import ru.mos.mostech.ews.BundleMessage;
import ru.mos.mostech.ews.Settings;

import javax.swing.*;
import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;

/**
 * Интерактивный пользовательский интерфейс аутентификатора на основе OpenJFX. Необходим
 * доступ к внутреннему urlhandler в недавних версиях JDK с помощью: --add-exports
 * java.base/sun.net.www.protocol.https=ALL-UNNAMED
 */
@Slf4j
public class O365InteractiveAuthenticatorFrame extends JFrame {

	private O365InteractiveAuthenticator authenticator;

	static {
		// register a stream handler for msauth protocol
		URL.setURLStreamHandlerFactory((String protocol) -> {
			if ("msauth".equals(protocol) || "urn".equals(protocol)) {
				return new URLStreamHandler() {
					@Override
					protected URLConnection openConnection(URL u) {
						return new URLConnection(u) {
							@Override
							public void connect() {
								// ignore
							}
						};
					}
				};
			}
			return null;
		});
	}

	String location;

	final JFXPanel fxPanel = new JFXPanel();

	public O365InteractiveAuthenticatorFrame() {
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (!authenticator.isAuthenticated && authenticator.errorCode == null) {
					authenticator.errorCode = "user closed authentication window";
				}
			}
		});

		setTitle(BundleMessage.format("UI_MT_EWS_GATEWAY"));

		JPanel mainPanel = new JPanel();

		mainPanel.add(fxPanel);
		add(BorderLayout.CENTER, mainPanel);

		pack();
		setResizable(true);
		// center frame
		setSize(600, 600);
		setLocationRelativeTo(null);
		setVisible(true);
		// bring window to top
		setAlwaysOnTop(true);
		setAlwaysOnTop(false);
	}

	public void setO365InteractiveAuthenticator(O365InteractiveAuthenticator authenticator) {
		this.authenticator = authenticator;
	}

	private void initFX(final JFXPanel fxPanel, final String url, final String redirectUri) {
		WebView webView = new WebView();
		final WebEngine webViewEngine = webView.getEngine();

		final ProgressBar loadProgress = new ProgressBar();
		loadProgress.progressProperty().bind(webViewEngine.getLoadWorker().progressProperty());

		StackPane hBox = new StackPane();
		hBox.getChildren().setAll(webView, loadProgress);
		Scene scene = new Scene(hBox);
		fxPanel.setScene(scene);

		webViewEngine.setUserAgent(Settings.getUserAgent());

		webViewEngine.setOnAlert(stringWebEvent -> SwingUtilities.invokeLater(() -> {
			String message = stringWebEvent.getData();
			JOptionPane.showMessageDialog(O365InteractiveAuthenticatorFrame.this, message);
		}));
		webViewEngine.setOnError(event -> log.error(event.getMessage()));

		webViewEngine.getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
			// with Java 15 url with code returns as CANCELLED
			if (newState == Worker.State.SUCCEEDED || newState == Worker.State.CANCELLED) {
				loadProgress.setVisible(false);
				location = webViewEngine.getLocation();
				updateTitleAndFocus(location);
				log.debug("Webview location: " + location);
				// override console.log
				O365InteractiveJSLogger.register(webViewEngine);
				if (log.isDebugEnabled()) {
					log.debug(dumpDocument(webViewEngine.getDocument()));
				}
				if (location.startsWith(redirectUri)) {
					log.debug("Location starts with redirectUri, check code");

					authenticator.isAuthenticated = location.contains("code=") && location.contains("&session_state=");
					if (!authenticator.isAuthenticated && location.contains("error=")) {
						authenticator.errorCode = location.substring(location.indexOf("error="));
					}
					if (authenticator.isAuthenticated) {
						log.debug("Authenticated location: " + location);
						String code = location.substring(location.indexOf("code=") + 5,
								location.indexOf("&session_state="));
						String sessionState = location.substring(location.lastIndexOf('='));

						log.debug("Authentication Code: " + code);
						log.debug("Authentication session state: " + sessionState);
						authenticator.code = code;
					}
					close();
				}
			}
			else if (newState == Worker.State.FAILED) {
				Throwable e = webViewEngine.getLoadWorker().getException();
				if (e != null) {
					handleError(e);
				}
				close();
			}
			else {
				log.debug(webViewEngine.getLoadWorker().getState() + " " + webViewEngine.getLoadWorker().getMessage()
						+ " " + webViewEngine.getLocation() + " ");
			}

		});
		webViewEngine.load(url);
	}

	private void updateTitleAndFocus(final String location) {
		SwingUtilities.invokeLater(() -> {
			setState(Frame.NORMAL);
			setAlwaysOnTop(true);
			setAlwaysOnTop(false);
			setTitle("MT-EWS: " + location);
		});
	}

	public String dumpDocument(Document document) {
		String result;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

			transformer.transform(new DOMSource(document),
					new StreamResult(new OutputStreamWriter(baos, StandardCharsets.UTF_8)));
			result = baos.toString(StandardCharsets.UTF_8);
		}
		catch (Exception e) {
			result = e + " " + e.getMessage();
		}
		return result;
	}

	public void authenticate(final String initUrl, final String redirectUri) {
		// Run initFX as JavaFX-Thread
		Platform.runLater(() -> {
			try {
				Platform.setImplicitExit(false);

				initFX(fxPanel, initUrl, redirectUri);
			}
			catch (Exception e) {
				handleError(e);
				close();
			}
		});
	}

	public void handleError(Throwable t) {
		log.error(t + " " + t.getMessage());
		authenticator.errorCode = t.getMessage();
		if (authenticator.errorCode == null) {
			authenticator.errorCode = t.toString();
		}
	}

	public void close() {
		SwingUtilities.invokeLater(() -> {
			setVisible(false);
			dispose();
		});
	}

}
