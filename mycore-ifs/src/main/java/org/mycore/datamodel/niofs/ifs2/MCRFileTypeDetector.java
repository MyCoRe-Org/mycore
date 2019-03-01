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

package org.mycore.datamodel.niofs.ifs2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.ifs2.MCRDirectory;
import org.mycore.datamodel.ifs2.MCRFile;
import org.mycore.datamodel.ifs2.MCRStoredNode;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRFileTypeDetector extends FileTypeDetector {

    private static final Logger LOGGER = LogManager.getLogger();

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileTypeDetector#probeContentType(java.nio.file.Path)
     */
    @Override
    public String probeContentType(Path path) throws IOException {
        LOGGER.debug(() -> "Probing content type of: " + path);
        if (!(path.getFileSystem() instanceof MCRIFSFileSystem)) {
            return null;
        }
        MCRStoredNode resolvePath = MCRFileSystemUtils.resolvePath(MCRPath.toMCRPath(path));
        if (resolvePath instanceof MCRDirectory) {
            throw new NoSuchFileException(path.toString());
        }
        MCRFile file = (MCRFile) resolvePath;
        String mimeType = Files.probeContentType(file.getLocalPath());
        LOGGER.debug(() -> "IFS mime-type: " + mimeType);
        return mimeType;
    }
}
