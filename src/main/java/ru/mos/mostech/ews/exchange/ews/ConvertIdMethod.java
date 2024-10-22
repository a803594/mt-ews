/*
DIT
 */

package ru.mos.mostech.ews.exchange.ews;

import ru.mos.mostech.ews.exchange.XMLStreamUtil;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * ConvertId implementation to retrieve primary mailbox address
 */
public class ConvertIdMethod extends EWSMethod {

	/**
	 * Build Resolve Names method
	 * @param value search value
	 */
	public ConvertIdMethod(String value) {
		super("SourceIds", "ConvertId", "ResponseMessages");
		addMethodOption(new AttributeOption("DestinationFormat", "EwsId"));
		unresolvedEntry = new ElementOption("m:SourceIds", new AlternateId("EwsId", value));
	}

	@Override
	protected Item handleItem(XMLStreamReader reader) throws XMLStreamException {
		Item responseItem = new Item();
		responseItem.type = "AlternateId";
		// skip to AlternateId
		while (reader.hasNext() && !XMLStreamUtil.isStartTag(reader, "AlternateId")) {
			reader.next();
		}
		if (XMLStreamUtil.isStartTag(reader, "AlternateId")) {
			String mailbox = reader.getAttributeValue(null, "Mailbox");
			if (mailbox != null) {
				responseItem.put("Mailbox", mailbox);
			}
			while (reader.hasNext() && !XMLStreamUtil.isEndTag(reader, "AlternateId")) {
				reader.next();
			}
		}
		return responseItem;
	}

}
