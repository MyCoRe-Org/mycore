package org.mycore.mets.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import javax.activation.MimetypesFileTypeMap;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mets.model.files.FLocat;
import org.mycore.mets.model.files.FileGrp;
import org.mycore.mets.model.struct.Fptr;
import org.mycore.mets.model.struct.PhysicalSubDiv;
import org.mycore.mets.model.struct.SmLink;

/**
 * Class is responsible for saving a mets document to a derivate. It also can
 * handle addition and removing files from a derivate.
 * 
 * @author shermann
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
    public static void saveMets(Document document, String derivateId) {
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
        MCRFile uploadFile = new MCRFile(fileName, MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(derivateId))
                .receiveDirectoryFromIFS());
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
    public static void updateMetsOnFileAdd(MCRDerivate derivate, MCRFile file) throws Exception {
        Document mets = getCurrentMets(derivate);
        if (mets == null) {
            LOGGER.info("Derivate with id \"" + derivate.getId().toString() + "\" has no mets file. Nothing to do");
            return;
        }
        mets = MCRMetsSave.updateOnFileAdd(mets, file);
        if (mets != null)
            MCRMetsSave.saveMets(mets, derivate.getId().toString());
    }

    /**
     * @param derivate
     * @return
     * @throws JDOMException
     * @throws IOException
     */
    private static Document getCurrentMets(MCRDerivate derivate) throws JDOMException, IOException {
        String mf = MCRConfiguration.instance().getString("MCR.Mets.Filename", "mets.xml");
        MCRFilesystemNode metsDocNode = derivate.receiveDirectoryFromIFS().getChild(mf);
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
            String fileId = FileGrp.PREFIX_MASTER + uuid;

            /* add to file section "use=master" */
            org.mycore.mets.model.files.File f = new org.mycore.mets.model.files.File(fileId, MimetypesFileTypeMap.getDefaultFileTypeMap()
                    .getContentType(new File(file.getName())));
            FLocat fLocat = new FLocat(FLocat.LOCTYPE_URL, file.getName());
            f.setFLocat(fLocat);

            // alter the mets document
            XPath xp = XPath.newInstance("mets:mets/mets:fileSec/mets:fileGrp");
            Element fileSec = (Element) xp.selectSingleNode(mets);
            fileSec.addContent(f.asElement());

            /* add to structMap physical */
            xp = XPath.newInstance("mets:mets/mets:structMap[@TYPE='PHYSICAL']/mets:div[@TYPE='physSequence']/mets:div[last()]/@ORDER");
            Attribute orderAttribute = (Attribute) xp.selectSingleNode(mets);
            PhysicalSubDiv div = new PhysicalSubDiv(PhysicalSubDiv.ID_PREFIX + fileId, PhysicalSubDiv.TYPE_PAGE,
                    orderAttribute.getIntValue() + 1);
            div.add(new Fptr(fileId));

            // actually alter the mets document
            xp = XPath.newInstance("mets:mets/mets:structMap[@TYPE='PHYSICAL']/mets:div[@TYPE='physSequence']");
            Element structMapPhys = (Element) xp.selectSingleNode(mets);
            structMapPhys.addContent(div.asElement());

            /* add to structLink */
            xp = XPath.newInstance("mets:mets/mets:structMap[@TYPE='LOGICAL']/mets:div/@ID");
            Attribute idAttribute = (Attribute) xp.selectSingleNode(mets);
            String rootID = idAttribute.getValue();

            xp = XPath.newInstance("mets:mets/mets:structLink");
            Element structLink = (Element) xp.selectSingleNode(mets);

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
    public static void updateMetsOnFileDelete(MCRDerivate derivate, MCRFile file) throws Exception {
        Document mets = getCurrentMets(derivate);
        if (mets == null) {
            LOGGER.info("Derivate with id \"" + derivate.getId().toString() + "\" has no mets file. Nothing to do");
            return;
        }
        mets = MCRMetsSave.updateOnFileDelete(mets, file);
        if (mets != null)
            MCRMetsSave.saveMets(mets, derivate.getId().toString());
    }

    /**
     * @param mets
     * @param file
     * @return
     */
    @SuppressWarnings("unchecked")
    private static Document updateOnFileDelete(Document mets, MCRFile file) {
        try {
            // remove file from mets:fileSec/mets:fileGrp
            XPath xp = XPath.newInstance("mets:mets/mets:fileSec/mets:fileGrp[@USE='MASTER']/mets:file/mets:FLocat[@xlink:href='"
                    + file.getAbsolutePath().substring(1) + "']/..");
            xp.addNamespace(MCRConstants.getStandardNamespace("mets"));
            xp.addNamespace(MCRConstants.getStandardNamespace("xlink"));

            Object obj = xp.selectSingleNode(mets);
            if (!(obj instanceof Element)) {
                return null;
            }
            Element metsFile = (Element) obj;
            metsFile.detach();

            String fileId = metsFile.getAttributeValue("ID");
            if (fileId == null) {
                LOGGER.error("The file id could not be found.");
                return null;
            }

            // remove file from mets:mets/mets:structMap[@TYPE='PHYSICAL']
            xp = XPath.newInstance("mets:mets/mets:structMap[@TYPE='PHYSICAL']/mets:div/mets:div/mets:fptr[@FILEID='" + fileId + "']/..");
            xp.addNamespace(MCRConstants.getStandardNamespace("mets"));

            obj = xp.selectSingleNode(mets);
            if (!(obj instanceof Element)) {
                LOGGER.error("Could not determine mets:div node with id " + fileId);
                return null;
            }
            Element metsDiv = (Element) obj;
            metsDiv.detach();

            //remove links in mets:structLink section
            String physMasterID = metsDiv.getAttributeValue("ID");
            if (physMasterID == null) {
                LOGGER.error("Could not get id for mets:div");
                return null;
            }
            xp = XPath.newInstance("mets:mets/mets:structLink/mets:smLink[@xlink:to='" + physMasterID + "']");
            xp.addNamespace(MCRConstants.getStandardNamespace("mets"));
            xp.addNamespace(MCRConstants.getStandardNamespace("xlink"));

            List<Element> selectNodes = xp.selectNodes(mets);
            List<String> listOfLogIds = new Vector<String>();

            for (Element element : selectNodes) {
                element.detach();
                listOfLogIds.add(element.getAttributeValue("from", MCRConstants.getStandardNamespace("xlink")));
            }

            modifyLogicalStructMap(mets, listOfLogIds);

        } catch (Exception ex) {
            LOGGER.error("Error occured while removing file " + file.getAbsolutePath() + " from the existing mets file", ex);
            return null;
        }

        return mets;
    }

    /**
     * @param mets
     * @param logicalIDs
     * @return
     * @throws Exception
     */
    private static Document modifyLogicalStructMap(Document mets, List<String> logicalIDs) throws Exception {
        // check if the structure which contained the deleted file is empty now
        for (String logId : logicalIDs) {
            if (!exists(mets, logId)) {
                LOGGER.info("No files are mapped to logical id " + logId);
                // does the logical div has children
                XPath xp = XPath.newInstance("mets:mets/mets:structMap[@TYPE='LOGICAL']/mets:div//mets:div[@ID='" + logId + "']");
                xp.addNamespace(MCRConstants.getStandardNamespace("mets"));

                Object obj = xp.selectSingleNode(mets);
                if (!(obj instanceof Element)) {
                    LOGGER.info("No mets:div was found for logical id " + logId);
                    continue;
                }
                Element logDiv = (Element) obj;

                // handle children
                if (logDiv.getChildren().size() > 0) {
                    handleChildren(mets, logDiv);
                }

                // handle parent
                xp = XPath.newInstance("mets:mets/mets:structMap[@TYPE='LOGICAL']/mets:div[1]");
                xp.addNamespace(MCRConstants.getStandardNamespace("mets"));

                Element logDivContainer = (Element) xp.selectSingleNode(mets);

                handleParents(mets, logDiv, logDivContainer);

                if (logDiv.getChildren().size() == 0) {
                    LOGGER.info("No children can be found for mets:div with id " + logId);
                    LOGGER.info("Removing mets:div with id " + logId + " from mets:structMap TYPE=\"LOGICAL\"");
                    logDiv.detach();
                }
            }
        }

        return mets;
    }

    /**
     * @param mets
     * @param logDiv
     * @param logDivContainer
     * @throws Exception
     */
    private static void handleParents(Document mets, Element logDiv, Element logDivContainer) throws Exception {
        Element parent = logDiv.getParentElement();

        String logIdOfParent = parent.getAttributeValue("ID");
        if (exists(mets, logIdOfParent)) {
            return;
        }
        //no files associated to the current log id, but the parent might have some
        if (parent.getParentElement() == logDivContainer) {
            parent.detach();
            return;
        } else {
            handleParents(mets, parent, logDivContainer);
        }
    }

    /**
     * @param mets
     * @param logDiv
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private static void handleChildren(Document mets, Element logDiv) throws Exception {
        List<Element> children = logDiv.getChildren();

        for (Element child : children) {
            String logIdOfChild = child.getAttributeValue("ID");
            if (exists(mets, logIdOfChild)) {
                return;
            }
            //no files associated to the current log id, but the children might have some
            if (child.getChildren().size() == 0) {
                child.detach();
                return;
            } else {
                handleChildren(mets, child);
            }
        }
    }

    /**
     * Checks whether there is a smLink under the given locical id.
     */
    @SuppressWarnings("unchecked")
    private static boolean exists(Document mets, String logicalId) throws Exception {
        XPath p = XPath.newInstance("mets:mets/mets:structLink/mets:smLink[@xlink:from='" + logicalId + "']");
        p.addNamespace(MCRConstants.getStandardNamespace("mets"));
        p.addNamespace(MCRConstants.getStandardNamespace("xlink"));

        List<Element> smLinks = p.selectNodes(mets);
        return smLinks.size() < 1 ? false : true;
    }

    /**
     * Updates the mets.xml belonging to the given derivate. Adds the file to
     * the mets document (updates file sections and stuff within the mets.xml)
     * 
     * @param derivate
     *            the derivate owning the mets.xml and the given file
     * @param filename
     *            file to add to the mets.xml
     * @throws Exception
     */
    @Deprecated
    public static void updateMetsOnFileAdd(MCRDerivate derivate, String filename) throws Exception {
        Document mets = getCurrentMets(derivate);
        if (mets == null) {
            LOGGER.info("Derivate with id \"" + derivate.getId().toString() + "\" has no mets file. Nothing to do");
            return;
        }
        mets = MCRMetsSave.updateOnFileAdd(mets, filename);
        if (mets != null)
            MCRMetsSave.saveMets(mets, derivate.getId().toString());
    }

    /**
     * Alters the mets file
     * 
     * @param mets
     *            the unmodified source
     * @param filename
     *            the file to add
     * @return the modified mets or null if an exception occures
     */
    @Deprecated
    private static Document updateOnFileAdd(Document mets, String filename) {
        try {
            UUID uuid = UUID.randomUUID();
            String fileId = FileGrp.PREFIX_MASTER + uuid;

            /* add to file section "use=master" */
            org.mycore.mets.model.files.File f = new org.mycore.mets.model.files.File(fileId, MimetypesFileTypeMap.getDefaultFileTypeMap()
                    .getContentType(new File(filename)));
            FLocat fLocat = new FLocat(FLocat.LOCTYPE_URL, filename);
            f.setFLocat(fLocat);

            // alter the mets document
            XPath xp = XPath.newInstance("mets:mets/mets:fileSec/mets:fileGrp");
            Element fileSec = (Element) xp.selectSingleNode(mets);
            fileSec.addContent(f.asElement());

            /* add to structMap physical */
            xp = XPath.newInstance("mets:mets/mets:structMap[@TYPE='PHYSICAL']/mets:div[@TYPE='physSequence']/mets:div[last()]/@ORDER");
            Attribute orderAttribute = (Attribute) xp.selectSingleNode(mets);
            PhysicalSubDiv div = new PhysicalSubDiv(PhysicalSubDiv.ID_PREFIX + fileId, PhysicalSubDiv.TYPE_PAGE,
                    orderAttribute.getIntValue() + 1);
            div.add(new Fptr(fileId));

            // actually alter the mets document
            xp = XPath.newInstance("mets:mets/mets:structMap[@TYPE='PHYSICAL']/mets:div[@TYPE='physSequence']");
            Element structMapPhys = (Element) xp.selectSingleNode(mets);
            structMapPhys.addContent(div.asElement());

            /* add to structLink */
            xp = XPath.newInstance("mets:mets/mets:structMap[@TYPE='LOGICAL']/mets:div/@ID");
            Attribute idAttribute = (Attribute) xp.selectSingleNode(mets);
            String rootID = idAttribute.getValue();

            xp = XPath.newInstance("mets:mets/mets:structLink");
            Element structLink = (Element) xp.selectSingleNode(mets);

            structLink.addContent((new SmLink(rootID, div.getId()).asElement()));

        } catch (Exception ex) {
            LOGGER.error("Error occured while adding file " + filename + " to the existing mets file", ex);
            return null;
        }

        return mets;
    }
}
