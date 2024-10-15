/*
DIT
 */

package ru.mos.mostech.ews.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Logger;
import ru.mos.mostech.ews.Settings;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Custom proxy selector based on MT-EWS settings.
 * Interactive O365 authentication relies on native HttpUrlConnection so we need to override default proxy selector.
 */

@Slf4j
public class MosTechEwsProxySelector extends ProxySelector {
    static final Logger LOGGER = Logger.getLogger(MosTechEwsProxySelector.class);

    static final List<Proxy> DIRECT = Collections.singletonList(Proxy.NO_PROXY);

    ProxySelector proxySelector;

    public MosTechEwsProxySelector(ProxySelector proxySelector) {
        this.proxySelector = proxySelector;
    }

    @Override
    public List<Proxy> select(URI uri) {
        boolean useSystemProxies = Settings.getBooleanProperty("mt.ews.useSystemProxies", Boolean.FALSE);
        boolean enableProxy = Settings.getBooleanProperty("mt.ews.enableProxy");
        String proxyHost = Settings.getProperty("mt.ews.proxyHost");
        int proxyPort = Settings.getIntProperty("mt.ews.proxyPort");
        String scheme = uri.getScheme();
        if ("socket".equals(scheme)) {
            return DIRECT;
        } else if (useSystemProxies) {
            List<Proxy> proxyes = proxySelector.select(uri);
            LOGGER.debug("Selected " + proxyes + " proxy for " + uri);
            return proxyes;
        } else if (enableProxy
                && proxyHost != null && proxyHost.length() > 0 && proxyPort > 0
                && !isNoProxyFor(uri)
                && ("http".equals(scheme) || "https".equals(scheme))) {
            // MT-EWS defined proxies
            ArrayList<Proxy> proxies = new ArrayList<>();
            proxies.add(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxyHost, proxyPort)));
            return proxies;
        } else {
            return DIRECT;
        }
    }

    private boolean isNoProxyFor(URI uri) {
        final String noProxyFor = Settings.getProperty("mt.ews.noProxyFor");
        if (noProxyFor != null) {
            final String urihost = uri.getHost().toLowerCase();
            final String[] domains = noProxyFor.toLowerCase().split(",\\s*");
            for (String domain : domains) {
                if (urihost.endsWith(domain)) {
                    return true; //break;
                }
            }
        }
        return false;
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        LOGGER.debug("Connection to " + uri + " failed, socket address " + sa + " " + ioe);
        proxySelector.connectFailed(uri, sa, ioe);
    }
}