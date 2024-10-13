/*
DIT
 */
package ru.mos.mostech.ews;

import org.apache.log4j.Logger;
import ru.mos.mostech.ews.caldav.CaldavServer;
import ru.mos.mostech.ews.exception.MosTechEwsException;
import ru.mos.mostech.ews.exchange.ExchangeSessionFactory;
import ru.mos.mostech.ews.exchange.auth.ExchangeAuthenticator;
import ru.mos.mostech.ews.http.HttpClientAdapter;
import ru.mos.mostech.ews.http.request.GetRequest;
import ru.mos.mostech.ews.imap.ImapServer;
import ru.mos.mostech.ews.ldap.LdapServer;
import ru.mos.mostech.ews.pop.PopServer;
import ru.mos.mostech.ews.smtp.SmtpServer;
import ru.mos.mostech.ews.ui.SimpleUi;
import ru.mos.mostech.ews.ui.tray.MosTechEwsTray;

import java.awt.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * DavGateway main class
 */
public final class MosTechEws {
    private static final Logger LOGGER = Logger.getLogger(MosTechEws.class);
    private static final String HTTP_MT_EWS_SOURCEFORGE_NET_VERSION_TXT = "";

    private static final Object LOCK = new Object();
    private static boolean shutdown = false;

    private MosTechEws() {
    }

    private static final ArrayList<AbstractServer> SERVER_LIST = new ArrayList<>();

    /**
     * Start the gateway, listen on specified smtp and pop3 ports
     *
     * @param args command line parameter config file path
     */
    public static void main(String[] args) throws IOException {
        boolean notray = false;
        boolean server = false;
        boolean token = false;
        for (String arg : args) {
            if (arg.startsWith("-")) {
                if ("-notray".equals(arg)) {
                    notray = true;
                } else if ("-server".equals(arg)) {
                    server = true;
                } else if ("-token".equals(arg)) {
                    token = true;
                }
            } else {
                Settings.setConfigFilePath(arg);
            }
        }

        Settings.load();
        if (token) {
            try {
                ExchangeAuthenticator authenticator = (ExchangeAuthenticator) Class.forName("ru.mos.mostech.ews.exchange.auth.O365InteractiveAuthenticator")
                        .getDeclaredConstructor().newInstance();
                authenticator.setUsername("");
                authenticator.authenticate();
                System.out.println(authenticator.getToken().getRefreshToken());
            } catch (IOException | ClassNotFoundException | NoSuchMethodException | InstantiationException |
                     IllegalAccessException | InvocationTargetException e) {
                System.err.println(e+" "+e.getMessage());
            }
            // force shutdown on Linux
            System.exit(0);
        } else {

            if (GraphicsEnvironment.isHeadless()) {
                // force server mode
                LOGGER.debug("Headless mode, do not create GUI");
                server = true;
            }
            if (server) {
                Settings.setProperty("mt.ews.server", "true");
                Settings.updateLoggingConfig();
            }


            if (Settings.getBooleanProperty("mt.ews.server")) {
                LOGGER.debug("Start MT-EWS in server mode");
            } else {
                LOGGER.debug("Start MT-EWS in GUI mode");
                if (!notray) {
                    SimpleUi.start();
                }
            }

            start();

            // server mode: all threads are daemon threads, do not let main stop
            if (Settings.getBooleanProperty("mt.ews.server")) {
                Runtime.getRuntime().addShutdownHook(new Thread("Shutdown") {
                    @Override
                    public void run() {
                        shutdown = true;
                        MosTechEwsTray.debug(new BundleMessage("LOG_GATEWAY_INTERRUPTED"));
                        MosTechEws.stop();
                        synchronized (LOCK) {
                            LOCK.notifyAll();
                        }
                    }
                });

                synchronized (LOCK) {
                    try {
                        while (!shutdown) {
                            LOCK.wait();
                        }
                    } catch (InterruptedException e) {
                        MosTechEwsTray.debug(new BundleMessage("LOG_GATEWAY_INTERRUPTED"));
                        Thread.currentThread().interrupt();
                    }
                }

            }
        }
    }

