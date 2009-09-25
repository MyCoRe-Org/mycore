package org.mycore.importer.derivate;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.mycore.common.MCRConstants;
import org.mycore.importer.mapping.MCRImportMappingManager;

/**
 * This class is the import abstraction of a mycore derivate.
 * 
 * @author Matthias Eichner
 */
public class MCRImportDerivate {

    private static final Logger LOGGER = Logger.getLogger(MCRImportDerivate.class);
    
    /**
     * The id of the derivate. Its important that this id is equal
     * to the id of the derivate mapping link.
     */
    private String derivateId;
    
    /**
     * The description of the derivate. Max character size is 256.
     */
    private String label;

    /**
     * The root file is the entry point when the derivate is viewed.
     */
    private String rootFile;

    /**
     * A list of all file paths of this derivate
     */
    private ArrayList<String> fileList;

    /**
     * A list of all linked internal MCRObject-IDs.
     */
    private ArrayList<String> linkedObjectIds;

    /**
     * Creates a new instance of a importer derivate.
     */
    public MCRImportDerivate(String derivateId) {
        this.derivateId = derivateId;
        this.fileList = new ArrayList<String>();
        this.linkedObjectIds = new ArrayList<String>();
    }

    /**
     * Sets the root file.
     * 
     * @param rootFile the new root document
     */
    public void setRootFile(String rootFile) {
        this.rootFile = rootFile;
        if(!fileList.contains(rootFile))
            fileList.add(rootFile);
    }

    /**
     * Returns the root file.
     *  
     * @return the path to the root file
     */
    public String getRootFile() {
        return rootFile;
    }

    /**
     * Sets a label for this derivate.
     * 
     * @param label the label
     */
    public void setLabel(String label) {
        this.label = label;
    }
    
    /**
     * Returns the label of this derivate.
     * 
     * @return the label as string
     */
    public String getLabel() {
        return label;
    }
    
    /**
     * Adds a new file to the derivate. Is it the first one,
     * then set it as root.
     * 
     * @param filePath the path of the file
     */
    public void addFile(String filePath) {
        if(fileList.size() == 0)
            rootFile = filePath;
        fileList.add(filePath);
    }

    /**
     * Removes a file from the derivate. If the removed one was the root,
     * the file at index 0 is set as new root.
     * 
     * @param filePath file which have to be removed
     */
    public void removeFile(String filePath) {
        fileList.remove(filePath);
        if(filePath.equals(rootFile) && fileList.size() > 0)
            rootFile = fileList.get(0);
    }

    /**
     * Returns a list of all files the derivate contains.
     * 
     * @return a list of files
     */
    public ArrayList<String> getFileList() {
        return fileList;
    }

    /**
     * Returns the id of the derivate.
     * 
     * @return the unique derivate id.
     */
    public String getDerivateId() {
        return derivateId;
    }

    /**
     * Adds a new linked object id to the object id list. This id is import
     * internal, so its not necessary that is something like DocPortal_author_xxx.
     * 
     * @param objectId a new object id
     */
    public void addLinkedObjectId(String objectId) {
        linkedObjectIds.add(objectId);
    }

    /**
     * Returns the linked object list.
     * 
     * @return a list of all mcrobjects which are linked to the derivate. 
     */
    public ArrayList<String> getLinkedObjectIds() {
        return linkedObjectIds;
    }
    
    
    public Element createXML() {
        if(linkedObjectIds.isEmpty())
            LOGGER.warn("MCRDerivate " + derivateId + " has no linked objects!");

        Element rootElement = new Element("mycorederivate");
        rootElement.addNamespaceDeclaration(MCRConstants.XSI_NAMESPACE);
        rootElement.addNamespaceDeclaration(MCRConstants.XLINK_NAMESPACE);
        rootElement.setAttribute("noNamespaceSchemaLocation", "datamodel-derivate.xsd", MCRConstants.XSI_NAMESPACE);
        rootElement.setAttribute("ID", derivateId);
        if(label == null || label.equals(""))
            label = derivateId;
        rootElement.setAttribute("label", label);

        Element derivateElement = new Element("derivate");
        rootElement.addContent(derivateElement);

        // linked objects
        Element linkmetasElement = new Element("linkmetas");
        linkmetasElement.setAttribute("class", "MCRMetaLinkID");
        for(String id : linkedObjectIds) {
            Element linkmetaElement = new Element("linkmeta");
            linkmetaElement.setAttribute("type", "locator", MCRConstants.XLINK_NAMESPACE);
            linkmetaElement.setAttribute("href", id, MCRConstants.XLINK_NAMESPACE);
            linkmetasElement.addContent(linkmetaElement);
        }
        derivateElement.addContent(linkmetasElement);

        // internals
        Element internalsElement = new Element("internals");
        internalsElement.setAttribute("class", "MCRMetaIFS");
        derivateElement.addContent(internalsElement);

        Element internalElement = new Element("internal");
//        internalElement.setAttribute("sourcepath", derivateId);
        String rootFileImage = rootFile;
        if(rootFileImage.contains("/"))
            rootFileImage = rootFileImage.substring(rootFileImage.lastIndexOf("/")+1, rootFileImage.length());
        internalElement.setAttribute("maindoc", rootFileImage);
        internalsElement.addContent(internalElement);

        // add empty service element to root
        rootElement.addContent(new Element("service"));

        return rootElement;
    }
}