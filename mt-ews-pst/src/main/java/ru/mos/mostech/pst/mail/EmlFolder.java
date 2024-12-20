/*
 *  Copyright 2024 Carlos Machado
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package ru.mos.mostech.pst.mail;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import ru.mos.mostech.pst.PstUtil;


public class EmlFolder extends LocalFolder {
    public static final String EML_FILE_EXTENSION = ".eml";
    private static final FileFilter EML_FILE_FILTER = (File pathname) -> {
        return pathname.isFile() && pathname.getName().endsWith(EML_FILE_EXTENSION);
    };

    public EmlFolder(Store store, File directory) {
        super(store, directory, EML_FILE_FILTER);
    }

    @Override
    public void appendMessage(Message msg) throws MessagingException {
        String id = getDescriptorNodeId(msg);
        String fileName = getEMLFileName(msg.getSubject(), id);
        File outputFile = new File(directory, fileName);
        try (FileOutputStream ouputStream = new FileOutputStream(outputFile)) {
            msg.writeTo(ouputStream);
        } catch (IOException ex) {
            throw new MessagingException("Failed to write to file", ex);
        }
    }

    @Override
    protected LocalFolder createInstance(Store store, File directory, FileFilter fileFilter) {
        return new EmlFolder(store, directory);
    }

    @Override
    public Message getMessage(int msgnum) throws MessagingException  {
        File emlFile = getFileEntries()[msgnum];
        try (FileInputStream emlFileStream = new FileInputStream(emlFile)) {
            // TODO: Null session? Is it Ok?
            MimeMessage msg = new MimeMessage(null, emlFileStream);
            return msg;
        } catch (IOException ex) {
            throw new MessagingException("Failed to get message", ex);
        }
    }

    /**
     * Генерирует допустимое имя файла, которое объединяет дескриптор с темой
     * электронной почты.
     *
     * @param subject Тема электронной почты.
     * @param descriptorIndex Индексное значение, которое уникально идентифицирует
     * электронное сообщение.
     * @return Допустимое имя файла.
     */
    static String getEMLFileName(String subject, String descriptorIndex) {
        if (subject == null || subject.isEmpty()) {
            String fileName = descriptorIndex + "-NoSubject" + EML_FILE_EXTENSION;
            return fileName;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(descriptorIndex).append("-");

        String normalizedSubject = PstUtil.normalizeString(subject);
        builder.append(normalizedSubject);
        builder.append(EML_FILE_EXTENSION);
        return builder.toString();
    }
}
