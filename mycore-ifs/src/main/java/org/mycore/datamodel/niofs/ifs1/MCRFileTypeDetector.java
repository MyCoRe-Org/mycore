/*
 * $Id$
 * $Revision: 5697 $ $Date: Jul 28, 2014 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.datamodel.niofs.ifs1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileContentTypeFactory;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRFileTypeDetector extends FileTypeDetector {

    private static final Logger LOGGER = LogManager.getLogger(MCRFileTypeDetector.class);

    private String defaultMimeType;

    /**
     * 
     */
    public MCRFileTypeDetector() {
        defaultMimeType = MCRFileContentTypeFactory.getDefaultType().getMimeType();
    }

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileTypeDetector#probeContentType(java.nio.file.Path)
     */
    @Override
    public String probeContentType(Path path) throws IOException {
        LOGGER.debug("Probing content type of: " + path);
        if (!(path.getFileSystem() instanceof MCRIFSFileSystem)) {
            return null;
        }
        MCRFilesystemNode resolvePath = MCRFileSystemProvider.resolvePath(MCRPath.toMCRPath(path));
        if (resolvePath == null) {
            throw new NoSuchFileException(path.toString());
        }
        if (resolvePath instanceof MCRDirectory) {
            throw new NoSuchFileException(path.toString());
        }
        MCRFile file = (MCRFile) resolvePath;
        String mimeType = file.getContentType().getMimeType();
        LOGGER.debug("IFS mime-type: " + mimeType);
        if (defaultMimeType.equals(mimeType)) {
            String systemMimeType = Files.probeContentType(file.getLocalFile().toPath());
            if (systemMimeType != null) {
                LOGGER.debug("System mime-type: " + systemMimeType);
                return systemMimeType;
            }
        }
        return mimeType;
    }
}
