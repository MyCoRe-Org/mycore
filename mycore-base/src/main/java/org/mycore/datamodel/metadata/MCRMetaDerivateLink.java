package org.mycore.datamodel.metadata;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.mycore.common.MCRException;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;

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

    public void setLinkToFile(MCRFile file) {
        String owner = file.getOwnerID();
        String path = file.getAbsolutePath();
        super.href = owner + path;
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

    public MCRFile getLinkedFile() {
        int index = super.href.indexOf('/');
        if (index < 0)
            return null;
        String owner = super.href.substring(0, index);
        String path = super.href.substring(index);
        MCRFilesystemNode rootNode = MCRFile.getRootNode(owner);
        return rootNode == null ? null : (MCRFile) ((MCRDirectory) rootNode).getChildByPath(path);
    }

    @Override
    public boolean isValid() {
        if (!super.isValid()) {
            return false;
        }
        if (getLinkedFile() == null) {
            LOGGER.warn("File not found: " + super.href);
            return false;
        }
        return true;
    }
}