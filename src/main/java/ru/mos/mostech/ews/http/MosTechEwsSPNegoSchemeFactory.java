/*
DIT
 */

package ru.mos.mostech.ews.http;

import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthSchemeFactory;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

/**
 * Override native SPNegoSchemeFactory to load MT-EWS specific Kerberos settings.
 */
public class MosTechEwsSPNegoSchemeFactory implements AuthSchemeFactory, AuthSchemeProvider {

    private final boolean stripPort;
    private final boolean useCanonicalHostname;

    /**
     * @since 4.4
     */
    public MosTechEwsSPNegoSchemeFactory(final boolean stripPort, final boolean useCanonicalHostname) {
        super();
        this.stripPort = stripPort;
        this.useCanonicalHostname = useCanonicalHostname;
    }

    public MosTechEwsSPNegoSchemeFactory(final boolean stripPort) {
        super();
        this.stripPort = stripPort;
        this.useCanonicalHostname = true;
    }

    public MosTechEwsSPNegoSchemeFactory() {
        this(true, true);
    }

    public boolean isStripPort() {
        return stripPort;
    }

    public boolean isUseCanonicalHostname() {
        return useCanonicalHostname;
    }

    @Override
    public AuthScheme newInstance(final HttpParams params) {
        return new MosTechEwsSPNegoScheme(this.stripPort, this.useCanonicalHostname);
    }

    @Override
    public AuthScheme create(final HttpContext context) {
        return new MosTechEwsSPNegoScheme(this.stripPort, this.useCanonicalHostname);
    }

}
