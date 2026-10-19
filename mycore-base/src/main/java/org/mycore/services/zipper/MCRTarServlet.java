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

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.mycore.datamodel.niofs.archive.MCRTarArchiver;

/**
 * Uses TAR format to deliver requested content.
 * 
 * This servlet produces TAR files as defined in POSIX.1-2001 standard and UTF-8 encoding for file names.
 * 
 * @author Thomas Scheffler (yagee)
 * @see MCRTarArchiver
 */
public class MCRTarServlet extends MCRCompressServlet<TarArchiveOutputStream> {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final MCRTarArchiver ARCHIVER = new MCRTarArchiver();

    @Override
    protected MCRTarArchiver getArchiver() {
        return ARCHIVER;
    }

}
