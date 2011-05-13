package org.mycore.mets.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

import javax.activation.MimetypesFileTypeMap;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.mycore.common.MCRConfiguration;
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
     *            the file to add to the mets.xml
     * @throws Exception
     */
    public static void update(MCRDerivate derivate, String file) throws Exception {
        String mf = MCRConfiguration.instance().getString("MCR.Mets.Filename", "mets.xml");
        MCRFilesystemNode metsDocNode = derivate.receiveDirectoryFromIFS().getChild(mf);
        if (!(metsDocNode instanceof MCRFile)) {
            LOGGER.info("Derivate with id \"" + derivate.getId().toString() + "\" has no mets file. Nothing to do");
            return;
        }
        Document mets = new SAXBuilder().build(((MCRFile) metsDocNode).getContentAsInputStream());
        mets = MCRMetsSave.updateMetsDocument(mets, file);
        if (mets != null)
            MCRMetsSave.saveMets(mets, derivate.getId().toString());
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
    private static Document updateMetsDocument(Document mets, String file) {
        try {
            UUID uuid = UUID.randomUUID();
            String fileId = FileGrp.PREFIX_MASTER + uuid;

            /* add to file section "use=master" */
            org.mycore.mets.model.files.File f = new org.mycore.mets.model.files.File(fileId, MimetypesFileTypeMap.getDefaultFileTypeMap()
                    .getContentType(new File(file)));
            FLocat fLocat = new FLocat(FLocat.LOCTYPE_URL, file);
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
            LOGGER.error("Error occured while adding file " + file + " to the existing mets file", ex);
            return null;
        }

        return mets;
    }
}
