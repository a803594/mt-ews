/*
DIT
 */
package ru.mos.mostech.ews.http;

import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.ui.SelectCertificateDialog;

import javax.net.ssl.X509KeyManager;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Специальный X509 менеджер ключей, который обрабатывает случаи, когда более одного
 * закрытого ключа достаточно для установления HTTPS-соединения, запрашивая у пользователя
 * выбор одного из них.
 */
@Slf4j
public class MosTechEwsX509KeyManager implements X509KeyManager {

	// Wrap an existing key manager to handle most of the interface as a pass through
	private final X509KeyManager keyManager;

	// Remember selected alias so we don't continually bug the user
	private String cachedAlias;

	/**
	 * Создает специализированный менеджер ключей, оборачивающий стандартный
	 * @param keyManager оригинальный менеджер ключей
	 */
	public MosTechEwsX509KeyManager(X509KeyManager keyManager) {
		this.keyManager = keyManager;
	}

	/**
	 * Получить алиасы клиента, просто передайте это в обернутый менеджер ключей
	 */
	public String[] getClientAliases(String string, Principal[] principals) {
		return keyManager.getClientAliases(string, principals);
	}

	/**
	 * Выберите псевдоним клиента. Некоторые серверы неправильно настроены и утверждают,
	 * что принимают любой клиентский сертификат во время SSL-рукопожатия, однако OWA
	 * аутентифицируется только с использованием одного сертификата.
	 * <p/>
	 * Этот метод позволяет пользователю выбрать правильный клиентский сертификат
	 */
	public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
		log.debug("Find client certificates issued by: " + Arrays.asList(issuers));
		// Build a list of all aliases
		ArrayList<String> aliases = new ArrayList<>();
		for (String keyTypeValue : keyType) {
			String[] keyAliases = keyManager.getClientAliases(keyTypeValue, issuers);

			if (keyAliases != null) {
				aliases.addAll(Arrays.asList(keyAliases));
			}
		}

		// If there are more than one show a dialog and return the selected alias
		if (aliases.size() > 1) {

			// If there's a saved pattern try to match it
			if (cachedAlias != null) {
				for (String alias : aliases) {
					if (cachedAlias.equals(stripAlias(alias))) {
						log.debug(alias + " matched cached alias: " + cachedAlias);
						return alias;
					}
				}

				// pattern didn't match, clear the pattern and ask user to select an alias
				cachedAlias = null;
			}

			String[] aliasesArray = aliases.toArray(new String[0]);
			String[] descriptionsArray = new String[aliasesArray.length];
			int i = 0;
			for (String alias : aliasesArray) {
				X509Certificate certificate = getCertificateChain(alias)[0];
				String subject = certificate.getSubjectX500Principal().getName();
				if (subject.contains("=")) {
					subject = subject.substring(subject.indexOf("=") + 1);
				}
				if (subject.contains(",")) {
					subject = subject.substring(0, subject.indexOf(","));
				}
				try {
					for (List<?> subjectAltName : certificate.getSubjectAlternativeNames()) {
						if (subjectAltName.get(1) instanceof String) {
							subject = " " + subjectAltName.get(1);
						}
					}
				}
				catch (Exception e) {
					// ignore
				}
				String issuer = certificate.getIssuerX500Principal().getName();
				if (issuer.contains("=")) {
					issuer = issuer.substring(issuer.indexOf("=") + 1);
				}
				if (issuer.contains(",")) {
					issuer = issuer.substring(0, issuer.indexOf(","));
				}
				descriptionsArray[i++] = subject + " [" + issuer + "]";
			}
			String selectedAlias;
			if (Settings.getBooleanProperty("mt.ews.server") || GraphicsEnvironment.isHeadless()) {
				// headless or server mode
				selectedAlias = chooseClientAlias(aliasesArray, descriptionsArray);
			}
			else {
				SelectCertificateDialog selectCertificateDialog = new SelectCertificateDialog(aliasesArray,
						descriptionsArray);

				selectedAlias = selectCertificateDialog.getSelectedAlias();
				log.debug("User selected Key Alias: " + selectedAlias);
			}

			cachedAlias = stripAlias(selectedAlias);
			log.debug("Stored Key Alias Pattern: " + cachedAlias);

			return selectedAlias;

			// exactly one, simply return that and don't bother the user
		}
		else if (aliases.size() == 1) {
			log.debug("One Private Key found, returning that");
			return aliases.get(0);

			// none, return null
		}
		else {
			log.debug("No Private Keys found");
			return null;
		}
	}

	private String chooseClientAlias(String[] aliasesArray, String[] descriptionsArray) {
		System.out.println("Choose client alias:");
		int i = 1;
		for (String aliasDescription : descriptionsArray) {
			System.out.println(i++ + ": " + aliasDescription);
		}
		BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
		int chosenIndex = 0;
		while (chosenIndex == 0 || chosenIndex > descriptionsArray.length) {
			try {
				System.out.print("Alias: ");
				chosenIndex = Integer.parseInt(inReader.readLine());
			}
			catch (NumberFormatException | IOException e) {
				System.out.println("Invalid");
			}
		}

		return aliasesArray[chosenIndex - 1];
	}

	/**
	 * Алиасы PKCS11 имеют формат: dd.0, где dd увеличивается каждый раз, когда соединение
	 * SSL переобговаривается
	 * @param alias исходный алиас
	 * @return алиас без префикса
	 */
	protected String stripAlias(String alias) {
		String value = alias;
		if (value != null && value.length() > 1) {
			char firstChar = value.charAt(0);
			int dotIndex = value.indexOf('.');
			if (firstChar >= '0' && firstChar <= '9' && dotIndex >= 0) {
				value = value.substring(dotIndex + 1);
			}
		}
		return value;
	}

	/**
	 * Проксирование к обернутому менеджеру ключей
	 */
	public String[] getServerAliases(String string, Principal[] prncpls) {
		return keyManager.getServerAliases(string, prncpls);
	}

	/**
	 * Проксирование к обернутому менеджеру ключей
	 */
	public String chooseServerAlias(String string, Principal[] prncpls, Socket socket) {
		return keyManager.chooseServerAlias(string, prncpls, socket);
	}

	/**
	 * Проксирование к обернутому менеджеру ключей
	 */
	public X509Certificate[] getCertificateChain(String string) {
		X509Certificate[] certificates = keyManager.getCertificateChain(string);
		for (X509Certificate certificate : certificates) {
			log.debug("Certificate chain: " + certificate.getSubjectX500Principal());
		}
		return certificates;
	}

	/**
	 * Проксирование к обернутому менеджеру ключей
	 */
	public PrivateKey getPrivateKey(String string) {
		return keyManager.getPrivateKey(string);
	}

}
