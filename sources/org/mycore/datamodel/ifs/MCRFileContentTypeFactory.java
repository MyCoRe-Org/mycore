/**
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
 *
 **/

package org.mycore.datamodel.ifs;

import org.mycore.common.*;
import org.mycore.common.xml.*;
import org.jdom.*;
import java.util.*;

/**
 * Provides methods to get the file content type with a given ID, or to detect
 * the file content type by providing file content header and filename.
 * 
 * The list of known file content types is defined in an XML file that is
 * specified in the property
 * <code>MCR.IFS.FileContentTypes.DefinitionFile</code>, and that file is
 * searched in the CLASSPATH directories or JAR files.
 * 
 * The class that implements the file content type detector must be specified by
 * the configuration property
 * <code>MCR.IFS.FileContentTypes.DetectorClass</code>.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRFileContentTypeFactory {
    /** Table for looking up all file content types by ID */
    protected static Hashtable typesTable = new Hashtable();

    /** The default file content type if unknown */
    protected static MCRFileContentType defaultType;

    /** The file content type detector implementation that is used */
    protected static MCRFileContentTypeDetector detector;

    static {
        MCRConfiguration config = MCRConfiguration.instance();

        Object obj = config
                .getInstanceOf("MCR.IFS.FileContentTypes.DetectorClass");
        detector = (MCRFileContentTypeDetector) (obj);

        String file = config
                .getString("MCR.IFS.FileContentTypes.DefinitionFile");

        Element xml = MCRURIResolver.instance().resolve("resource:" + file);
        List types = xml.getChildren("type");

        for (int i = 0; i < types.size(); i++) {
            // Build file content type from XML element
            Element xType = (Element) (types.get(i));
            String ID = xType.getAttributeValue("ID");
            String label = xType.getChildTextTrim("label");
            String url = xType.getChildTextTrim("url");
            String mime = xType.getChildTextTrim("mime");

            MCRFileContentType type = new MCRFileContentType(ID, label, url,
                    mime);
            typesTable.put(ID, type);

            // Add a detection rule for this file content type
            Element xRules = xType.getChild("rules");
            if (xRules != null)
                detector.addRule(type, xRules);
        }

        // Set default file content type from attribute "default"
        String defaultID = xml.getAttributeValue("default");
        defaultType = getType(defaultID);
    }

    /**
     * Returns the file content type with the given ID
     * 
     * @param ID
     *            The non-null ID of the content type that should be returned
     * @return The file content type with the given ID
     * 
     * @throws MCRConfigurationException
     *             if no such file content type is known in the system
     */
    public static MCRFileContentType getType(String ID)
            throws MCRConfigurationException {
        MCRArgumentChecker.ensureNotEmpty(ID, "ID");

        if (typesTable.containsKey(ID))
            return (MCRFileContentType) (typesTable.get(ID));
        else {
            String msg = "There is no file content type with ID = " + ID
                    + " configured";
            throw new MCRConfigurationException(msg);
        }
    }

    /**
     * Returns true if the file content type with the given ID is configured
     * 
     * @param ID
     *            The non-null ID of the content type that should be returned
     * @return true if content type is available, else false
     */
    public static boolean isTypeAvailable(String ID)
            throws MCRConfigurationException {
        MCRArgumentChecker.ensureNotEmpty(ID, "ID");
        return typesTable.containsKey(ID);
    }

    /**
     * Returns the default file content type to be used if content type is
     * unknown
     */
    public static MCRFileContentType getDefaultType() {
        return defaultType;
    }

    /**
     * Detects the file content type from filename and file content header.
     * 
     * @param filename
     *            the name of the file, may be null
     * @param header
     *            the first bytes of the file header, may be null or empty
     * @return the file content type detected, or the default file content type
     *         if detection was not possible
     */
    public static MCRFileContentType detectType(String filename, byte[] header) {
        if (filename == null)
            filename = "";
        if (header == null)
            header = new byte[0];

        MCRFileContentType type = detector.detectType(filename.trim(), header);
        return (type == null ? defaultType : type);
    }
}