/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.mets.tools;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.mycore.common.MCRStreamUtils;
import org.mycore.common.MCRXlink;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRPathContent;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.common.MCRMarkManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRContentTypes;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.mets.model.MCRMetsModelHelper;
import org.mycore.mets.model.Mets;
import org.mycore.mets.model.files.FLocat;
import org.mycore.mets.model.files.File;
import org.mycore.mets.model.files.FileGrp;
import org.mycore.mets.model.struct.Fptr;
import org.mycore.mets.model.struct.LOCTYPE;
import org.mycore.mets.model.struct.LogicalDiv;
import org.mycore.mets.model.struct.LogicalStructMap;
import org.mycore.mets.model.struct.PhysicalDiv;
import org.mycore.mets.model.struct.PhysicalStructMap;
import org.mycore.mets.model.struct.PhysicalSubDiv;
import org.mycore.mets.model.struct.SmLink;
import org.mycore.mets.model.struct.StructLink;
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

    public static final String ALTO_FOLDER_PREFIX = "alto/";

    public static final String TEI_FOLDER_PREFIX = "tei/";

    public static final String TRANSLATION_FOLDER_PREFIX = "translation.";

    public static final String TRANSCRIPTION_FOLDER_PREFIX = "transcription";

    public static final String UNKNOWN_FILEGROUP = "UNKNOWN";

    private static final Logger LOGGER = LogManager.getLogger();

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
            LOGGER.warn("Storing mets.xml for {} failed cause the given document was invalid.", derivateId);
            return false;
        }

        try (OutputStream metsOut = Files.newOutputStream(metsFile)) {
            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            xout.output(document, metsOut);
            LOGGER.info("Storing file content from \"{}\" to derivate \"{}\"",
                MCRMetsSave::getMetsFileName, () -> derivateId);
        } catch (Exception e) {
            LOGGER.error(e);
            return false;
        }
        return true;
    }

    public static String getMetsFileName() {
        return MCRConfiguration2.getString("MCR.Mets.Filename").orElse("mets.xml");
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
            LOGGER.info("Derivate with id \"{}\" has no mets file. Nothing to do", derivateID);
            return;
        }
        mets = updateOnFileAdd(mets, file);
        if (mets != null) {
            saveMets(mets, derivateID);
        }

    }

    /**
     * Returns the mets.xml as JDOM document for the given derivate or null.
     *
     * @param derivateID the derivate identifier
     * @return the mets.xml as JDOM document
     */
    private static Document getCurrentMets(String derivateID) throws JDOMException, IOException {
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
            // check for file existance (if a derivate with mets.xml is uploaded
            String relPath = MCRXMLFunctions.encodeURIPath(file.getOwnerRelativePath().substring(1), true);

            // Check if file already exists -> if yes do nothing
            String fileExistPathString = "mets:mets/mets:fileSec/mets:fileGrp/mets:file/mets:FLocat[@xlink:href='"
                + relPath + "']";
            XPathExpression<Element> xpath = XPathFactory.instance().compile(fileExistPathString, Filters.element(),
                null, MCRConstants.METS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);

            if (!xpath.evaluate(mets).isEmpty()) {
                String msgTemplate = "The File : '%s' already exists in mets.xml";
                LOGGER.warn(() -> String.format(Locale.ROOT, msgTemplate, relPath));
                return null;
            } else {
                String msgTemplate = "The File : '%s' does not exists in mets.xml";
                LOGGER.warn(() -> String.format(Locale.ROOT, msgTemplate, relPath));
            }

            // add to file section
            String contentType = MCRContentTypes.probeContentType(file);
            LOGGER.warn("Content Type is : {}", contentType);
            String fileGrpUSE = getFileGroupUse(file);

            String fileId = new MessageFormat("{0}_{1}", Locale.ROOT)
                .format(new Object[] { fileGrpUSE.toLowerCase(Locale.ROOT), getFileBase(relPath) });
            File fileAsMetsFile = new File(fileId, contentType);

            FLocat fLocat = new FLocat(LOCTYPE.URL, relPath);
            fileAsMetsFile.setFLocat(fLocat);

            Element fileSec = getFileGroup(mets, fileGrpUSE);
            fileSec.addContent(fileAsMetsFile.asElement());

            if (fileGrpUSE.equals(MCRMetsModelHelper.MASTER_USE)) {
                updateOnImageFile(mets, fileId, relPath);
            } else {
                updateOnCustomFile(mets, fileId, relPath);
            }
        } catch (Exception ex) {
            LOGGER.error("Error occured while adding file {} to the existing mets file", file, ex);
            return null;
        }

        return mets;
    }

    private static void updateOnImageFile(Document mets, String fileId, String path) {
        LOGGER.debug("FILE is a image!");
        //check if custom files are present and save the ids
        List<String> customFileGroups = getFileGroups(mets);

        // add to structMap physical
        PhysicalSubDiv div = new PhysicalSubDiv(PhysicalSubDiv.ID_PREFIX + fileId, PhysicalSubDiv.TYPE_PAGE);
        div.add(new Fptr(fileId));

        customFileGroups.stream()
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

        String matchId = searchFileInGroup(mets, path, MCRMetsModelHelper.MASTER_USE);

        if (matchId == null) {
            // there is no file wich belongs to the alto xml so just return
            LOGGER.warn("no file found wich belongs to the custom xml : {}", path);
            return;
        }
        // check if there is a physical file
        Element physPageElement = getPhysicalFile(mets, matchId);
        if (physPageElement != null) {
            physPageElement.addContent(new Fptr(fileId).asElement());
            LOGGER.warn("physical page found for file {}", matchId);
        } else {
            LOGGER.warn("no physical page found for file {}", matchId);
        }
    }

    private static Element getPhysicalFile(Document mets, String matchId) {
        XPathExpression<Element> xpath;
        String physicalFileExistsXpathString = String
            .format(
                Locale.ROOT,
                "mets:mets/mets:structMap[@TYPE='PHYSICAL']/mets:div[@TYPE='physSequence']" +
                    "/mets:div[mets:fptr/@FILEID='%s']",
                matchId);
        xpath = XPathFactory.instance().compile(physicalFileExistsXpathString, Filters.element(), null,
            MCRConstants.METS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);

        return xpath.evaluateFirst(mets);
    }

    private static List<String> getFileGroups(Document mets) {
        final XPathExpression<Attribute> xpath = XPathFactory.instance()
            .compile("mets:mets/mets:fileSec/mets:fileGrp/@USE", Filters.attribute(), null,
                MCRConstants.METS_NAMESPACE);
        return xpath.evaluate(mets).stream().map(Attribute::getValue).collect(Collectors.toList());

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
            "mets:mets/mets:structMap[@TYPE='PHYSICAL']/mets:div[@TYPE='physSequence']", Filters.element(),
            null, MCRConstants.METS_NAMESPACE);
        return xpath.evaluateFirst(mets);
    }

    /**
     * Decides in which file group the file should be inserted
     *
     * @param file the to check
     * @return the id of the filegGroup
     */
    public static String getFileGroupUse(MCRPath file) {
        return MCRMetsModelHelper.getUseForHref(file.getOwnerRelativePath()).orElse(UNKNOWN_FILEGROUP);
    }

    /**
     * Searches a file in a group, which matches a filename.
     *
     * @param mets the mets file to search
     * @param path the path to the alto file (e.g. "alto/alto_file.xml" when searching in DEFAULT_FILE_GROUP_USE or
     *             "image_file.jpg" when searchin in ALTO_FILE_GROUP_USE)
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
        String cleanPath = getCleanPath(path);

        for (Element fileLoc : fileLocList) {
            Attribute hrefAttribute = fileLoc.getAttribute(MCRXlink.HREF, MCRConstants.XLINK_NAMESPACE);
            String hrefAttributeValue = hrefAttribute.getValue();
            String hrefPath = getCleanPath(removeExtension(hrefAttributeValue));

            if (hrefPath.equals(removeExtension(cleanPath))) {
                matchId = ((Element) fileLoc.getParent()).getAttributeValue("ID");
                break;
            }
        }
        return matchId;
    }

    private static String getCleanPath(String path) {
        String cleanPath = path;
        if (cleanPath.startsWith(ALTO_FOLDER_PREFIX)) {
            cleanPath = cleanPath.substring(ALTO_FOLDER_PREFIX.length());
        } else if (cleanPath.startsWith(TEI_FOLDER_PREFIX)) {
            cleanPath = cleanPath.substring(TEI_FOLDER_PREFIX.length());

            if (cleanPath.startsWith(TRANSLATION_FOLDER_PREFIX)) {
                // e.g. tei/TRANSLATION_FOLDER_PREFIXDE/folder/file.tif -> folder/file.tif
                cleanPath = cleanPath.substring(TRANSLATION_FOLDER_PREFIX.length());
                cleanPath = cleanPath.substring(cleanPath.indexOf('/') + 1);
            } else if (cleanPath.startsWith(TRANSCRIPTION_FOLDER_PREFIX)) {
                cleanPath = cleanPath.substring(TRANSCRIPTION_FOLDER_PREFIX.length() + 1);
            }
        }
        return cleanPath;
    }

    private static String removeExtension(String fileName) {
        int dotPosition = fileName.lastIndexOf('.');
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
    public static void updateMetsOnFileDelete(MCRPath file) throws JDOMException, SAXException, IOException {
        MCRObjectID derivateID = MCRObjectID.getInstance(file.getOwner());
        Document mets = getCurrentMets(derivateID.toString());
        if (mets == null) {
            LOGGER.info("Derivate with id \"{}\" has no mets file. Nothing to do", derivateID);
            return;
        }
        try {
            Mets modifiedMets = new Mets(mets);
            UpdateMETSOnFileDeleteAction action = new UpdateMETSOnFileDeleteAction(modifiedMets, file);
            action.apply();
            saveMets(mets, derivateID);
        } catch (Exception e) {
            LOGGER.error("Error occurred while removing file {} from the existing mets file", file, e);
        }
    }

    /**
     * Inserts the given URNs into the mets document.
     *
     * @param derivate The {@link MCRDerivate} which contains the mets file
     */
    public static void updateMetsOnUrnGenerate(MCRDerivate derivate) {
        if (MCRMarkManager.getInstance().isMarkedForDeletion(derivate)) {
            return;
        }
        try {
            Map<String, String> urnFileMap = derivate.getUrnMap();
            if (!urnFileMap.isEmpty()) {
                updateMetsOnUrnGenerate(derivate.getId(), urnFileMap);
            } else {
                LOGGER.debug("There are no URN to insert");
            }
        } catch (Exception e) {
            LOGGER.error("Read derivate XML cause error", e);
        }
    }

    /**
     * Inserts the given URNs into the Mets document.
     * @param derivateID The {@link MCRObjectID} of the Derivate wich contains the METs file
     * @param fileUrnMap a {@link Map} which contains the file as key and the urn as  as value
     */
    public static void updateMetsOnUrnGenerate(MCRObjectID derivateID, Map<String, String> fileUrnMap)
        throws JDOMException, SAXException, IOException {
        Document mets = getCurrentMets(derivateID.toString());
        if (mets == null) {
            LOGGER.info("Derivate with id \"{}\" has no mets file. Nothing to do", derivateID);
            return;
        }
        LOGGER.info("Update {} URNS in mets.xml", fileUrnMap::size);
        Mets metsObject = new Mets(mets);
        updateURNsInMetsDocument(metsObject, fileUrnMap);
        saveMets(metsObject.asDocument(), derivateID);
    }

    /**
     * Inserts the given URNs into the {@link Mets} Object.
     * @param mets the {@link Mets} object were the URNs should be inserted.
     * @param fileUrnMap a {@link Map} wich contains the file as key and the urn as  as value
     */
    public static void updateURNsInMetsDocument(Mets mets, Map<String, String> fileUrnMap) {
        // put all files of the mets in a list
        List<FileGrp> fileGroups = mets.getFileSec().getFileGroups();
        List<File> files = new ArrayList<>();
        for (FileGrp fileGrp : fileGroups) {
            files.addAll(fileGrp.getFileList());
        }

        // combine the filename and the id in a map
        Map<String, String> idFileMap = new HashMap<>();
        for (File file : files) {
            idFileMap.put(file.getId(), file.getFLocat().getHref());
        }

        List<PhysicalSubDiv> childs = ((PhysicalStructMap) mets.getStructMap(PhysicalStructMap.TYPE)).getDivContainer()
            .getChildren();
        for (PhysicalSubDiv divChild : childs) {
            String idMets = divChild.getChildren().getFirst().getFileId();

            // check if there is a URN for the file
            String file = "/" + URLDecoder.decode(idFileMap.get(idMets), StandardCharsets.UTF_8);
            if (fileUrnMap.containsKey(file)) {
                divChild.setContentIds(fileUrnMap.get(file));
            }
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
     * @return true if all files in the {@link org.mycore.datamodel.ifs2.MCRDirectory} appears in the fileGroup
     */
    public static boolean isComplete(final FileGrp fileGroup, MCRPath rootDir) {
        final AtomicBoolean complete = new AtomicBoolean(true);
        try {
            Files.walkFileTree(rootDir, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!file.getFileName().toString().equals(getMetsFileName())) {
                        MCRPath mcrPath = MCRPath.ofPath(file);
                        String path;
                        try {
                            path = MCRXMLFunctions
                                .encodeURIPath(mcrPath.getOwnerRelativePath().substring(1), true);//remove leading '/'
                        } catch (URISyntaxException e) {
                            throw new IOException(e);
                        }
                        if (!fileGroup.contains(path)) {
                            LOGGER.warn("{} does not appear in {}!", () -> path, mcrPath::getOwner);
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

    /**
     * Call this method to update the mets.xml if files of the derivate have changed. Files will be added or removed
     * from the mets:fileSec and mets:StructMap[@type=PHYSICAL]. The mets:structLink part will be rebuild after.
     *
     * <p>This method takes care of the group assignment. For example: image files will be added to the MASTER
     * group and ALTO files to the ALTO group. It will also bundle files with the same name e.g. sample1.tiff and
     * alto/sample1.xml to the same physical struct map div.</p>
     *
     * <p><b>Important:</b> This method does not update the mets.xml in the derivate, its just updating the given mets
     * instance.</p>
     *
     * @param mets the mets to update
     * @param derivatePath path to the derivate -&gt; required for looking up new files
     * @throws IOException derivate couldn't be read
     */
    public static void updateFiles(Mets mets, final MCRPath derivatePath) throws IOException {
        UpdateMETSOnFileChangeAction action = new UpdateMETSOnFileChangeAction(mets, derivatePath);
        action.apply();
    }

    private static void addFileToMets(Mets mets, MCRPath derivatePath, String href,
        PhysicalDiv physicalDiv, StructLink structLink, LogicalDiv lastLogicalDiv) {
        String fileBase = getFileBase(href);
        MCRPath filePath = (MCRPath) derivatePath.resolve(href);

        try {
            String fileGroupUse = getFileGroupUse(filePath);
            // build file
            String mimeType = MCRContentTypes.probeContentType(filePath);
            String fileId = fileGroupUse.toLowerCase(Locale.ROOT) + "_" + fileBase;
            File file = new File(fileId, mimeType);
            file.setFLocat(new FLocat(LOCTYPE.URL, href));

            // fileSec
            FileGrp fileGroup = mets.getFileSec().getFileGroup(fileGroupUse);
            if (fileGroup == null) {
                fileGroup = new FileGrp(fileGroupUse);
                mets.getFileSec().addFileGrp(fileGroup);
            }
            fileGroup.addFile(file);

            // structMap physical
            String existingFileID = mets.getFileSec().getFileGroups().stream()
                .filter(grp -> !grp.getUse().equals(fileGroupUse))
                .flatMap(grp -> grp.getFileList().stream()).filter(brotherFile -> fileBase
                    .equals(getFileBase(brotherFile.getFLocat().getHref())))
                .map(File::getId).findAny()
                .orElse(null);
            PhysicalSubDiv physicalSubDiv;
            if (existingFileID != null) {
                // there is a file (e.g. img or alto) which the same file base -> add the file to this mets:div
                physicalSubDiv = physicalDiv.byFileId(existingFileID);
                physicalSubDiv.add(new Fptr(file.getId()));
            } else {
                // there is no mets:div with this file
                physicalSubDiv = new PhysicalSubDiv(PhysicalSubDiv.ID_PREFIX + fileBase,
                    PhysicalSubDiv.TYPE_PAGE);
                physicalSubDiv.add(new Fptr(file.getId()));
                physicalDiv.add(physicalSubDiv);
            }
            // add to struct link
            structLink.addSmLink(new SmLink(lastLogicalDiv.getId(), physicalSubDiv.getId()));
        } catch (Exception exc) {
            LOGGER.error(() -> "Unable to add file " + href + " to mets.xml of " + derivatePath.getOwner(), exc);
        }
    }

    /**
     * Returns a list of files in the given path. This does not return directories!
     *
     * @param path the path to list
     * @param ignore paths which should be ignored
     * @return list of <code>MCRPath's</code> files
     * @throws IOException if an I/O error is thrown when accessing the starting file.
     */
    public static List<MCRPath> listFiles(MCRPath path, Collection<MCRPath> ignore) throws IOException {
        try (Stream<Path> pathStream = Files.walk(path).filter(Files::isRegularFile)) {
            return pathStream.map(MCRPath::ofPath)
                .filter(MCRStreamUtils.not(ignore::contains))
                .sorted()
                .collect(Collectors.toList());
        }
    }

    /**
     * Builds new mets:fileGrp's based on the given paths using the mycore derivate convetions.
     *
     * <ul>
     *     <li><b>root folder</b> -&gt; mets:fileGrp[@USE=MASTER]</li>
     *     <li><b>alto/ folder</b> -&gt; mets:fileGrp[@USE=ALTO]</li>
     *     <li><b>tei/translation folder</b> -&gt; mets:fileGrp[@USE=TRANSLATION</li>
     *     <li><b>tei/transcription folder</b> -&gt; mets:fileGrp[@USE=TRANSCRIPTION</li>
     * </ul>
     *
     * @param paths the paths to check for the groups
     * @return a list of new created <code>FileGrp</code> objects
     */
    public static List<FileGrp> buildFileGroups(List<MCRPath> paths) {
        return listFileUse(paths).stream()
            .map(FileGrp::new)
            .collect(Collectors.toList());
    }

    /**
     * Returns a list of all <code>MCRMetsFileUse</code> in the given paths.
     *
     * @param paths paths to check
     * @return list of <code>MCRMetsFileUse</code>
     */
    public static List<String> listFileUse(List<MCRPath> paths) {
        Set<String> fileUseSet = new HashSet<>();
        for (MCRPath path : paths) {
            final Optional<String> use = MCRMetsModelHelper.getUseForHref(path.getOwnerRelativePath());
            use.ifPresent(fileUseSet::add);
        }
        return new ArrayList<>(fileUseSet);
    }

    /**
     * Returns the name without any path information or file extension. Usable to create mets ID's.
     *
     * <ul>
     *     <li>abc123.jpg -&gt; abc123</li>
     *     <li>alto/abc123.xml -&gt; abc123</li>
     * </ul>
     *
     * @param href the href to get the file base name
     * @return the href shortcut
     */
    public static String getFileBase(String href) {
        String fileName = Paths.get(href).getFileName().toString();
        int endIndex = fileName.lastIndexOf('.');
        if (endIndex != -1) {
            fileName = fileName.substring(0, endIndex);
        }
        return MCRXMLFunctions.toNCNameSecondPart(fileName);
    }

    /**
     * Returns the name without any path information or file extension. Useable to create mets ID's.
     *
     * <ul>
     *     <li>abc123.jpg -&gt; abc123</li>
     *     <li>alto/abc123.xml -&gt; abc123</li>
     * </ul>
     *
     * @param path the href to get the file base name
     * @return the href shortcut
     */
    public static String getFileBase(MCRPath path) {
        return getFileBase(path.getOwnerRelativePath().substring(1));
    }

    /**
     * Returns the mets:file/@ID for the given path.
     *
     * @param path path to the file
     * @return mets:file ID
     */
    public static String getFileId(MCRPath path) {
        String prefix = MCRMetsModelHelper.getUseForHref(path.getOwnerRelativePath()).orElse(UNKNOWN_FILEGROUP);
        String base = getFileBase(path);
        return prefix + "_" + base;
    }

    private record UpdateMETSOnFileChangeAction(Mets mets, MCRPath derivatePath) {

        public void apply() throws IOException {
            List<String> metsFiles = getMetsFiles();
            try(Stream<Path> pathStream = Files.walk(derivatePath).filter(MCRStreamUtils.not(Files::isDirectory))) {
                List<String> derivateFiles = getDerivateFiles(pathStream);

                List<String> removedFiles = new ArrayList<>(metsFiles);
                removedFiles.removeAll(derivateFiles);
                List<String> addedFiles = new ArrayList<>(derivateFiles);
                Collections.sort(addedFiles);
                addedFiles.removeAll(metsFiles);

                StructLink structLink = mets.getStructLink();
                PhysicalStructMap physicalStructMap = mets.getPhysicalStructMap();
                PhysicalDiv physicalDiv = physicalStructMap.getDivContainer();

                // remove old files
                List<String> unlinkedLogicalIds = removeFiles(removedFiles, physicalDiv, structLink);
                // fix unlinked logical divs
                if (!unlinkedLogicalIds.isEmpty()) {
                    fixUnlinkedLogicalDivs(physicalStructMap, unlinkedLogicalIds, structLink);
                }
                // get last logical div
                addFiles(addedFiles, physicalDiv, structLink);
            }
        }

        private static void fixUnlinkedLogicalDivs(PhysicalStructMap physicalStructMap, List<String> unlinkedLogicalIds,
            StructLink structLink) {
            List<PhysicalSubDiv> physicalChildren = physicalStructMap.getDivContainer().getChildren();
            String firstPhysicalID = physicalChildren.isEmpty() ? physicalStructMap.getDivContainer().getId()
                                                                : physicalChildren.getFirst().getId();
            // a logical div is not linked anymore -> link with first physical div
            unlinkedLogicalIds.forEach(from -> structLink.addSmLink(new SmLink(from, firstPhysicalID)));
        }

        private List<String> removeFiles(List<String> removedFiles, PhysicalDiv physicalDiv, StructLink structLink) {
            List<String> unlinkedLogicalIds = new ArrayList<>();
            // remove files
            for (String href : removedFiles) {
                File file = null;
                // remove from fileSec
                for (FileGrp grp : mets.getFileSec().getFileGroups()) {
                    file = grp.getFileByHref(href);
                    if (file != null) {
                        grp.removeFile(file);
                        break;
                    }
                }
                if (file == null) {
                    continue;
                }
                // remove from physical
                PhysicalSubDiv physicalSubDiv = physicalDiv.byFileId(file.getId());
                physicalSubDiv.remove(physicalSubDiv.getFptr(file.getId()));
                if (physicalSubDiv.getChildren().isEmpty()) {
                    physicalDiv.remove(physicalSubDiv);
                }
                // remove from struct link
                for (SmLink smLink : structLink.getSmLinkByTo(physicalSubDiv.getId())) {
                    structLink.removeSmLink(smLink);
                    if (structLink.getSmLinkByFrom(smLink.getFrom()).isEmpty()) {
                        unlinkedLogicalIds.add(smLink.getFrom());
                    }
                }
            }
            return unlinkedLogicalIds;
        }

        private void addFiles(List<String> addedFiles, PhysicalDiv physicalDiv, StructLink structLink) {
            LogicalDiv divContainer = mets.getLogicalStructMap().getDivContainer();
            List<LogicalDiv> descendants = divContainer.getDescendants();
            LogicalDiv lastLogicalDiv = descendants.isEmpty() ? divContainer : descendants.getLast();
            // add files
            addedFiles.forEach(href -> {
                addFileToMets(mets, derivatePath, href, physicalDiv, structLink, lastLogicalDiv);
            });
        }

        private static List<String> getDerivateFiles(Stream<Path> pathStream) {
            return pathStream
                .map(MCRPath::ofPath)
                .map(MCRPath::getOwnerRelativePath)
                .map(path -> path.substring(1))
                .filter(href -> !Objects.equals(href, "mets.xml"))
                .toList();
        }

        private List<String> getMetsFiles() {
            return mets.getFileSec().getFileGroups().stream()
                .flatMap(fileGrp -> fileGrp.getFileList().stream())
                .map(File::getFLocat)
                .map(FLocat::getHref)
                .toList();
        }

    }

    private record UpdateMETSOnFileDeleteAction(Mets mets, MCRPath file) {

        public void apply() {
            String href = file.getOwnerRelativePath().substring(1);

            PhysicalStructMap physStructMap = (PhysicalStructMap) mets.getStructMap(PhysicalStructMap.TYPE);
            PhysicalDiv divContainer = physStructMap.getDivContainer();

            // search the right group and remove the file from the group
            for (FileGrp fileGrp : mets.getFileSec().getFileGroups()) {
                if (!fileGrp.contains(href)) {
                    continue;
                }
                File fileToRemove = fileGrp.getFileByHref(href);
                fileGrp.removeFile(fileToRemove);
                List<PhysicalSubDiv> physicalSubDivsToRemove = removeFptrs(fileGrp, divContainer, fileToRemove);
                removePhysicalSubDivs(physicalSubDivsToRemove, divContainer);
            }
        }

        private List<PhysicalSubDiv> removeFptrs(FileGrp fileGrp, PhysicalDiv divContainer, File fileToRemove) {
            List<PhysicalSubDiv> physicalSubDivsToRemove = new ArrayList<>();
            // remove file from mets:mets/mets:structMap[@TYPE='PHYSICAL']
            for (PhysicalSubDiv physicalSubDiv : divContainer.getChildren()) {
                List<Fptr> fptrsToRemove = new ArrayList<>();
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
                    LOGGER.info(() -> String.format(Locale.ROOT, "remove fptr \"%s\" from mets.xml of \"%s\"",
                        fptrToRemove.getFileId(), file.getOwner()));
                    physicalSubDiv.remove(fptrToRemove);
                }
            }
            return physicalSubDivsToRemove;
        }

        private void removePhysicalSubDivs(List<PhysicalSubDiv> physicalSubDivsToRemove, PhysicalDiv divContainer) {
            for (PhysicalSubDiv physicalSubDivToRemove : physicalSubDivsToRemove) {
                //remove links in mets:structLink section
                List<SmLink> list = mets.getStructLink().getSmLinkByTo(physicalSubDivToRemove.getId());
                LogicalStructMap logicalStructMap = (LogicalStructMap) mets
                    .getStructMap(LogicalStructMap.TYPE);

                for (SmLink linkToRemove : list) {
                    rmSmLink(linkToRemove, logicalStructMap);
                }
                divContainer.remove(physicalSubDivToRemove);
            }
        }

        private void rmSmLink(SmLink linkToRemove, LogicalStructMap logicalStructMap) {
            LOGGER.info(() -> String.format(Locale.ROOT, "remove smLink from \"%s\" to \"%s\"",
                linkToRemove.getFrom(), linkToRemove.getTo()));
            mets.getStructLink().removeSmLink(linkToRemove);
            // modify logical struct Map
            String logID = linkToRemove.getFrom();
            // the deleted file was not directly assigned to a structure
            if (logicalStructMap.getDivContainer().getId().equals(logID)) {
                return;
            }
            LogicalDiv logicalDiv = logicalStructMap.getDivContainer().getLogicalSubDiv(logID);
            if (logicalDiv == null) {
                LOGGER.error("Could not find 'LogicalDiv' with id '{}'.", () -> logID);
                return;
            }
            // there are still files for this logical sub div, nothing to do
            if (!mets.getStructLink().getSmLinkByFrom(logicalDiv.getId()).isEmpty()) {
                return;
            }
            // the logical div has other divs included, nothing to do
            if (!logicalDiv.getChildren().isEmpty()) {
                return;
            }
            /*
             * the log div might be in a hierarchy of divs, which may now be empty
             * (only containing empty directories), if so the parent of the log div
             * must be deleted
             * */
            handleParents(logicalDiv);

            logicalStructMap.getDivContainer().remove(logicalDiv);
        }

        private void handleParents(LogicalDiv logDiv) {
            LogicalDiv parent = logDiv.getParent();
            // there are files for the parent of the log div, thus nothing to do
            if (!mets.getStructLink().getSmLinkByFrom(parent.getId()).isEmpty()) {
                return;
            }
            //no files associated to the parent of the log div
            LogicalDiv logicalDiv = ((LogicalStructMap) mets.getStructMap(LogicalStructMap.TYPE)).getDivContainer();
            if (parent.getParent().equals(logicalDiv)) {
                //the parent the log div container itself, thus we quit here and remove the log div
                logicalDiv.remove(parent);
            } else {
                handleParents(parent);
            }
        }

    }

}
