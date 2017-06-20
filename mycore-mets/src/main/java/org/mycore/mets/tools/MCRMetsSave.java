package org.mycore.mets.tools;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRPathContent;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.ifs2.MCRDirectory;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRContentTypes;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.mets.model.Mets;
import org.mycore.mets.model.files.FLocat;
import org.mycore.mets.model.files.FileGrp;
import org.mycore.mets.model.struct.Fptr;
import org.mycore.mets.model.struct.LOCTYPE;
import org.mycore.mets.model.struct.LogicalDiv;
import org.mycore.mets.model.struct.LogicalStructMap;
import org.mycore.mets.model.struct.PhysicalDiv;
import org.mycore.mets.model.struct.PhysicalStructMap;
import org.mycore.mets.model.struct.PhysicalSubDiv;
import org.mycore.mets.model.struct.SmLink;
import org.xml.sax.SAXException;

/**
 * Class is responsible for saving a mets document to a derivate. It also can
 * handle addition and removing files from a derivate.
 * 
 * @author shermann
 *          Sebastian Hofmann
 *
 * TODO: Complete rework needed
 */
public class MCRMetsSave {

    private final static Logger LOGGER = LogManager.getLogger(MCRMetsSave.class);

    public static final String ALTO_FILE_GROUP_USE = "ALTO";

    public static final String DEFAULT_FILE_GROUP_USE = "MASTER";

    public static final String TRANSCRIPTION_FILE_GROUP_USE = "TRANSCRIPTION";

    public static final String TRANSLATION_FILE_GROUP_USE = "TRANSLATION";

    public static final String ALTO_FOLDER_PREFIX = "alto/";

    public static final String TEI_FOLDER_PREFIX = "tei/";

    public static final String TRANSLATION_FOLDER_PREFIX = "translation.";

    public static final String TRANSCRIPTION_FOLDER_PREFIX = "transcription";

    /**
     * Saves the content of the given document to file and then adds the file to
     * the derivate with the given id. The name of the file depends on property
     * 'MCR.Mets.Filename'. If this property has not been set 'mets.xml' is used
     * as a default filename.
     * 
     * @return
     *          true if the given document was successfully saved, otherwise false
     */
    public static synchronized boolean saveMets(Document document, MCRObjectID derivateId) {
        return saveMets(document, derivateId, true, true);
    }

