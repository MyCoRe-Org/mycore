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
import java.io.Serial;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.Deflater;

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.mycore.datamodel.niofs.MCRPath;

import jakarta.servlet.ServletOutputStream;

/**
 * Uses ZIP format to deliver requested content.
 * {@link Deflater#BEST_COMPRESSION} is used for compression.
 * @author Thomas Scheffler
 * @see MCRZipArchiver
 */
public class MCRZipServlet extends MCRCompressServlet<ZipArchiveOutputStream> {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final MCRZipArchiver ARCHIVER = new MCRZipArchiver();

    @Override
    protected void sendCompressedDirectory(MCRPath file, BasicFileAttributes attrs, ZipArchiveOutputStream container)
        throws IOException {
        ARCHIVER.sendDirectory(file, attrs, container);
    }

    @Override
    protected void sendCompressedFile(MCRPath file, BasicFileAttributes attrs, ZipArchiveOutputStream container)
        throws IOException {
        ARCHIVER.sendFile(file, attrs, container);
    }

    @Override
    protected void sendMetadataCompressed(String fileName, byte[] content, long lastModified,
        ZipArchiveOutputStream container) throws IOException {
        ARCHIVER.sendMetadata(fileName, content, lastModified, container);
    }

    @Override
    protected String getMimeType() {
        return ARCHIVER.getMimeType();
    }

    @Override
    protected String getFileExtension() {
        return ARCHIVER.getFileExtension();
    }

    @Override
    protected ZipArchiveOutputStream createContainer(ServletOutputStream sout, String comment) {
        return ARCHIVER.createContainer(sout, comment);
    }

    @Override
    protected void disposeContainer(ZipArchiveOutputStream container) throws IOException {
        container.finish();
    }
}
