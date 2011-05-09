package org.mycore.mets.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
import org.mycore.datamodel.metadata.MCRMetaInterface;
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
import org.mycore.mets.model.struct.Div;
import org.mycore.mets.model.struct.Fptr;
import org.mycore.mets.model.struct.LogicalStructMap;
import org.mycore.mets.model.struct.PhysicalStructMap;
import org.mycore.mets.model.struct.SmLink;
import org.mycore.mets.model.struct.StructLink;
import org.mycore.mets.model.struct.SubDiv;

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
     * Hashmap to store logical and physical divs. An entry is added
     * for each derivate link.
     */
    protected HashMap<SubDiv, SubDiv> structLinkMap;

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

        // create mets sections
        this.structLinkMap = new HashMap<SubDiv, SubDiv>();
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
        mets.setPysicalStructMap(this.physicalStructMap);
        mets.setLogicalStructMap(this.logicalStructMap);
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
                continue;
            } else if (node instanceof MCRDirectory) {
                MCRDirectory subDir = (MCRDirectory) node;
                addFolder(fgroup, subDir, ignoreNodes);
            } else {
                MCRFile subFile = (MCRFile) node;
                // create new file
                final UUID uuid = UUID.randomUUID();
                final String fileID = FileGrp.PREFIX_MASTER + uuid.toString();
                final String mimeType = MCRFileContentTypeFactory.getType(subFile.getContentTypeID()).getMimeType();
                File file = new File(fileID, mimeType);
                // set fLocat
                try {
                    final String href = new URI(null, subFile.getAbsolutePath().substring(1), null).toString();
                    FLocat fLocat = new FLocat(FLocat.LOCTYPE_URL, href);
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
        Div mainDiv = new Div("phys_" + this.mcrDer.getId().toString(), Div.TYPE_PHYS_SEQ);
        pstr.setDivContainer(mainDiv);
        // run through files
        FileGrp masterGroup = this.fileSection.getFileGroup(FileGrp.USE_MASTER);
        List<File> fList = masterGroup.getfList();
        int order = 1;
        for(File file : fList) {
            String fileId = file.getId();
            // add page
            SubDiv page = new SubDiv(SubDiv.ID_PREFIX + fileId, SubDiv.TYPE_PAGE, order++, true);
            mainDiv.addSubDiv(page);
            // add file pointer
            Fptr fptr = new Fptr(fileId);
            page.addFptr(fptr);
        }
        return pstr;
    }

    protected LogicalStructMap createLogicalStruct() {
        LogicalStructMap lstr = new LogicalStructMap();
        MCRObjectID derId = this.mcrDer.getId();
        // create main div
        String id = "log_" + derId.toString();
        String amdId = this.dmdSection.getId();
        String dmdId = this.dmdSection.getId();
        Div mainDiv = new Div(id, dmdId, amdId, getType(this.rootObj), getLabel(this.rootObj));
        mainDiv.setOrder(1);
        lstr.setDivContainer(mainDiv);

        // run through all children
        List<MCRMetaLinkID> links = this.rootObj.getStructure().getChildren();
        for(int i = 0; i < links.size(); i++) {
            MCRMetaLinkID linkId = links.get(i);
            SubDiv subDiv = createLogicalStruct(linkId.getXLinkHrefID(), derId, i + 1);
            mainDiv.addSubDiv(subDiv);
        }
        return lstr;
    }

    private SubDiv createLogicalStruct(MCRObjectID mcrId, MCRObjectID derId, int order) {
        // create new logical sub div
        MCRObject mcrObj = MCRMetadataManager.retrieveMCRObject(mcrId);
        String id = "log_" + mcrId.toString();
        SubDiv logicalDiv = new SubDiv(id, getType(mcrObj), order, getLabel(mcrObj));
        // check if a derivate link exists and get the linked file
        String linkedFile = getLinkedFile(derId, mcrObj);
        if(linkedFile != null) {
            String fileId = getFileId(linkedFile);
            if(fileId != null) {
                SubDiv physicalDiv = getPhysicalDiv(fileId);
                if(physicalDiv != null) {
                    // add physical and logical div to hashmap
                    // map is used in structLink
                    structLinkMap.put(physicalDiv, logicalDiv);
                }
            }
        }
        // run through all children
        List<MCRMetaLinkID> links = mcrObj.getStructure().getChildren();
        for(int i = 0; i < links.size(); i++) {
            MCRMetaLinkID linkId = links.get(i);
            SubDiv subDiv = createLogicalStruct(linkId.getXLinkHrefID(), derId, i + 1);
            logicalDiv.addLogicalDiv(subDiv);
        }
        return logicalDiv;
    }

    private String getFileId(String linkedFile) {
        if(linkedFile == null)
            return null;
        FileGrp masterGroup = this.fileSection.getFileGroup(FileGrp.USE_MASTER);
        for(File f : masterGroup.getfList())
            if(linkedFile.equals(f.getFLocat().getHref()))
                return f.getId();
        return null;
    }

    private SubDiv getPhysicalDiv(String fileId) {
        if(fileId == null)
            return null;
        Div mainDiv = this.physicalStructMap.getDivContainer();
        for(SubDiv subDiv : mainDiv.getSubDivList())
            if(subDiv.getId().contains(fileId))
                return subDiv;
        return null;
    }

    protected StructLink createStructLink() {
        StructLink structLink = new StructLink();
        SubDiv currentLogicalDiv = logicalStructMap.getDivContainer().asLogicalSubDiv();
        Div mainDiv = this.physicalStructMap.getDivContainer();
        List<SubDiv> subDivList = mainDiv.getSubDivList();
        for(SubDiv physLink : subDivList) {
            SubDiv newLogicalDiv = structLinkMap.get(physLink);
            if(newLogicalDiv != null) {
                currentLogicalDiv = newLogicalDiv;
            }
            SmLink smLink = new SmLink(currentLogicalDiv, physLink);
            structLink.addSmLink(smLink);
        }
        return structLink;
    }

    protected String getLinkedFile(MCRObjectID derId, MCRObject mcrObj) {
        MCRMetaElement me = mcrObj.getMetadata().getMetadataElement(getEnclosingDerivateLinkName());
        // no derivate link
        if(me == null)
            return null;
        Iterator<MCRMetaInterface> it = me.iterator();
        while(it.hasNext()) {
            // return if its no derivate
            MCRMetaInterface mi = it.next();
            if(!(mi instanceof MCRMetaDerivateLink))
                continue;
            // return if the href is null
            String href = ((MCRMetaDerivateLink)mi).getXLinkHref();
            if(href == null)
                continue;
            // return if the href doesn't contain the split slash
            int indexOfSlash = href.indexOf("/");
            if(indexOfSlash == -1)
                continue;
            // return if its the wrong derivate
            String derIdAsString = href.substring(0, indexOfSlash);
            if(!derId.equals(MCRObjectID.getInstance(derIdAsString)))
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
