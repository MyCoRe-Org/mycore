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
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRURLContent;
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

    protected static final String MCR_ENCODING;

    // the DOM document
    protected Document jdomDocument = null;

    // the object content
    protected MCRObjectID mcrId = null;

    protected String mcrVersion = null;

    protected String mcrSchema = null;

    protected final MCRObjectService mcrService;

    // other
    protected static final String NL;

    protected static final String SLASH;

    protected boolean importMode = false;

    // logger
    private static final Logger LOGGER = LogManager.getLogger();

    static {
        NL = System.getProperty("line.separator");
        SLASH = System.getProperty("file.separator");
        // Default Encoding
        MCR_ENCODING = MCRConfiguration2.getString("MCR.Metadata.DefaultEncoding").orElse(DEFAULT_ENCODING);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Encoding = {}", MCR_ENCODING);
        }
    }

    /**
     * This is the constructor of the MCRBase class. It make an instance of the
     * parser class and the metadata class. <br>
     * 
     * @exception MCRException
     *                general Exception of MyCoRe
     */
    public MCRBase() throws MCRException {
        mcrVersion = MCRCoreVersion.getVersion();
        mcrSchema = "";

        // Service class
        mcrService = new MCRObjectService();
    }

    protected void setFromJDOM(Document doc) {
        jdomDocument = doc;
        setUp();
    }

    protected void setUp() {
        if (jdomDocument == null) {
            throw new MCRException("The JDOM document is null or empty.");
        }

        Element rootElement = jdomDocument.getRootElement();
        setId(MCRObjectID.getInstance(rootElement.getAttributeValue("ID")));
        setVersion(rootElement.getAttributeValue("version"));
        setSchema(rootElement.getAttribute("noNamespaceSchemaLocation", XSI_NAMESPACE).getValue());

        // get the service data of the object
        Element serviceElement = rootElement.getChild("service");
        if (serviceElement != null) {
            mcrService.setFromDOM(serviceElement);
        }
    }

    /**
     * This methode return the object id. If this is not set, null was returned.
     * 
     * @return the id as MCRObjectID
     */
    public final MCRObjectID getId() {
        return mcrId;
    }

    /**
     * This methode return the MyCoRe version of the data structure.
     * 
     * @return the version as a string
     */
    public final String getVersion() {
        return mcrVersion;
    }

    /**
     * Method returns the object schema. If the schema is not set <code>null</code> will be returned.
     * 
     * @return the schema as a string
     */
    public final String getSchema() {
        return mcrSchema;
    }

    /**
     * This methode return the instance of the MCRObjectService class. If this
     * was not found, null was returned.
     * 
     * @return the instance of the MCRObjectService class
     */
    public final MCRObjectService getService() {
        return mcrService;
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
        Document jdom = MCRXMLParserFactory.getParser().parseXML(new MCRURLContent(uri.toURL()));
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
        mcrId = id;
    }

    /**
     * This methods set the MyCoRe version to the string 'Version 1.3'.
     */
    public final void setVersion(String version) {
        mcrVersion = version != null ? version : MCRCoreVersion.getVersion();
    }

    /**
     * This methode set the object schema.
     * 
     * @param schema
     *            the object schema
     */
    public final void setSchema(String schema) {
        if (schema == null) {
            mcrSchema = "";

            return;
        }

        mcrSchema = schema.trim();
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
        elm.setAttribute("noNamespaceSchemaLocation", mcrSchema, XSI_NAMESPACE);
        elm.setAttribute("ID", mcrId.toString());
        elm.setAttribute("version", mcrVersion);
        return doc;
    }

    /**
     * Creates the JSON representation of this object.
     * 
     * <pre>
     *   {
     *     id: "mycore_project_00000001",
     *     version: "3.0"
     *   }
     * </pre>
     * 
     */
    public JsonObject createJSON() {
        JsonObject object = new JsonObject();
        object.addProperty("id", mcrId.toString());
        object.addProperty("version", mcrVersion);
        return object;
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
            LOGGER.warn("The content of this object '{}' is invalid.", mcrId, exc);
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
        if (mcrId == null) {
            throw new MCRException("MCRObjectID is undefined.");
        }
        if (getSchema() == null || getSchema().length() == 0) {
            throw new MCRException("XML Schema of '" + mcrId + "' is undefined.");
        }
        MCRObjectService service = getService();
        if (service == null) {
            throw new MCRException("The <service> part of '" + mcrId + "' is undefined.");
        }
        try {
            service.validate();
        } catch (MCRException exc) {
            throw new MCRException("The <service> part of '" + mcrId + "' is invalid.", exc);
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
        return this.mcrId.toString();
    }
}
