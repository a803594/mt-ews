/*
DIT
 */

package ru.mos.mostech.ews.http;

import org.apache.http.auth.AuthScheme;
import org.apache.http.protocol.HttpContext;

public class MosTechEwsNTLMSchemeFactory implements org.apache.http.auth.AuthSchemeProvider {
    @Override
    public AuthScheme create(HttpContext context) {
        return new MosTechEwsNTLMScheme();
    }
}
