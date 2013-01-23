package org.mycore.importer.derivate;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;

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
    private String mainDoc;

    /**
     * A set of all file to upload
     */
    private HashSet<String> fileSet;

    /**
     * The linked object
     */
    private String linkmeta;

    /**
     * Creates a new instance of a importer derivate.
     */
    public MCRImportDerivate(String derivateId) {
        this.derivateId = derivateId;
        this.fileSet = new HashSet<String>();
    }

    /**
     * Sets the main document of the derivate. In general this is only
     * the name of the file (e.g. mypic.png), but the method can also
     * handle paths. If a path is set which is not in the file list, it
     * is added to.
     * 
     * @param mainDoc the new main file
     */
    public void setMainDocument(String mainDoc) {
        File mainDocFile = new File(mainDoc);
        if(mainDocFile.exists()) {
            if(!fileSet.contains(mainDocFile.getAbsolutePath()))
                fileSet.add(mainDocFile.getAbsolutePath());
            this.mainDoc = mainDocFile.getName();
        } else
            this.mainDoc = mainDoc;
    }

    /**
     * Returns the main document of the derivate. This is only
     * the name of the file and not a path.
     *  
     * @return main document
     */
    public String getMainDocument() {
        return mainDoc;
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
     * then set it as the new main document.
     * 
     * @param filePath the path of the file
     */
    public void addFile(String filePath) {
        File file = new File(filePath);
        if(file.exists()) {
            if(fileSet.isEmpty())
                setMainDocument(file.getAbsolutePath());
            fileSet.add(file.getAbsolutePath());
        } else
            LOGGER.warn("'" + filePath + "' does not exist!");
    }

    /**
     * Removes a file from the derivate. If the removed one was the main document,
     * the next file is set as main document.
     * 
     * @param filePath file which have to be removed
     */
    public void removeFile(String filePath) {
        File file = new File(filePath);
        fileSet.remove(file.getAbsolutePath());
        String fileName = file.getName();
        if(fileName.equals(mainDoc) && !fileSet.isEmpty())
            setMainDocument(fileSet.iterator().next());
    }

    /**
     * Returns a set of all files the derivate contains.
     * 
     * @return a list of files
     */
    public HashSet<String> getFileSet() {
        return fileSet;
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
     * Sets the object which is linked to this derivate.
     * 
     * @param objectId the new object id
     */
    public void setLinkedObject(String objectId) {
        this.linkmeta = objectId;
    }

    /**
     * Returns the id of the object which is linked with this derivate.
     * 
     * @return import id of the linked object 
     */
    public String getLinkedObjectId() {
        return this.linkmeta;
    }

    /**
     * Creates a import derivate xml element.
     * 
     * @return a new mcrImportDerivate element
     */
    public Element createXML() {
        if(linkmeta == null || linkmeta.equals(""))
            LOGGER.warn("MCRImportDerivate '" + derivateId + "' has no linked object!");
        if(fileSet.isEmpty())
            LOGGER.warn("MCRImportDerivate '" + derivateId + "' has no files to upload!");

        // create root element with id and label
        Element rootElement = new Element("mcrImportDerivate");
        rootElement.addNamespaceDeclaration(MCRConstants.XLINK_NAMESPACE);
        rootElement.setAttribute("importId", derivateId);
        if(label == null || label.equals(""))
            label = derivateId;
        rootElement.setAttribute("label", label);

        // linked object
        Element linkmetasElement = new Element("linkmetas");
        linkmetasElement.setAttribute("class", "MCRMetaLinkID");
        if(linkmeta != null) {
            Element linkmetaElement = new Element("linkmeta");
            linkmetaElement.setAttribute("type", "locator", MCRConstants.XLINK_NAMESPACE);
            linkmetaElement.setAttribute("href", linkmeta, MCRConstants.XLINK_NAMESPACE);
            linkmetasElement.addContent(linkmetaElement);
        }
        rootElement.addContent(linkmetasElement);

        // files - mainDoc & file list 
        Element filesElement = new Element("files");
        if(mainDoc != null && !mainDoc.equals(""))
            filesElement.setAttribute("mainDoc", mainDoc);
        else
            LOGGER.warn("MCRImportDerivate '" + derivateId + "' hasn't a main document!");
        List<String> fileList = new ArrayList<String>();
        fileList.addAll(fileSet);
        Collections.sort(fileList);
        for (String filePath : fileList) {
            Element fileElement = new Element("file");
            fileElement.setText(filePath);
            filesElement.addContent(fileElement);
        }
        rootElement.addContent(filesElement);

        return rootElement;
    }
}