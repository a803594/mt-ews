/*
DIT
 */

package ru.mos.mostech.ews.exchange.ews;

import ru.mos.mostech.ews.exchange.XMLStreamUtil;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.Writer;

public class GetUserPhotoMethod extends EWSMethod {

	public enum SizeRequested {

		HR48x48, HR64x64, HR96x96, HR120x120, HR240x240, HR360x360, HR432x432, HR504x504, HR648x648

	}

	protected String email;

	protected SizeRequested sizeRequested;

	protected String contentType = null;

	protected String pictureData = null;

	/**
	 * Get User Configuration method.
	 */
	public GetUserPhotoMethod(String email, SizeRequested sizeRequested) {
		super("GetUserPhoto", "GetUserPhoto");
		this.email = email;
		this.sizeRequested = sizeRequested;
	}

	@Override
	protected void writeSoapBody(Writer writer) throws IOException {
		writer.write("<m:Email>");
		writer.write(email);
		writer.write("</m:Email>");

		writer.write("<m:SizeRequested>");
		writer.write(sizeRequested.toString());
		writer.write("</m:SizeRequested>");

	}

	@Override
	protected void handleCustom(XMLStreamReader reader) throws XMLStreamException {
		if (XMLStreamUtil.isStartTag(reader, "PictureData")) {
			pictureData = reader.getElementText();
			if (pictureData.isEmpty()) {
				pictureData = null;
			}
		}
		if (XMLStreamUtil.isStartTag(reader, "ContentType")) {
			contentType = reader.getElementText();
		}

	}

	public String getContentType() {
		return contentType;
	}

	public String getPictureData() {
		return pictureData;
	}

}
