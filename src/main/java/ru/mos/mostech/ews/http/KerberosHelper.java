/*
DIT
 */
package ru.mos.mostech.ews.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Logger;
import org.ietf.jgss.*;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.ui.CredentialPromptDialog;

import javax.security.auth.RefreshFailedException;
import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.PrivilegedAction;
import java.security.Security;


/**
 * Kerberos helper class.
 */

@Slf4j
public class KerberosHelper {
    protected static final Logger LOGGER = Logger.getLogger(KerberosHelper.class);
    protected static final Object LOCK = new Object();
    protected static final KerberosCallbackHandler KERBEROS_CALLBACK_HANDLER;
    private static LoginContext clientLoginContext;

    static {
        // Load Jaas configuration from class
        Security.setProperty("login.configuration.provider", "mt.ews.http.KerberosLoginConfiguration");
        // Kerberos callback handler singleton
        KERBEROS_CALLBACK_HANDLER = new KerberosCallbackHandler();
    }

    private KerberosHelper() {
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")

@Slf4j
    protected static class KerberosCallbackHandler implements CallbackHandler {
        String principal;
        String password;

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback callback : callbacks) {
                if (callback instanceof NameCallback) {
                    if (principal == null) {
                        // if we get there kerberos token is missing or invalid
                        if (Settings.getBooleanProperty("mt.ews.server") || GraphicsEnvironment.isHeadless()) {
                            // headless or server mode
                            System.out.print(((NameCallback) callback).getPrompt());
                            BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
                            principal = inReader.readLine();
                        } else {
                            CredentialPromptDialog credentialPromptDialog = new CredentialPromptDialog(((NameCallback) callback).getPrompt());
                            principal = credentialPromptDialog.getPrincipal();
                            password = String.valueOf(credentialPromptDialog.getPassword());
                        }
                    }
                    if (principal == null) {
                        throw new IOException("KerberosCallbackHandler: failed to retrieve principal");
                    }
                    ((NameCallback) callback).setName(principal);

                } else if (callback instanceof PasswordCallback) {
                    if (password == null) {
                        // if we get there kerberos token is missing or invalid
                        if (Settings.getBooleanProperty("mt.ews.server") || GraphicsEnvironment.isHeadless()) {
                            // headless or server mode
                            System.out.print(((PasswordCallback) callback).getPrompt());
                            BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
                            password = inReader.readLine();
                        }
                    }
                    if (password == null) {
                        throw new IOException("KerberosCallbackHandler: failed to retrieve password");
                    }
                    ((PasswordCallback) callback).setPassword(password.toCharArray());

                } else {
                    throw new UnsupportedCallbackException(callback);
                }
            }
        }
    }

    /**
     * Force client principal in callback handler
     *
     * @param principal client principal
     */
    public static void setClientPrincipal(String principal) {
        KERBEROS_CALLBACK_HANDLER.principal = principal;
    }

    /**
     * Force client password in callback handler
     *
     * @param password client password
     */
    public static void setClientPassword(String password) {
        KERBEROS_CALLBACK_HANDLER.password = password;
    }

    /**
     * Get response Kerberos token for host with provided token.
     *
     * @param protocol target protocol
     * @param host     target host
     * @param token    input token
     * @return response token
     * @throws GSSException   on error
     * @throws LoginException on error
     */
    public static byte[] initSecurityContext(final String protocol, final String host, final byte[] token) throws GSSException, LoginException {
        return initSecurityContext(protocol, host, null, token);
    }

    /**
     * Get response Kerberos token for host with provided token, use client provided delegation credentials.
     * Used to authenticate with target host on a gateway server with client credentials,
     * gateway must have its own principal authorized for delegation
     *
     * @param protocol             target protocol
     * @param host                 target host
     * @param delegatedCredentials client delegated credentials
     * @param token                input token
     * @return response token
     * @throws GSSException   on error
     * @throws LoginException on error
     */
    public static byte[] initSecurityContext(final String protocol, final String host, final GSSCredential delegatedCredentials, final byte[] token) throws GSSException, LoginException {
        LOGGER.debug("KerberosHelper.initSecurityContext " + protocol + '@' + host + ' ' + token.length + " bytes token");

        synchronized (LOCK) {
            // check cached TGT
            if (clientLoginContext != null) {
                for (Object ticket : clientLoginContext.getSubject().getPrivateCredentials(KerberosTicket.class)) {
                    KerberosTicket kerberosTicket = (KerberosTicket) ticket;
                    if (kerberosTicket.getServer().getName().startsWith("krbtgt") && !kerberosTicket.isCurrent()) {
                        LOGGER.debug("KerberosHelper.clientLogin cached TGT expired, try to relogin");
                        clientLoginContext = null;
                    }
                }
            }
            // create client login context
            if (clientLoginContext == null) {
                final LoginContext localLoginContext = new LoginContext("spnego-client", KERBEROS_CALLBACK_HANDLER);
                localLoginContext.login();
                clientLoginContext = localLoginContext;
            }
            // try to renew almost expired tickets
            for (Object ticket : clientLoginContext.getSubject().getPrivateCredentials(KerberosTicket.class)) {
                KerberosTicket kerberosTicket = (KerberosTicket) ticket;
                LOGGER.debug("KerberosHelper.clientLogin ticket for " + kerberosTicket.getServer().getName() + " expires at " + kerberosTicket.getEndTime());
                if (kerberosTicket.getEndTime().getTime() < System.currentTimeMillis() + 10000) {
                    if (kerberosTicket.isRenewable()) {
                        try {
                            kerberosTicket.refresh();
                        } catch (RefreshFailedException e) {
                            LOGGER.debug("KerberosHelper.clientLogin failed to renew ticket " + kerberosTicket);
                        }
                    } else {
                        LOGGER.debug("KerberosHelper.clientLogin ticket is not renewable");
                    }
                }
            }

            Object result = internalInitSecContext(protocol, host, delegatedCredentials, token);
            if (result instanceof GSSException) {
                LOGGER.info("KerberosHelper.initSecurityContext exception code " + ((GSSException) result).getMajor() + " minor code " + ((GSSException) result).getMinor() + " message " + ((Throwable) result).getMessage());
                throw (GSSException) result;
            }

            LOGGER.debug("KerberosHelper.initSecurityContext return " + ((byte[]) result).length + " bytes token");
            return (byte[]) result;
        }
    }

    protected static Object internalInitSecContext(final String protocol, final String host, final GSSCredential delegatedCredentials, final byte[] token) {
        return Subject.doAs(clientLoginContext.getSubject(), (PrivilegedAction<Object>) () -> {
            Object result;
            GSSContext context = null;
            try {
                GSSManager manager = GSSManager.getInstance();
                GSSName serverName = manager.createName(protocol + '@' + host, GSSName.NT_HOSTBASED_SERVICE);
                // Kerberos v5 OID
                Oid krb5Oid = new Oid("1.2.840.113554.1.2.2");

                context = manager.createContext(serverName, krb5Oid, delegatedCredentials, GSSContext.DEFAULT_LIFETIME);

                //context.requestMutualAuth(true);
                // TODO: used by IIS to pass token to Exchange ?
                context.requestCredDeleg(true);

                result = context.initSecContext(token, 0, token.length);
            } catch (GSSException e) {
                result = e;
            } finally {
                if (context != null) {
                    try {
                        context.dispose();
                    } catch (GSSException e) {
                        LOGGER.debug("KerberosHelper.internalInitSecContext " + e + ' ' + e.getMessage());
                    }
                }
            }
            return result;
        });
    }

    /**
     * Create server side Kerberos login context for provided credentials.
     *
     * @param serverPrincipal server principal
     * @param serverPassword  server passsword
     * @return LoginContext server login context
     * @throws LoginException on error
     */
    public static LoginContext serverLogin(final String serverPrincipal, final String serverPassword) throws LoginException {
        LoginContext serverLoginContext = new LoginContext("spnego-server", callbacks -> {
            for (Callback callback : callbacks) {
                if (callback instanceof NameCallback) {
                    final NameCallback nameCallback = (NameCallback) callback;
                    nameCallback.setName(serverPrincipal);
                } else if (callback instanceof PasswordCallback) {
                    final PasswordCallback passCallback = (PasswordCallback) callback;
                    passCallback.setPassword(serverPassword.toCharArray());
                } else {
                    throw new UnsupportedCallbackException(callback);
                }
            }

        });
        serverLoginContext.login();
        return serverLoginContext;
    }

    /**
     * Contains server Kerberos context information in server mode.
     */

