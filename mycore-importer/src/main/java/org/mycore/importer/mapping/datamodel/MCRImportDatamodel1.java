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

    /*
     * (non-Javadoc)
     * @see org.mycore.importer.mapping.datamodel.MCRImportDatamodel#getEnclosingName(java.lang.String)
     */
    public String getEnclosingName(String metadataName) {
        Element metadataChild = findMetadataChild(metadataName);
        if(metadataChild == null)
            return null;
        return metadataChild.getParentElement().getAttributeValue("name");
    }

    /*
     * (non-Javadoc)
     * @see org.mycore.importer.mapping.datamodel.MCRImportDatamodel#getClassname(java.lang.String)
     */
    public String getClassname(String metadataName) {
        Element metadataChild = findMetadataChild(metadataName);
        if(metadataChild == null)
            return null;
        return metadataChild.getAttributeValue("class");
    }

    /**
     * Datamodel1 doesnt support not inherit, so null is returned.
     * This is equal to dm2 'ignore'.
     */
    public Boolean isNotinherit(String metadataName) {
        return null;
    }

    /**
     * Datamodel1 doesnt support not Heritable, so null is returned.
     * This is equal to dm2 'ignore'.
     */
    public Boolean isHeritable(String metadataName) {
        return null;
    }

    public String getClassName(String metadataName) {
        Element metadataElement = findMetadataChild(metadataName);
        if(metadataElement == null)
            return null;
        return metadataElement.getAttributeValue("class");
    }

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
        LOGGER.error("Couldnt find metadata element '" + metadataName + "' in " + 
                datamodel.getBaseURI() + "!");
        return null;
    }

}