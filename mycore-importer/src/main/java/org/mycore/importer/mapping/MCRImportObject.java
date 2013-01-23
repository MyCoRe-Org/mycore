package org.mycore.importer.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.importer.mapping.datamodel.MCRImportDatamodel;
import org.mycore.importer.mapping.datamodel.MCRImportDatamodel.Inheritance;

/**
 * <p>
 * The import object is an abstraction of the </code>MCRObject</code>.
 * It has an id, a label, a parent and a list of meta data elements.
 * </p><p>
 * A <code>MCRImportObject</code> always depends on a
 * <code>MCRImportDatamodel<code>. This is necessary for adding meta data
 * elements, for the schema location and for general validation of the
 * object.
 * </p>
 * For further processing call <code>createXML</code> to create a jdom
 * element of this object.
 * 
 * @author Matthias Eichner
 */
public class MCRImportObject {

    private static final Logger LOGGER = Logger.getLogger(MCRImportObject.class);

    /**
     * Import id of the object.
     */
    protected String id;
    /**
     * Label of the object.
     */
    protected String label;
    protected MCRImportDatamodel datamodel;

    protected Element parentElement;
    protected List<Element> derivateList;
    protected Hashtable<String, MCRImportMetadata> metadataTable;
    protected Element serviceElement;

    public MCRImportObject(MCRImportDatamodel datamodel) {
        this.derivateList = new ArrayList<Element>();
        this.metadataTable = new Hashtable<String, MCRImportMetadata>();
        this.datamodel = datamodel;
        this.serviceElement = new Element("service");
    }

    /**
    * This method creates a XML stream for all object data.
    *
    * @return the root element of the mycore object
    */
    public Element createXML() {
        Element mcrObjectElement = new Element("mycoreobject");
        // some attributes
        mcrObjectElement.setAttribute("ID", id);
        if(label == null || label.equals(""))
            label = id;
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
            structureElement.addContent(derivatesElement);
        }

        // metadata part
        Element metadataElement = new Element("metadata");
        mcrObjectElement.addContent(metadataElement);

        for(MCRImportMetadata metadata : metadataTable.values()) {
            metadataElement.addContent( metadata.createXML() );
        }
        // service element
        mcrObjectElement.addContent(serviceElement);

