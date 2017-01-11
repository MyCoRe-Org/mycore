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

package org.mycore.datamodel.niofs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRContentTypes {

    private static final Logger LOGGER = LogManager.getLogger(MCRContentTypes.class);

    private static List<FileTypeDetector> fileTypeDetectors;

    static {
        try {
            fileTypeDetectors = getInstalledDetectors();
        } catch (Exception exc) {
            LOGGER.error("Unable to retrieve installed file type detectors", exc);
        }
    }

    private MCRContentTypes() {
    }

    private static List<FileTypeDetector> getInstalledDetectors() {
        ArrayList<FileTypeDetector> detectors = new ArrayList<>();
        ServiceLoader<FileTypeDetector> serviceLoader = ServiceLoader.load(FileTypeDetector.class);
        for (FileTypeDetector fileTypeDetector : serviceLoader) {
            LOGGER.info("Adding content type detector: " + fileTypeDetector.getClass());
            detectors.add(fileTypeDetector);
        }
        return detectors;
    }

    /**
     * Probes the content type of a file.
     * 
     * Same as {@link Files#probeContentType(Path)} but uses context class loader.
     * @param path
     *              the path to the file to probe
     * @return The content type of the file, or null if the content type cannot be determined
     * @throws IOException if an I/O error occurs
     */
    public static String probeContentType(Path path) throws IOException {
        LOGGER.debug("Probing content type: " + path);
        for (FileTypeDetector fileTypeDetector : fileTypeDetectors) {
            LOGGER.debug("Using type detector: " + fileTypeDetector.getClass());
            String contentType = fileTypeDetector.probeContentType(path);
            if (contentType != null) {
                LOGGER.debug("Content type: " + contentType);
                return contentType;
            }
        }
        return Files.probeContentType(path);
    }

}
