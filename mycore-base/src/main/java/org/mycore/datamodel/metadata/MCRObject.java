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
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.xml.sax.SAXParseException;

/**
 * This class implements all methode for handling one metadata object. Methodes
 * of this class can read the XML metadata by using a XML parser, manipulate the
 * data in the abstract persistence data store and return the XML stream to the
 * user application. Additionally, this class provides the public user interface
 * for the linking of MCRObjects against other MCRObjects with metadata
 * inheritance.
 * 
 * @author Jens Kupferschmidt
 * @author Mathias Hegner
 * @version $Revision$ $Date$
 */
final public class MCRObject extends MCRBase {
    // the object content
    private MCRObjectStructure mcr_struct = null;

    private MCRObjectMetadata mcr_metadata = null;

    /**
     * This is the constructor of the MCRObject class. It creates an instance of
     * the parser class and the metadata class. <br>
     * The constructor reads the following information from the property file:
     * <ul>
     * <li>MCR.XMLParser.Class</li>
     * </ul>
     * 
     * @exception MCRException
     *                general Exception of MyCoRe
     * @exception MCRConfigurationException
     *                a special exception for configuartion data
     */
    public MCRObject() throws MCRException, MCRConfigurationException {
        super();

        // Metadata class
        mcr_metadata = new MCRObjectMetadata();

        // Structure class
        mcr_struct = new MCRObjectStructure(LOGGER);
    }

    /**
     * @param bytes
     * @param valid
     * @throws SAXParseException
     */
    public MCRObject(byte[] bytes, boolean valid) throws SAXParseException {
        super(bytes, valid);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param doc
     * @throws SAXParseException
     */
    public MCRObject(Document doc) {
        super(doc);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param uri
     * @throws SAXParseException
     */
    public MCRObject(URI uri) throws SAXParseException {
        super(uri);
        // TODO Auto-generated constructor stub
    }

    /**
     * This methode return the object metadata element selected by tag. If this
     * was not found, null was returned.
     * 
     * @return the metadata tag part as a object that extend MCRMetaElement
     */
    public final MCRMetaElement getMetadataElement(String tag) {
        return mcr_metadata.getMetadataElement(tag);
    }

    /**
     * This method returns the instance of the MCRObjectMetadata class. If there
     * was no MCRObjectMetadata found, null will be returned.
     * 
     * @return the instance of the MCRObjectMetadata class
     */
    public final MCRObjectMetadata getMetadata() {
        return mcr_metadata;
    }

    /**
     * This methode return the instance of the MCRObjectStructure class. If this
     * was not found, null was returned.
     * 
     * @return the instance of the MCRObjectStructure class
     */
    public final MCRObjectStructure getStructure() {
        return mcr_struct;
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

        setRoot();

        setStructure();

        setMetadata();

        setService();
    }

    private void setRoot() {
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
        LOGGER.debug("MCRObject set schemafile: " + mcr_schema);
    }

    private void setStructure() {
        // get the structure data of the object
        org.jdom.Element jdom_element_root = jdom_document.getRootElement();
        org.jdom.Element jdom_element = jdom_element_root.getChild("structure");
        mcr_struct = new MCRObjectStructure(LOGGER);
        mcr_struct.setFromDOM(jdom_element);
    }

    private void setMetadata() {
        // get the metadata of the object
        org.jdom.Element jdom_element_root = jdom_document.getRootElement();
        org.jdom.Element jdom_element = jdom_element_root.getChild("metadata");
        mcr_metadata = new MCRObjectMetadata();
        mcr_metadata.setFromDOM(jdom_element);
    }

    private void setService() {
        org.jdom.Element jdom_element;
        // get the service data of the object
        org.jdom.Element jdom_element_root = jdom_document.getRootElement();
        jdom_element = jdom_element_root.getChild("service");
        mcr_service = new MCRObjectService();
        mcr_service.setFromDOM(jdom_element);
    }

    /**
     * This methode set the object metadata part named by a tag.
     * 
     * @param obj
     *            the class object of a metadata part
     * @param tag
     *            the tag of a metadata part
     * @return true if set was succesful, otherwise false
     */
    public final boolean setMetadataElement(MCRMetaElement obj, String tag) {
        if (obj == null) {
            return false;
        }

        if (tag == null || (tag = tag.trim()).length() == 0) {
            return false;
        }

        return mcr_metadata.setMetadataElement(obj, tag);
    }

    /**
     * This methode set the object MCRObjectStructure.
     * 
     * @param structure
     *            the object MCRObjectStructure part
     */
    public final void setStructure(MCRObjectStructure structure) {
        if (structure != null) {
            mcr_struct = structure;
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

        org.jdom.Element elm = new org.jdom.Element("mycoreobject");
        org.jdom.Document doc = new org.jdom.Document(elm);
        elm.addNamespaceDeclaration(XSI_NAMESPACE);
        elm.addNamespaceDeclaration(XLINK_NAMESPACE);
        elm.setAttribute("noNamespaceSchemaLocation", mcr_schema, XSI_NAMESPACE);
        elm.setAttribute("ID", mcr_id.toString());
        elm.setAttribute("label", mcr_label);
        elm.setAttribute("version", mcr_version);
        elm.addContent(mcr_struct.createXML());
        elm.addContent(mcr_metadata.createXML());
        elm.addContent(mcr_service.createXML());

        return doc;
    }

    /**
     * The method print all informations about this MCRObject.
     */
    public final void debug() {
        LOGGER.debug("MCRObject ID : " + mcr_id.toString());
        LOGGER.debug("MCRObject Label : " + mcr_label);
        LOGGER.debug("MCRObject Schema : " + mcr_schema);
        LOGGER.debug("");
        mcr_metadata.debug();
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.metadata.MCRBase#isValid()
     */
    @Override
    public boolean isValid() {
        return super.isValid() && getMetadata().isValid() && getStructure().isValid() && getService().isValid();
    }

    public void checkLinkTargets() {
        for (int i = 0; i < getMetadata().size(); i++) {
            MCRMetaElement elm = getMetadata().getMetadataElement(i);
            for (int j = 0; j < elm.size(); j++) {
                MCRMetaInterface inf = elm.getElement(j);
                if (inf instanceof MCRMetaClassification) {
                    String classID = ((MCRMetaClassification) inf).getClassId();
                    String categID = ((MCRMetaClassification) inf).getCategId();
                    boolean exists = MCRCategoryDAOFactory.getInstance().exist(new MCRCategoryID(classID, categID));
                    if (exists) {
                        continue;
                    }
                    MCRActiveLinkException activeLink = new MCRActiveLinkException("Failure while adding link!. Destination does not exist.");
                    String destination = classID + "##" + categID;
                    activeLink.addLink(getId().toString(), destination);
                    // throw activeLink;
                    // TODO: should trigger undo-Event
                }
                if (inf instanceof MCRMetaLinkID) {
                    String destination = ((MCRMetaLinkID) inf).getXLinkHref();
                    if (!MCRXMLMetadataManager.instance().exists(new MCRObjectID(destination))) {
                        continue;
                    }
                    MCRActiveLinkException activeLink = new MCRActiveLinkException("Failure while adding link!. Destination does not exist.");
                    activeLink.addLink(getId().toString(), destination);
                    // throw activeLink;
                    // TODO: should trigger undo-Event
                }
            }
        }
    }

}
