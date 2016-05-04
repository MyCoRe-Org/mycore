package org.mycore.datamodel.metadata;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.niofs.MCRPath;

public class MCRMetaDerivateLink extends MCRMetaLink {

    private static final String ANNOTATION = "annotation";

    private static final String ATTRIBUTE = "lang";

    private static final Logger LOGGER = Logger.getLogger(MCRMetaDerivateLink.class);

    private HashMap<String, String> map;

    /** Constructor initializes the HashMap */
    public MCRMetaDerivateLink() {
        super();
        map = new HashMap<String, String>();
    }

    public void setLinkToFile(MCRPath file) {
        String owner = file.getOwner();
        String path = file.subpath(0, file.getNameCount() - 1).toString();
        try {
            path = MCRXMLFunctions.encodeURIPath(path);
        } catch (URISyntaxException uriExc) {
            LOGGER.warn("Unable to encode URI path " + path, uriExc);
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

    @Override
    public boolean isValid() {
        if (!super.isValid()) {
            return false;
        }
        try {
            MCRPath linkedFile = getLinkedFile();
            if (linkedFile == null || !Files.exists(linkedFile)) {
                LOGGER.warn("File not found: " + super.href);
                return false;
            }
        } catch (Exception exc) {
            LOGGER.warn("Unable to validate derivate link " + getXLinkHref(), exc);
            return false;
        }
        return true;
    }

}