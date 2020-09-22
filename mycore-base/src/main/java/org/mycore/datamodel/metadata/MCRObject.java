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

    /**
     * constant value for the object id length
     */
    public static final int MAX_LABEL_LENGTH = 256;

    /**
     * the object content
     */
    private final MCRObjectStructure structure;

    private final MCRObjectMetadata metadata;

    protected String mcrLabel = null;

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
        structure = new MCRObjectStructure();
        metadata = new MCRObjectMetadata();
        mcrLabel = "";
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
    public MCRObjectMetadata getMetadata() {
        return metadata;
    }

    /**
     * This method return the instance of the MCRObjectStructure class. If this
     * was not found, null was returned.
     * 
     * @return the instance of the MCRObjectStructure class
     */
    public MCRObjectStructure getStructure() {
        return structure;
    }

    /**
     * This methode return the object label. If this is not set, null was
     * returned.
     * 
     * @return the lable as a string
     */
    public String getLabel() {
        return mcrLabel;
    }

    /**
     * This method set the object label.
     * 
     * @param label
     *            the object label
     */
    public void setLabel(String label) {
        if (label == null) {
            mcrLabel = label;
        } else {
            mcrLabel = label.trim();
            if (mcrLabel.length() > MAX_LABEL_LENGTH) {
                mcrLabel = mcrLabel.substring(0, MAX_LABEL_LENGTH);
            }
        }
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
    protected void setUp() throws MCRException {
        super.setUp();

        setLabel(jdomDocument.getRootElement().getAttributeValue("label"));

        // get the structure data of the object
        Element structureElement = jdomDocument.getRootElement().getChild("structure");
        if (structureElement != null) {
            structure.setFromDOM(structureElement);
        }

        // get the metadata of the object
        Element metadataElement = jdomDocument.getRootElement().getChild("metadata");

        if (metadataElement != null) {
            metadata.setFromDOM(metadataElement);
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
    public Document createXML() throws MCRException {
        try {
            Document doc = super.createXML();
            Element elm = doc.getRootElement();
            if (mcrLabel != null) {
                elm.setAttribute("label", mcrLabel);
            }
            elm.addContent(structure.createXML());
            elm.addContent(metadata.createXML());
            elm.addContent(mcrService.createXML());
            return doc;
        } catch (MCRException exc) {
            throw new MCRException("The content of '" + mcrId + "' is invalid.", exc);
        }
    }

    /**
     * Creates the JSON representation of this object. Extends the {@link MCRBase#createJSON()}
     * method with the following content:
     * 
     * <pre>
     *   {
     *     id: "mycore_project_00000001",
     *     version: "3.0"
     *     label: "my mycore object",
     *     
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
        if (mcrLabel != null) {
            object.addProperty("label", mcrLabel);
        }
        object.add("structure", structure.createJSON());
        object.add("metadata", metadata.createJSON());
        object.add("service", mcrService.createJSON());
        return object;
    }

    @Override
    protected String getRootTagName() {
        return ROOT_NAME;
    }

    /**
     * The method print all informations about this MCRObject.
     */
    public void debug() {
        if (LOGGER.isDebugEnabled()) {
            if (mcrId == null) {
                LOGGER.debug("MCRObject ID : missing");
            } else {
                LOGGER.debug("MCRObject ID : {}", mcrId);
            }
            LOGGER.debug("MCRObject Label : {}", mcrLabel);
            LOGGER.debug("MCRObject Schema : {}", mcrSchema);
            LOGGER.debug("");
        }
        structure.debug();
        metadata.debug();
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
        if (getId().equals(getParent())) {
            throw new MCRException("This object '" + getId() + "' cannot be parent/child of itself.");
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
