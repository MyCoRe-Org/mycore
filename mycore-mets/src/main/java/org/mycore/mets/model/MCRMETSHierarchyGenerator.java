package org.mycore.mets.model;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaDerivateLink;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectUtils;
import org.mycore.datamodel.niofs.MCRContentTypes;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.mets.model.files.FLocat;
import org.mycore.mets.model.files.File;
import org.mycore.mets.model.files.FileGrp;
import org.mycore.mets.model.files.FileSec;
import org.mycore.mets.model.header.MetsHdr;
import org.mycore.mets.model.sections.AmdSec;
import org.mycore.mets.model.sections.DmdSec;
import org.mycore.mets.model.struct.Fptr;
import org.mycore.mets.model.struct.LOCTYPE;
import org.mycore.mets.model.struct.LogicalDiv;
import org.mycore.mets.model.struct.LogicalStructMap;
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

    private static final Logger LOGGER = LogManager.getLogger(MCRMETSHierarchyGenerator.class);

    protected MCRDerivate mcrDer;

    protected MCRObject rootObj;

    protected MetsHdr metsHdr;

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
    private Map<String, List<String>> structLinkMap;

    @Override
    public synchronized Mets getMETS(MCRPath dir, Set<MCRPath> ignoreNodes) throws IOException {
        long startTime = System.currentTimeMillis();
        String derivateId = dir.getOwner();
        setup(derivateId);
        Mets mets = createMets(dir, ignoreNodes);
        LOGGER.info(
            "mets creation for derivate " + derivateId + " took " + (System.currentTimeMillis() - startTime) + "ms!");
        return mets;
    }

    /**
     * Initializes the derivate and the root object.
     * 
     * @param derivateId the derivate id to setup
     */
    protected void setup(String derivateId) {
        // get derivate
        MCRObjectID derId = MCRObjectID.getInstance(derivateId);
        this.mcrDer = MCRMetadataManager.retrieveMCRDerivate(derId);
        // get mycore object
        MCRObjectID objId = this.mcrDer.getDerivate().getMetaLink().getXLinkHrefID();
        this.rootObj = MCRMetadataManager.retrieveMCRObject(objId);
    }

    /**
     * Does the mets creation.
     * 
     * @param dir
     * @param ignoreNodes
     * @return
     * @throws IOException
     */
    protected Mets createMets(MCRPath dir, Set<MCRPath> ignoreNodes) throws IOException {
        LOGGER.info("create mets for derivate " + this.mcrDer.getId().toString() + "...");

        this.structLinkMap = new HashMap<String, List<String>>();

        // create mets sections
        this.metsHdr = createMetsHdr();
        this.amdSection = createAmdSection();
        this.dmdSection = createDmdSection();
        this.fileSection = createFileSection(dir, ignoreNodes);
        this.physicalStructMap = createPhysicalStruct();
        this.logicalStructMap = createLogicalStruct();
        this.structLink = createStructLink();

        // add to mets
        Mets mets = new Mets();
        mets.setMetsHdr(metsHdr);
        mets.addAmdSec(this.amdSection);
        mets.addDmdSec(this.dmdSection);
        mets.setFileSec(this.fileSection);
        mets.addStructMap(this.physicalStructMap);
        mets.addStructMap(this.logicalStructMap);
        mets.setStructLink(this.structLink);
        return mets;
    }

    /**
     * Creates a new mets header with current dates and record status = autogenerated.
     * 
     * @return generated mets header section.
     */
    protected MetsHdr createMetsHdr() {
        MetsHdr hdr = new MetsHdr();
        hdr.setCreateDate(Instant.now());
        hdr.setLastModDate(Instant.now());
        hdr.setRecordStatus("autogenerated");
        return hdr;
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
    protected FileSec createFileSection(MCRPath dir, Set<MCRPath> ignoreNodes) throws IOException {
        FileSec fsec = new FileSec();
        FileGrp fgroup = new FileGrp(FileGrp.USE_MASTER);
        fsec.addFileGrp(fgroup);
        addFolder(fgroup, dir, ignoreNodes);
        return fsec;
    }

    private void addFolder(final FileGrp fgroup, MCRPath dir, Set<MCRPath> ignoreNodes) throws IOException {
        SortedMap<MCRPath, BasicFileAttributes> files = new TreeMap<>(), directories = new TreeMap<>();
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
            for (Path child : dirStream) {
                MCRPath path = MCRPath.toMCRPath(child);
                if (ignoreNodes.contains(path)) {
                    continue;
                }
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                if (attrs.isDirectory()) {
                    directories.put(path, attrs);
                } else {
                    String contentType = MCRContentTypes.probeContentType(path);
                    if (contentType.startsWith("image")) {
                        files.put(path, attrs);
                    }
                }
            }
        }
        for (Map.Entry<MCRPath, BasicFileAttributes> file : files.entrySet()) {
            final UUID uuid = UUID.randomUUID();
            final String fileID = File.PREFIX_MASTER + uuid.toString();
            final String mimeType = MCRContentTypes.probeContentType(file.getKey());
            File metsFile = new File(fileID, mimeType);
            // set fLocat
            try {
                final String href = MCRXMLFunctions.encodeURIPath(file.getKey().getOwnerRelativePath().substring(1),
                    true);
                FLocat fLocat = new FLocat(LOCTYPE.URL, href);
                metsFile.setFLocat(fLocat);
            } catch (URISyntaxException uriSyntaxException) {
                LOGGER.error("invalid href", uriSyntaxException);
            }
            fgroup.addFile(metsFile);
        }
        for (Map.Entry<MCRPath, BasicFileAttributes> directory : directories.entrySet()) {
            addFolder(fgroup, directory.getKey(), ignoreNodes);
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
        for (File file : fList) {
            String fileId = file.getId();
            // add page
            PhysicalSubDiv page = new PhysicalSubDiv(PhysicalSubDiv.ID_PREFIX + fileId, PhysicalSubDiv.TYPE_PAGE);
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
        String amdId = this.amdSection.getId();
        String dmdId = this.dmdSection.getId();
        LogicalDiv logicalDiv = new LogicalDiv(objId.toString(), getType(this.rootObj), getLabel(this.rootObj), amdId,
            dmdId);
        lstr.setDivContainer(logicalDiv);
        // run through all children
        createLogicalStruct(this.rootObj, logicalDiv);
        // remove not linked logical divs
        Iterator<LogicalDiv> it = logicalDiv.getChildren().iterator();
        while (it.hasNext()) {
            LogicalDiv child = it.next();
            if (!validateLogicalStruct(logicalDiv, child))
                it.remove();
        }
        return lstr;
    }

    /**
     * Creates the logical structure recursive. 
     * 
     * @param parentObject mycore object
     * @param parentLogicalDiv
     */
    private void createLogicalStruct(MCRObject parentObject, LogicalDiv parentLogicalDiv) {
        // run through all children
        List<MCRObject> children = getChildren(parentObject);
        for (int i = 0; i < children.size(); i++) {
            MCRObject childObject = children.get(i);
            // create new logical sub div
            String id = childObject.getId().toString();
            LogicalDiv logicalChildDiv = new LogicalDiv(id, getType(childObject), getLabel(childObject));
            // add to parent
            parentLogicalDiv.add(logicalChildDiv);
            // check if a derivate link exists and get the linked file
            Optional<String> linkedFileOptional = getLinkedFile(childObject);
            linkedFileOptional.flatMap(linkedFile -> getFileId(linkedFile)).ifPresent(fileId -> {
                PhysicalSubDiv physicalDiv = getPhysicalDiv(fileId);
                if (physicalDiv != null) {
                    String physicalDivId = physicalDiv.getId();
                    List<String> logChildDivIDs = this.structLinkMap.get(physicalDivId);
                    if (logChildDivIDs == null) {
                        logChildDivIDs = new ArrayList<>();
                    }
                    logChildDivIDs.add(logicalChildDiv.getId());
                    this.structLinkMap.put(physicalDivId, logChildDivIDs);
                }
            });
            // do recursive call for children
            createLogicalStruct(childObject, logicalChildDiv);
        }
    }

    /**
     * Returns all children id's of this MCRObject.
     * 
     * @param parentObject the mycore object
     */
    protected List<MCRObject> getChildren(MCRObject parentObject) {
        return MCRObjectUtils.getChildren(parentObject);
    }

    /**
     * Its important to remove not linked logical divs without children to
     * get a valid logical structure.
     * 
     * @param parent
     * @param logicalDiv
     * @return
     */
    private boolean validateLogicalStruct(LogicalDiv parent, LogicalDiv logicalDiv) {
        // has link
        String logicalDivId = logicalDiv.getId();
        for (List<String> logivalDivIDs : structLinkMap.values()) {
            if (logivalDivIDs.contains(logicalDivId)) {
                return true;
            }
        }
        // has children with link
        Iterator<LogicalDiv> it = logicalDiv.getChildren().iterator();
        while (it.hasNext()) {
            LogicalDiv child = it.next();
            if (validateLogicalStruct(logicalDiv, child))
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
        for (PhysicalSubDiv physLink : subDivList) {
            List<String> logicalIdList = structLinkMap.get(physLink.getId());
            if (logicalIdList != null) {
                for (String logicalId : logicalIdList) {
                    currentLogicalDivId = logicalId;
                    structLink.addSmLink(new SmLink(currentLogicalDivId, physLink.getId()));
                }
            } else {
                structLink.addSmLink(new SmLink(currentLogicalDivId, physLink.getId()));
            }
        }
        return structLink;
    }

    /**
     * Runs through all USE="MASTER" files and tries to find the corresponding
     * mets:file @ID.
     * 
     * @param uriEncodedLinkedFile the file to find
     * @return the fileSec @ID
     */
    private Optional<String> getFileId(String uriEncodedLinkedFile) {
        FileGrp masterGroup = this.fileSection.getFileGroup(FileGrp.USE_MASTER);
        return masterGroup.getFileList().stream().filter(file -> {
            String href = file.getFLocat().getHref();
            boolean equals = href.equals(uriEncodedLinkedFile);
            boolean equalsWithoutSlash = uriEncodedLinkedFile.startsWith("/")
                && href.equals(uriEncodedLinkedFile.substring(1));
            return equals || equalsWithoutSlash;
        }).map(File::getId).findFirst();
    }

    /**
     * Returns a physical sub div by the given fileId.
     * 
     * @param fileId id of a file element in fileGrp
     * @return
     */
    private PhysicalSubDiv getPhysicalDiv(String fileId) {
        if (fileId == null) {
            return null;
        }
        PhysicalDiv mainDiv = this.physicalStructMap.getDivContainer();
        return mainDiv.getChildren()
            .stream()
            .filter(subDiv -> Objects.nonNull(subDiv.getFptr(fileId)))
            .findAny()
            .orElse(null);
    }

    /**
     * Returns the URI encoded file path of the first derivate link.
     * 
     * @param mcrObj object which contains the derivate link
     */
    protected Optional<String> getLinkedFile(MCRObject mcrObj) {
        MCRMetaElement me = mcrObj.getMetadata().getMetadataElement(getEnclosingDerivateLinkName());
        // no derivate link
        if (me == null) {
            return Optional.empty();
        }
        return StreamSupport.stream(me.spliterator(), false)
            .filter(metaInterface -> metaInterface instanceof MCRMetaDerivateLink)
            .map(MCRMetaDerivateLink.class::cast)
            .filter(link -> this.mcrDer.getId().equals(MCRObjectID.getInstance(link.getOwner())))
            .map(MCRMetaDerivateLink::getRawPath)
            .findFirst();
    }

    /**
     * Type attribute used in logical structure. Something like journal, article,
     * book...
     */
    protected abstract String getType(MCRObject obj);

    /**
     * Returns the label of an object. Used in logical structure.
     */
    protected abstract String getLabel(MCRObject obj);

    /**
     * Enclosing name of the derivate link element.
     * In journals this is 'derivateLinks', in archive 'def.derivateLink'.
     */
    protected abstract String getEnclosingDerivateLinkName();

    /**
     * Name of the derivate link element. E.g. 'derivateLink'.
     */
    protected abstract String getDerivateLinkName();

}
