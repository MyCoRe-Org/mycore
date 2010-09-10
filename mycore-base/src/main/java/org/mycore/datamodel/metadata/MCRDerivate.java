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

import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.net.URI;

import org.jdom.Document;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.xml.sax.SAXParseException;

/**
 * This class implements all methode for handling one derivate object. Methodes
 * of this class can read the XML metadata by using a XML parser, manipulate the
 * data in the abstract persistence data store and return the XML stream to the
 * user application.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
final public class MCRDerivate extends MCRBase {
    /**
     * constant value for the object id length
     */

    // the object content
    private MCRObjectDerivate mcr_derivate = null;

    /**
     * This is the constructor of the MCRDerivate class. It make an instance of
     * the parser class and the metadata class.
     * 
     * @exception MCRException
     *                general Exception of MyCoRe
     * @exception MCRConfigurationException
     *                a special exception for configuartion data
     */
    public MCRDerivate() throws MCRException, MCRConfigurationException {
        super();

        // Derivate class
        mcr_derivate = new MCRObjectDerivate();
    }

    /**
     * @param bytes
     * @param valid
     * @throws SAXParseException
     */
    public MCRDerivate(byte[] bytes, boolean valid) throws SAXParseException {
        super(bytes, valid);
    }

    /**
     * @param doc
     * @throws SAXParseException
     */
    public MCRDerivate(Document doc) {
        super(doc);
    }

    /**
     * @param uri
     * @throws SAXParseException
     */
    public MCRDerivate(URI uri) throws SAXParseException {
        super(uri);
    }

    /**
     * This methode return the instance of the MCRObjectDerivate class. If this
     * was not found, null was returned.
     * 
     * @return the instance of the MCRObjectDerivate class
     */
    public final MCRObjectDerivate getDerivate() {
        return mcr_derivate;
    }

    /**
     * The given DOM was convert into an internal view of metadata. This are the
     * object ID and the object label, also the blocks structure, flags and
     * metadata.
     * 
     * @exception MCRException
     *                general Exception of MyCoRe
     */
    protected final void setUp() throws MCRException {
        if (jdom_document == null) {
            throw new MCRException("The JDOM document is null or empty.");
        }

        // get object ID from DOM
        org.jdom.Element jdom_element_root = jdom_document.getRootElement();
        mcr_id = new MCRObjectID(jdom_element_root.getAttributeValue("ID"));
        mcr_label = jdom_element_root.getAttributeValue("label").trim();

        if (mcr_label.length() > MAX_LABEL_LENGTH) {
            mcr_label = mcr_label.substring(0, MAX_LABEL_LENGTH);
        }

        mcr_version = jdom_element_root.getAttributeValue("version");
        if (mcr_version == null || (mcr_version = mcr_version.trim()).length() == 0) {
            setVersion();
        }

        mcr_schema = jdom_element_root.getAttribute("noNamespaceSchemaLocation", XSI_NAMESPACE).getValue().trim();

        org.jdom.Element jdom_element;

        // get the derivate data of the object
        jdom_element = jdom_element_root.getChild("derivate");
        mcr_derivate.setFromDOM(jdom_element);

        // get the service data of the object
        jdom_element = jdom_element_root.getChild("service");
        mcr_service.setFromDOM(jdom_element);
    }

    /**
     * This methode set the object MCRObjectDerivate.
     * 
     * @param derivate
     *            the object MCRObjectDerivate part
     */
    public final void setDerivate(MCRObjectDerivate derivate) {
        if (derivate != null) {
            mcr_derivate = derivate;
        }
    }

    /**
     * This methode create a XML stream for all object data.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Document with the XML data of the object as byte array
     */
    @Override
    public final org.jdom.Document createXML() throws MCRException {
        if (!isValid()) {
            throw new MCRException("The content is not valid.");
        }

        org.jdom.Element elm = new org.jdom.Element("mycorederivate");
        org.jdom.Document doc = new org.jdom.Document(elm);
        elm.addNamespaceDeclaration(XSI_NAMESPACE);
        elm.addNamespaceDeclaration(XLINK_NAMESPACE);
        elm.setAttribute("noNamespaceSchemaLocation", mcr_schema, XSI_NAMESPACE);
        elm.setAttribute("ID", mcr_id.toString());
        elm.setAttribute("label", mcr_label);
        elm.setAttribute("version", mcr_version);
        elm.addContent(mcr_derivate.createXML());
        elm.addContent(mcr_service.createXML());

        return doc;
    }

    /**
     * The methode receive the multimedia object(s) for the given MCRObjectID
     * and returned it as MCRDirectory.
     * 
     * @param id
     *            the object ID
     * @return the MCRDirectory of the multimedia object
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final MCRDirectory receiveDirectoryFromIFS(String id) throws MCRPersistenceException {
        // check the ID
        mcr_id = new MCRObjectID(id);

        // receive the IFS informations
        MCRDirectory difs = MCRDirectory.getRootDirectory(mcr_id.toString());

        if (difs == null) {
            throw new MCRPersistenceException("Error while receiving derivate with " + "ID " + mcr_id.toString() + " from IFS.");
        }

        return difs;
    }

    /**
     * The method print all informations about this MCRObject.
     */
    public final void debug() {
        LOGGER.debug("MCRDerivate ID : " + mcr_id.toString());
        LOGGER.debug("MCRDerivate Label : " + mcr_label);
        LOGGER.debug("MCRDerivate Schema : " + mcr_schema);
        LOGGER.debug("");
    }

    @Override
    public boolean isValid() {
        if (!super.isValid()) {
            LOGGER.warn("MCRBase.isValid() == false;");
            return false;
        }
        if (!getDerivate().isValid()) {
            LOGGER.warn("MCRObjectDerivate.isValid() == false;");
            return false;
        }
        return true;
    }

}
