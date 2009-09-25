package org.mycore.importer.derivate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

public class MCRImportDerivateFileManager {

    private static final Logger LOGGER = Logger.getLogger(MCRImportDerivateFileManager.class);

    private Element rootElement;

    private File linkedFile;

    /**
     * Creates a new instance of <code>MCRDerivatefileManager</code>.
     * 
     * @param derivateDirectory directory where the linked file is saved
     * @param createNewFile if false and the linked file already exists
     * it is loaded, otherwise a new one is created
     * @throws IOException
     * @throws JDOMException
     */
    public MCRImportDerivateFileManager(File derivateDirectory, boolean createNewFile) throws IOException, JDOMException {        
        if(!derivateDirectory.exists())
            derivateDirectory.mkdir();
        if(!derivateDirectory.isDirectory())
            LOGGER.error(derivateDirectory + " is not a directory");
        this.linkedFile = new File(derivateDirectory.getAbsolutePath() + "/derivateFileLinking.xml");
        loadDerivateFileLinkingFile(createNewFile);
    }

    private void loadDerivateFileLinkingFile(boolean createNewFile) throws IOException, JDOMException {
        if(createNewFile == true || !linkedFile.exists()) {
            rootElement = new Element("derivateFileLinking");
            return;
        }
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(linkedFile);
        rootElement = doc.getRootElement();
    }

    /**
     * Adds a new derivate and all its files to the dom tree.
     * 
     * @param derivate the derivate to add
     */
    public void addDerivateFileLinking(MCRImportDerivate derivate) {
        Element derivateElement = new Element("derivate");
        derivateElement.setAttribute("id", derivate.getDerivateId());
        for(String path : derivate.getFileList()) {
            Element pathElement = new Element("path");
            pathElement.setText(path);
            derivateElement.addContent(pathElement);
        }
        rootElement.addContent(derivateElement);
    }

    @SuppressWarnings("unchecked")
    public List<String> getPathListOfDerivate(String derivateId) {
        List<String> filepathList = new ArrayList<String>();
        List<Element> derivateList = rootElement.getChildren("derivate");
        for(Element derivateElement : derivateList) {
            if(derivateId.equals(derivateElement.getAttributeValue("id"))) {
                // correct element in list found -> save all path
                List<Element> pathList = derivateElement.getChildren("path");
                for(Element pathElement : pathList) {
                    if(pathElement.getText() != null && !pathElement.getText().equals(""))
                        filepathList.add(pathElement.getText());
                }
                break;
            }
        }
        return filepathList;
    }

    /**
     * Saves the dom tree to the file system.
     * 
     * @throws IOException
     */
    public void save() throws IOException {
        XMLOutputter outputter = new XMLOutputter();
        FileOutputStream output = new FileOutputStream(linkedFile);
        if(rootElement.getDocument() == null)
            outputter.output(new Document(rootElement), output);
        else
            outputter.output(rootElement.getDocument(), output);
        output.close();
    }
}