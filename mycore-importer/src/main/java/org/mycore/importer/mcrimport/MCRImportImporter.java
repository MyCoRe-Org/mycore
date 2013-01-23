package org.mycore.importer.mcrimport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.importer.MCRImportConfig;
import org.mycore.importer.classification.MCRImportClassificationMap;
import org.mycore.importer.classification.MCRImportClassificationMappingManager;
import org.mycore.importer.event.MCRImportStatusEvent;
import org.mycore.importer.event.MCRImportStatusListener;

/**
 * This class does the import to mycore. Ids assigned by
 * the mapping manager will be replaced by valid mycore ids. If
 * classification mapping is activated the corresponding values
 * are also replaced. To start the import call <code>startImport</code>
 */
public class MCRImportImporter {

    private static final Logger LOGGER = Logger.getLogger(MCRImportImporter.class);

    private static final String LOAD_OBJECT_COMMAND = "load object from file ";

    private static final String LOAD_DERIVATE_COMMAND = "internal import derivate ";

    private MCRImportConfig config;

    private SAXBuilder builder;

    private File tempDirectory;

    private Hashtable<String, MCRImportFileStatus> idTable = new Hashtable<String, MCRImportFileStatus>();

    protected ArrayList<MCRImportStatusListener> listenerList;

    protected MCRImportClassificationMappingManager classManager;

    /**
     * Contains all commands in the correct order.
     */
    protected LinkedList<String> commandList;

    // import status variables  
    protected long objectCount;

    protected long currentObject;

    protected ArrayList<String> errorObjectList;

    public LinkedList<String> getCommandList() {
        return commandList;
    }

    /**
     * Creates a new instance of a MyCoRe importer. The constructor reads the config part
     * of the mapping file and initializes all important variables. To start the import
     * call <code>startImport()</code>.
     * 
     * @param mappingFile the xml mapping file
     * @throws IOException
     * @throws JDOMException
     */
    public MCRImportImporter(File mappingFile) throws IOException, JDOMException {
        if (!mappingFile.exists())
            throw new FileNotFoundException(mappingFile.getAbsolutePath());
        this.builder = new SAXBuilder();
        Element rootElement = getRootElement(mappingFile);
        // get the config from the import xml file
        this.config = new MCRImportConfig(rootElement);
        File mainDirectory = new File(config.getSaveToPath());
        if (!mainDirectory.exists())
            throw new FileNotFoundException(mainDirectory.getAbsolutePath());

        // delete the temp directory from previous imports and create it again
        tempDirectory = new File(config.getSaveToPath(), "_temp");
        LOGGER.info("delete '_temp' directory");
        if (MCRUtils.deleteDirectory(tempDirectory))
            LOGGER.warn("Unable to delete temp directory " + tempDirectory.getAbsolutePath());
        if (tempDirectory.mkdirs())
            LOGGER.warn("Unable to create temp directory " + tempDirectory.getAbsolutePath());

        // create the classification manager
        this.classManager = new MCRImportClassificationMappingManager(new File(config.getSaveToPath() + "classification/"));
        if (this.classManager.getClassificationMapList().isEmpty())
            LOGGER.warn("No classification mapping documents found! Check if the folder 'classification'"
                + " in the import directory exists and all files ends with '.xml'.");

        if (!classManager.isCompletelyFilled()) {
            StringBuilder error = new StringBuilder("The following classification mapping keys are not set:\n");
            for (MCRImportClassificationMap map : classManager.getClassificationMapList()) {
                for (String emptyImportValue : map.getEmptyImportValues())
                    error.append(" " + emptyImportValue + "\n");
            }
            error.append("Before the import can start, all mycore values have to be set or" + " the classifcation mapping needs to be disabled!");
            throw new MCRException(error.toString());
        }

        // create the listener list
        this.listenerList = new ArrayList<MCRImportStatusListener>();
        this.commandList = new LinkedList<String>();

        // build the id table
        buildIdTable(mainDirectory);
    }

    /**
     * Returns the root element of a xml file.
     * 
     * @param file xml file
     * @return
     * @throws IOException
     * @throws JDOMException
     */
    private Element getRootElement(File file) throws IOException, JDOMException {
        // load the mapping xml file document
        Document document = builder.build(file);
        // set the root element
        return document.getRootElement();
    }

