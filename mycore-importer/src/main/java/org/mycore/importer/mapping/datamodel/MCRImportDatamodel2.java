package org.mycore.importer.mapping.datamodel;

import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.importer.mapping.MCRImportMetadataResolverManager;

public class MCRImportDatamodel2 extends MCRImportAbstractDatamodel {

    private static final Logger LOGGER = Logger.getLogger(MCRImportDatamodel2.class);

    public MCRImportDatamodel2(Document datamodel, MCRImportMetadataResolverManager metadataResolverManager) {
        super(datamodel, metadataResolverManager);
    }

    /*
     * (non-Javadoc)
     * @see org.mycore.importer.mapping.datamodel.MCRImportDatamodel#getEnclosingName(java.lang.String)
     */
    public String getEnclosingName(String metadataName) {
        Element metadataElement = findMetadataChild(metadataName);
        if(metadataElement == null)
            return null;
        String parentName = metadataElement.getAttributeValue("enclosingName");
        if(parentName != null)
            return parentName;
        return "def." + metadataName;
    }

    /*
     * (non-Javadoc)
     * @see org.mycore.importer.mapping.datamodel.MCRImportDatamodel#getClassname(java.lang.String)
     */
    public String getClassname(String metadataName) {
        Element metadataElement = findMetadataChild(metadataName);
        if(metadataElement == null)
            return null;
        String type = metadataElement.getAttributeValue("type");
        if(type == null || type.equals("")) {
            LOGGER.error("No type attribute defined for metadata '" + metadataName + "' in " + 
                          datamodel.getBaseURI() + "!");
            return null;
        }
        return getMetadataResolverManager().getClassNameByType(type);
    }

    /*
     * (non-Javadoc)
     * @see org.mycore.importer.mapping.datamodel.MCRImportDatamodel#isNotinherit(java.lang.String)
     */
    public Boolean isNotinherit(String metadataName) {
        Element metadataElement = findMetadataChild(metadataName);
        if(metadataElement == null)
            return null;
        String notInheritValue = metadataElement.getAttributeValue("notinherit");
        if(notInheritValue == null || notInheritValue.equals("ignore"))
            return null;
        return Boolean.valueOf(notInheritValue);
    }

    /*
     * (non-Javadoc)
     * @see org.mycore.importer.mapping.datamodel.MCRImportDatamodel#isHeritable(java.lang.String)
     */
    public Boolean isHeritable(String metadataName) {
        Element metadataElement = findMetadataChild(metadataName);
        String notInheritValue = metadataElement.getAttributeValue("heritable");
        if(notInheritValue == null || notInheritValue.equals("ignore"))
            return null;
        return Boolean.valueOf(notInheritValue);
    }

    public String getType(String metadataName) {
        Element metadataElement = findMetadataChild(metadataName);
        if(metadataElement == null)
            return null;
        return metadataElement.getAttributeValue("type");
    }

    @SuppressWarnings("unchecked")
    protected Element findMetadataChild(String metadataName) {
        Element cachedElement = getCachedMetadataTable().get(metadataName);
        if(cachedElement != null)
            return cachedElement;

        Element rootElement = datamodel.getRootElement();
        Element metadata = rootElement.getChild("metadata");
        List<Element> metadataElements = metadata.getChildren("element");
        // go through all elements
        for (Element metadataElement : metadataElements) {
            if (metadataName.equals(metadataElement.getAttributeValue("name"))) {
                // right metadata found -> cache & return it
                addCachedMetadataElement(metadataName, metadataElement);
                return metadataElement;
            }
        }
        LOGGER.error("Couldnt find metadata element '" + metadataName + "' in " + 
                datamodel.getBaseURI() + "!");
        return null;
    }
}