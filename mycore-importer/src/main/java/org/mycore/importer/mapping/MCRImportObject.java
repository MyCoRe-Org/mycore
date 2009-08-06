package org.mycore.importer.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.mycore.common.MCRConstants;
import org.mycore.importer.mapping.datamodel.MCRImportDatamodel;

/**
 * The import object is an abstraction of the mycore object.
 * It has an id, a label, a parent, a list of childs and a
 * list of metadata abstractions. 
 * 
 * @author Matthias Eichner
 */
public class MCRImportObject {

    private static final Logger LOGGER = Logger.getLogger(MCRImportObject.class);

    protected String id;
    protected String label;
    protected MCRImportDatamodel datamodel;

    protected Element parentElement;
    protected List<Element> derivateList;
    protected Hashtable<String, MCRImportMetadata> metadataTable;

    public MCRImportObject(MCRImportDatamodel datamodel) {
        this.derivateList = new ArrayList<Element>();
        this.metadataTable = new Hashtable<String, MCRImportMetadata>();
        this.datamodel = datamodel;
    }

    public Element createXML() {
        Element mcrObjectElement = new Element("mycoreobject");
        // some attributes
        mcrObjectElement.setAttribute("ID", id);
        if(label != null)
            mcrObjectElement.setAttribute("label", label);
        mcrObjectElement.setAttribute("version", "2.0");
        String schemaLocation = getSchemaLocation();
        if(schemaLocation != null)
            mcrObjectElement.setAttribute("noNamespaceSchemaLocation", schemaLocation, MCRConstants.XSI_NAMESPACE);

        // set xlink namespace
        mcrObjectElement.addNamespaceDeclaration(MCRConstants.XLINK_NAMESPACE);

        // structure part
        Element structureElement = new Element("structure");
        mcrObjectElement.addContent(structureElement);
        if(parentElement != null) {
            Element parentsElement = new Element("parents");
            parentsElement.setAttribute("class", "MCRMetaLinkID");
            parentsElement.addContent(parentElement);
            structureElement.addContent(parentsElement);
        }
        if(derivateList.size() > 0) {
            Element derivatesElement = new Element("derobjects");
            derivatesElement.setAttribute("class", "MCRMetaLinkID");
            for(Element derivateElement : derivateList) {
                derivatesElement.addContent(derivateElement);
            }
        }

        // metadata part
        Element metadataElement = new Element("metadata");
        mcrObjectElement.addContent(metadataElement);

        for(MCRImportMetadata metadata : metadataTable.values()) {
            metadataElement.addContent( metadata.createXML() );
        }

        return mcrObjectElement;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDatamodel(MCRImportDatamodel datamodel) {
        this.datamodel = datamodel;
    }

    public MCRImportDatamodel getDatamodel() {
        return datamodel;
    }
    
    public String getSchemaLocation() {
        int indexOfFS = datamodel.getPath().lastIndexOf("/") + 1;
        int indexOfPoint = datamodel.getPath().lastIndexOf(".");
        return datamodel.getPath().substring(indexOfFS, indexOfPoint) + ".xsd";        
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Element getParent() {
        return parentElement;
    }

    public void setParent(Element parent) {
        this.parentElement = parent;
    }

    public List<Element> getDerivateList() {
        return derivateList;
    }

    public Collection<MCRImportMetadata> getMetadataList() {
        return metadataTable.values();
    }

    public void addDerivate(Element derivateElement) {
        derivateList.add(derivateElement);
    }

    public void addMetadataChild(Element metadataChild) {
        String tag = metadataChild.getName();
        // Compare the tag with the metadata table.
        // If no metadata object exists create a new one,
        // otherwise add it.
        MCRImportMetadata metadata = metadataTable.get(tag);
        if(metadata == null) {
            metadata = createMetadata(tag);
            metadataTable.put(tag, metadata);
        }

        // add child
        metadata.addChild(metadataChild);
    }

    protected MCRImportMetadata createMetadata(String metadataName) {
        try {
            // receive the enclosingName, classname, heritable and notinherit from the datamodel
            String className = datamodel.getClassname(metadataName);
            Boolean notInherit = datamodel.isNotinherit(metadataName);
            Boolean heritable = datamodel.isHeritable(metadataName);
            String tag = datamodel.getEnclosingName(metadataName);

            // create the metadata object
            MCRImportMetadata metaData = new MCRImportMetadata(tag);
            metaData.setClassName(className);
            if(notInherit != null)
                metaData.setNotinherit(notInherit);
            if(heritable != null)
                metaData.setHeritable(heritable);
            return metaData;
        } catch(Exception exc) {
            LOGGER.error(exc);
        }
        return null;
    }

    public MCRImportMetadata getMetadata(String metadataName) {
        return metadataTable.get(metadataName);
    }
}
