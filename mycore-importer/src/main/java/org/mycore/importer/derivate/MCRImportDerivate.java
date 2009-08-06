package org.mycore.importer.derivate;

import java.util.ArrayList;

import org.jdom.Element;
import org.mycore.common.MCRConstants;

/**
 * This class is the import abstraction of a mycore derivate.
 * 
 * @author Matthias Eichner
 */
public class MCRImportDerivate {

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
     * Creates a new instance of a importer derivate.
     */
    public MCRImportDerivate(String derivateId) {
        this.derivateId = derivateId;
        this.fileList = new ArrayList<String>();
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

    public Element createXML() {
        Element rootElement = new Element("mycorederivate");
        rootElement.addNamespaceDeclaration(MCRConstants.XSI_NAMESPACE);
        rootElement.addNamespaceDeclaration(MCRConstants.XLINK_NAMESPACE);
        rootElement.setAttribute("datamodel-derivate.xsd", "noNamespaceSchemaLocation", MCRConstants.XSI_NAMESPACE);
        rootElement.setAttribute("ID", derivateId);
        if(label != null && !label.equals(""))
            rootElement.setAttribute("label", label);

        Element derivateElement = new Element("derivate");
        rootElement.addContent(derivateElement);

        Element internalsElement = new Element("internals");
        internalsElement.setAttribute("class", "MCRMetaIFS");
        derivateElement.addContent(internalsElement);

        Element internalElement = new Element("internal");
        internalElement.setAttribute("sourcepath", derivateId);
        internalElement.setAttribute("maindoc", rootFile);
        internalsElement.addContent(internalElement);

        return rootElement;
    }
}