    /**
     * Browses through the specified directory to add a valid import
     * xml file to the id hash table.
     * 
     * @param dir the directory where to search
     */
    protected void buildIdTable(File dir) {
        LOGGER.info("Import preprocessing... This could take some time!");
        for (File file : dir.listFiles()) {
            if (file.isDirectory())
                // call this method recursive if its a directory
                buildIdTable(file);
            else if (file.getName().endsWith(".xml")) {
                // if is a valid import file
                Document doc = null;
                try {
                    doc = builder.build(file);
                } catch (Exception exc) {
                    continue;
                }
                Element rE = doc.getRootElement();
                // mycore objects
                if (rE.getName().equals("mycoreobject")) {
                    String importId = rE.getAttributeValue("ID");
                    if (importId == null || importId.equals(""))
                        continue;
                    idTable.put(importId, new MCRImportFileStatus(importId, file.getAbsolutePath(), MCRImportFileType.MCROBJECT));
                } else if (config.isUseDerivates() && config.isImportToMycore() && rE.getName().equals("mcrImportDerivate")) {
                    // derivate objects
                    String importId = rE.getAttributeValue("importId");
                    if (importId == null || importId.equals(""))
                        continue;
                    idTable.put(importId, new MCRImportFileStatus(importId, file.getAbsolutePath(), MCRImportFileType.MCRDERIVATE));
                }
            }
        }
        LOGGER.info("Preprocessing finished!");
    }

    /**
     * This method starts the import. The whole id table
     * will be passed through and every entry will be imported.
     */
    public void generateMyCoReFiles() {
        // some info variables
        this.currentObject = 0;
        this.errorObjectList = new ArrayList<String>();

        // print start informations
        objectCount = idTable.size();
        LOGGER.info("Start with generating MyCoRe xml files!");
        LOGGER.info(objectCount + " objects to import");

        long startTime = System.currentTimeMillis();
        for (MCRImportFileStatus fs : idTable.values()) {
            // object is saved on disk with resolved links etc.
            if (fs.isSavedInTempDirectory())
                continue;
            generateMyCoReXmlFileById(fs.getImportId());
        }

        // print end informations
        long duration = System.currentTimeMillis() - startTime;
        long durationInMinutes = (duration / 1000) / 60;
        LOGGER.info("MyCoRe files successfully generated");
        LOGGER.info("Finished in " + durationInMinutes + " minutes");
        LOGGER.info(objectCount - errorObjectList.size() + " of " + objectCount + " objects successfully generated");
        if (errorObjectList.size() > 0) {
            StringBuilder errorLog = new StringBuilder("The following objects causes errors\n");
            for (String errorObject : errorObjectList) {
                errorLog.append(" " + errorObject + "\n");
            }
            LOGGER.info(errorLog.toString());
        }
    }

    /**
     * Imports an object by its import id. If no object with this
     * id was found an error occur. 
     * 
     * @param importId id to get the right xml file to import
     * @throws IOException
     * @throws JDOMException
     * @throws MCRActiveLinkException
     */
    protected void generateMyCoReXmlFileById(String importId) {
        try {
            // print status informations
            currentObject++;
            StringBuilder importStatus = new StringBuilder(String.valueOf(currentObject));
            importStatus.append("/").append(String.valueOf(objectCount));
            String statusBuffer = "(" + importStatus + ") ";
            LOGGER.info(statusBuffer.toString() + "Try to generate " + importId);

            // check if import id exists
            MCRImportFileStatus fs = idTable.get(importId);
            if (fs == null) {
                LOGGER.error("there is no object with the id '" + importId + "' defined!");
                return;
            }

            MCRImportFileType type = fs.getType();
            Document mcrDocument = null;
            StringBuffer loadCommand = null;
            // create the xml files
            if (type.equals(MCRImportFileType.MCROBJECT)) {
                mcrDocument = createMCRObjectXml(fs);
                loadCommand = new StringBuffer(LOAD_OBJECT_COMMAND);
            } else if (type.equals(MCRImportFileType.MCRDERIVATE)) {
                mcrDocument = createMCRDerivateXml(fs);
                loadCommand = new StringBuffer(LOAD_DERIVATE_COMMAND);
            } else {
                LOGGER.warn("Unknown type " + type.toString());
                return;
            }

            // save xml file to temp dir
            File mcrFile = saveDocumentToTemp(fs, mcrDocument);
            if (mcrFile == null) {
                LOGGER.warn("Cannot create import file. Cancel import id " + importId);
                return;
            }

            fs.setSavedInTempDirectory(true);
            String mcrId = fs.getMycoreId().toString();

            // add load command to the command list
            loadCommand.append(mcrFile.getAbsolutePath());
            if (type.equals(MCRImportFileType.MCRDERIVATE)) {
                loadCommand.append(" and upload files ");
                loadCommand.append(config.isImportFilesToMycore());
            }
            commandList.add(loadCommand.toString());

            // fire events
            if (type.equals(MCRImportFileType.MCROBJECT))
                fireMCRObjectGenerated(mcrId);
            else if (type.equals(MCRImportFileType.MCRDERIVATE))
                fireMCRDerivateGenerated(mcrId);

            // print successfully imported status infos
            LOGGER.info(statusBuffer.toString() + "Object successfully generated " + importId + " - " + mcrId);
        } catch (Exception e) {
            errorObjectList.add(importId);
            LOGGER.error("Error while generating object with import id '" + importId + "'!", e);
        }
    }