    /**
     * Start MT-EWS listeners.
     */
    public static void start() {
        SERVER_LIST.clear();

        int smtpPort = Settings.getIntProperty("mt.ews.smtpPort");
        if (smtpPort != 0) {
            SERVER_LIST.add(new SmtpServer(smtpPort));
        }
        int popPort = Settings.getIntProperty("mt.ews.popPort");
        if (popPort != 0) {
            SERVER_LIST.add(new PopServer(popPort));
        }
        int imapPort = Settings.getIntProperty("mt.ews.imapPort");
        if (imapPort != 0) {
            SERVER_LIST.add(new ImapServer(imapPort));
        }
        int caldavPort = Settings.getIntProperty("mt.ews.caldavPort");
        if (caldavPort != 0) {
            SERVER_LIST.add(new CaldavServer(caldavPort));
        }
        int ldapPort = Settings.getIntProperty("mt.ews.ldapPort");
        if (ldapPort != 0) {
            SERVER_LIST.add(new LdapServer(ldapPort));
        }

        BundleMessage.BundleMessageList messages = new BundleMessage.BundleMessageList();
        BundleMessage.BundleMessageList errorMessages = new BundleMessage.BundleMessageList();
        for (AbstractServer server : SERVER_LIST) {
            try {
                server.bind();
                server.start();
                messages.add(new BundleMessage("LOG_PROTOCOL_PORT", server.getProtocolName(), server.getPort()));
            } catch (MosTechEwsException e) {
                errorMessages.add(e.getBundleMessage());
            }
        }

        final String currentVersion = getCurrentVersion();
        boolean showStartupBanner = Settings.getBooleanProperty("mt.ews.showStartupBanner", true);
        if (showStartupBanner) {
            MosTechEwsTray.info(new BundleMessage("LOG_MT_EWS_GATEWAY_LISTENING", currentVersion, messages));
        }
        if (!errorMessages.isEmpty()) {
            MosTechEwsTray.error(new BundleMessage("LOG_MESSAGE", errorMessages));
        }

        // check for new version in a separate thread
        new Thread("CheckRelease") {
            @Override
            public void run() {
                String releasedVersion = getReleasedVersion();
                if (!currentVersion.isEmpty() && releasedVersion != null && currentVersion.compareTo(releasedVersion) < 0) {
                    MosTechEwsTray.info(new BundleMessage("LOG_NEW_VERSION_AVAILABLE", releasedVersion));
                }

            }
        }.start();

    }

    /**
     * Stop all listeners, shutdown connection pool and clear session cache.
     */
    public static void stop() {
        MosTechEws.stopServers();
        // close pooled connections
        ExchangeSessionFactory.shutdown();
        MosTechEwsTray.info(new BundleMessage("LOG_GATEWAY_STOP"));
        MosTechEwsTray.dispose();
    }

    /**
     * Stop all listeners and clear session cache.
     */
    public static void restart() {
        MosTechEws.stopServers();
        // clear session cache
        ExchangeSessionFactory.shutdown();
        MosTechEws.start();
    }

    private static void stopServers() {
        for (AbstractServer server : SERVER_LIST) {
            server.close();
            try {
                server.join();
            } catch (InterruptedException e) {
                MosTechEwsTray.warn(new BundleMessage("LOG_EXCEPTION_WAITING_SERVER_THREAD_DIE"), e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Get current MT-EWS version.
     *
     * @return current version
     */
    public static String getCurrentVersion() {
        Package mtEwsPackage = MosTechEws.class.getPackage();
        String currentVersion = mtEwsPackage.getImplementationVersion();
        if (currentVersion == null) {
            currentVersion = "";
        }
        return currentVersion;
    }

    /**
     * Get latest released version from SourceForge.
     *
     * @return latest version
     */
    public static String getReleasedVersion() {
        String version = null;
        if (!Settings.getBooleanProperty("mt.ews.disableUpdateCheck")) {
            try (HttpClientAdapter httpClientAdapter = new HttpClientAdapter(HTTP_MT_EWS_SOURCEFORGE_NET_VERSION_TXT)) {
                GetRequest getRequest = new GetRequest(HTTP_MT_EWS_SOURCEFORGE_NET_VERSION_TXT);
                getRequest = httpClientAdapter.executeFollowRedirect(getRequest);
                version = getRequest.getResponseBodyAsString();
                LOGGER.debug("MT-EWS released version: " + version);
            } catch (IOException e) {
                MosTechEwsTray.debug(new BundleMessage("LOG_UNABLE_TO_GET_RELEASED_VERSION"));
            }
        }
        return version;
    }
}
