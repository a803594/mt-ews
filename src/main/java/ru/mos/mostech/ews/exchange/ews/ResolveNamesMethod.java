/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import ru.mos.mostech.ews.exchange.XMLStreamUtil;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Метод разрешения имен.
 */
public class ResolveNamesMethod extends EWSMethod {

	protected static final AttributeOption RETURN_FULL_CONTACT_DATA = new AttributeOption("ReturnFullContactData",
			"true");

	/**
	 * Метод построения разрешения имен
	 * @param value значение поиска
	 */
	public ResolveNamesMethod(String value) {
		super("Contact", "ResolveNames", "ResolutionSet");
		addMethodOption(SearchScope.ActiveDirectory);
		addMethodOption(RETURN_FULL_CONTACT_DATA);
		unresolvedEntry = new ElementOption("m:UnresolvedEntry", value);
	}

	@Override
	protected Item handleItem(XMLStreamReader reader) throws XMLStreamException {
		Item responseItem = new Item();
		responseItem.type = "Contact";
		// skip to Contact
		while (reader.hasNext() && !XMLStreamUtil.isStartTag(reader, "Resolution")) {
			reader.next();
		}
		while (reader.hasNext() && !XMLStreamUtil.isEndTag(reader, "Resolution")) {
			reader.next();
			if (XMLStreamUtil.isStartTag(reader)) {
				String tagLocalName = reader.getLocalName();
				if ("Mailbox".equals(tagLocalName)) {
					handleMailbox(reader, responseItem);
				}
				else if ("Contact".equals(tagLocalName)) {
					handleContact(reader, responseItem);
				}
			}
		}
		return responseItem;
	}

	protected void handleMailbox(XMLStreamReader reader, Item responseItem) throws XMLStreamException {
		while (reader.hasNext() && !XMLStreamUtil.isEndTag(reader, "Mailbox")) {
			reader.next();
			if (XMLStreamUtil.isStartTag(reader)) {
				String tagLocalName = reader.getLocalName();
				if ("Name".equals(tagLocalName)) {
					responseItem.put(tagLocalName, XMLStreamUtil.getElementText(reader));
				}
				else if ("EmailAddress".equals(tagLocalName)) {
					responseItem.put(tagLocalName, XMLStreamUtil.getElementText(reader));
				}
			}
		}
	}

	protected void handleContact(XMLStreamReader reader, Item responseItem) throws XMLStreamException {
		while (reader.hasNext() && !XMLStreamUtil.isEndTag(reader, "Contact")) {
			reader.next();
			if (XMLStreamUtil.isStartTag(reader)) {
				String tagLocalName = reader.getLocalName();
				if ("EmailAddresses".equals(tagLocalName)) {
					handleEmailAddresses(reader, responseItem);
				}
				else if ("PhysicalAddresses".equals(tagLocalName)) {
					handlePhysicalAddresses(reader, responseItem);
				}
				else if ("PhoneNumbers".equals(tagLocalName)) {
					handlePhoneNumbers(reader, responseItem);
				}
				else {
					responseItem.put(tagLocalName, XMLStreamUtil.getElementText(reader));
				}
			}
		}
	}

	protected void handlePhysicalAddress(XMLStreamReader reader, Item responseItem, String addressType)
			throws XMLStreamException {
		while (reader.hasNext() && !XMLStreamUtil.isEndTag(reader, "Entry")) {
			reader.next();
			if (XMLStreamUtil.isStartTag(reader)) {
				String tagLocalName = reader.getLocalName();
				String value = XMLStreamUtil.getElementText(reader);
				responseItem.put(addressType + tagLocalName, value);
			}
		}
	}

	protected void handlePhysicalAddresses(XMLStreamReader reader, Item responseItem) throws XMLStreamException {
		while (reader.hasNext() && !XMLStreamUtil.isEndTag(reader, "PhysicalAddresses")) {
			reader.next();
			if (XMLStreamUtil.isStartTag(reader)) {
				String tagLocalName = reader.getLocalName();
				if ("Entry".equals(tagLocalName)) {
					String key = getAttributeValue(reader, "Key");
					handlePhysicalAddress(reader, responseItem, key);
				}
			}
		}
	}

	protected void handlePhoneNumbers(XMLStreamReader reader, Item responseItem) throws XMLStreamException {
		while (reader.hasNext() && !XMLStreamUtil.isEndTag(reader, "PhoneNumbers")) {
			reader.next();
			if (XMLStreamUtil.isStartTag(reader)) {
				String tagLocalName = reader.getLocalName();
				if ("Entry".equals(tagLocalName)) {
					String key = getAttributeValue(reader, "Key");
					String value = XMLStreamUtil.getElementText(reader);
					responseItem.put(key, value);
				}
			}
		}
	}

	@Override
	protected void handleEmailAddresses(XMLStreamReader reader, Item responseItem) throws XMLStreamException {
		while (reader.hasNext() && !XMLStreamUtil.isEndTag(reader, "EmailAddresses")) {
			reader.next();
			if (XMLStreamUtil.isStartTag(reader)) {
				String tagLocalName = reader.getLocalName();
				if ("Entry".equals(tagLocalName)) {
					String value = XMLStreamUtil.getElementText(reader);
					if (value != null) {
						if (value.startsWith("smtp:") || value.startsWith("SMTP:")) {
							value = value.substring(5);
							// get smtp address only if not already available through
							// Mailbox info
							responseItem.putIfAbsent("EmailAddress", value);
						}
					}
				}
			}
		}
	}

}