    /**
     * Imports a mycore object xml file to mycore by its path. All internal import
     * ids and classification mapping values (if enabled) are resolved.
     * 
     * @param fs the path of the xml file which should be imported
     * @throws IOException
     * @throws JDOMException
     * @throws MCRActiveLinkException
     */
    protected Document createMCRObjectXml(MCRImportFileStatus fs) throws IOException, JDOMException, MCRActiveLinkException, URISyntaxException {
        Document doc = builder.build(fs.getImportObjectPath());
        // resolve links
        resolveLinks(doc);
        // map classification values
        mapClassificationValues(doc);

        // use the xsi:noNamespaceSchemaLocation to get the type
        String schemaLocation = doc.getRootElement().getAttributeValue("noNamespaceSchemaLocation", MCRConstants.XSI_NAMESPACE);
        if (schemaLocation == null) {
            LOGGER.error("Couldnt get object type because there is no xsi:noNamespaceSchemaLocation defined for object " + doc.getBaseURI());
            return null;
        }
        // remove 'datamodel-' and '.xsd' to get a valid object type (e.g. author)
        String objectType = schemaLocation.substring(schemaLocation.indexOf("-") + 1, schemaLocation.lastIndexOf('.'));
        // create the next id
        String baseBuf = config.getProjectName() + "_" + objectType;
        MCRObjectID mcrObjId = getNextFreeId(baseBuf.toString());
        // set the new id in the xml document
        doc.getRootElement().setAttribute("ID", mcrObjId.toString());
        // set the new mycore id
        fs.setMycoreId(mcrObjId);
        return doc;
    }

    /**
     * This method imports a derivate in mycore from a xml file. All internal links
     * are resolved. If the derivate is successfully imported a status event is
     * fired.
     * 
     * @param fs the file to the derivate
     * @return the mcrId of the successfully imported derivate
     * @throws IOException
     * @throws JDOMException
     * @throws MCRActiveLinkException
     * @throws URISyntaxException
     */
    protected Document createMCRDerivateXml(MCRImportFileStatus fs) throws IOException, JDOMException, MCRActiveLinkException, URISyntaxException {
        Document doc = builder.build(fs.getImportObjectPath());
        // resolve links
        resolveLinks(doc);
        // create the next id
        String baseBuf = config.getProjectName() + "_derivate";
        MCRObjectID mcrDerivateId = getNextFreeId(baseBuf.toString());
        // set the new id in the xml document
        doc.getRootElement().setAttribute("ID", mcrDerivateId.toString());
        fs.setMycoreId(mcrDerivateId);
        return doc;
    }

    /**
     * Returns a new mycore object id depending on the base.
     * 
     * @param base the base string (e.g. DocPortal_author);
     * @return a new mcr object id
     */
    protected MCRObjectID getNextFreeId(String base) {
        MCRObjectID mcrObjId = MCRObjectID.getNextFreeId(base);
        return mcrObjId;
    }

