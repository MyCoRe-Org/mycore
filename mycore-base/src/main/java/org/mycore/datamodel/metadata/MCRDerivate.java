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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRException;

import com.google.gson.JsonObject;

/**
 * This class holds all information of a derivate. For persistence operations
 * see methods of {@link MCRMetadataManager}.
 * 
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler
 */
public final class MCRDerivate extends MCRBase {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String ROOT_NAME = "mycorederivate";

    public static final int MAX_LABEL_LENGTH = 256;

    // the object content
    private MCRObjectDerivate mcrDerivate;

    private int order;

    protected String mcrLabel = null;

    /**
     * This is the constructor of the MCRDerivate class. It make an instance of
     * the parser class and the metadata class.
     * 
     * @exception MCRException
     *                general Exception of MyCoRe
     */
    public MCRDerivate() throws MCRException {
        super();
        mcrDerivate = new MCRObjectDerivate(getId());
        order = 1;
    }

    public MCRDerivate(byte[] bytes, boolean valid) throws JDOMException {
        this();
        setFromXML(bytes, valid);
    }

    public MCRDerivate(Document doc) {
        this();
        setFromJDOM(doc);
    }

    public MCRDerivate(URI uri) throws IOException, JDOMException {
        this();
        setFromURI(uri);
    }

    /**
     * This methode return the instance of the MCRObjectDerivate class. If this
     * was not found, null was returned.
     * 
     * @return the instance of the MCRObjectDerivate class
     */
    public MCRObjectDerivate getDerivate() {
        return mcrDerivate;
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

        // get the derivate data of the object
        Element derivateElement = jdomDocument.getRootElement().getChild("derivate");
        mcrDerivate = new MCRObjectDerivate(mcrId, derivateElement);
    }

    /**
     * This methode create a XML stream for all object data.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Document with the XML data of the object as byte array
     */
    @Override
    public Document createXML() throws MCRException {
        Document doc = super.createXML();
        Element elm = doc.getRootElement();
        elm.setAttribute("order", String.valueOf(order));
        if (mcrLabel != null) {
            elm.setAttribute("label", mcrLabel);
        }
        elm.addContent(mcrDerivate.createXML());
        elm.addContent(mcrService.createXML());
        return doc;
    }

    @Override
    protected String getRootTagName() {
        return ROOT_NAME;
    }

    /**
     * Reads all files and urns from the derivate.
     * 
     * @return A {@link Map} which contains the files as key and the urns as value.
     * If no URN assigned the map will be empty.
     */
    public Map<String, String> getUrnMap() {
        Map<String, String> fileUrnMap = new HashMap<>();

        XPathExpression<Element> filesetPath = XPathFactory.instance().compile("./mycorederivate/derivate/fileset",
            Filters.element());

        Element result = filesetPath.evaluateFirst(this.createXML());
        if (result == null) {
            return fileUrnMap;
        }
        String urn = result.getAttributeValue("urn");

        if (urn != null) {
            XPathExpression<Element> filePath = XPathFactory
                .instance()
                .compile("./mycorederivate/derivate/fileset[@urn='" + urn + "']/file", Filters.element());
            List<Element> files = filePath.evaluate(this.createXML());

            for (Element currentFileElement : files) {
                String currentUrn = currentFileElement.getChildText("urn");
                String currentFile = currentFileElement.getAttributeValue("name");
                fileUrnMap.put(currentFile, currentUrn);
            }
        }
        return fileUrnMap;
    }

    /**
     * This methode return the label or null if it was not set. 
     * 
     * @return the label as a string
     */
    public String getLabel() {
        return mcrLabel;
    }

    /**
     * The method print all informations about this MCRObject.
     */
    public void debug() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MCRDerivate ID : {}", mcrId);
            LOGGER.debug("MCRDerivate Schema : {}", mcrSchema);
            LOGGER.debug("");
        }
    }

    /**
     * Validates this MCRDerivate. This method throws an exception if:
     *  <ul>
     *  <li>the mcr_id is null</li>
     *  <li>the XML schema is null or empty</li>
     *  <li>the service part is null or invalid</li>
     *  <li>the MCRObjectDerivate is null or invalid</li>
     *  </ul>
     * 
     * @throws MCRException the MCRDerivate is invalid
     */
    @Override
    public void validate() throws MCRException {
        super.validate();
        MCRObjectDerivate derivate = getDerivate();
        if (derivate == null) {
            throw new MCRException("The <derivate> part of '" + getId() + "' is undefined.");
        }
        try {
            derivate.validate();
        } catch (Exception exc) {
            throw new MCRException("The <derivate> part of '" + getId() + "' is invalid.", exc);
        }
    }

    /**
     * @return the {@link MCRObjectID} of the owner of the derivate
     */
    public MCRObjectID getOwnerID() {
        return this.getDerivate().getMetaLink().getXLinkHrefID();
    }

    @Override
    public void setId(MCRObjectID id) {
        super.setId(id);
        this.mcrDerivate.setDerivateID(id);
    }

    /**
     * This method set the derivate label.
     * 
     * @param label - the derivate label
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

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    protected void setFromJDOM(Document doc) {
        super.setFromJDOM(doc);
        this.order = Optional.ofNullable(doc.getRootElement().getAttributeValue("order"))
            .map(Integer::valueOf)
            .orElse(1);
    }

    @Override
    public JsonObject createJSON() {
        JsonObject base = super.createJSON();
        if (mcrLabel != null) {
            base.addProperty("label", mcrLabel);
        }
        return base;
    }
}
