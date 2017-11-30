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

import java.io.IOException;
import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.xml.sax.SAXParseException;

import com.google.gson.JsonObject;

/**
 * This class holds all information of a metadata object.
 * For persistence operations see methods of {@link MCRMetadataManager}.
 *  
 * @author Jens Kupferschmidt
 * @author Mathias Hegner
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public final class MCRObject extends MCRBase {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String ROOT_NAME = "mycoreobject";

    // the object content
    private final MCRObjectStructure mcr_struct;

    private final MCRObjectMetadata mcr_metadata;


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
     */
    public MCRObject() throws MCRException {
        super();
        mcr_struct = new MCRObjectStructure();
        mcr_metadata = new MCRObjectMetadata();
    }

    public MCRObject(byte[] bytes, boolean valid) throws SAXParseException {
        this();
        setFromXML(bytes, valid);
    }

    public MCRObject(Document doc) {
        this();
        setFromJDOM(doc);
    }

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
    @Override
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
     * This method creates a XML stream for all object data.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Document with the XML data of the object as byte array
     */
    @Override
    public final Document createXML() throws MCRException {
        try {
            Document doc = super.createXML();
            Element elm = doc.getRootElement();
            elm.addContent(mcr_struct.createXML());
            elm.addContent(mcr_metadata.createXML());
            elm.addContent(mcr_service.createXML());
            return doc;
        } catch (MCRException exc) {
            throw new MCRException("The content of '" + mcr_id + "' is invalid.", exc);
        }
    }

    /**
     * Creates the JSON representation of this object. Extends the {@link MCRBase#createJSON()}
     * method with the following content:
     * 
     * <pre>
     *   {
     *     structure: {@link MCRObjectStructure#createJSON},
     *     metadata: {@link MCRObjectMetadata#createJSON},
     *     service: {@link MCRObjectService#createJSON},
     *   }
     * </pre>
     * 
     * @return a json gson representation of this object
     */
    @Override
    public JsonObject createJSON() {
        JsonObject object = super.createJSON();
        object.add("structure", mcr_struct.createJSON());
        object.add("metadata", mcr_metadata.createJSON());
        object.add("service", mcr_service.createJSON());
        return object;
    }

    @Override
    protected String getRootTagName() {
        return ROOT_NAME;
    }

    /**
     * The method print all informations about this MCRObject.
     */
    public final void debug() {
        if (LOGGER.isDebugEnabled()) {
            if (mcr_id == null) {
                LOGGER.debug("MCRObject ID : missing");
            } else {
                LOGGER.debug("MCRObject ID : {}", mcr_id);
            }
            LOGGER.debug("MCRObject Label : {}", mcr_label);
            LOGGER.debug("MCRObject Schema : {}", mcr_schema);
            LOGGER.debug("");
        }
        mcr_struct.debug();
        mcr_metadata.debug();
    }

    /**
     * Validates this MCRObject. This method throws an exception if:
     *  <ul>
     *  <li>the mcr_id is null</li>
     *  <li>the XML schema is null or empty</li>
     *  <li>the service part is null or invalid</li>
     *  <li>the structure part is null or invalid</li>
     *  <li>the metadata part is null or invalid</li>
     *  </ul>
     * 
     * @throws MCRException the MCRObject is invalid
     */
    @Override
    public void validate() {
        super.validate();
        MCRObjectStructure structure = getStructure();
        MCRObjectMetadata metadata = getMetadata();
        if (structure == null) {
            throw new MCRException("The <structure> part of '" + getId() + "' is undefined.");
        }
        if (metadata == null) {
            throw new MCRException("The <metadata> part of '" + getId() + "' is undefined.");
        }
        try {
            structure.validate();
        } catch (MCRException exc) {
            throw new MCRException("The <structure> part of '" + getId() + "' is invalid.", exc);
        }
        try {
            metadata.validate();
        } catch (MCRException exc) {
            throw new MCRException("The <metadata> part of '" + getId() + "' is invalid.", exc);
        }
    }

    /**
     * @return true if the MCRObject has got a parent mcrobject, false otherwise 
     */
    public boolean hasParent() {
        return getStructure().getParentID() != null;
    }

    public MCRObjectID getParent() {
        return getStructure().getParentID();
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
                    if (MCRMetadataManager.exists(destination)) {
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