    /**
     * Saves a xml document to the temp directory.
     * 
     * @param fs contains general object informations
     * @param documentToSave the xml document
     * @return the saved file
     * @throws FileNotFoundException
     */
    protected File saveDocumentToTemp(MCRImportFileStatus fs, Document documentToSave) throws FileNotFoundException {
        File saveToFile = getMCRXmlFile(fs.getMycoreId());
        if (saveToFile == null)
            return null;
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(saveToFile);
            outputter.output(documentToSave, output);
        } catch (IOException ioExc) {
            LOGGER.error("while saving document to temp directory " + saveToFile.getAbsolutePath());
            return null;
        } finally {
            try {
                output.close();
            } catch (IOException ioExc) {
                LOGGER.error("while saving document to temp directory " + saveToFile.getAbsolutePath(), ioExc);
                return null;
            }
        }
        return saveToFile;
    }

    public File getMCRXmlFile(MCRObjectID mcrId) {
        File subfolder = new File(this.tempDirectory, mcrId.getTypeId());
        if (!subfolder.exists()) {
            if (!subfolder.mkdirs()) {
                LOGGER.warn("Unable to create folder " + subfolder.getAbsolutePath());
                return null;
            }
        }
        String fileName = mcrId.toString() + ".xml";
        return new File(subfolder, fileName.toString());
    }

    /**
     * Parses the document to resolve all links. Each linked object
     * will be directly imported to receive the correct mycore id. This
     * id is then set at the href attribute.
     * 
     * @param doc the document where the links have to be resolved
     * @throws IOException
     * @throws JDOMException
     * @throws MCRActiveLinkException
     */
    protected void resolveLinks(Document doc) {
        Iterator<Element> it = doc.getRootElement().getDescendants(new LinkIdFilter());
        while (it.hasNext()) {
            Element linkElement = it.next();
            String linkId = linkElement.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE);
            // try to get the mycore id from the hashtable
            MCRImportFileStatus fs = idTable.get(linkId);
            if (fs == null) {
                // print error only if its not a internal mycore id
                if (!MCRMetadataManager.exists(MCRObjectID.getInstance(linkId)))
                    LOGGER.error("Invalid id " + linkId + " found in file " + doc.getBaseURI() + " at element " + linkElement.getName()
                        + linkElement.getAttributes());
                continue;
            }
            // if null -> the linked object is currently not imported -> do it
            if (fs.getMycoreId() == null)
                generateMyCoReXmlFileById(linkId);

            // set the new mycoreId
            if (fs.getMycoreId() != null) {
                linkElement.setAttribute("href", fs.getMycoreId().toString(), MCRConstants.XLINK_NAMESPACE);
            } else {
                LOGGER.error("Couldnt resolve reference for link " + linkId + " in " + doc.getBaseURI());
            }
        }
    }

    /**
     * Parses the document to map all classification values in
     * the document with the classification mapping files.
     * 
     * @param doc the document where the classifications have to be mapped
     */
    protected void mapClassificationValues(Document doc) throws IOException, JDOMException, MCRActiveLinkException {
        Iterator<Element> it = doc.getRootElement().getDescendants(new ClassificationFilter());
        while (it.hasNext()) {
            Element classElement = it.next();
            // classid & categid
            String classId = classElement.getAttributeValue("classid");
            String categId = classElement.getAttributeValue("categid");

            if (classId == null || categId == null || classId.equals("") || categId.equals(""))
                continue;

            // get the mycore value from the classifcation mapping file
            String mcrValue = classManager.getMyCoReValue(classId, categId);

            if (mcrValue == null || mcrValue.equals("") || mcrValue.equals(categId))
                continue;

            // set the new mycore value
            classElement.setAttribute("categid", mcrValue);
        }
    }

    /**
     * Use this method to register a listener and get informed
     * about the import progress.
     * 
     * @param l the listener to add
     */
    public void addStatusListener(MCRImportStatusListener l) {
        listenerList.add(l);
    }

    /**
     * Remove a registerd listener.
     * 
     * @param l the listener to remove
     */
    public void removeStatusListener(MCRImportStatusListener l) {
        listenerList.remove(l);
    }

    /**
     * Sends all registerd listeners that a mycore object is
     * successfully generated in temp directory.
     * 
     * @param mcrId the record which is mapped
     */
    private void fireMCRObjectGenerated(String mcrId) {
        for (MCRImportStatusListener l : listenerList) {
            MCRImportStatusEvent e = new MCRImportStatusEvent(this, mcrId);
            l.objectGenerated(e);
        }
    }

    /**
     * Sends all registerd listeners that a mycore object is
     * successfully generated in temp directory.
     * 
     * @param derId the record which is mapped
     */
    private void fireMCRDerivateGenerated(String derId) {
        for (MCRImportStatusListener l : listenerList) {
            MCRImportStatusEvent e = new MCRImportStatusEvent(this, derId);
            l.derivateGenerated(e);
        }
    }

    /**
     * Internal filter class which returns only true
     * if the element is a xlink. 
     */
    private static class LinkIdFilter extends ElementFilter {
        private static final long serialVersionUID = 1L;

        public Element filter(Object arg0) {
            Element e = super.filter(arg0);
            if (e != null) {
                Element p = e.getParentElement();
                // check the class attribute of the parent element
                if (p != null && "MCRMetaLinkID".equals(p.getAttributeValue("class"))) {
                    // exists a href attribute and if its not empty
                    String href = e.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE);
                    if (!(href == null || href.equals(""))) {
                        return e;
                    }
                }
            }
            return null;
        }
    }

    /**
     * Internal filter calls which returns only true
     * if the element is a classification.
     */
    private static class ClassificationFilter extends ElementFilter {
        private static final long serialVersionUID = 1L;

        public Element filter(Object arg0) {
            Element e = super.filter(arg0);
            if (e != null) {
                Element p = e.getParentElement();
                // check the class attribute of the parent element
                return (p != null && "MCRMetaClassification".equals(p.getAttributeValue("class"))) ? e : null;
            }
            return null;
        }
    }
}
