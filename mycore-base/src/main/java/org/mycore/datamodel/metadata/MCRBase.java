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

package org.mycore.datamodel.metadata;

import static org.mycore.common.MCRConstants.DEFAULT_ENCODING;
import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.io.IOException;
import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.MCRCoreVersion;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRVFSContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.xml.sax.SAXParseException;

import com.google.gson.JsonObject;

/**
 * This class is a abstract basic class for objects in the MyCoRe Project. It is
 * the frame to produce a full functionality object.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public abstract class MCRBase {
    /**
     * constant value for the object id length
     */
    public final static int MAX_LABEL_LENGTH = 256;

    // from configuration
    protected static final MCRConfiguration mcr_conf;

    protected static final String mcr_encoding;

    // the DOM document
    protected org.jdom2.Document jdom_document = null;

    // the object content
    protected MCRObjectID mcr_id = null;

    protected String mcr_label = null;

    protected String mcr_version = null;

    protected String mcr_schema = null;

    protected final MCRObjectService mcr_service;

    // other
    protected static final String NL;

    protected static final String SLASH;

    protected boolean importMode = false;

    // logger
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Load static data for all MCRObjects
     */
    static {
        NL = System.getProperty("line.separator");
        SLASH = System.getProperty("file.separator");
        // Load the configuration
        mcr_conf = MCRConfiguration.instance();

        // Default Encoding
        mcr_encoding = mcr_conf.getString("MCR.Metadata.DefaultEncoding", DEFAULT_ENCODING);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Encoding = " + mcr_encoding);
        }
    }

    /**
     * This is the constructor of the MCRBase class. It make an instance of the
     * parser class and the metadata class. <br>
     * 
     * @exception MCRException
     *                general Exception of MyCoRe
     * @exception MCRConfigurationException
     *                a special exception for configuration data
     */
    public MCRBase() throws MCRException, MCRConfigurationException {
        mcr_label = "";
        mcr_version = MCRCoreVersion.getVersion();
        mcr_schema = "";

        // Service class
        mcr_service = new MCRObjectService();
    }

    protected void setFromJDOM(Document doc) {
        jdom_document = doc;
        setUp();
    }

    protected void setUp() {
        if (jdom_document == null) {
            throw new MCRException("The JDOM document is null or empty.");
        }

        org.jdom2.Element rootElement = jdom_document.getRootElement();
        setId(MCRObjectID.getInstance(rootElement.getAttributeValue("ID")));
        setLabel(rootElement.getAttributeValue("label"));
        setVersion(rootElement.getAttributeValue("version"));
        setSchema(rootElement.getAttribute("noNamespaceSchemaLocation", XSI_NAMESPACE).getValue());

        // get the service data of the object
        Element serviceElement = rootElement.getChild("service");
        if (serviceElement != null) {
            mcr_service.setFromDOM(serviceElement);
        }
    }

    /**
     * This methode return the object id. If this is not set, null was returned.
     * 
     * @return the id as MCRObjectID
     */
    public final MCRObjectID getId() {
        return mcr_id;
    }

    /**
     * This methode return the object label. If this is not set, null was
     * returned.
     * 
     * @return the lable as a string
     */
    public final String getLabel() {
        return mcr_label;
    }

    /**
     * This methode return the MyCoRe version of the data structure.
     * 
     * @return the version as a string
     */
    public final String getVersion() {
        return mcr_version;
    }

    /**
     * Method returns the object schema. If the schema is not set <code>null</code> will be returned.
     * 
     * @return the schema as a string
     */
    public final String getSchema() {
        return mcr_schema;
    }

    /**
     * This methode return the instance of the MCRObjectService class. If this
     * was not found, null was returned.
     * 
     * @return the instance of the MCRObjectService class
     */
    public final MCRObjectService getService() {
        return mcr_service;
    }

    /**
     * This method read the XML input stream from an URI to build up the
     * MyCoRe-Object.
     * 
     * @param uri
     *            an URI
     * @exception MCRException
     *                general Exception of MyCoRe
     */
    protected final void setFromURI(URI uri) throws MCRException, SAXParseException, IOException {
        Document jdom = MCRXMLParserFactory.getParser().parseXML(new MCRVFSContent(uri));
        setFromJDOM(jdom);
    }

    /**
     * This method read the XML input stream from a byte array to build up the
     * MyCoRe-Object.
     * 
     * @param xml
     *            a XML string
     * @exception MCRException
     *                general Exception of MyCoRe
     */
    protected final void setFromXML(byte[] xml, boolean valid) throws MCRException, SAXParseException {
        Document jdom = MCRXMLParserFactory.getParser(valid).parseXML(new MCRByteContent(xml));
        setFromJDOM(jdom);
    }

    /**
     * This method set the object ID.
     * 
     * @param id
     *            the object ID
     */
    public void setId(MCRObjectID id) {
        mcr_id = id;
    }

    /**
     * This method set the object label.
     * 
     * @param label
     *            the object label
     */
    public final void setLabel(String label) {
        mcr_label = label.trim();

        if (mcr_label.length() > MAX_LABEL_LENGTH) {
            mcr_label = mcr_label.substring(0, MAX_LABEL_LENGTH);
        }
    }

    /**
     * This methods set the MyCoRe version to the string 'Version 1.3'.
     */
    public final void setVersion(String version) {
        mcr_version = version != null ? version : MCRCoreVersion.getVersion();
    }

    /**
     * This methode set the object schema.
     * 
     * @param schema
     *            the object schema
     */
    public final void setSchema(String schema) {
        if (schema == null) {
            mcr_schema = "";

            return;
        }

        mcr_schema = schema.trim();
    }

    /**
     * This method create a XML stream for all object data.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Document with the XML data of the object as byte array
     */
    public Document createXML() throws MCRException {
        validate();
        Element elm = new Element(getRootTagName());
        Document doc = new Document(elm);
        elm.addNamespaceDeclaration(XSI_NAMESPACE);
        elm.addNamespaceDeclaration(XLINK_NAMESPACE);
        elm.setAttribute("noNamespaceSchemaLocation", mcr_schema, XSI_NAMESPACE);
        elm.setAttribute("ID", mcr_id.toString());
        if (mcr_label != null) {
            elm.setAttribute("label", mcr_label);
        }
        elm.setAttribute("version", mcr_version);
        return doc;
    }

    /**
     * Creates the JSON representation of this object.
     * 
     * <pre>
     *   {
     *     id: "mycore_project_00000001",
     *     label: "my mycore base object",
     *     version: "3.0"
     *   }
     * </pre>
     * 
     */
    public JsonObject createJSON() {
        JsonObject base = new JsonObject();
        base.addProperty("id", mcr_id.toString());
        if (mcr_label != null) {
            base.addProperty("label", mcr_label);
        }
        base.addProperty("version", mcr_version);
        return base;
    }

    protected abstract String getRootTagName();

    /**
     * This method check the validation of the content of this class. The method
     * returns <em>true</em> if
     * <ul>
     * <li>the mcr_id value is valid
     * <li>the label value is not null or empty
     * </ul>
     * otherwise the method return <em>false</em>
     * 
     * @return a boolean value
     */
    public boolean isValid() {
        try {
            validate();
            return true;
        } catch (MCRException exc) {
            LOGGER.warn("The content of this object '" + mcr_id + "' is invalid.", exc);
        }
        return false;
    }

    /**
     * Validates the content of this class. This method throws an exception if:
     *  <ul>
     *  <li>the mcr_id is null</li>
     *  <li>the XML schema is null or empty</li>
     *  <li>the service part is null or invalid</li>
     *  </ul>
     * 
     * @throws MCRException the content is invalid
     */
    public void validate() throws MCRException {
        if (mcr_id == null) {
            throw new MCRException("MCRObjectID is undefined.");
        }
        if (getSchema() == null || getSchema().length() == 0) {
            throw new MCRException("XML Schema of '" + mcr_id + "' is undefined.");
        }
        MCRObjectService service = getService();
        if (service == null) {
            throw new MCRException("The <service> part of '" + mcr_id + "' is undefined.");
        }
        try {
            service.validate();
        } catch (MCRException exc) {
            throw new MCRException("The <service> part of '" + mcr_id + "' is invalid.", exc);
        }
    }

    public boolean isImportMode() {
        return importMode;
    }

    public void setImportMode(boolean importMode) {
        this.importMode = importMode;
    }

    @Override
    public String toString() {
        return this.mcr_id.toString();
    }
}
