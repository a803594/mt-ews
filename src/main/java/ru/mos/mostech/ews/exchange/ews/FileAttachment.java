/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Writer;

/**
 * File Attachment.
 */

@Slf4j
public class FileAttachment {
    protected String name;
    protected String contentType;
    protected String content;
    protected String attachmentId;
    protected boolean isContactPhoto;

    /**
     * Default constructor
     */
    public FileAttachment() {
        // empty constructor
    }

    /**
     * Build file attachment.
     *
     * @param name        attachment name
     * @param contentType content type
     * @param content     body as string
     */
    public FileAttachment(String name, String contentType, String content) {
        this.name = name;
        this.contentType = contentType;
        this.content = content;
    }

    /**
     * Write XML content to writer.
     *
     * @param writer writer
     * @throws IOException on error
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
     * Exchange 2010 only: set contact photo flag on attachment.
     *
     * @param isContactPhoto contact photo flag
     */
    public void setIsContactPhoto(boolean isContactPhoto) {
        this.isContactPhoto = isContactPhoto;
    }

}
