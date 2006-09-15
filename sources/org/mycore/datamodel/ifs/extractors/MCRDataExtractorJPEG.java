/*
 * $RCSfile$
 * $Revision$ $Date$
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

package org.mycore.datamodel.ifs.extractors;

import java.io.InputStream;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.jdom.Element;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

/**
 * Extracts EXIF and IPTC metadata from JPEG files. Uses the metadata extraction
 * library from Drew Noakes in the com.drew.** packages. See
 * http://www.drewnoakes.com/code/exif/ for details.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRDataExtractorJPEG extends MCRDataExtractor {

    /** The logger */
    private final static Logger LOGGER = Logger.getLogger(MCRDataExtractorJPEG.class);

    protected String getSupportedContentTypeIDs() {
        return "jpeg";
    }

    protected void extractData(Element container, InputStream in) throws JpegProcessingException {
        Metadata metadata = JpegMetadataReader.readMetadata(in);
        Iterator directories = metadata.getDirectoryIterator();
        while (directories.hasNext()) {
            Directory directory = (Directory) directories.next();
            extractDirectoryData(container, directory);
        }
    }

    /**
     * Extract data from the given metadata directory (like EXIF, IPTC)
     */
    private void extractDirectoryData(Element xData, Directory directory) {
        try {
            Element xDirectory = new Element("directory");
            xData.addContent(xDirectory);
            String dirName = directory.getName();
            if (dirName != null)
                xDirectory.setAttribute("name", dirName);
            Iterator tags = directory.getTagIterator();
            while (tags.hasNext()) {
                Tag tag = (Tag) tags.next();
                extractTagData(xDirectory, tag);
            }
        } catch (Exception ex) {
            LOGGER.debug(ex.getClass().getName() + ": " + ex.getLocalizedMessage());
        }
    }

    /**
     * Extract data of a single metadata tag
     */
    private void extractTagData(Element xDirectory, Tag tag) {
        try {
            Element xTag = new Element("tag");
            xTag.setAttribute("name", tag.getTagName());
            xTag.setText(tag.getDescription());
            xDirectory.addContent(xTag);
        } catch (Exception ex) {
            LOGGER.debug(ex.getClass().getName() + ": " + ex.getLocalizedMessage());
        }
    }

    /**
     * Test application that outputs extracted metadata for a given local file.
     * 
     * @param args
     *            the path to a locally stored JPEG file
     */
    public static void main(String[] args) {
        new MCRDataExtractorJPEG().testLocalFile(args[0]);
    }
}
