package org.mycore.mets.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileContentTypeFactory;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaDerivateLink;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mets.model.files.FLocat;
import org.mycore.mets.model.files.File;
import org.mycore.mets.model.files.FileGrp;
import org.mycore.mets.model.files.FileSec;
import org.mycore.mets.model.sections.AmdSec;
import org.mycore.mets.model.sections.DmdSec;
import org.mycore.mets.model.struct.AbstractLogicalDiv;
import org.mycore.mets.model.struct.Fptr;
import org.mycore.mets.model.struct.LOCTYPE;
import org.mycore.mets.model.struct.LogicalDiv;
import org.mycore.mets.model.struct.LogicalStructMap;
import org.mycore.mets.model.struct.LogicalSubDiv;
import org.mycore.mets.model.struct.PhysicalDiv;
import org.mycore.mets.model.struct.PhysicalStructMap;
import org.mycore.mets.model.struct.PhysicalSubDiv;
import org.mycore.mets.model.struct.SmLink;
import org.mycore.mets.model.struct.StructLink;

/**
 * This class generates a METS xml file for the METS-Editor. In difference to the default
 * implementation, the hierarchy of the MCRObjects and their derivate links are considered.
 * Starting from the root element, all children are hierarchically recorded in the logical
 * structure map of the METS file. If your application supports derivate links, the struct link
 * part links to this files.
 * 
 * @author Matthias Eichner
 */
public abstract class MCRMETSHierarchyGenerator extends MCRMETSGenerator {

    private static final Logger LOGGER = Logger.getLogger(MCRMETSHierarchyGenerator.class);

    protected MCRDerivate mcrDer;
    protected MCRObject rootObj;

    protected AmdSec amdSection;
    protected DmdSec dmdSection;
    protected FileSec fileSection;
    protected PhysicalStructMap physicalStructMap;
    protected LogicalStructMap logicalStructMap;
    protected StructLink structLink;

    /**
     * Hashmap to store logical and physical ids. An entry is added
     * for each derivate link.
     */
    private Map<String, String> structLinkMap;
    
    @Override
    public synchronized Mets getMETS(MCRDirectory dir, Set<MCRFilesystemNode> ignoreNodes) {
        long startTime = System.currentTimeMillis();        
        // get derivate
        MCRObjectID derId = MCRObjectID.getInstance(dir.getOwnerID());
        this.mcrDer = MCRMetadataManager.retrieveMCRDerivate(derId);
        // get mycore object
        MCRObjectID objId = this.mcrDer.getDerivate().getMetaLink().getXLinkHrefID();
        this.rootObj = MCRMetadataManager.retrieveMCRObject(objId);

        LOGGER.info("create mets for derivate " + derId.toString() + "...");

        this.structLinkMap = new HashMap<String, String>();

        // create mets sections
        this.amdSection = createAmdSection();
        this.dmdSection = createDmdSection();
        this.fileSection = createFileSection(dir, ignoreNodes);
        this.physicalStructMap = createPhysicalStruct();
        this.logicalStructMap = createLogicalStruct();
        this.structLink = createStructLink();

        // add to mets
        Mets mets = new Mets();
        mets.addAmdSec(this.amdSection);
        mets.addDmdSec(this.dmdSection);
        mets.setFileSec(this.fileSection);
        mets.addStructMap(this.physicalStructMap);
        mets.addStructMap(this.logicalStructMap);
        mets.setStructLink(this.structLink);

        LOGGER.info("mets creation for derivate " + derId.toString() + " took " +
                    (System.currentTimeMillis() - startTime) +  "ms!");

        return mets;
    }

    /**
     * Creates a new empty amd section. Id is amd_{derivate id}.
     * 
     * @return generated amd section.
     */
    protected AmdSec createAmdSection() {
        String amdId = "amd_" + this.mcrDer.getId().toString();
        return new AmdSec(amdId);
    }

    /**
     * Creates a new empty dmd section. Id is dmd_{derivate id}.
     * 
     * @return generated dmd section.
     */
    protected DmdSec createDmdSection() {
        String dmdSec = "dmd_" + this.mcrDer.getId().toString();
        return new DmdSec(dmdSec);
    }

    /**
     * This method runs recursive through the directories
     * and add each file to the file section.
     * 
     * @param dir the root directoy
     * @param ignoreNodes nodes to ignored
     * @return generated file secion.
     */
    protected FileSec createFileSection(MCRDirectory dir, Set<MCRFilesystemNode> ignoreNodes) {
        FileSec fsec = new FileSec();
        FileGrp fgroup = new FileGrp(FileGrp.USE_MASTER);
        fsec.addFileGrp(fgroup);
        addFolder(fgroup, dir, ignoreNodes);
        return fsec;
    }