        return mcrObjectElement;
    }

    /**
     * Returns the id of the import object.
     * 
     * @return the import id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the import id of this object.
     * 
     * @param id new id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the used data model.
     * 
     * @return data model
     */
    public MCRImportDatamodel getDatamodel() {
        return datamodel;
    }

    /**
     * Returns the label of the import object.
     * 
     * @return label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets a new label.
     * 
     * @param label new label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Returns the parent element of this import object.
     * 
     * @return parent element
     */
    public Element getParent() {
        return parentElement;
    }

    /**
     * Sets a new parent element for the object.<p>
     * e.g.: 
     * &lt;parent inherited="0" xlink:type="locator" xlink:href="import_id"/&gt;
     * </p>
     * 
     * @param parent the new parent element
     */
    public void setParent(Element parent) {
        if(!parent.getName().equals("parent")) {
            LOGGER.error("Parent element tag has to be 'parent' and not '" + parent.getName() + "'!");
            return;
        }
        this.parentElement = parent;
    }

    /**
     * Sets a new service element.<p>
     * e.g.:</br>
     * &lt;service&gt;</br>
     * &nbsp;&nbsp;...</br>
     * &lt;/service&gt;
     * </p>
     *
     * @param serviceElement the new service element
     */
    public void setServiceElement(Element serviceElement) {
        if(!serviceElement.getName().equals("service")) {
            LOGGER.error("Service element tag has to be 'service' and not '" + serviceElement.getName() + "'!");
            return;
        }
        this.serviceElement = serviceElement;
    }
    
    /**
     * Returns the service element.
     * 
     * @return service element
     */
    public Element getServiceElement() {
        return serviceElement;
    }

    /**
     * Returns a list of all derivates which are set in this import object.
     * 
     * @return list of derivates
     */
    public List<Element> getDerivateList() {
        return derivateList;
    }

    /**
     * Returns a list of all meta data elements.
     * 
     * @return a list of <code>MCRImportMetadata</code>
     */
    public Collection<MCRImportMetadata> getMetadataList() {
        return metadataTable.values();
    }

    /**
     * <p>Adds a new derivate to the object. The name of the element has
     * to be "derobject".</p><p>
     * e.g.: &lt;derobject xlink:type="locator" xlink:href="import_derivate_id" xlink:label="label"&gt;
     * </p>
     * 
     * @param derivateElement a new derivate element
     */
    public void addDerivate(Element derivateElement) {
        if(!derivateElement.getName().equals("derobject")) {
            LOGGER.error("Derivate element tag has to be 'derobject' and not '" + derivateElement.getName() + "'!");
            return;
        }
        derivateList.add(derivateElement);
    }

    /**
     * <p>Adds a new meta data element.</p><p>e.g.: 
     * &lt;title xml:lang="de" form="plain"&gt;Sample title&lt;/title&gt;
     * </p>
     * 
     * @param metadataChild the child to add
     * @return the added <code>MCRImportMetadata</code>
     */
    public MCRImportMetadata addMetadataChild(Element metadataChild) {
        String tag = metadataChild.getName();
        // Compare the tag with the metadata table.
        // If no metadata object exists create a new one,
        // otherwise add it.
        MCRImportMetadata metadata = metadataTable.get(tag);
        if(metadata == null) {
            metadata = createMetadata(tag);
            if(metadata != null)
                metadataTable.put(tag, metadata);
        }
        if(metadata == null)
            return null;

        // add child
        metadata.addChild(metadataChild);
        return metadata;
    }

    /**
     * Adds a new <code>MCRImportMetadata</code> element to the object.
     * If a metadata object with the same tag already exists its overwritten.
     * 
     * @param metadata the metadata to add
     * @return the previous <code>MCRImportMetadata</code> of the specified key in this hashtable,
     * or null if it did not have one 
     */
    public MCRImportMetadata addImportMetadata(MCRImportMetadata metadata) {
        return metadataTable.put(metadata.getTag(), metadata);
    }

    /**
     * Returns a <code>MCRImportMetadata</code> object from the internal
     * metadata table. If no metadata with the specified name is found,
     * null is returned.
     * 
     * @param metadataName name of the metadata element (e.g. title not def.title!)
     * @return instance of <code>MCRImportMetadata</code>
     */
    public MCRImportMetadata getMetadata(String metadataName) {
        return metadataTable.get(metadataName);
    }

    /**
     * Returns the schema location. e.g.: "datamodel-author.xsd"
     * 
     * @return schema location of the data model
     */
    protected String getSchemaLocation() {
        int indexOfFS = datamodel.getPath().lastIndexOf("/") + 1;
        int indexOfPoint = datamodel.getPath().lastIndexOf(".");
        String objectType = datamodel.getPath().substring(indexOfFS, indexOfPoint);
        return "datamodel-" + objectType + ".xsd";
    }

    /**
     * This method creates a enclosing meta data object in dependence of the
     * metadataName. This method is only called once for each meta data 
     * element.
     * 
     * @param metadataName name of the metadata
     * @return a new instance of <code>MCRImportMetadata</code>
     */
    protected MCRImportMetadata createMetadata(String metadataName) {
        // receive the enclosingName, classname, heritable and notinherit from the datamodel
        String className = datamodel.getClassname(metadataName);
        Inheritance notInherit = datamodel.isNotinherit(metadataName);
        Inheritance heritable = datamodel.isHeritable(metadataName);
        String tag = datamodel.getEnclosingName(metadataName);
        if (className == null || tag == null) {
            LOGGER.error("Cannot create metadata element '" + metadataName + "'! Because the class name "
                    + " (e.g. 'classification') or the enclosing name (e.g. 'def.title') is null.");
            return null;
        }
        // create the metadata object
        MCRImportMetadata metaData = new MCRImportMetadata(tag);
        metaData.setClassName(className);
        if (notInherit != Inheritance.IGNORE)
            metaData.setNotinherit(notInherit.getBoolean());
        if (heritable != Inheritance.IGNORE)
            metaData.setHeritable(heritable.getBoolean());
        return metaData;
    }

    /**
     * Checks if the import object is valid. Currently it checks only if
     * required metadata elements are missing (minOccurs > 0).
     * 
     * @return true if the import object is valid, otherwise false
     */
    public boolean isValid() {
        boolean valid = true;
        StringBuilder errorMsg = new StringBuilder();
        errorMsg.append("(").append(id).append(") The following errors occur:\n");
        // is id set?
        if(id == null || id.equals("")) {
            errorMsg.append("Id is missing\n");
            valid = false;
        }
        StringBuilder requiredMsg = new StringBuilder();
        // checks if all required metadata elements are set
        List<String> metadataNameList = datamodel.getMetadataNames();
        for(String name : metadataNameList) {
            if(datamodel.isRequired(name)) {
                MCRImportMetadata md = metadataTable.get(name);
                if(md == null) {
                    valid = false;
                    String enclosingName = datamodel.getEnclosingName(name);
                    requiredMsg.append("-").append(enclosingName).append("\n");
                }
            }
        }
        if(!valid) {
            if(requiredMsg.length() > 0) {
                errorMsg.append("Required metadata elements are missing:\n");
                errorMsg.append(requiredMsg);
            }
            LOGGER.warn(errorMsg.toString());
        }
        return valid;
    }
}