/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import java.io.IOException;
import java.io.Writer;

/**
 * Вложение файла.
 */
public class FileAttachment {

	protected String name;

	protected String contentType;

	protected String content;

	protected String attachmentId;

	protected boolean isContactPhoto;

	/**
	 * Конструктор по умолчанию
	 */
	public FileAttachment() {
		// empty constructor
	}

	/**
	 * Создать вложение файла.
	 * @param name имя вложения
	 * @param contentType тип контента
	 * @param content тело в виде строки
	 */
	public FileAttachment(String name, String contentType, String content) {
		this.name = name;
		this.contentType = contentType;
		this.content = content;
	}

	/**
	 * Записать содержимое XML в писатель.
	 * @param writer писатель
	 * @throws IOException в случае ошибки
	 */
	public void write(Writer writer) throws IOException {
		writer.write("<t:FileAttachment>");
		if (name != null) {
			writer.write("<t:Name>");
			writer.write(name);
			writer.write("</t:Name>");
		}
		if (contentType != null) {
			writer.write("<t:ContentType>");
			writer.write(contentType);
			writer.write("</t:ContentType>");
		}
		if (isContactPhoto) {
			writer.write("<t:IsContactPhoto>true</t:IsContactPhoto>");
		}
		if (content != null) {
			writer.write("<t:Content>");
			writer.write(content);
			writer.write("</t:Content>");
		}
		writer.write("</t:FileAttachment>");
	}

	/**
	 * Exchange 2010 только: установить флаг фотографии контакта на вложении.
	 * @param isContactPhoto флаг фотографии контакта
	 */
	public void setIsContactPhoto(boolean isContactPhoto) {
		this.isContactPhoto = isContactPhoto;
	}

}
