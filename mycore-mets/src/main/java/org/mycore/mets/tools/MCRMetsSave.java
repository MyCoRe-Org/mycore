package org.mycore.mets.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.activation.MimetypesFileTypeMap;

import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mets.model.Mets;
import org.mycore.mets.model.files.FLocat;
import org.mycore.mets.model.files.FileGrp;
import org.mycore.mets.model.files.FileSec;
import org.mycore.mets.model.struct.AbstractLogicalDiv;
import org.mycore.mets.model.struct.Fptr;
import org.mycore.mets.model.struct.LOCTYPE;
import org.mycore.mets.model.struct.LogicalDiv;
import org.mycore.mets.model.struct.LogicalStructMap;
import org.mycore.mets.model.struct.LogicalSubDiv;
import org.mycore.mets.model.struct.PhysicalStructMap;
import org.mycore.mets.model.struct.PhysicalSubDiv;
import org.mycore.mets.model.struct.SmLink;

/**
 * Class is responsible for saving a mets document to a derivate. It also can
 * handle addition and removing files from a derivate.
 * 
 * @author shermann
 *          Sebastian Hofmann
 */
public class MCRMetsSave {

    private final static Logger LOGGER = Logger.getLogger(MCRMetsSave.class);

    /**
     * Saves the content of the given document to file and then adds the file to
     * the derivate with the given id. The name of the file depends on property
     * 'MCR.Mets.Filename'. If this property has not been set 'mets.xml' is used
     * as a default filename.
     * 
     * @param document
     * @param derivateId
     */
    public static void saveMets(Document document, MCRObjectID derivateId) {
        File tmp = null;
        /* save temporary file */
        try {
            tmp = save(document);
        } catch (Exception ex) {
            LOGGER.error("Could not save file on disk.", ex);
            return;
        }
        // add the file to the existing derivate in ifs
        String fileName = MCRConfiguration.instance().getString("MCR.Mets.Filename", "mets.xml");
        LOGGER.info("Storing file content from \"" + tmp.getName() + "\" to derivate \"" + derivateId + "\"");
        MCRFile uploadFile = new MCRFile(fileName, MCRMetadataManager.retrieveMCRDerivate(derivateId).receiveDirectoryFromIFS());
        uploadFile.setContentFrom(tmp);

        // delete temporary file
        if (!tmp.delete()) {
            LOGGER.warn("Temporary file \"" + tmp.getName() + "\"could not be deleted ");
        }
    }

    /**
     * Saves the given document to disk, pattern is as follows <br/>
     * <br/>
     * <code>File.createTempFile("mets", ".xml");</code>
     * 
     * @return the reference to the mets file
     */
    private static File save(Document mets) throws Exception {
        File file = File.createTempFile(UUID.randomUUID().toString(), ".xml");
        FileOutputStream stream = new FileOutputStream(file);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        outputter.output(mets, stream);
        stream.close();
        return file;
    }

    /**
     * Updates the mets.xml belonging to the given derivate. Adds the file to
     * the mets document (updates file sections and stuff within the mets.xml)
     * 
     * @param derivate
     *            the derivate owning the mets.xml and the given file
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
     * @param derivate
     * @return
     * @throws JDOMException
     * @throws IOException
     */
    private static Document getCurrentMets(String derivateID) throws JDOMException, IOException {
        String mf = MCRConfiguration.instance().getString("MCR.Mets.Filename", "mets.xml");
        MCRDirectory rootDir = MCRDirectory.getRootDirectory(derivateID);
        if (rootDir == null) {
            return null;
        }
        MCRFilesystemNode metsDocNode = rootDir.getChild(mf);
        if (!(metsDocNode instanceof MCRFile)) {
            return null;
        }
        Document mets = new SAXBuilder().build(((MCRFile) metsDocNode).getContentAsInputStream());
        return mets;
    }

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

            /* add to file section "use=master" */
            String contentType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(new File(file.getName()));
            LOGGER.info(MessageFormat.format("Content Type is : {0}", contentType));
            org.mycore.mets.model.files.File f = new org.mycore.mets.model.files.File(fileId, contentType);
            FLocat fLocat = new FLocat(LOCTYPE.URL, file.getName());
            f.setFLocat(fLocat);

