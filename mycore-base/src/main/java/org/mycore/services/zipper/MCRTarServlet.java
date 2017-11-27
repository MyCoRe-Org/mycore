/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

import javax.servlet.ServletOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * Uses TAR format to deliver requested content.
 * 
 * This servlet produces TAR files as defined in POSIX.1-2001 standard and UTF-8 encoding for file names.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRTarServlet extends MCRCompressServlet<TarArchiveOutputStream> {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger(MCRTarServlet.class);

    @Override
    protected void sendCompressedDirectory(MCRPath file, BasicFileAttributes attrs,
        TarArchiveOutputStream container) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(getFilename(file), TarArchiveEntry.LF_DIR);
        entry.setModTime(attrs.lastModifiedTime().toMillis());
        container.putArchiveEntry(entry);
        container.closeArchiveEntry();
    }

    @Override
    protected void sendCompressedFile(MCRPath file, BasicFileAttributes attrs,
        TarArchiveOutputStream container) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(getFilename(file));
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
    protected void sendMetadataCompressed(String fileName, byte[] content, long lastModified,
        TarArchiveOutputStream container) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(fileName);
        entry.setModTime(lastModified);
        entry.setSize(content.length);
        container.putArchiveEntry(entry);
        container.write(content);
        container.closeArchiveEntry();
    }

    @Override
    protected String getMimeType() {
        return "application/x-tar";
    }

    @Override
    protected String getFileExtension() {
        return "tar";
    }

    @Override
    protected TarArchiveOutputStream createContainer(ServletOutputStream sout, String comment) {
        LOGGER.info("Constructing tar archive: {}", comment);
        TarArchiveOutputStream tout = new TarArchiveOutputStream(sout, "UTF8");
        tout.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
        tout.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        return tout;
    }

    @Override
    protected void disposeContainer(TarArchiveOutputStream container) throws IOException {
        container.finish();
    }
}