    /**
     * Saves the content of the given document to file, if no mets present and then adds the file to
     * the derivate with the given id. The name of the file depends on property
     * 'MCR.Mets.Filename'. If this property has not been set 'mets.xml' is used
     * as a default filename.
     * 
     * @param overwrite 
     *          if true existing mets-file will be overwritten
     * @param validate
     *          if true the document will be validated before its stored
     * @return
     *          true if the given document was successfully saved, otherwise false
     */
    public static synchronized boolean saveMets(Document document, MCRObjectID derivateId, boolean overwrite,
        boolean validate) {
        // add the file to the existing derivate in ifs
        MCRPath metsFile = getMetsFile(derivateId.toString());

        if (metsFile == null) {
            metsFile = createMetsFile(derivateId.toString());
        } else if (!overwrite) {
            return false;
        }

        if (validate && !Mets.isValid(document)) {
            LOGGER.warn("Storing mets.xml for " + derivateId + " failed cause the given document was invalid.");
            return false;
        }

        try (OutputStream metsOut = Files.newOutputStream(metsFile)) {
            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            xout.output(document, metsOut);
            LOGGER.info("Storing file content from \"" + getMetsFileName() + "\" to derivate \"" + derivateId + "\"");
        } catch (Exception e) {
            LOGGER.error(e);
            return false;
        }
        return true;
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
     */
    public static void updateMetsOnFileAdd(MCRPath file) throws Exception {
        MCRObjectID derivateID = MCRObjectID.getInstance(file.getOwner());
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
        MCRPath metsFile = getMetsFile(derivateID);
        return metsFile == null ? null : new MCRPathContent(metsFile).asXML();
    }

    public static MCRPath getMetsFile(String derivateID) {
        MCRPath metsFile = createMetsFile(derivateID);
        return Files.exists(metsFile) ? metsFile : null;
    }

    public static MCRPath createMetsFile(String derivateID) {
        return MCRPath.getPath(derivateID, "/mets.xml");
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
    private static Document updateOnFileAdd(Document mets, MCRPath file) {
        try {
            UUID uuid = UUID.randomUUID();

            // check for file existance (if a derivate with mets.xml is uploaded
            String relPath = MCRXMLFunctions.encodeURIPath(file.getOwnerRelativePath().substring(1));

            // Check if file already exists -> if yes do nothing
            String fileExistPathString = "mets:mets/mets:fileSec/mets:fileGrp/mets:file/mets:FLocat[@xlink:href='"
                + relPath + "']";
            XPathExpression<Element> xpath = XPathFactory.instance().compile(fileExistPathString, Filters.element(),
                null, MCRConstants.METS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);

            if (xpath.evaluate(mets).size() > 0) {
                String msgTemplate = "The File : '%s' already exists in mets.xml";
                LOGGER.warn(String.format(Locale.ROOT, msgTemplate, relPath));
                return null;
            } else {
                String msgTemplate = "The File : '%s' does not exists in mets.xml";
                LOGGER.warn(String.format(Locale.ROOT, msgTemplate, relPath));
            }

            // add to file section
            String contentType = MCRContentTypes.probeContentType(file);
            LOGGER.warn(MessageFormat.format("Content Type is : {0}", contentType));
            String fileGrpUSE = getFileGroupUse(file);

            String fileId = MessageFormat.format("{0}_{1}", fileGrpUSE.toLowerCase(Locale.ROOT), uuid);
            org.mycore.mets.model.files.File fileAsMetsFile = new org.mycore.mets.model.files.File(fileId, contentType);

            FLocat fLocat = new FLocat(LOCTYPE.URL, relPath);
            fileAsMetsFile.setFLocat(fLocat);

            Element fileSec = getFileGroup(mets, fileGrpUSE);
            fileSec.addContent(fileAsMetsFile.asElement());

            if (fileGrpUSE.equals(DEFAULT_FILE_GROUP_USE)) {
                updateOnImageFile(mets, fileId, relPath);
            } else {
                updateOnCustomFile(mets, fileId, relPath);
            }
        } catch (Exception ex) {
            LOGGER.error("Error occured while adding file " + file + " to the existing mets file", ex);
            return null;
        }

        return mets;
    }

    private static void updateOnImageFile(Document mets, String fileId, String path) {
        LOGGER.debug("FILE is a image!");
        //check if custom files are present and save the ids
        String[] customFileGroups = { TRANSCRIPTION_FILE_GROUP_USE, ALTO_FILE_GROUP_USE, TRANSLATION_FILE_GROUP_USE };

        // add to structMap physical
        PhysicalSubDiv div = new PhysicalSubDiv(PhysicalSubDiv.ID_PREFIX + fileId, PhysicalSubDiv.TYPE_PAGE);
        div.add(new Fptr(fileId));

        Arrays.stream(customFileGroups)
            .map(customFileGroup -> searchFileInGroup(mets, path, customFileGroup))
            .filter(Objects::nonNull)
            .map(Fptr::new)
            .forEach(div::add);

        // actually alter the mets document
        Element structMapPhys = getPhysicalStructmap(mets);
        structMapPhys.addContent(div.asElement());

        // add to structLink
        SmLink smLink = getDefaultSmLink(mets, div);

        Element structLink = getStructLink(mets);
        structLink.addContent(smLink.asElement());
    }

    private static void updateOnCustomFile(Document mets, String fileId, String path) {
        LOGGER.debug("FILE is a custom file (ALTO/TEI)!");

        String matchId = searchFileInGroup(mets, path, DEFAULT_FILE_GROUP_USE);

        if (matchId == null) {
            // there is no file wich belongs to the alto xml so just return
            LOGGER.warn("no file found wich belongs to the custom xml : " + path);
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
        String physicalFileExistsXpathString = String
            .format(
                Locale.ROOT,
                "mets:mets/mets:structMap[@TYPE='PHYSICAL']/mets:div[@TYPE='physSequence']/mets:div[mets:fptr/@FILEID='%s']",
                matchId);
        xpath = XPathFactory.instance().compile(physicalFileExistsXpathString, Filters.element(), null,
            MCRConstants.METS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);

        return xpath.evaluateFirst(mets);
    }

    private static Element getFileGroup(Document mets, String fileGrpUSE) {
        XPathExpression<Element> xpath;// alter the mets document
        String fileGroupXPathString = String.format(Locale.ROOT, "mets:mets/mets:fileSec/mets:fileGrp[@USE='%s']",
            fileGrpUSE);
        xpath = XPathFactory.instance().compile(fileGroupXPathString, Filters.element(), null,
            MCRConstants.METS_NAMESPACE);
        Element element = xpath.evaluateFirst(mets);

        if (element == null) {
            // section does not exist
            Element fileGroupElement = new FileGrp(fileGrpUSE).asElement();
            String fileSectionPath = "mets:mets/mets:fileSec";
            xpath = XPathFactory.instance().compile(fileSectionPath, Filters.element(), null,
                MCRConstants.METS_NAMESPACE);
            Element fileSectionElement = xpath.evaluateFirst(mets);
            if (fileSectionElement == null) {
                throw new MCRPersistenceException("There is no fileSection in mets.xml!");
            }
            fileSectionElement.addContent(fileGroupElement);
            element = fileGroupElement;
        }

        return element;
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
        attributeXpath = XPathFactory.instance().compile("mets:mets/mets:structMap[@TYPE='LOGICAL']/mets:div/@ID",
            Filters.attribute(), null, MCRConstants.METS_NAMESPACE);
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
        xpath = XPathFactory.instance().compile("mets:mets/mets:structLink", Filters.element(), null,
            MCRConstants.METS_NAMESPACE);
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
        xpath = XPathFactory.instance().compile(
            "mets:mets/mets:structMap[@TYPE='PHYSICAL']/mets:div[@TYPE='physSequence']", Filters.element(), null,
            MCRConstants.METS_NAMESPACE);
        return xpath.evaluateFirst(mets);
    }

    /**
     * Decides in which file group the file should be inserted
     *
     * @param file
     * @return the id of the filegGroup
     */
    private static String getFileGroupUse(MCRPath file) {
        String filePath = file.getOwnerRelativePath();
        String teiFolder = "/" + TEI_FOLDER_PREFIX;
        String altoFolder = "/" + ALTO_FOLDER_PREFIX;

        if (filePath.startsWith(altoFolder) && filePath.endsWith(".xml")) {
            return ALTO_FILE_GROUP_USE;
        } else if (filePath.startsWith(teiFolder)) {
            // translations have to start with TRANSLATION_FOLDER_PREFIX
            String pathInTeiFolder = filePath.substring(teiFolder.length());
            if (pathInTeiFolder.startsWith(TRANSLATION_FOLDER_PREFIX)) {
                return TRANSLATION_FILE_GROUP_USE;
            }
            return TRANSCRIPTION_FILE_GROUP_USE;
        } else {
            return DEFAULT_FILE_GROUP_USE;
        }
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
        String relatedFileExistPathString = String.format(Locale.ROOT,
            "mets:mets/mets:fileSec/mets:fileGrp[@USE='%s']/mets:file/mets:FLocat", searchFileGroup);
        xpath = XPathFactory.instance().compile(relatedFileExistPathString, Filters.element(), null,
            MCRConstants.METS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);
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
        } else if (path.startsWith(TEI_FOLDER_PREFIX)) {
            path = path.substring(TEI_FOLDER_PREFIX.length());

            if (path.startsWith(TRANSLATION_FOLDER_PREFIX)) {
                // e.g. tei/TRANSLATION_FOLDER_PREFIXDE/folder/file.tif -> folder/file.tif
                path = path.substring(TRANSLATION_FOLDER_PREFIX.length());
                path = path.substring(path.indexOf("/") + 1);
            } else if (path.startsWith(TRANSCRIPTION_FOLDER_PREFIX)) {
                path = path.substring(TRANSCRIPTION_FOLDER_PREFIX.length() + 1);
            }
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
     */
    public static void updateMetsOnFileDelete(MCRPath file) throws Exception {
        MCRObjectID derivateID = MCRObjectID.getInstance(file.getOwner());
        Document mets = getCurrentMets(derivateID.toString());
        if (mets == null) {
            LOGGER.info("Derivate with id \"" + derivateID + "\" has no mets file. Nothing to do");
            return;
        }
        mets = MCRMetsSave.updateOnFileDelete(mets, file);
        if (mets != null) {
            MCRMetsSave.saveMets(mets, derivateID);
        }
    }

    /**
     * Inserts the given URNs into the Mets document.
     * @param derivateID The {@link MCRObjectID} of the Derivate wich contains the METs file
     * @param fileUrnMap a {@link Map} wich contains the file as key and the urn as  as value
     */
    public static void updateMetsOnUrnGenerate(MCRObjectID derivateID, Map<String, String> fileUrnMap)
        throws Exception {
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

        List<PhysicalSubDiv> childs = ((PhysicalStructMap) mets.getStructMap(PhysicalStructMap.TYPE)).getDivContainer()
            .getChildren();
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
    private static Document updateOnFileDelete(Document mets, MCRPath file) {
        Mets modifiedMets = null;
        try {
            modifiedMets = new Mets(mets);
            String href = file.getOwnerRelativePath().substring(1);

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
                                if (fileGrp.getUse().equals(FileGrp.USE_MASTER)) {
                                    physicalSubDivsToRemove.add(physicalSubDiv);
                                } else {
                                    fptrsToRemove.add(fptr);
                                }
                            }
                        }
                        for (Fptr fptrToRemove : fptrsToRemove) {
                            LOGGER.warn(String.format(Locale.ROOT, "remove fptr \"%s\" from mets.xml of \"%s\"",
                                fptrToRemove.getFileId(), file.getOwner()));
                            physicalSubDiv.remove(fptrToRemove);
                        }
                    }
                    for (PhysicalSubDiv physicalSubDivToRemove : physicalSubDivsToRemove) {
                        //remove links in mets:structLink section
                        List<SmLink> list = modifiedMets.getStructLink().getSmLinkByTo(physicalSubDivToRemove.getId());
                        LogicalStructMap logicalStructMap = (LogicalStructMap) modifiedMets
                            .getStructMap(LogicalStructMap.TYPE);

                        for (SmLink linkToRemove : list) {
                            LOGGER.warn(String.format(Locale.ROOT, "remove smLink from \"%s\" to \"%s\"",
                                linkToRemove.getFrom(), linkToRemove.getTo()));
                            modifiedMets.getStructLink().removeSmLink(linkToRemove);
                            // modify logical struct Map
                            String logID = linkToRemove.getFrom();

                            // the deleted file was not directly assigned to a structure
                            if (logicalStructMap.getDivContainer().getId().equals(logID)) {
                                continue;
                            }

                            LogicalDiv logicalDiv = logicalStructMap.getDivContainer().getLogicalSubDiv(
                                logID);
                            if (!(logicalDiv instanceof LogicalDiv)) {
                                LOGGER.error("Could not find " + LogicalDiv.class.getSimpleName() + " with id "
                                    + logID);
                                LOGGER.error("Mets document remains unchanged");
                                return mets;
                            }

                            LogicalDiv logicalSubDiv = (LogicalDiv) logicalDiv;

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
                        divContainer.remove(physicalSubDivToRemove);
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Error occured while removing file " + file + " from the existing mets file", ex);
            return null;
        }

        return modifiedMets.asDocument();
    }

    /**
     * @param mets
     * @param logDiv
     * @throws Exception
     */
    private static void handleParents(LogicalDiv logDiv, Mets mets) throws Exception {
        LogicalDiv parent = logDiv.getParent();

        // there are files for the parent of the log div, thus nothing to do
        if (mets.getStructLink().getSmLinkByFrom(parent.getId()).size() > 0) {
            return;
        }

        //no files associated to the parent of the log div
        LogicalDiv logicalDiv = ((LogicalStructMap) mets.getStructMap(LogicalStructMap.TYPE)).getDivContainer();
        if (parent.getParent() == logicalDiv) {
            //the parent the log div container itself, thus we quit here and remove the log div
            logicalDiv.remove((LogicalDiv) parent);

            return;
        } else {
            handleParents((LogicalDiv) parent, mets);
        }
    }

    /**
     * @return true if all files owned by the derivate appearing in the master file group or false otherwise 
     */
    public static boolean isComplete(Mets mets, MCRObjectID derivateId) {
        try {
            FileGrp fileGroup = mets.getFileSec().getFileGroup(FileGrp.USE_MASTER);
            MCRPath rootPath = MCRPath.getPath(derivateId.toString(), "/");
            return isComplete(fileGroup, rootPath);
        } catch (Exception ex) {
            LOGGER.error("Error while validating mets", ex);
            return false;
        }
    }

    /**
     * @return true if all files in the {@link MCRDirectory} appears in the fileGroup
     */
    public static boolean isComplete(final FileGrp fileGroup, MCRPath rootDir) {
        final AtomicBoolean complete = new AtomicBoolean(true);
        try {
            Files.walkFileTree(rootDir, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!file.getFileName().toString().equals(MCRMetsSave.getMetsFileName())) {
                        MCRPath mcrPath = MCRPath.toMCRPath(file);
                        String path;
                        try {
                            path = MCRXMLFunctions.encodeURIPath(mcrPath.getOwnerRelativePath().substring(1));//remove leading '/'
                        } catch (URISyntaxException e) {
                            throw new IOException(e);
                        }
                        if (!fileGroup.contains(path)) {
                            LOGGER.warn(MessageFormat.format("{0} does not appear in {1}!", path, mcrPath.getOwner()));
                            complete.set(false);
                            return FileVisitResult.TERMINATE;
                        }
                    }
                    return super.visitFile(file, attrs);
                }
            });
        } catch (Exception ex) {
            LOGGER.error("Error while validating mets", ex);
            return false;
        }

        return complete.get();
    }

}
