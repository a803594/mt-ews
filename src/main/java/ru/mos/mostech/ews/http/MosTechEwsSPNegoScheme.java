/*
DIT
 */

package ru.mos.mostech.ews.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.Credentials;
import org.apache.http.impl.auth.SPNegoScheme;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;

import javax.security.auth.RefreshFailedException;
import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.security.PrivilegedAction;
import java.security.Security;

/**
 * Override native SPNegoScheme to handle Kerberos.
 * Try to get Kerberos ticket from session, if this fails use callbacks to get credentials from user.
 */
@Slf4j
public class MosTechEwsSPNegoScheme extends SPNegoScheme {
    
    protected static final Object LOCK = new Object();
    protected static final KerberosHelper.KerberosCallbackHandler KERBEROS_CALLBACK_HANDLER;
    private static LoginContext clientLoginContext;

    static {
        // Load Jaas configuration from class
        Security.setProperty("login.configuration.provider", "mt.ews.http.KerberosLoginConfiguration");
        // Kerberos callback handler singleton
        KERBEROS_CALLBACK_HANDLER = new KerberosHelper.KerberosCallbackHandler();
    }

    public MosTechEwsSPNegoScheme(final boolean stripPort, final boolean useCanonicalHostname) {
        super(stripPort, useCanonicalHostname);
    }

    public MosTechEwsSPNegoScheme(final boolean stripPort) {
        super(stripPort);
    }

    public MosTechEwsSPNegoScheme() {
        super();
    }

    @Override
    protected byte[] generateGSSToken(final byte[] input, final Oid oid, final String authServer, final Credentials credentials) throws GSSException {
        String protocol = "HTTP";

        log.debug("KerberosHelper.initSecurityContext " + protocol + '@' + authServer + ' ' + input.length + " bytes token");

        synchronized (LOCK) {
            // check cached TGT
            if (clientLoginContext != null) {
                for (Object ticket : clientLoginContext.getSubject().getPrivateCredentials(KerberosTicket.class)) {
                    KerberosTicket kerberosTicket = (KerberosTicket) ticket;
                    if (kerberosTicket.getServer().getName().startsWith("krbtgt") && !kerberosTicket.isCurrent()) {
                        log.debug("KerberosHelper.clientLogin cached TGT expired, try to relogin");
                        clientLoginContext = null;
                    }
                }
            }
            // create client login context
            if (clientLoginContext == null) {
                final LoginContext localLoginContext;
                try {
                    localLoginContext = new LoginContext("spnego-client", KERBEROS_CALLBACK_HANDLER);
                    localLoginContext.login();
                    clientLoginContext = localLoginContext;
                } catch (LoginException e) {
                    log.error(e.getMessage(), e);
                    throw new GSSException(GSSException.FAILURE);
                }
            }
            // try to renew almost expired tickets
            for (Object ticket : clientLoginContext.getSubject().getPrivateCredentials(KerberosTicket.class)) {
                KerberosTicket kerberosTicket = (KerberosTicket) ticket;
                log.debug("KerberosHelper.clientLogin ticket for " + kerberosTicket.getServer().getName() + " expires at " + kerberosTicket.getEndTime());
                if (kerberosTicket.getEndTime().getTime() < System.currentTimeMillis() + 10000) {
                    if (kerberosTicket.isRenewable()) {
                        try {
                            kerberosTicket.refresh();
                        } catch (RefreshFailedException e) {
                            log.debug("KerberosHelper.clientLogin failed to renew ticket " + kerberosTicket);
                        }
                    } else {
                        log.debug("KerberosHelper.clientLogin ticket is not renewable");
                    }
                }
            }

            Object result = internalGenerateGSSToken(input, oid, authServer, credentials);

            if (result instanceof GSSException) {
                log.info("KerberosHelper.initSecurityContext exception code " + ((GSSException) result).getMajor() + " minor code " + ((GSSException) result).getMinor() + " message " + ((Throwable) result).getMessage());
                throw (GSSException) result;
            }

            log.debug("KerberosHelper.initSecurityContext return " + ((byte[]) result).length + " bytes token");
            return (byte[]) result;
        }
    }

    protected Object internalGenerateGSSToken(final byte[] input, final Oid oid, final String authServer, final Credentials credentials) {
        return Subject.doAs(clientLoginContext.getSubject(), (PrivilegedAction<Object>) () -> {
            Object result;
            try {
                result = super.generateGSSToken(input, oid, authServer, credentials);
            } catch (GSSException e) {
                result = e;
            }
            return result;
        });
    }

}