    private void addFolder(FileGrp fgroup, MCRDirectory dir, Set<MCRFilesystemNode> ignoreNodes) {
        MCRFilesystemNode[] children = dir.getChildren(MCRDirectory.SORT_BY_NAME_IGNORECASE);
        for (MCRFilesystemNode node : children) {
            if (ignoreNodes.contains(node)) {
            } else if (node instanceof MCRDirectory) {
                MCRDirectory subDir = (MCRDirectory) node;
                addFolder(fgroup, subDir, ignoreNodes);
            } else {
                MCRFile subFile = (MCRFile) node;
                // create new file
                final UUID uuid = UUID.randomUUID();
                final String fileID = File.PREFIX_MASTER + uuid.toString();
                final String mimeType = MCRFileContentTypeFactory.getType(subFile.getContentTypeID()).getMimeType();
                File file = new File(fileID, mimeType);
                // set fLocat
                try {
                    final String href = new URI(null, subFile.getAbsolutePath().substring(1), null).toString();
                    FLocat fLocat = new FLocat(LOCTYPE.URL, href);
                    file.setFLocat(fLocat);
                } catch(URISyntaxException uriSyntaxException) {
                    LOGGER.error("invalid href",uriSyntaxException);
                    continue;
                }
                fgroup.addFile(file);
            }
        }
    }

    /**
     * This method creates the physical structure map.
     * 
     * @return generated pyhiscal struct map secion.
     */
    protected PhysicalStructMap createPhysicalStruct() {
        PhysicalStructMap pstr = new PhysicalStructMap();
        // set main div
        PhysicalDiv physicalDiv = new PhysicalDiv("phys_" + this.mcrDer.getId().toString(), PhysicalDiv.TYPE_PHYS_SEQ);
        pstr.setDivContainer(physicalDiv);
        // run through files
        FileGrp masterGroup = this.fileSection.getFileGroup(FileGrp.USE_MASTER);
        List<File> fList = masterGroup.getFileList();
        int order = 1;
        for(File file : fList) {
            String fileId = file.getId();
            // add page
            PhysicalSubDiv page = new PhysicalSubDiv(PhysicalSubDiv.ID_PREFIX + fileId, PhysicalSubDiv.TYPE_PAGE, order++);
            physicalDiv.add(page);
            // add file pointer
            Fptr fptr = new Fptr(fileId);
            page.add(fptr);
        }
        return pstr;
    }

    protected LogicalStructMap createLogicalStruct() {
        LogicalStructMap lstr = new LogicalStructMap();
        MCRObjectID objId = this.rootObj.getId();
        // create main div
        String id = "log_" + objId.toString();
        String amdId = this.dmdSection.getId();
        String dmdId = this.amdSection.getId();
        LogicalDiv logicalDiv = new LogicalDiv(id, getType(this.rootObj), getLabel(this.rootObj), 1, amdId, dmdId);
        lstr.setDivContainer(logicalDiv);
        // run through all children
        createLogicalStruct(this.rootObj, logicalDiv);
        // remove not linked logical divs
        Iterator<LogicalSubDiv> it = logicalDiv.getChildren().iterator();
        while(it.hasNext()) {
            LogicalSubDiv child = it.next();
            if(!validateLogicalStruct(logicalDiv, child))
                it.remove();
        }
        return lstr;
    }

    /**
     * Creates the logical structure recursive. 
     * 
     * @param parentObject mycore object
     * @param derId 
     * @param parentLogicalDiv
     */
    private void createLogicalStruct(MCRObject parentObject, AbstractLogicalDiv parentLogicalDiv) {
        // run through all children
        List<MCRMetaLinkID> links = parentObject.getStructure().getChildren();
        for(int i = 0; i < links.size(); i++) {
            MCRMetaLinkID linkId = links.get(i);
            MCRObjectID childId = MCRObjectID.getInstance(linkId.getXLinkHref());
            MCRObject childObject = MCRMetadataManager.retrieveMCRObject(childId);
            // create new logical sub div
            String id = "log_" + childId.toString();
            LogicalSubDiv logicalChildDiv = new LogicalSubDiv(id, getType(childObject), getLabel(childObject), i+1);
            // add to parent
            parentLogicalDiv.add(logicalChildDiv);
            // check if a derivate link exists and get the linked file
            String linkedFile = getLinkedFile(childObject);
            if(linkedFile != null) {
                try {
                    String uriEncodedFile = new URI(null, linkedFile, null).toString();
                    String fileId = getFileId(uriEncodedFile);
                    if(fileId != null) {
                        PhysicalSubDiv physicalDiv = getPhysicalDiv(fileId);
                        if(physicalDiv != null) {
                            this.structLinkMap.put(physicalDiv.getId(), logicalChildDiv.getId());
                        }
                    }
                } catch(Exception exc) {
                    LOGGER.error("",exc);
                }
            }
            // do recursive call for children
            createLogicalStruct(childObject, logicalChildDiv);
        }
    }

