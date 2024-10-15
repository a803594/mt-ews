/*
DIT
 */

package ru.mos.mostech.ews.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.AuthScheme;
import org.apache.http.protocol.HttpContext;


@Slf4j
public class MosTechEwsNTLMSchemeFactory implements org.apache.http.auth.AuthSchemeProvider {
    @Override
    public AuthScheme create(HttpContext context) {
        return new MosTechEwsNTLMScheme();
    }
}
