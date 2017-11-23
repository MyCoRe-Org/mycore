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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.niofs.MCRPath;

public class MCRMetaDerivateLink extends MCRMetaLink {

    private static final String ANNOTATION = "annotation";

    private static final String ATTRIBUTE = "lang";

    private static final Logger LOGGER = LogManager.getLogger();

    private HashMap<String, String> map;

    /** Constructor initializes the HashMap */
    public MCRMetaDerivateLink() {
        super();
        map = new HashMap<>();
    }

    public void setLinkToFile(MCRPath file) {
        String owner = file.getOwner();
        String path = file.subpath(0, file.getNameCount() - 1).toString();
        try {
            path = MCRXMLFunctions.encodeURIPath(path, true);
        } catch (URISyntaxException uriExc) {
            LOGGER.warn("Unable to encode URI path {}", path, uriExc);
        }
        super.href = owner + '/' + path;
    }

    public void setFromDOM(org.jdom2.Element element) throws MCRException {
        super.setFromDOM(element);
        List<Element> childrenList = element.getChildren(MCRMetaDerivateLink.ANNOTATION);
        if (childrenList == null)
            return;

        for (Element anAnnotation : childrenList) {
            String key = anAnnotation.getAttributeValue(MCRMetaDerivateLink.ATTRIBUTE, Namespace.XML_NAMESPACE);
            String annotationText = anAnnotation.getText();
            this.map.put(key, annotationText);
        }
    }

    public Element createXML() throws MCRException {
        Element elm = super.createXML();

        for (String key : map.keySet()) {
            Element annotationElem = new Element(MCRMetaDerivateLink.ANNOTATION);
            annotationElem.setAttribute(MCRMetaDerivateLink.ATTRIBUTE, key, Namespace.XML_NAMESPACE);
            String content = map.get(key);
            if (content == null || content.length() == 0)
                continue;
            annotationElem.addContent(content);
            elm.addContent(annotationElem);
        }

        return elm;
    }

    /**
     * Returns the owner of this derivate link. In most cases this is
     * the derivate id itself.
     *
     * @return the owner of this derivate link.
     */
    public String getOwner() {
        int index = super.href.indexOf('/');
        if (index < 0) {
            return null;
        }
        return super.href.substring(0, index);
    }

    /**
     * Returns the URI decoded path of this derivate link. Use {@link #getRawPath()}
     * if you want the URI encoded path.
     *
     * @return path of this derivate link
     * @throws URISyntaxException the path couldn't be decoded
     */
    public String getPath() throws URISyntaxException {
        return new URI(getRawPath()).getPath();
    }

    /**
     * Returns the raw path of this derivate link. Be aware that
     * this path is URI encoded. Use {@link #getPath()} if you want
     * the URI decoded path.
     *
     * @return URI encoded path
     */
    public String getRawPath() {
        int index = super.href.indexOf('/');
        if (index < 0) {
            return null;
        }
        return super.href.substring(index);
    }

    /**
     * Returns the {@link MCRPath} to this derivate link.
     *
     * @return path to this derivate link
     * @throws URISyntaxException the path part of this derivate link couldn't be decoded because
     *           its an invalid URI
     */
    public MCRPath getLinkedFile() throws URISyntaxException {
        return MCRPath.getPath(getOwner(), getPath());
    }

    /**
     * Validates this MCRMetaDerivateLink. This method throws an exception if:
     * <ul>
     * <li>the subtag is not null or empty</li>
     * <li>the lang value was supported</li>
     * <li>the inherited value is lower than zero</li>
     * <li>the linked files is null or does not exist</li>
     * </ul>
     *
     * @throws MCRException the MCRMetaDerivateLink is invalid
     */
    public void validate() throws MCRException {
        super.validate();
        try {
            MCRPath linkedFile = getLinkedFile();

            if (linkedFile == null) {
                throw new MCRException(getSubTag() + ": linked file is null");
            }

            if (!Files.exists(linkedFile)) {
                LOGGER.warn("{}: File not found: {}", getSubTag(), super.href);
            }

        } catch (Exception exc) {
            throw new MCRException(getSubTag() + ": Error while getting linked file " + super.href, exc);
        }
    }

}
