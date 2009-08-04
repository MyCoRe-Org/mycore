package org.mycore.importer.mapping.datamodel;

import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.mycore.importer.mapping.MCRImportMetadataResolverManager;

public class MCRImportDatamodel2 extends MCRImportAbstractDatamodel {

    public MCRImportDatamodel2(Document datamodel, MCRImportMetadataResolverManager metadataResolverManager) {
        super(datamodel, metadataResolverManager);
    }

    public String getEnclosingName(String metadataName) {
        Element metadataElement = findMetadataChild(metadataName);
        String parentName = metadataElement.getAttributeValue("enclosingName");
        if(parentName != null)
            return parentName;
        return "def." + metadataName;
    }

    public String getClassname(String metadataName) {
        Element metadataElement = findMetadataChild(metadataName);
        String type = metadataElement.getAttributeValue("type");
        return getMetadataResolverManager().getClassNameByType(type);
    }

    public Boolean isNotinherit(String metadataName) {
        Element metadataElement = findMetadataChild(metadataName);
        String notInheritValue = metadataElement.getAttributeValue("notinherit");
        if(notInheritValue == null || notInheritValue.equals("ignore"))
            return null;
        return Boolean.valueOf(notInheritValue);
    }

    public Boolean isHeritable(String metadataName) {
        Element metadataElement = findMetadataChild(metadataName);
        String notInheritValue = metadataElement.getAttributeValue("heritable");
        if(notInheritValue == null || notInheritValue.equals("ignore"))
            return null;
        return Boolean.valueOf(notInheritValue);
    }

    public String getType(String metadataName) {
        Element metadataElement = findMetadataChild(metadataName);
        return metadataElement.getAttributeValue("type");
    }

//    @Override
//    protected boolean isCached(String metadataName) {
//        if(getCachedMetadataList() != null && metadataName.equals(getCachedMetadataList().getAttribute("name")))
//            return true;
//        return false;
//    }

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
        return null;
    }
}