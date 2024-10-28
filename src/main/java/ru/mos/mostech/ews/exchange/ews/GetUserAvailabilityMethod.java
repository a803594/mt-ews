/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import ru.mos.mostech.ews.exchange.XMLStreamUtil;

import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.Writer;

/**
 * Метод GetUserAvailability.
 */
public class GetUserAvailabilityMethod extends EWSMethod {

	protected final String attendee;

	protected final String start;

	protected final String end;

	protected String mergedFreeBusy;

	protected final int interval;

	/**
	 * Создание метода EWS
	 * @param attendee адрес электронной почты участника
	 * @param start дата начала в формате zulu Exchange
	 * @param end дата окончания в формате zulu Exchange
	 * @param interval интервал занятости в минутах
	 */
	public GetUserAvailabilityMethod(String attendee, String start, String end, int interval) {
		super("FreeBusy", "GetUserAvailabilityRequest");
		this.attendee = attendee;
		this.start = start;
		this.end = end;
		this.interval = interval;
	}

	@Override
	protected void writeSoapBody(Writer writer) throws IOException {
		// write UTC timezone
		writer.write("<t:TimeZone>" + "<t:Bias>0</t:Bias>" + "<t:StandardTime>" + "<t:Bias>0</t:Bias>"
				+ "<t:Time>02:00:00</t:Time>" + "<t:DayOrder>1</t:DayOrder>" + "<t:Month>3</t:Month>"
				+ "<t:DayOfWeek>Sunday</t:DayOfWeek>" + "</t:StandardTime>" + "<t:DaylightTime>" + "<t:Bias>0</t:Bias>"
				+ "<t:Time>02:00:00</t:Time>" + "<t:DayOrder>1</t:DayOrder>" + "<t:Month>10</t:Month>"
				+ "<t:DayOfWeek>Sunday</t:DayOfWeek>" + "</t:DaylightTime>" + "</t:TimeZone>");
		// write attendee address
		writer.write("<m:MailboxDataArray>" + "<t:MailboxData>" + "<t:Email>" + "<t:Address>");
		writer.write(attendee);
		writer.write("</t:Address>" + "</t:Email>" + "<t:AttendeeType>Required</t:AttendeeType>" + "</t:MailboxData>"
				+ "</m:MailboxDataArray>");
		// freebusy range
		writer.write("<t:FreeBusyViewOptions>" + "<t:TimeWindow>" + "<t:StartTime>");
		writer.write(start);
		writer.write("</t:StartTime>" + "<t:EndTime>");
		writer.write(end);
		writer.write("</t:EndTime>" + "</t:TimeWindow>" + "<t:MergedFreeBusyIntervalInMinutes>" + interval
				+ "</t:MergedFreeBusyIntervalInMinutes>" + "<t:RequestedView>MergedOnly</t:RequestedView>"
				+ "</t:FreeBusyViewOptions>");
	}

	@Override
	protected void handleCustom(XMLStreamReader reader) {
		if (XMLStreamUtil.isStartTag(reader, "MergedFreeBusy")) {
			this.mergedFreeBusy = XMLStreamUtil.getElementText(reader);
		}
	}

	/**
	 * Получить объединённую строку занятости.
	 * @return строка занятости
	 */
	public String getMergedFreeBusy() {
		return mergedFreeBusy;
	}

}
