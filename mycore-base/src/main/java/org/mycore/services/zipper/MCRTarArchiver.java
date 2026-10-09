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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * Uses TAR format.
 *
 * This class produces TAR files as defined in POSIX.1-2001 standard and UTF-8 encoding for file names.
 */
public class MCRTarArchiver extends MCRArchiver<TarArchiveOutputStream> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getMimeType() {
        return "application/x-tar";
    }

    @Override
    public String getFileExtension() {
        return "tar";
    }

    @Override
    public TarArchiveOutputStream createContainer(OutputStream out, String comment) {
        LOGGER.info("Constructing tar archive: {}", comment);
        TarArchiveOutputStream tout = new TarArchiveOutputStream(out, "UTF8");
        tout.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
        tout.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        return tout;
    }

    @Override
    public void sendDirectory(MCRPath dir, BasicFileAttributes attrs, TarArchiveOutputStream container)
        throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(getEntryName(dir) + '/');
        entry.setModTime(attrs.lastModifiedTime().toMillis());
        container.putArchiveEntry(entry);
        container.closeArchiveEntry();
    }

    @Override
    public void sendFile(MCRPath file, BasicFileAttributes attrs, TarArchiveOutputStream container)
        throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(getEntryName(file));
        entry.setModTime(attrs.lastModifiedTime().toMillis());
        entry.setSize(attrs.size());
        container.putArchiveEntry(entry);
        try {
            Files.copy(file, container);
        } finally {
            container.closeArchiveEntry();
        }
    }

    @Override
    public void sendMetadata(String fileName, byte[] content, long lastModified, TarArchiveOutputStream container)
        throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(fileName);
        entry.setModTime(lastModified);
        entry.setSize(content.length);
        container.putArchiveEntry(entry);
        container.write(content);
        container.closeArchiveEntry();
    }
}
