package org.mycore.importer.mapping.datamodel;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.filter.Filters;
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
     * Datamodel1 doesn't support inheritance. False is returned.
     * This is equal to dm2 'ignore'.
     */
    public Inheritance isNotinherit(String metadataName) {
        return Inheritance.IGNORE;
    }

    /**
     * Datamodel1 doesn't support inheritance. False is returned.
     * This is equal to dm2 'ignore'.
     */
    public Inheritance isHeritable(String metadataName) {
        return Inheritance.IGNORE;
    }

    /*
     * (non-Javadoc)
     * @see org.mycore.importer.mapping.datamodel.MCRImportDatamodel#isRequired(java.lang.String)
     */
    public boolean isRequired(String metadataName) {
        Element metadataElement = findMetadataChild(metadataName);
        if(metadataElement == null)
            return false;
        String minOccurs = metadataElement.getParentElement().getAttributeValue("minOccurs");
        // by default minOccur is 1 -> required
        if(minOccurs == null)
            return true;
        try {
            int iMinOccurs = Integer.valueOf(minOccurs);
            return iMinOccurs > 0;
        } catch(NumberFormatException nfe) {
            LOGGER.error("The minOccurs value in the metadata element '" + metadataName + "' in datamodel '" +
                    datamodel.getBaseURI() + "' is invalid (not a number)!");
        }
        return false;
    }

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
            Element metadataChildElement = (Element) element.getContent(Filters.element()).get(0);
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

    public List<String> getMetadataNames() {
        List<String> nameList = new ArrayList<String>();
        Element rootElement = datamodel.getRootElement();
        Element metadata = rootElement.getChild("metadata");
        for(Element e : (List<Element>)metadata.getContent(new ElementFilter("element"))) {
            // the metadataName is defined in the child of a metadataElement
            Element metadataChildElement = (Element) e.getContent(Filters.element()).get(0);
            nameList.add(metadataChildElement.getAttributeValue("name"));
        }
        return nameList;
    }

}