            // alter the mets document
            XPathExpression<Element> xpath = XPathFactory.instance().compile("mets:mets/mets:fileSec/mets:fileGrp", Filters.element(),
                null, MCRConstants.METS_NAMESPACE);
            Element fileSec = xpath.evaluateFirst(mets);
            fileSec.addContent(f.asElement());

            /* add to structMap physical */
            XPathExpression<Attribute> attributeXpath = XPathFactory.instance().compile(
                "mets:mets/mets:structMap[@TYPE='PHYSICAL']/mets:div[@TYPE='physSequence']/mets:div[last()]/@ORDER", Filters.attribute(),
                null, MCRConstants.METS_NAMESPACE);
            Attribute orderAttribute = attributeXpath.evaluateFirst(mets);
            PhysicalSubDiv div = new PhysicalSubDiv(PhysicalSubDiv.ID_PREFIX + fileId, PhysicalSubDiv.TYPE_PAGE,
                orderAttribute.getIntValue() + 1);
            div.add(new Fptr(fileId));

            // actually alter the mets document
            xpath = XPathFactory.instance().compile("mets:mets/mets:structMap[@TYPE='PHYSICAL']/mets:div[@TYPE='physSequence']",
                Filters.element(), null, MCRConstants.METS_NAMESPACE);
            Element structMapPhys = xpath.evaluateFirst(mets);
            structMapPhys.addContent(div.asElement());

            /* add to structLink */
            attributeXpath = XPathFactory.instance().compile("mets:mets/mets:structMap[@TYPE='LOGICAL']/mets:div/@ID", Filters.attribute(),
                null, MCRConstants.METS_NAMESPACE);
            Attribute idAttribute = attributeXpath.evaluateFirst(mets);
            String rootID = idAttribute.getValue();

            xpath = XPathFactory.instance().compile("mets:mets/mets:structLink", Filters.element(), null, MCRConstants.METS_NAMESPACE);
            Element structLink = xpath.evaluateFirst(mets);

            structLink.addContent((new SmLink(rootID, div.getId()).asElement()));

        } catch (Exception ex) {
            LOGGER.error("Error occured while adding file " + file.getAbsolutePath() + " to the existing mets file", ex);
            return null;
        }

        return mets;
    }

    /**
     * Updates the mets.xml belonging to the given derivate. Removes the file
     * from the mets document (updates file sections and stuff within the
     * mets.xml)
     * 
     * @param derivate
     *            the derivate owning the mets.xml and the given file
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
     * @param derivate The {@link MCRDerivate} wich contains the METs file
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

            // remove file from mets:fileSec/mets:fileGrp
            org.mycore.mets.model.files.File fileToRemove = modifiedMets.getFileSec().getFileGroup(FileGrp.USE_MASTER).getFileByHref(href);
            FileSec fileSec = modifiedMets.getFileSec();
            FileGrp fileGroup = fileSec.getFileGroup(FileGrp.USE_MASTER);
            fileGroup.removeFile(fileToRemove);

            // remove file from mets:mets/mets:structMap[@TYPE='PHYSICAL']
            PhysicalStructMap physStructMap = (PhysicalStructMap) modifiedMets.getStructMap(PhysicalStructMap.TYPE);
            physStructMap.getDivContainer().remove(PhysicalSubDiv.ID_PREFIX + fileToRemove.getId());

            //remove links in mets:structLink section
            List<SmLink> list = modifiedMets.getStructLink().getSmLinkByTo(PhysicalSubDiv.ID_PREFIX + fileToRemove.getId());
            LogicalStructMap logicalStructMap = (LogicalStructMap) modifiedMets.getStructMap(LogicalStructMap.TYPE);

            for (SmLink linkToRemove : list) {
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
        } catch (Exception ex) {
            LOGGER.error("Error occured while removing file " + file.getAbsolutePath() + " from the existing mets file", ex);
            return null;
        }

        return modifiedMets.asDocument();
    }

    /**
     * @param mets
     * @param logDiv
     * @param logDivContainer
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
     * @param derivateId
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
