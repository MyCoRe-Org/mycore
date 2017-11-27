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

package org.mycore.datamodel.ifs;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.xml.MCRURIResolver;

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

    static final Logger LOGGER = LogManager.getLogger(MCRURIResolver.class);

    /** Table for looking up all file content types by ID */
    protected static Hashtable<String, MCRFileContentType> typesTable = new Hashtable<>();

    /** The default file content type if unknown */
    protected static MCRFileContentType defaultType;

    /** The file content type detector implementation that is used */
    protected static MCRFileContentTypeDetector detector;

    static {
        MCRConfiguration config = MCRConfiguration.instance();

        detector = config.getInstanceOf("MCR.IFS.FileContentTypes.DetectorClass");

        String file = config.getString("MCR.IFS.FileContentTypes.DefinitionFile");

        Element xml = MCRURIResolver.instance().resolve("resource:" + file);
        if (xml == null) {
            throw new MCRException("Unable to initialize file content type factory because '" + file
                + "' does not exist! Check the MCR.IFS.FileContentTypes.DefinitionFile property.");
        }
        List<Element> types = xml.getChildren("type");

        for (Object type1 : types) {
            // Build file content type from XML element
            Element xType = (Element) type1;
            String ID = xType.getAttributeValue("ID");
            String label = xType.getChildTextTrim("label");
            String url = xType.getChildTextTrim("url");
            String mime = xType.getChildTextTrim("mime");

            MCRFileContentType type = new MCRFileContentType(ID, label, url, mime);
            typesTable.put(ID, type);

            // Add a detection rule for this file content type
            Element xRules = xType.getChild("rules");

            if (xRules != null) {
                detector.addRule(type, xRules);
            }
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
    public static MCRFileContentType getType(String ID) throws MCRConfigurationException {
        if (Objects.requireNonNull(ID, "ID" + " is null").trim().isEmpty()) {
            throw new MCRUsageException("ID" + " is an empty String");
        }

        if (typesTable.containsKey(ID)) {
            return typesTable.get(ID);
        }
        String msg = "There is no file content type with ID = " + ID + " configured";
        throw new MCRConfigurationException(msg);
    }

    /**
     * Returns the file content type with the given mime type
     * 
     * @param mimeType
     *            The non-null mimeType of the content type that should be returned
     * @return The file content type with the given ID
     * 
     * @throws MCRConfigurationException
     *             if no such file content type is known in the system
     */
    public static MCRFileContentType getTypeByMimeType(String mimeType) throws MCRConfigurationException {
        HashSet<String> types = new HashSet<>(typesTable.keySet());

        for (String key : types) {
            MCRFileContentType contentType = typesTable.get(key);

            if (mimeType.equals(contentType.getMimeType()))
                return typesTable.get(key);
        }
        String msg = "There is no file content type for mime type = " + mimeType + " configured";
        throw new MCRConfigurationException(msg);
    }

    /**
     * Returns true if the file content type with the given ID is configured
     * 
     * @param ID
     *            The non-null ID of the content type that should be returned
     * @return true if content type is available, else false
     */
    public static boolean isTypeAvailable(String ID) throws MCRConfigurationException {
        if (Objects.requireNonNull(ID, "ID" + " is null").trim().isEmpty()) {
            throw new MCRUsageException("ID" + " is an empty String");
        }

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
        if (filename == null) {
            filename = "";
        }

        if (header == null) {
            header = new byte[0];
        }

        MCRFileContentType type = detector.detectType(filename.trim(), header);

        return type == null ? defaultType : type;
    }
}