@Slf4j
    public static class SecurityContext {
        /**
         * response token
         */
        public byte[] token;
        /**
         * authenticated principal
         */
        public String principal;
        /**
         * client delegated credential
         */
        public GSSCredential clientCredential;
    }

    /**
     * Check client provided Kerberos token in server login context
     *
     * @param serverLoginContext server login context
     * @param token              Kerberos client token
     * @return result with client principal and optional returned Kerberos token
     * @throws GSSException on error
     */
    public static SecurityContext acceptSecurityContext(LoginContext serverLoginContext, final byte[] token) throws GSSException {
        Object result = Subject.doAs(serverLoginContext.getSubject(), (PrivilegedAction<Object>) () -> {
            Object innerResult;
            SecurityContext securityContext = new SecurityContext();
            GSSContext context = null;
            try {
                GSSManager manager = GSSManager.getInstance();

                // get server credentials from context
                Oid krb5oid = new Oid("1.2.840.113554.1.2.2");
                GSSCredential serverCreds = manager.createCredential(null/* use name from login context*/,
                        GSSCredential.DEFAULT_LIFETIME,
                        krb5oid,
                        GSSCredential.ACCEPT_ONLY/* server mode */);
                context = manager.createContext(serverCreds);

                securityContext.token = context.acceptSecContext(token, 0, token.length);
                if (context.isEstablished()) {
                    securityContext.principal = context.getSrcName().toString();
                    LOGGER.debug("Authenticated user: " + securityContext.principal);
                    if (!context.getCredDelegState()) {
                        LOGGER.debug("Credentials can not be delegated");
                    } else {
                        // Get client delegated credentials from context (gateway mode)
                        securityContext.clientCredential = context.getDelegCred();
                    }
                }
                innerResult = securityContext;
            } catch (GSSException e) {
                innerResult = e;
            } finally {
                if (context != null) {
                    try {
                        context.dispose();
                    } catch (GSSException e) {
                        LOGGER.debug("KerberosHelper.acceptSecurityContext " + e + ' ' + e.getMessage());
                    }
                }
            }
            return innerResult;
        });
        if (result instanceof GSSException) {
            LOGGER.info("KerberosHelper.acceptSecurityContext exception code " + ((GSSException) result).getMajor() + " minor code " + ((GSSException) result).getMinor() + " message " + ((Throwable) result).getMessage());
            throw (GSSException) result;
        }
        return (SecurityContext) result;
    }
}