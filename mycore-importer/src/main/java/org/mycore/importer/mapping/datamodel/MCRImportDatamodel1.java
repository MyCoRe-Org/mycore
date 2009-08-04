package org.mycore.importer.mapping.datamodel;

import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.mycore.importer.mapping.MCRImportMetadataResolverManager;

public class MCRImportDatamodel1 extends MCRImportAbstractDatamodel {

    private static final Logger LOGGER = Logger.getLogger(MCRImportDatamodel1.class);

    public MCRImportDatamodel1(Document datamodel, MCRImportMetadataResolverManager metadataResolverManager) {
        super(datamodel, metadataResolverManager);
    }

    public String getEnclosingName(String metadataName) {
        Element metadataChild = findMetadataChild(metadataName);
        return metadataChild.getParentElement().getAttributeValue("name");
    }

    public String getClassname(String metadataName) {
        Element metadataChild = findMetadataChild(metadataName);
        return metadataChild.getAttributeValue("class");
    }

    /**
     * Datamodel1 doesnt support not inherit, so null
     * will be returned
     */
    public Boolean isNotinherit(String metadataName) {
        return null;
    }

    /**
     * Datamodel1 doesnt support not Heritable, so null
     * will be returned
     */
    public Boolean isHeritable(String metadataName) {
        return null;
    }

    public String getClassName(String metadataName) {
        Element metadataElement = findMetadataChild(metadataName);
        if(metadataElement == null) {
            LOGGER.error("Couldnt find metadata definition " + metadataName + " in datamodel " + datamodel.getBaseURI());
            return null;
        }
        return metadataElement.getAttributeValue("class");
    }

//    protected boolean isCached(String metadataName) {
//        Element cachedMetadaElement = getCachedMetadataList();
//        if( metadataName != null && !metadataName.equals("") &&
//            cachedMetadaElement != null &&
//            metadataName.equals(getCachedMetadataList().getAttribute("name")))
//            return true;
//        return false;
//    }

    @SuppressWarnings("unchecked")
    protected Element findMetadataChild(String metadataName) {
        Element cachedElement = getCachedMetadataTable().get(metadataName);
        if(cachedElement != null)
            return cachedElement;

        Element rootElement = datamodel.getRootElement();
        Element metadataElement = rootElement.getChild("metadata");
        List<Element> metadataElements = metadataElement.getChildren("element");
        // go through all elements
        for (Element element : metadataElements) {
            // the metadataName is defined in the child of a metadataElement
            Element metadataChildElement = (Element) element.getContent(new ElementFilter()).get(0);
            if (metadataName.equals(metadataChildElement.getAttributeValue("name"))) {
                // right child found -> cache & return it
                addCachedMetadataElement(metadataName, metadataChildElement);
                return metadataChildElement;
            }
        }
        return null;
    }

}