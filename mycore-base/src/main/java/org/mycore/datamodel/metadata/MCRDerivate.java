/*
 * 
 * $Revision$ $Date$
 * 
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 * 
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.mycore.datamodel.metadata;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRException;
import org.xml.sax.SAXParseException;

/**
 * This class holds all information of a derivate. For persistence operations
 * see methods of {@link MCRMetadataManager}.
 * 
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler
 * @version $Revision$ $Date: 2010-09-30 17:49:21 +0200 (Thu, 30 Sep
 *          2010) $
 */
public final class MCRDerivate extends MCRBase {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String ROOT_NAME = "mycorederivate";

    // the object content
    private MCRObjectDerivate mcr_derivate;

    /**
     * This is the constructor of the MCRDerivate class. It make an instance of
     * the parser class and the metadata class.
     * 
     * @exception MCRException
     *                general Exception of MyCoRe
     */
    public MCRDerivate() throws MCRException {
        super();
        mcr_derivate = new MCRObjectDerivate(getId());
    }

    public MCRDerivate(byte[] bytes, boolean valid) throws SAXParseException {
        this();
        setFromXML(bytes, valid);
    }

    public MCRDerivate(Document doc) {
        this();
        setFromJDOM(doc);
    }

    public MCRDerivate(URI uri) throws SAXParseException, IOException {
        this();
        setFromURI(uri);
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
    @Override
    protected final void setUp() throws MCRException {
        super.setUp();

        // get the derivate data of the object
        Element derivateElement = jdom_document.getRootElement().getChild("derivate");
        mcr_derivate = new MCRObjectDerivate(mcr_id, derivateElement);
    }

    /**
     * This methode create a XML stream for all object data.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Document with the XML data of the object as byte array
     */
    @Override
    public final org.jdom2.Document createXML() throws MCRException {
        Document doc = super.createXML();
        Element elm = doc.getRootElement();
        elm.addContent(mcr_derivate.createXML());
        elm.addContent(mcr_service.createXML());
        return doc;
    }

    @Override
    protected String getRootTagName() {
        return ROOT_NAME;
    }

    /**
     * Reads all files and urns from the derivate.
     * 
     * @return A {@link Map} which contains the files as key and the urns as value. If no URN assigned the map will be empty.
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
     * The method print all informations about this MCRObject.
     */
    public final void debug() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MCRDerivate ID : {}", mcr_id);
            LOGGER.debug("MCRDerivate Label : {}", mcr_label);
            LOGGER.debug("MCRDerivate Schema : {}", mcr_schema);
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
        this.mcr_derivate.setDerivateID(id);
    }
}