    /**
     * Its important to remove not linked logical divs without children to
     * get a valid logical structure.
     * 
     * @param parent
     * @param logicalDiv
     * @return
     */
    private boolean validateLogicalStruct(AbstractLogicalDiv parent, LogicalSubDiv logicalDiv) {
        // has link
        if(this.structLinkMap.containsValue(logicalDiv.getId()))
            return true;
        // has children with link
        Iterator<LogicalSubDiv> it = logicalDiv.getChildren().iterator();
        while(it.hasNext()) {
            LogicalSubDiv child = it.next();
            if(validateLogicalStruct(logicalDiv, child))
                return true;
            // nothing -> delete it
            it.remove();
        }
        return false;
    }

    protected StructLink createStructLink() {
        StructLink structLink = new StructLink();
        String currentLogicalDivId = logicalStructMap.getDivContainer().getId();
        PhysicalDiv physicalDiv = this.physicalStructMap.getDivContainer();
        List<PhysicalSubDiv> subDivList = physicalDiv.getChildren();
        for(PhysicalSubDiv physLink : subDivList) {
            String logicalId = structLinkMap.get(physLink.getId());
            if(logicalId != null) {
                currentLogicalDivId = logicalId;
            }
            structLink.addSmLink(new SmLink(currentLogicalDivId, physLink.getId()));
        }
        return structLink;
    }

    private String getFileId(String uriEncodedLinkedFile) {
        if(uriEncodedLinkedFile == null)
            return null;
        FileGrp masterGroup = this.fileSection.getFileGroup(FileGrp.USE_MASTER);
        for(File f : masterGroup.getFileList())
            if(uriEncodedLinkedFile.equals(f.getFLocat().getHref()))
                return f.getId();
        return null;
    }

    /**
     * Returns a physical sub div by the given fileId.
     * 
     * @param fileId id of a file element in fileGrp
     * @return
     */
    private PhysicalSubDiv getPhysicalDiv(String fileId) {
        if(fileId == null)
            return null;
        PhysicalDiv mainDiv = this.physicalStructMap.getDivContainer();
        for(PhysicalSubDiv subDiv : mainDiv.getChildren())
            if(subDiv.getId().contains(fileId))
                return subDiv;
        return null;
    }

    /**
     * Returns the file name of a derivate link.
     * 
     * @param derId derivate which contains the file
     * @param mcrObj object which contains the derivate link
     * @return
     */
    protected String getLinkedFile(MCRObject mcrObj) {
        MCRMetaElement me = mcrObj.getMetadata().getMetadataElement(getEnclosingDerivateLinkName());
        // no derivate link
        if(me == null)
            return null;
        for (Object mi : me) {
            // return if its no derivate
            if (!(mi instanceof MCRMetaDerivateLink))
                continue;
            // return if the href is null
            String href = ((MCRMetaDerivateLink) mi).getXLinkHref();
            if (href == null)
                continue;
            // return if the href doesn't contain the split slash
            int indexOfSlash = href.indexOf("/");
            if (indexOfSlash == -1)
                continue;
            // return if its the wrong derivate
            String derIdAsString = href.substring(0, indexOfSlash);
            if (!this.mcrDer.getId().equals(MCRObjectID.getInstance(derIdAsString)))
                continue;
            // finally get the linked file
            return href.substring(indexOfSlash + 1);
        }
        return null;
    }

    /**
     * Type attribute used in logical structure. Something like journal, article,
     * book...
     * 
     * @param obj
     * @return
     */
    protected abstract String getType(MCRObject obj);

    /**
     * Returns the label of an object. Used in logical structure.
     * 
     * @param obj
     * @return
     */
    protected abstract String getLabel(MCRObject obj);

    /**
     * Enclosing name of the derivate link element.
     * In journals this is 'derivateLinks', in archive 'def.derivateLink'.
     * 
     * @return
     */
    protected abstract String getEnclosingDerivateLinkName();
    /**
     * Name of the derivate link element. E.g. 'derivateLink'.
     * 
     * @return
     */
    protected abstract String getDerivateLinkName();

}
