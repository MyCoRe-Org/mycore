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

import java.io.IOException;
import java.net.URI;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
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
        super.setUp();

        // get the structure data of the object
        Element structureElement = jdom_document.getRootElement().getChild("structure");
        if (structureElement != null) {
            mcr_struct.setFromDOM(structureElement);
        }

        // get the metadata of the object
        Element metadataElement = jdom_document.getRootElement().getChild("metadata");

        if (metadataElement != null) {
            mcr_metadata.setFromDOM(metadataElement);
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
        if (!super.isValid()) {
            LOGGER.warn("MCRBase is invalid");
            return false;
        }
        if (!getStructure().isValid()) {
            LOGGER.warn("Structure is invalid");
            return false;
        }
        if (!getMetadata().isValid()) {
            LOGGER.warn("Metadata is invalid");
            return false;
        }
        return true;
    }

    /**
     * @return true if the MCRObject has got a parent mcrobject, false otherwise 
     */
    public boolean hasParent() {
        return getStructure().getParentID() != null;
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
                    MCRObjectID destination = ((MCRMetaLinkID) inf).getXLinkHrefID();
                    if (!MCRXMLMetadataManager.instance().exists(destination)) {
                        continue;
                    }
                    MCRActiveLinkException activeLink = new MCRActiveLinkException("Failure while adding link!. Destination does not exist.");
                    activeLink.addLink(getId().toString(), destination.toString());
                    // throw activeLink;
                    // TODO: should trigger undo-Event
                }
            }
        }
    }

}
