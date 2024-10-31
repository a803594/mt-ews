package ru.mos.mostech.ews.util;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
public class ZipUtil {

	private ZipUtil() {
	}

	public static void zipFolder(Path source, OutputStream os) throws IOException {
		try (ZipOutputStream zos = new ZipOutputStream(os)) {
			Files.walkFileTree(source, new SimpleFileVisitor<>() {

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
					if (attributes.isSymbolicLink()) {
						return FileVisitResult.CONTINUE;
					}

					try (FileInputStream fis = new FileInputStream(file.toFile())) {
						Path targetFile = source.relativize(file);
						zos.putNextEntry(new ZipEntry(targetFile.toString()));

						byte[] buffer = new byte[1024];
						int len;
						while ((len = fis.read(buffer)) > 0) {
							zos.write(buffer, 0, len);
						}

						zos.closeEntry();
					}
					catch (IOException e) {
						log.error(e.getMessage(), e);
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException e) {
					log.error(e.getMessage(), e);
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}

}
