package org.mycore.mets.tools;

import org.apache.log4j.Logger;
import org.jdom2.*;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mets.model.Mets;
import org.mycore.mets.model.files.FLocat;
import org.mycore.mets.model.files.FileGrp;
import org.mycore.mets.model.struct.*;
import org.xml.sax.SAXException;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.*;

/**
 * Class is responsible for saving a mets document to a derivate. It also can
 * handle addition and removing files from a derivate.
 * 
 * @author shermann
 *          Sebastian Hofmann
 */
public class MCRMetsSave {

    private final static Logger LOGGER = Logger.getLogger(MCRMetsSave.class);
    public static final String ALTO_FILE_GROUP_USE = "ALTO";
    public static final String DEFAULT_FILE_GROUP_USE = "MASTER";
    public static final String ALTO_FOLDER_PREFIX = "alto/";

    /**
     * Saves the content of the given document to file and then adds the file to
     * the derivate with the given id. The name of the file depends on property
     * 'MCR.Mets.Filename'. If this property has not been set 'mets.xml' is used
     * as a default filename.
     * 
     * @param document
     * @param derivateId
     */
    public static synchronized void saveMets(Document document, MCRObjectID derivateId) {
        saveMets(document, derivateId, true);
    }

    /**
     * Saves the content of the given document to file, if no mets present and then adds the file to
     * the derivate with the given id. The name of the file depends on property
     * 'MCR.Mets.Filename'. If this property has not been set 'mets.xml' is used
     * as a default filename.
     * 
     * @param document
     * @param derivateId
     * @param overwrite 
     *          if true existing mets-file will be overwritten
     */
    public static synchronized void saveMets(Document document, MCRObjectID derivateId, boolean overwrite) {
        // add the file to the existing derivate in ifs
        MCRFile metsFile = getMetsFile(derivateId.toString());
        
        if(metsFile == null){
            metsFile = createMetsFile(derivateId.toString());
        }else if(!overwrite){
            return;
        }
        
        try {
            metsFile.setContentFrom(document);
            LOGGER.info("Storing file content from \"" + getMetsFileName() + "\" to derivate \"" + derivateId + "\"");
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    public static String getMetsFileName() {
        return MCRConfiguration.instance().getString("MCR.Mets.Filename", "mets.xml");
    }

    /**
     * Updates the mets.xml belonging to the given derivate. Adds the file to
     * the mets document (updates file sections and stuff within the mets.xml)
     *
     * @param file
     *            a handle for the file to add to the mets.xml
     * @throws Exception
     */
    public static void updateMetsOnFileAdd(MCRFile file) throws Exception {
        MCRObjectID derivateID = MCRObjectID.getInstance(file.getOwnerID());
        Document mets = getCurrentMets(derivateID.toString());
        if (mets == null) {
            LOGGER.info("Derivate with id \"" + derivateID + "\" has no mets file. Nothing to do");
            return;
        }
        mets = MCRMetsSave.updateOnFileAdd(mets, file);
        if (mets != null)
            MCRMetsSave.saveMets(mets, derivateID);

    }

    /**
     * @param derivateID
     * @return
     * @throws JDOMException
     * @throws IOException
     * @throws SAXException 
     */
    private static Document getCurrentMets(String derivateID) throws JDOMException, IOException, SAXException {
        MCRFile metsFile = getMetsFile(derivateID);
        return metsFile == null ? null : metsFile.getContent().asXML();
    }

    public static MCRFile getMetsFile(String derivateID) {
        MCRDirectory rootDir = MCRDirectory.getRootDirectory(derivateID);
        if (rootDir == null) {
            return null;
        }
        
        MCRFilesystemNode metsFile = rootDir.getChild(getMetsFileName());
        if(!(metsFile instanceof MCRFile)){
            return null;
        }
        
        return (MCRFile) metsFile;
    }
    
    public static MCRFile createMetsFile(String derivateID) {
        MCRDirectory rootDir = MCRDirectory.getRootDirectory(derivateID);
        if (rootDir == null) {
            return null;
        }
        
        return new MCRFile(getMetsFileName(), rootDir);
    }

    // TODO: should use mets-model api
    /**
     * Alters the mets file
     * 
     * @param mets
     *            the unmodified source
     * @param file
     *            the file to add
     * @return the modified mets or null if an exception occures
     */
    private static Document updateOnFileAdd(Document mets, MCRFile file) {
        try {
            UUID uuid = UUID.randomUUID();
            String fileId = org.mycore.mets.model.files.File.PREFIX_MASTER + uuid;

            // check for file existance (if a derivate with mets.xml is uploaded
            String path = file.getAbsolutePath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            // Check if file already exists -> if yes do nothing
            String fileExistPathString = "mets:mets/mets:fileSec/mets:fileGrp/mets:file/mets:FLocat[@xlink:href='" + path + "']";
            XPathExpression<Element> xpath = XPathFactory.instance().compile(fileExistPathString, Filters.element(),
                    null, MCRConstants.METS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);

            if (xpath.evaluate(mets).size() > 0) {
                String msgTemplate = "The File : '%s' already exists in mets.xml";
                LOGGER.warn(String.format(Locale.ROOT, msgTemplate, file.getAbsolutePath()));
                return null;
            } else {
                String msgTemplate = "The File : '%s' does not exists in mets.xml";
                LOGGER.warn(String.format(Locale.ROOT, msgTemplate, file.getAbsolutePath()));
            }

            // add to file section
            String contentType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(new File(file.getName()));
            LOGGER.warn(MessageFormat.format("Content Type is : {0}", contentType));

            org.mycore.mets.model.files.File fileAsMetsFile = new org.mycore.mets.model.files.File(fileId, contentType);
            String filePath = file.getAbsolutePath();
            if(!filePath.equals("/")) {
                filePath = filePath.substring(1);
            }

            FLocat fLocat = new FLocat(LOCTYPE.URL, filePath);
            fileAsMetsFile.setFLocat(fLocat);


            String fileGrpUSE = getFileGroupUse(file);
            Element fileSec = getFileGroup(mets, fileGrpUSE);
            fileSec.addContent(fileAsMetsFile.asElement());

            if (fileGrpUSE.equals(ALTO_FILE_GROUP_USE)) {
                updateOnAltoFile(mets, fileId, path);
            } else {
                updateOnImageFile(mets, fileId, path);
            }

        } catch (Exception ex) {
            LOGGER.error("Error occured while adding file " + file.getAbsolutePath() + " to the existing mets file", ex);
            return null;
        }

        return mets;
    }

    private static void updateOnImageFile(Document mets, String fileId, String path) throws DataConversionException {
        LOGGER.debug("FILE is a image!");
        //check if alto file is present
        String matchId = searchFileInGroup(mets, path, ALTO_FILE_GROUP_USE);
        if (matchId != null) {
            Element existingPhysicalFile = getPhysicalFile(mets, matchId);
            if (existingPhysicalFile != null) {
                existingPhysicalFile.addContent(new Fptr(fileId).asElement());
                return;
            }
        }

        // add to structMap physical
        int newOrder = getNewOrder(mets);
        PhysicalSubDiv div = new PhysicalSubDiv(PhysicalSubDiv.ID_PREFIX + fileId, PhysicalSubDiv.TYPE_PAGE,
                newOrder);

        if (matchId != null) {
            div.add(new Fptr(matchId));
        }

        // actually alter the mets document
        Element structMapPhys = getPhysicalStructmap(mets);
        structMapPhys.addContent(div.asElement());

        // add to structLink
        SmLink smLink = getDefaultSmLink(mets, div);

        Element structLink = getStructLink(mets);
        structLink.addContent(smLink.asElement());
    }

    /**
     * gets a new order for a page in a mets file
     * @param mets
     * @return
     * @throws DataConversionException
     */
    private static int getNewOrder(Document mets) throws DataConversionException {
        XPathExpression<Attribute> attributeXpath = XPathFactory.instance().compile(
                "mets:mets/mets:structMap[@TYPE='PHYSICAL']/mets:div[@TYPE='physSequence']/mets:div[last()]/@ORDER",
                Filters.attribute(), null, MCRConstants.METS_NAMESPACE);
        Attribute orderAttribute = attributeXpath.evaluateFirst(mets);
        return orderAttribute.getIntValue() + 1;
    }

    private static void updateOnAltoFile(Document mets, String fileId, String path) {
        LOGGER.debug("FILE is alto!");

        String matchId = searchFileInGroup(mets, path, DEFAULT_FILE_GROUP_USE);

        if (matchId == null) {
            // there is no file wich belongs to the alto xml so just return
            LOGGER.warn("no file found wich belongs to the alto xml : " + path);
            return;
        }
        // check if there is a physical file
        Element physPageElement = getPhysicalFile(mets, matchId);
        if (physPageElement != null) {
            physPageElement.addContent(new Fptr(fileId).asElement());
            LOGGER.warn("physical page found for file " + matchId);
        } else {
            LOGGER.warn("no physical page found for file " + matchId);
        }
    }

    private static Element getPhysicalFile(Document mets, String matchId) {
        XPathExpression<Element> xpath;
        String physicalFileExistsXpathString = String.format(Locale.ROOT, "mets:mets/mets:structMap[@TYPE='PHYSICAL']/mets:div[@TYPE='physSequence']/mets:div[mets:fptr/@FILEID='%s']", matchId);
        xpath = XPathFactory.instance().compile(physicalFileExistsXpathString, Filters.element(),
                null, MCRConstants.METS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);

        return xpath.evaluateFirst(mets);
    }

    private static Element getFileGroup(Document mets, String fileGrpUSE) {
        XPathExpression<Element> xpath;// alter the mets document
        String fileGroupXPathString = String.format(Locale.ROOT, "mets:mets/mets:fileSec/mets:fileGrp[@USE='%s']", fileGrpUSE);
        xpath = XPathFactory.instance().compile(fileGroupXPathString, Filters.element(),
                null, MCRConstants.METS_NAMESPACE);
        return xpath.evaluateFirst(mets);
    }

    /**
     * Build the default smLink. The PhysicalSubDiv is simply linked to the root chapter of the mets document.
     *
     * @param mets the mets document
     * @param div  the PhysicalSubDiv which should be linked
     * @return the default smLink
     */
    private static SmLink getDefaultSmLink(Document mets, PhysicalSubDiv div) {
        XPathExpression<Attribute> attributeXpath;
        attributeXpath = XPathFactory.instance().compile("mets:mets/mets:structMap[@TYPE='LOGICAL']/mets:div/@ID", Filters.attribute(),
                null, MCRConstants.METS_NAMESPACE);
        Attribute idAttribute = attributeXpath.evaluateFirst(mets);
        String rootID = idAttribute.getValue();
        return new SmLink(rootID, div.getId());
    }

    /**
     * Gets the StructLink of a mets document
     *
     * @param mets the mets document
     * @return the StructLink of a mets document
     */
    private static Element getStructLink(Document mets) {
        XPathExpression<Element> xpath;
        xpath = XPathFactory.instance().compile("mets:mets/mets:structLink", Filters.element(), null, MCRConstants.METS_NAMESPACE);
        return xpath.evaluateFirst(mets);
    }

    /**
     * Gets the physicalStructMap of a mets document
     *
     * @param mets the mets document
     * @return the physicalStructmap of the mets document
     */
    private static Element getPhysicalStructmap(Document mets) {
        XPathExpression<Element> xpath;
        xpath = XPathFactory.instance().compile("mets:mets/mets:structMap[@TYPE='PHYSICAL']/mets:div[@TYPE='physSequence']",
                Filters.element(), null, MCRConstants.METS_NAMESPACE);
        return xpath.evaluateFirst(mets);
    }

    /**
     * Decides in which file group the file should be inserted
     *
     * @param file
     * @return the id of the filegGroup
     */
    private static String getFileGroupUse(MCRFile file) {
        String filePath = file.getAbsolutePath();
        // check wich fileGroup is the right
        String fileGrpUSE;
        if (filePath.startsWith("/alto/") && filePath.endsWith(".xml")) {
            fileGrpUSE = ALTO_FILE_GROUP_USE;
        } else {
            fileGrpUSE = DEFAULT_FILE_GROUP_USE;
        }
        return fileGrpUSE;
    }

    /**
     * Searches a file in a group, which matches a filename.
     *
     * @param mets            the mets file to search
     * @param path            the path to the alto file (e.g. "alto/alto_file.xml" when searching in DEFAULT_FILE_GROUP_USE or "image_file.jpg" when searchin in ALTO_FILE_GROUP_USE)
     * @param searchFileGroup
     * @return the id of the matching file or null if there is no matching file
     */
    private static String searchFileInGroup(Document mets, String path, String searchFileGroup) {
        XPathExpression<Element> xpath;// first check all files in default file group
        String relatedFileExistPathString = String.format(Locale.ROOT, "mets:mets/mets:fileSec/mets:fileGrp[@USE='%s']/mets:file/mets:FLocat", searchFileGroup);
        xpath = XPathFactory.instance().compile(relatedFileExistPathString, Filters.element(),
                null, MCRConstants.METS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);
        List<Element> fileLocList = xpath.evaluate(mets);
        String matchId = null;


        // iterate over all files
        path = getCleanPath(path);

        for (Element fileLoc : fileLocList) {
            Attribute hrefAttribute = fileLoc.getAttribute("href", MCRConstants.XLINK_NAMESPACE);
            String hrefAttributeValue = hrefAttribute.getValue();
            String hrefPath = getCleanPath(removeExtension(hrefAttributeValue));

            if (hrefPath.equals(removeExtension(path))) {
                matchId = ((Element) fileLoc.getParent()).getAttributeValue("ID");
                break;
            }
        }
        return matchId;
    }

    private static String getCleanPath(String path) {
        if (path.startsWith(ALTO_FOLDER_PREFIX)) {
            path = path.substring(ALTO_FOLDER_PREFIX.length());
        }
        return path;
    }

    private static String removeExtension(String fileName) {
        int dotPosition = fileName.lastIndexOf(".");
        return fileName.substring(0, dotPosition);
    }

    /**
     * Updates the mets.xml belonging to the given derivate. Removes the file
     * from the mets document (updates file sections and stuff within the
     * mets.xml)
     *
     * @param file
     *            a handle for the file to add to the mets.xml
     * @throws Exception
     */
    public static void updateMetsOnFileDelete(MCRFile file) throws Exception {
        MCRObjectID derivateID = MCRObjectID.getInstance(file.getOwnerID());
        Document mets = getCurrentMets(derivateID.toString());
        if (mets == null) {
            LOGGER.info("Derivate with id \"" + derivateID + "\" has no mets file. Nothing to do");
            return;
        }
        mets = MCRMetsSave.updateOnFileDelete(mets, file);
        if (mets != null)
            MCRMetsSave.saveMets(mets, derivateID);
    }

    /**
     * Inserts the given URNs into the Mets document.
     * @param derivateID The {@link MCRObjectID} of the Derivate wich contains the METs file
     * @param fileUrnMap a {@link Map} wich contains the file as key and the urn as  as value
     * @throws Exception
     */
    public static void updateMetsOnUrnGenerate(MCRObjectID derivateID, Map<String, String> fileUrnMap) throws Exception {
        Document mets = getCurrentMets(derivateID.toString());
        if (mets == null) {
            LOGGER.info(MessageFormat.format("Derivate with id \"{0}\" has no mets file. Nothing to do", derivateID));
            return;
        }
        LOGGER.info(MessageFormat.format("Update {0} URNS in Mets.xml", fileUrnMap.size()));

        Mets metsObject = new Mets(mets);
        updateURNsInMetsDocument(metsObject, fileUrnMap);
        saveMets(metsObject.asDocument(), derivateID);
    }

    /**
     * Inserts the given URNs into the {@link Mets} Object.
     * @param mets the {@link Mets} object were the URNs should be inserted.
     * @param fileUrnMap a {@link Map} wich contains the file as key and the urn as  as value
     * @throws Exception
     */
    public static void updateURNsInMetsDocument(Mets mets, Map<String, String> fileUrnMap) throws Exception {
        // put all files of the mets in a list
        List<FileGrp> fileGroups = mets.getFileSec().getFileGroups();
        List<org.mycore.mets.model.files.File> files = new ArrayList<org.mycore.mets.model.files.File>();
        for (FileGrp fileGrp : fileGroups) {
            files.addAll(fileGrp.getFileList());
        }

        // combine the filename and the id in a map
        Map<String, String> idFileMap = new HashMap<String, String>();
        for (org.mycore.mets.model.files.File file : files) {
            idFileMap.put(file.getId(), file.getFLocat().getHref());
        }

        List<PhysicalSubDiv> childs = ((PhysicalStructMap) mets.getStructMap(PhysicalStructMap.TYPE)).getDivContainer().getChildren();
        for (PhysicalSubDiv divChild : childs) {
            String idMets = divChild.getChildren().get(0).getFileId();

            // check if there is a URN for the file
            String file = "/" + URLDecoder.decode(idFileMap.get(idMets), "UTF-8");
            if (fileUrnMap.containsKey(file)) {
                divChild.setContentids(fileUrnMap.get(file));
            }
        }
    }

    /**
     * @param mets
     * @param file
     * @return
     */
    private static Document updateOnFileDelete(Document mets, MCRFile file) {
        Mets modifiedMets = null;
        try {
            modifiedMets = new Mets(mets);
            String href = file.getAbsolutePath().substring(1);

            PhysicalStructMap physStructMap = (PhysicalStructMap) modifiedMets.getStructMap(PhysicalStructMap.TYPE);
            PhysicalDiv divContainer = physStructMap.getDivContainer();

            // search the right group and remove the file from the group
            List<FileGrp> fileGroups = modifiedMets.getFileSec().getFileGroups();
            for (FileGrp fileGrp : fileGroups) {
                if (fileGrp.contains(href)) {
                    org.mycore.mets.model.files.File fileToRemove = fileGrp.getFileByHref(href);
                    fileGrp.removeFile(fileToRemove);

                    ArrayList<PhysicalSubDiv> physicalSubDivsToRemove = new ArrayList<>();
                    // remove file from mets:mets/mets:structMap[@TYPE='PHYSICAL']
                    for (PhysicalSubDiv physicalSubDiv : divContainer.getChildren()) {
                        ArrayList<Fptr> fptrsToRemove = new ArrayList<Fptr>();
                        for (Fptr fptr : physicalSubDiv.getChildren()) {
                            if (fptr.getFileId().equals(fileToRemove.getId())) {
                                fptrsToRemove.add(fptr);
                            }
                        }
                        for (Fptr fptrToRemove : fptrsToRemove) {
                            LOGGER.warn(String.format(Locale.ROOT, "remove fptr \"%s\" from mets.xml of \"%s\"", fptrToRemove.getFileId(), file.getOwnerID()));
                            physicalSubDiv.remove(fptrToRemove);
                        }

                        // Remove smlinks only if there is no file linked
                        if (physicalSubDiv.getChildren().size() == 0) {

                            //remove links in mets:structLink section
                            List<SmLink> list = modifiedMets.getStructLink().getSmLinkByTo(physicalSubDiv.getId());
                            LogicalStructMap logicalStructMap = (LogicalStructMap) modifiedMets.getStructMap(LogicalStructMap.TYPE);

                            for (SmLink linkToRemove : list) {
                                LOGGER.warn(String.format(Locale.ROOT, "remove smLink from \"%s\" to \"%s\"", linkToRemove.getFrom(), linkToRemove.getTo()));
                                modifiedMets.getStructLink().removeSmLink(linkToRemove);
                                // modify logical struct Map
                                String logID = linkToRemove.getFrom();

                                // the deleted file was not directly assigned to a structure
                                if (logicalStructMap.getDivContainer().getId().equals(logID)) {
                                    continue;
                                }

                                AbstractLogicalDiv logicalDiv = logicalStructMap.getDivContainer().getLogicalSubDiv(logID);
                                if (!(logicalDiv instanceof LogicalSubDiv)) {
                                    LOGGER.error("Could not find " + LogicalSubDiv.class.getSimpleName() + " with id " + logID);
                                    LOGGER.error("Mets document remains unchanged");
                                    return mets;
                                }

                                LogicalSubDiv logicalSubDiv = (LogicalSubDiv) logicalDiv;

                                // there are still files for this logical sub div, nothing to do
                                if (modifiedMets.getStructLink().getSmLinkByFrom(logicalSubDiv.getId()).size() > 0) {
                                    continue;
                                }

                                // the logical div has other divs included, nothing to do
                                if (logicalSubDiv.getChildren().size() > 0) {
                                    continue;
                                }

                                /*
                                 * the log div might be in a hierarchy of divs, which may now be empty
                                 * (only containing empty directories), if so the parent of the log div
                                 * must be deleted
                                 * */
                                handleParents(logicalSubDiv, modifiedMets);

                                logicalStructMap.getDivContainer().remove(logicalSubDiv);
                            }

                            physicalSubDivsToRemove.add(physicalSubDiv);
                        }
                    }
                    for (PhysicalSubDiv physicalSubDivToRemove : physicalSubDivsToRemove) {
                        divContainer.remove(physicalSubDivToRemove);
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Error occured while removing file " + file.getAbsolutePath() + " from the existing mets file", ex);
            return null;
        }

        return modifiedMets.asDocument();
    }

    /**
     * @param mets
     * @param logDiv
     * @throws Exception
     */
    private static void handleParents(LogicalSubDiv logDiv, Mets mets) throws Exception {
        AbstractLogicalDiv parent = logDiv.getParent();

        // there are files for the parent of the log div, thus nothing to do
        if (mets.getStructLink().getSmLinkByFrom(parent.getId()).size() > 0) {
            return;
        }

        //no files associated to the parent of the log div
        LogicalDiv logicalDiv = ((LogicalStructMap) mets.getStructMap(LogicalStructMap.TYPE)).getDivContainer();
        if (parent.getParent() == logicalDiv) {
            //the parent the log div container itself, thus we quit here and remove the log div
            logicalDiv.remove((LogicalSubDiv) parent);

            return;
        } else {
            handleParents((LogicalSubDiv) parent, mets);
        }
    }

    /**
     * @param mets
     * @param derivateId
     * 
     * @return true if all files owned by the derivate appearing in the master file group or false otherwise 
     */
    public static boolean isComplete(Mets mets, MCRObjectID derivateId) {
        try {
            FileGrp fileGroup = mets.getFileSec().getFileGroup(FileGrp.USE_MASTER);
            MCRDirectory ifs = MCRDirectory.getRootDirectory(derivateId.toString());
            return isComplete(fileGroup, ifs);
        } catch (Exception ex) {
            LOGGER.error("Error while validating mets", ex);
            return false;
        }
    }

    /**
     * 
     * @param fileGroup
     * @param ifs
     * @return true if all files in the {@link MCRDirectory} appears in the fileGroup
     */
    public static boolean isComplete(FileGrp fileGroup, MCRDirectory ifs) {
        try {
            for (MCRFilesystemNode node : ifs.getChildren()) {
                if (node.getName().equals(MCRJSONProvider.DEFAULT_METS_FILENAME)) {
                    continue;
                }
                if (node instanceof MCRDirectory && !isComplete(fileGroup, (MCRDirectory) node)) {
                    return false;
                } else if (node instanceof MCRDirectory) {
                    continue;
                }

                String path = MCRXMLFunctions.encodeURIPath(node.getAbsolutePath()).substring(1);//remove leading '/'
                if (!fileGroup.contains(path)) {
                    LOGGER.warn(MessageFormat.format("{0} does not appear in {1}!", path, ifs.getOwnerID()));
                    return false;
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Error while validating mets", ex);
            return false;
        }

        return true;
    }

}
