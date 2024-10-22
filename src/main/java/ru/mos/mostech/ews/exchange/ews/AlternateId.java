/*
DIT
 */

package ru.mos.mostech.ews.exchange.ews;

import ru.mos.mostech.ews.util.StringUtil;

import java.io.IOException;
import java.io.Writer;

public class AlternateId extends ElementOption {

	String format;

	// fake mailbox email
	String mailbox = "blah@blah.com";

	protected AlternateId(String format, String id) {
		super("AlternateId", id);
		this.format = format;
	}

	protected AlternateId(String format, String id, String mailbox) {
		super("AlternateId", id);
		this.format = format;
		this.mailbox = mailbox;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void write(Writer writer) throws IOException {
		writer.write("<t:AlternateId  Format=\"");
		writer.write(format);
		writer.write("\" Id=\"");
		writer.write(StringUtil.xmlEncode(value));
		writer.write("\" Mailbox=\"");
		writer.write(StringUtil.xmlEncode(mailbox));
		writer.write("\"/>");
	}

}
