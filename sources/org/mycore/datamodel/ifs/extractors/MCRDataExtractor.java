/*
 * 
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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.ifs.MCRFile;

/**
 * Event handler that extracts data like technical metadata (ID3 from MP3, EXIF
 * from JPEG etc.) whenever an MCRFile's content is changed. The extracted data
 * is stored in MCRFile's additional xml data.
 * 
 * @see org.mycore.datamodel.ifs.MCRFilesystemNode#getAdditionalData()
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public abstract class MCRDataExtractor extends MCREventHandlerBase {

    /** The logger */
    private final static Logger LOGGER = Logger.getLogger(MCRDataExtractorJPEG.class);

    /**
     * Convenience method that prints out extracted data of a local file
     * 
     * @param filePath
     *            the path of the file to be tested
     */
    protected void testLocalFile(String filePath) {
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(filePath));
            String name = getClass().getName();
            Element data = new Element(name.substring(name.lastIndexOf('.') + 1));
            extractData(data, in);
            System.out.println(outputData(data));
            in.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Returns the XML data element as a String
     * 
     * @param data
     *            the extracted data as XML element
     * @return the XML pretty-outputted as a String
     */
    protected String outputData(Element data) {
        XMLOutputter xout = new XMLOutputter();
        xout.setFormat(Format.getPrettyFormat().setEncoding("ISO-8859-1"));
        return xout.outputString(data);
    }

    protected void handleFileCreated(MCREvent evt, MCRFile file) {
        String supported = " " + getSupportedContentTypeIDs() + " ";
        if (supported.indexOf(" " + file.getContentTypeID() + " ") == -1)
            return;

        try {
            InputStream in = new BufferedInputStream(file.getContentAsInputStream());
            String name = getClass().getName();
            Element data = new Element(name.substring(name.lastIndexOf('.') + 1));
            extractData(data, in);
            if (LOGGER.isDebugEnabled())
                LOGGER.debug(outputData(data));
            if (data.getChildren().size() > 0)
                file.setAdditionalData(data);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void handleFileUpdated(MCREvent evt, MCRFile file) {
        handleFileCreated(evt, file);
    }

    /**
     * Extracts metadata from a file. This method must be overwritten by
     * subclasses.
     * 
     * @param container
     *            empty XML element that the extractor should fill with data
     * @param in
     *            the InputStream to read the file's content from
     * @return the XML element containing the extracted data
     * @throws Exception
     */
    protected abstract void extractData(Element container, InputStream in) throws Exception;

    /**
     * Returns the IDs of the FileContentTypes that are supported by this
     * metadata extractor. Only if the given file matches one of these types,
     * metadata is extracted.
     * 
     * @return a String of supported MCRFileContentType ID(s), separated by
     *         spaces
     */
    protected abstract String getSupportedContentTypeIDs();

    /**
     * Adds extracted metadata value to the resulting XML output, if it is not
     * null or empty.
     */
    protected void addDataValue(Element parent, String name, String value) {
        if (value == null)
            return;
        value = value.trim();
        if (value.length() == 0)
            return;
        if (value.equals("0") || value.equals("0.0"))
            return;
        parent.addContent(new Element(name).setText(value));
    }
}
