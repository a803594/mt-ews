/*
DIT
 */

package ru.mos.mostech.ews.exchange.ews;

import java.io.IOException;
import java.io.Writer;

public class OccurrenceItemId extends ItemId {

	protected final int instanceIndex;

	/**
	 * Build Item id object from item id and change key.
	 * @param recurringMasterId recurring master id
	 * @param instanceIndex occurrence index
	 */
	public OccurrenceItemId(String recurringMasterId, int instanceIndex) {
		super("OccurrenceItemId", recurringMasterId);
		this.instanceIndex = instanceIndex;
	}

	/**
	 * Build Item id object from item id and change key.
	 * @param recurringMasterId recurring master id
	 * @param changeKey change key
	 * @param instanceIndex occurrence index
	 */
	public OccurrenceItemId(String recurringMasterId, String changeKey, int instanceIndex) {
		super("OccurrenceItemId", recurringMasterId, changeKey);
		this.instanceIndex = instanceIndex;
	}

	/**
	 * Write item id as XML.
	 * @param writer request writer
	 * @throws IOException on error
	 */
	public void write(Writer writer) throws IOException {
		writer.write("<t:");
		writer.write(name);
		writer.write(" RecurringMasterId=\"");
		writer.write(id);
		if (changeKey != null) {
			writer.write("\" ChangeKey=\"");
			writer.write(changeKey);
		}
		writer.write("\" InstanceIndex=\"");
		writer.write(String.valueOf(instanceIndex));
		writer.write("\"/>");
	}

}
