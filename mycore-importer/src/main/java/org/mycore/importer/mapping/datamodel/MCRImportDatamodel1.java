package org.mycore.importer.mapping.datamodel;

import java.util.ArrayList;
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
            return iMinOccurs > 0 ? true : false;
        } catch(NumberFormatException nfe) {
            LOGGER.error("The minOccurs value in the metadata element '" + metadataName + "' in datamodel '" +
                    datamodel.getBaseURI() + "' is invalid (not a number)!");
        }
        return false;
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

    @SuppressWarnings("unchecked")
    public List<String> getMetadataNames() {
        List<String> nameList = new ArrayList<String>();
        Element rootElement = datamodel.getRootElement();
        Element metadata = rootElement.getChild("metadata");
        for(Element e : (List<Element>)metadata.getContent(new ElementFilter("element"))) {
            // the metadataName is defined in the child of a metadataElement
            Element metadataChildElement = (Element) e.getContent(new ElementFilter()).get(0);
            nameList.add(metadataChildElement.getAttributeValue("name"));
        }
        return nameList;
    }

}