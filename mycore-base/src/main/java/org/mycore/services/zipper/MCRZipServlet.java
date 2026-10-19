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

import java.io.Serial;
import java.util.zip.Deflater;

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.mycore.datamodel.niofs.archive.MCRZipArchiver;

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
    protected MCRZipArchiver getArchiver() {
        return ARCHIVER;
    }

}
