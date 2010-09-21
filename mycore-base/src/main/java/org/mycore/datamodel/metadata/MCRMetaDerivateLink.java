package org.mycore.datamodel.metadata;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.Namespace;
import org.mycore.common.MCRException;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;

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

    public void setFromDOM(org.jdom.Element element) throws MCRException {
        super.setFromDOM(element);
        @SuppressWarnings("unchecked")
        List<Element> childrenList = element.getChildren(MCRMetaDerivateLink.ANNOTATION);
        if (childrenList == null)
            return;
        Iterator<Element> annotationsIter = childrenList.iterator();

        while (annotationsIter.hasNext()) {
            org.jdom.Element anAnnotation = annotationsIter.next();
            String key = anAnnotation.getAttributeValue(MCRMetaDerivateLink.ATTRIBUTE,
                    Namespace.XML_NAMESPACE);
            String annotationText = anAnnotation.getText();
            this.map.put(key, annotationText);
        }
    }

    public Element createXML() throws MCRException {
        Element elm = super.createXML();

        Iterator<String> keys = map.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            org.jdom.Element annotationElem = new Element(MCRMetaDerivateLink.ANNOTATION);
            annotationElem
                    .setAttribute(MCRMetaDerivateLink.ATTRIBUTE, key, Namespace.XML_NAMESPACE);
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
        return (MCRFile) ((MCRDirectory) MCRFile.getRootNode(owner)).getChildByPath(path);
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