package ru.mos.mostech.ews.server;

import lombok.experimental.UtilityClass;

@UtilityClass
public class IspResponse {

	private final String TEMPLATE = """
			<?xml version="1.0"?>
			<clientConfig version="1.1">
			    <emailProvider id="mt-ews">
			        <domain>${domain}</domain>
			        <displayName>${domain}</displayName>
			        <displayShortName>${domain}</displayShortName>

			        <incomingServer type="imap">
			            <hostname>localhost</hostname>
			            <port>${imapPort}</port>
			            <socketType>${socketType}</socketType>
			            <username>${username}</username>
			            <authentication>password-cleartext</authentication>
			        </incomingServer>

			        <outgoingServer type="smtp">
			            <hostname>localhost</hostname>
			            <port>${smtpPort}</port>
			            <socketType>${socketType}</socketType>
			            <username>${username}</username>
			            <authentication>password-cleartext</authentication>
			        </outgoingServer>

			    </emailProvider>

			    <addressBook type="carddav">
			        <username>${username}</username>
			         <authentication>http-basic</authentication>
			        <serverURL>${davUrl}</serverURL>
			    </addressBook>

			    <calendar type="caldav">
			        <username>${username}</username>
			        <authentication>http-basic</authentication>
			        <serverURL>${davUrl}</serverURL>
			    </calendar>

			</clientConfig>
			""";

	public String build(String domain, String username, int imapPort, int smtpPort, int davPort, boolean isSecure) {

		// String davUrl = (isSecure ? "https://" : "http://") + "localhost:" + davPort;
		String davUrl = "http://" + "localhost:" + davPort;
		String socketType = isSecure ? "ssl" : "plain";
		return TEMPLATE.replace("${domain}", domain)
			.replace("${username}", username)
			.replace("${imapPort}", String.valueOf(imapPort))
			.replace("${smtpPort}", String.valueOf(smtpPort))
			.replace("${davUrl}", davUrl)
			.replace("${socketType}", socketType);
	}

}
