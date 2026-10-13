/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.services.zipper;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.Deflater;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * Uses ZIP format.
 * {@link Deflater#BEST_COMPRESSION} is used for compression.
 */
public class MCRZipArchiver extends MCRArchiver<ZipArchiveOutputStream> {

    @Override
    public String getMimeType() {
        return "application/zip";
    }

    @Override
    public String getFileExtension() {
        return "zip";
    }

    @Override
    public ZipArchiveOutputStream createContainer(OutputStream out, String comment) {
        ZipArchiveOutputStream zout = new ZipArchiveOutputStream(new BufferedOutputStream(out));
        zout.setComment(comment);
        zout.setLevel(Deflater.BEST_COMPRESSION);
        return zout;
    }

    @Override
    public void sendDirectory(MCRPath dir, BasicFileAttributes attrs, ZipArchiveOutputStream container)
        throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(getEntryName(dir) + "/");
        entry.setTime(attrs.lastModifiedTime().toMillis());
        container.putArchiveEntry(entry);
        container.closeArchiveEntry();
    }

    @Override
    public void sendFile(MCRPath file, BasicFileAttributes attrs, ZipArchiveOutputStream container)
        throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(getEntryName(file));
        entry.setTime(attrs.lastModifiedTime().toMillis());
        entry.setSize(attrs.size());
        container.putArchiveEntry(entry);
        try {
            Files.copy(file, container);
        } finally {
            container.closeArchiveEntry();
        }
    }

    @Override
    public void sendMetadata(String fileName, byte[] content, long lastModified, ZipArchiveOutputStream container)
        throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(fileName);
        entry.setSize(content.length);
        entry.setTime(lastModified);
        container.putArchiveEntry(entry);
        container.write(content);
        container.closeArchiveEntry();
    }
}
