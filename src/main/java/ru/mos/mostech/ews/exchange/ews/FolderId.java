/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import java.io.IOException;
import java.io.Writer;

/**
 * Идентификатор папки.
 */
public class FolderId extends Option {

	protected String changeKey;

	protected String mailbox;

	/**
	 * Создать FolderId с указанным именем тега.
	 * @param name имя тега поля
	 * @param value значение id
	 * @param changeKey ключ изменения папки
	 * @param mailbox имя общей почтового ящика
	 */
	protected FolderId(String name, String value, String changeKey, String mailbox) {
		this(name, value, changeKey);
		this.mailbox = mailbox;
	}

	/**
	 * Создать FolderId с указанным именем тега.
	 * @param name имя поля тега
	 * @param value значение id
	 * @param changeKey ключ изменения папки
	 */
	protected FolderId(String name, String value, String changeKey) {
		super(name, value);
		this.changeKey = changeKey;
	}

	/**
	 * Построить идентификатор папки из элемента ответа.
	 * @param item элемент ответа
	 */
	public FolderId(EWSMethod.Item item) {
		this("t:FolderId", item.get("FolderId"), item.get("ChangeKey"));
	}

	@Override
	public void write(Writer writer) throws IOException {
		writer.write('<');
		writer.write(name);
		writer.write(" Id=\"");
		writer.write(value);
		if (changeKey != null) {
			writer.write("\" ChangeKey=\"");
			writer.write(changeKey);
		}
		if (mailbox == null) {
			writer.write("\"/>");
		}
		else {
			writer.write("\"><t:Mailbox><t:EmailAddress>");
			writer.write(mailbox);
			writer.write("</t:EmailAddress></t:Mailbox></");
			writer.write(name);
			writer.write('>');
		}
	}

}
