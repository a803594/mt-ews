/*
DIT
 */

package ru.mos.mostech.ews.http;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.auth.*;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.impl.auth.AuthSchemeBase;
import org.apache.http.impl.auth.NTLMEngineException;
import org.apache.http.message.BufferedHeader;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.CharArrayBuffer;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.security.cert.Certificate;

/**
 * Дубликат NTLMScheme из HttpClient для реализации связывания каналов.
 */
public class MosTechEwsNTLMScheme extends AuthSchemeBase {

	enum State {

		UNINITIATED, CHALLENGE_RECEIVED, MSG_TYPE1_GENERATED, MSG_TYPE2_RECEVIED, MSG_TYPE3_GENERATED, FAILED,

	}

	private State state;

	private final MosTechEwsNTLMEngineImpl engine;

	private HttpContext httpContext;

	private String challenge;

	public MosTechEwsNTLMScheme() {
		this.engine = new MosTechEwsNTLMEngineImpl();
		this.state = State.UNINITIATED;
		this.challenge = null;
	}

	@Override
	protected void parseChallenge(CharArrayBuffer buffer, int beginIndex, int endIndex)
			throws MalformedChallengeException {
		this.challenge = buffer.substringTrimmed(beginIndex, endIndex);
		if (this.challenge.isEmpty()) {
			if (this.state == State.UNINITIATED) {
				this.state = State.CHALLENGE_RECEIVED;
			}
			else {
				this.state = State.FAILED;
			}
		}
		else {
			if (this.state.compareTo(State.MSG_TYPE1_GENERATED) < 0) {
				this.state = State.FAILED;
				throw new MalformedChallengeException("Out of sequence NTLM response message");
			}
			else if (this.state == State.MSG_TYPE1_GENERATED) {
				this.state = State.MSG_TYPE2_RECEVIED;
			}
		}
	}

	@Override
	public String getSchemeName() {
		return "ntlm";
	}

	@Override
	public String getParameter(String name) {
		// String parameters not supported
		return null;
	}

	@Override
	public String getRealm() {
		// NTLM does not support the concept of an authentication realm
		return null;
	}

	@Override
	public boolean isConnectionBased() {
		return true;
	}

	@Override
	public boolean isComplete() {
		return this.state == State.MSG_TYPE3_GENERATED || this.state == State.FAILED;
	}

	@Override
	public Header authenticate(final Credentials credentials, final HttpRequest request, final HttpContext httpContext)
			throws AuthenticationException {
		this.httpContext = httpContext;
		return authenticate(credentials, request);
	}

	@Override
	public Header authenticate(Credentials credentials, HttpRequest request) throws AuthenticationException {
		NTCredentials ntcredentials = null;
		try {
			ntcredentials = (NTCredentials) credentials;
		}
		catch (final ClassCastException e) {
			throw new InvalidCredentialsException(
					"Credentials cannot be used for NTLM authentication: " + credentials.getClass().getName());
		}
		String response = null;
		if (this.state == State.FAILED) {
			throw new AuthenticationException("NTLM authentication failed");
		}
		else if (this.state == State.CHALLENGE_RECEIVED) {
			response = this.engine.generateType1Msg(ntcredentials.getDomain(), ntcredentials.getWorkstation());
			this.state = State.MSG_TYPE1_GENERATED;
		}
		else if (this.state == State.MSG_TYPE2_RECEVIED) {
			// retrieve certificate from connection and pass it to NTLM engine
			ManagedHttpClientConnection routedConnection = (ManagedHttpClientConnection) httpContext
				.getAttribute(ExecutionContext.HTTP_CONNECTION);
			try {
				Certificate[] certificates = routedConnection.getSSLSession().getPeerCertificates();
				this.engine.setPeerServerCertificate(certificates[0]);
			}
			catch (SSLPeerUnverifiedException e) {
				throw new NTLMEngineException(e.getMessage(), e);
			}
			response = this.engine.generateType3Msg(ntcredentials.getUserName(), ntcredentials.getPassword(),
					ntcredentials.getDomain(), ntcredentials.getWorkstation(), this.challenge);
			this.state = State.MSG_TYPE3_GENERATED;
		}
		else {
			throw new AuthenticationException("Unexpected state: " + this.state);
		}
		final CharArrayBuffer buffer = new CharArrayBuffer(32);
		if (isProxy()) {
			buffer.append(AUTH.PROXY_AUTH_RESP);
		}
		else {
			buffer.append(AUTH.WWW_AUTH_RESP);
		}
		buffer.append(": NTLM ");
		buffer.append(response);
		return new BufferedHeader(buffer);
	}

}
