package org.mycore.importer.mapping.datamodel;

import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.mycore.importer.mapping.MCRImportMetadataResolverTable;

public class MCRImportDatamodel2 extends MCRImportAbstractDatamodel {

    public MCRImportDatamodel2(Document datamodel) {
        super(datamodel);
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
        return MCRImportMetadataResolverTable.getClassNameByType(type);
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
    
    @Override
    protected boolean isCached(String metadataName) {
        if(metadataName.equals(getCachedMetadataElement().getAttribute("name")))
            return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    protected Element findMetadataChild(String metadataName) {
        if (isCached(metadataName))
            return getCachedMetadataElement();
        Element rootElement = datamodel.getRootElement();
        List<Element> metadataElements = rootElement.getChildren("element");
        // go through all elements
        for (Element metadataElement : metadataElements) {
            if (metadataName.equals(metadataElement.getAttributeValue("name"))) {
                // right metadata found -> cache & return it
                setCachedMetadataElement(metadataElement);
                return metadataElement;
            }
        }
        return null;
    }
}