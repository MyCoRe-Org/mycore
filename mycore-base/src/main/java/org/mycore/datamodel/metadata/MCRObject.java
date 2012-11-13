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

import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.io.IOException;
import java.net.URI;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.xml.sax.SAXParseException;

/**
 * This class holds all information of a metadata object.
 * For persistence operations see methods of {@link MCRMetadataManager}.
 *  
 * @author Jens Kupferschmidt
 * @author Mathias Hegner
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
final public class MCRObject extends MCRBase {
    // the object content
    private final MCRObjectStructure mcr_struct;

    private final MCRObjectMetadata mcr_metadata;

    private static final Logger LOGGER = Logger.getLogger(MCRObject.class);

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
        mcr_struct = new MCRObjectStructure();
        mcr_metadata = new MCRObjectMetadata();
    }

    /**
     * @param bytes
     * @param valid
     * @throws SAXParseException
     */
    public MCRObject(byte[] bytes, boolean valid) throws SAXParseException, IOException {
        this();
        setFromXML(bytes, valid);
    }

    /**
     * @param doc
     * @throws SAXParseException
     */
    public MCRObject(Document doc) {
        this();
        setFromJDOM(doc);
    }

    /**
     * @param uri
     * @throws SAXParseException
     */
    public MCRObject(URI uri) throws SAXParseException, IOException {
        this();
        setFromURI(uri);
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
     * This method return the instance of the MCRObjectStructure class. If this
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
        mcr_id = MCRObjectID.getInstance(jdom_element_root.getAttributeValue("ID"));
        mcr_label = jdom_element_root.getAttributeValue("label");

        if (mcr_label != null && (mcr_label = mcr_label.trim()).length() > MAX_LABEL_LENGTH) {
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
        if (jdom_element != null) {
            mcr_struct.setFromDOM(jdom_element);
        }
    }

    private void setMetadata() {
        // get the metadata of the object
        org.jdom.Element jdom_element_root = jdom_document.getRootElement();
        org.jdom.Element jdom_element = jdom_element_root.getChild("metadata");

        if (jdom_element != null) {
            mcr_metadata.setFromDOM(jdom_element);
        }
    }

    private void setService() {
        org.jdom.Element jdom_element;
        // get the service data of the object
        org.jdom.Element jdom_element_root = jdom_document.getRootElement();
        jdom_element = jdom_element_root.getChild("service");
        if (jdom_element != null) {
            mcr_service.setFromDOM(jdom_element);
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
    public final Document createXML() throws MCRException {
        Document doc = super.createXML();
        Element elm = doc.getRootElement();
        elm.addContent(mcr_struct.createXML());
        elm.addContent(mcr_metadata.createXML());
        elm.addContent(mcr_service.createXML());
        return doc;
    }

    @Override
    protected String getRootTagName() {
        return "mycoreobject";
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

    /**
     * @return true if the MCRObject has got a parent mcrobject, false otherwise 
     */
    public boolean hasParent() {
        return getStructure().getParentID() == null ? false : true;
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
                    MCRActiveLinkException activeLink = new MCRActiveLinkException(
                            "Failure while adding link!. Destination does not exist.");
                    String destination = classID + "##" + categID;
                    activeLink.addLink(getId().toString(), destination);
                    // throw activeLink;
                    // TODO: should trigger undo-Event
                }
                if (inf instanceof MCRMetaLinkID) {
                    MCRObjectID destination = ((MCRMetaLinkID) inf).getXLinkHrefID();
                    if (!MCRXMLMetadataManager.instance().exists(destination)) {
                        continue;
                    }
                    MCRActiveLinkException activeLink = new MCRActiveLinkException(
                            "Failure while adding link!. Destination does not exist.");
                    activeLink.addLink(getId().toString(), destination.toString());
                    // throw activeLink;
                    // TODO: should trigger undo-Event
                }
            }
        }
    }

}
