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

package org.mycore.mets.model;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRStreamUtils;
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
import org.mycore.mets.misc.DefaultStructLinkGenerator;
import org.mycore.mets.model.files.FLocat;
import org.mycore.mets.model.files.File;
import org.mycore.mets.model.files.FileGrp;
import org.mycore.mets.model.files.FileSec;
import org.mycore.mets.model.header.MetsHdr;
import org.mycore.mets.model.sections.AmdSec;
import org.mycore.mets.model.sections.DmdSec;
import org.mycore.mets.model.struct.AbstractDiv;
import org.mycore.mets.model.struct.Area;
import org.mycore.mets.model.struct.Fptr;
import org.mycore.mets.model.struct.LOCTYPE;
import org.mycore.mets.model.struct.LogicalDiv;
import org.mycore.mets.model.struct.LogicalStructMap;
import org.mycore.mets.model.struct.PhysicalDiv;
import org.mycore.mets.model.struct.PhysicalStructMap;
import org.mycore.mets.model.struct.PhysicalSubDiv;
import org.mycore.mets.model.struct.Seq;
import org.mycore.mets.model.struct.StructLink;
import org.mycore.mets.tools.MCRMetsSave;

/**
 * This class generates a METS xml file for the METS-Editor. In difference to the default
 * implementation, the hierarchy of the MCRObjects and their derivate links are considered.
 * Starting from the root element, all children are hierarchically recorded in the logical
 * structure map of the METS file. If your application supports derivate links, the struct link
 * part links to those files.
 *
 * @author Matthias Eichner
 */
public abstract class MCRMETSHierarchyGenerator extends MCRMETSAbstractGenerator {

    private static final Logger LOGGER = LogManager.getLogger();

    protected MCRDerivate mcrDer;

    protected MCRObject rootObj;

    protected MetsHdr metsHdr;

    protected AmdSec amdSection;

    protected DmdSec dmdSection;

    protected FileSec fileSection;

    protected PhysicalStructMap physicalStructMap;

    protected LogicalStructMap logicalStructMap;

    protected StructLink structLink;

    protected List<FileRef> files;

    /**
     * Hashmap to store logical and physical ids.
     */
    protected Map<LogicalDiv, List<PhysicalSubDiv>> structLinkMap;

    public MCRMETSHierarchyGenerator() {
        this.files = new ArrayList<>();
    }

    @Override
    public synchronized Mets generate() throws MCRException {
        long startTime = System.currentTimeMillis();
        String derivateId = getOwner();
        setup(derivateId);
        try {
            Mets mets = createMets();
            LOGGER.info("mets creation for derivate {} took {}ms!",
                () -> derivateId, () -> System.currentTimeMillis() - startTime);
            return mets;
        } catch (Exception exc) {
            throw new MCRException("Unable to create mets.xml of " + derivateId, exc);
        }
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
     * @return the new created mets
     * @throws IOException files of the path couldn't be read
     */
    protected Mets createMets() throws IOException {
        LOGGER.info("create mets for derivate {} …", mcrDer::getId);

        this.structLinkMap = new HashMap<>();

        // create mets sections
        this.metsHdr = createMetsHdr();
        this.amdSection = createAmdSection();
        this.dmdSection = createDmdSection();
        this.fileSection = createFileSection();
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
        String amdId = "amd_" + this.mcrDer.getId();
        return new AmdSec(amdId);
    }

    /**
     * Creates a new empty dmd section. Id is dmd_{derivate id}.
     *
     * @return generated dmd section.
     */
    protected DmdSec createDmdSection() {
        String dmdSec = "dmd_" + this.mcrDer.getId();
        return new DmdSec(dmdSec);
    }

    /**
     * Creates the file section.
     *
     * @return generated file secion.
     */
    protected FileSec createFileSection() throws IOException {
        FileSec fileSec = new FileSec();

        List<MCRPath> filePaths = MCRMetsSave.listFiles(getDerivatePath(), getIgnorePaths());
        List<FileGrp> fileGrps = MCRMetsSave.buildFileGroups(filePaths);
        fileGrps.forEach(fileSec::addFileGrp);

        for (MCRPath file : filePaths) {
            String contentType = MCRContentTypes.probeContentType(file);
            FileRef ref = buildFileRef(file, contentType);
            this.files.add(ref);
        }

        for (FileRef ref : this.files) {
            String use = MCRMetsModelHelper.getUseForHref(ref.getPath().getOwnerRelativePath()).orElse("UNKNOWN");
            FileGrp fileGrp = fileGrps.stream().filter(grp -> grp.getUse().equals(use)).findFirst().orElse(null);
            if (fileGrp == null) {
                LOGGER.warn(
                    "Unable to add file '{}' because cannot find corresponding group  with @USE='{}'. "
                        + "Ignore file and continue.",
                    ref::toFileId, () -> use);
                continue;
            }
            addFile(ref, fileGrp);
        }

        return fileSec;
    }

    protected void addFile(FileRef fileRef, FileGrp fileGroup) {
        File imageFile = new File(fileRef.toFileId(fileGroup), fileRef.getContentType());
        try {
            final String href = fileRef.toFileHref(fileGroup);
            FLocat fLocat = new FLocat(LOCTYPE.URL, href);
            imageFile.setFLocat(fLocat);
            fileGroup.addFile(imageFile);
        } catch (Exception exc) {
            LOGGER.error(() -> "invalid href for " + fileRef.getPath(), exc);
        }
    }

    /**
     * This method creates the physical structure map.
     *
     * @return generated pyhiscal struct map secion.
     */
    protected PhysicalStructMap createPhysicalStruct() {
        PhysicalStructMap pstr = new PhysicalStructMap();
        PhysicalDiv physicalDiv = new PhysicalDiv("phys_" + this.mcrDer.getId(), PhysicalDiv.TYPE_PHYS_SEQ);
        pstr.setDivContainer(physicalDiv);
        // run through file references
        for (FileRef ref : this.files) {
            String physId = ref.toPhysId();
            PhysicalSubDiv page = physicalDiv.get(physId);
            if (page == null) {
                page = new PhysicalSubDiv(physId, PhysicalSubDiv.TYPE_PAGE);
                getOrderLabel(ref.toFileId()).ifPresent(page::setOrderLabel);
                physicalDiv.add(page);
            }
            page.add(new Fptr(ref.toFileId()));
        }
        return pstr;
    }

    /**
     * Returns the order label for the given file.
     *
     * @param fileId of the mets:file in the mets:fileSec
     * @return optional order label
     */
    protected Optional<String> getOrderLabel(String fileId) {
        return getOldMets().map(oldMets -> {
            PhysicalSubDiv subDiv = oldMets.getPhysicalStructMap().getDivContainer().byFileId(fileId);
            if (subDiv == null) {
                LOGGER.warn("Unable to get @ORDERLABEL of physical div '{}'.", fileId);
                return null;
            }
            return subDiv.getOrderLabel();
        });
    }

    /**
     * Creates the logical struct map.
     *
     * @return a newly created logical struct map
     */
    protected LogicalStructMap createLogicalStruct() {
        LogicalStructMap lsm = newLogicalStructMap();
        mergeOldLogicalStructMap(lsm);
        return lsm;
    }

    protected LogicalStructMap newLogicalStructMap() {
        LogicalStructMap lstr = new LogicalStructMap();
        MCRObjectID objId = this.rootObj.getId();
        // create main div
        String amdId = this.amdSection.getId();
        String dmdId = this.dmdSection.getId();
        LogicalDiv logicalDiv = new LogicalDiv(objId.toString(), getType(this.rootObj), getLabel(this.rootObj), amdId,
            dmdId);
        lstr.setDivContainer(logicalDiv);
        // run through all children
        newLogicalStructMap(this.rootObj, logicalDiv);
        return lstr;
    }

    /**
     * Creates the logical structure recursive.
     *
     * @param parentObject mycore object
     * @param parentLogicalDiv parent div
     */
    protected void newLogicalStructMap(MCRObject parentObject, LogicalDiv parentLogicalDiv) {
        // run through all children
        List<MCRObject> children = getChildren(parentObject);
        children.forEach(childObject -> {
            // create new logical sub div
            String id = childObject.getId().toString();
            LogicalDiv logicalChildDiv = new LogicalDiv(id, getType(childObject), getLabel(childObject));
            // add to parent
            parentLogicalDiv.add(logicalChildDiv);
            // check if a derivate link exists and get the linked file
            FileGrp masterGroup = this.fileSection.getFileGroup(FileGrp.USE_MASTER);
            updateStructLinkMapUsingDerivateLinks(logicalChildDiv, childObject, masterGroup);
            // do recursive call for children
            newLogicalStructMap(childObject, logicalChildDiv);
        });
    }

    /**
     * Runs through the logical part of the old mets and copies the ALTO part (mets:fptr/mets:seq/mets:area)
     * to the newly created logical struct map. This is done by comparing the mets:div @ID's of the old and the new
     * logical struct map. If two @ID's are equal, we can assume that it is the same mets:div and we just copy all
     * the old mets:fptr's.
     *
     * @param logicalStructMap the logical struct map to enhance
     */
    protected void mergeOldLogicalStructMap(LogicalStructMap logicalStructMap) {
        if (!this.getOldMets().isPresent()) {
            return;
        }
        Mets oldMets = this.getOldMets().get();
        LogicalStructMap oldLsm = oldMets.getLogicalStructMap();
        FileGrp oldAltoGroup = oldMets.getFileSec().getFileGroup("ALTO");
        FileGrp newAltoGroup = this.fileSection.getFileGroup("ALTO");

        List<LogicalDiv> descendants = oldLsm.getDivContainer().getDescendants();
        descendants.stream().filter(div -> !div.getFptrList().isEmpty()).forEach(oldDiv -> {
            String id = oldDiv.getId();
            LogicalDiv newDiv = logicalStructMap.getDivContainer().getLogicalSubDiv(id);
            if (newDiv != null) {
                for (Fptr fptr : oldDiv.getFptrList()) {
                    copyFptr(oldAltoGroup, newAltoGroup, fptr).ifPresent(newFptr -> newDiv.getFptrList().add(newFptr));
                }
                updateStructLinkMapUsingALTO(newDiv);
            } else {
                LOGGER.warn(
                    "Unable to find logical div @ID='{}' of previous mets.xml in this generated mets.xml. "
                        + "Be aware that the content of the 'old' logical div cannot be copied and therefore cannot "
                        + "be preserved!",
                    id);
            }
        });
    }

    private Optional<Fptr> copyFptr(FileGrp oldGrp, FileGrp newGrp, Fptr oldFptr) {
        Fptr newFptr = new Fptr();
        for (Seq oldSeq : oldFptr.getSeqList()) {
            Seq newSeq = new Seq();
            for (Area oldArea : oldSeq.getAreaList()) {
                if (oldArea.getBetype() == null) {
                    continue;
                }
                String oldFileID = oldArea.getFileId();
                File oldFile = oldGrp.getFileById(oldFileID);
                String href = oldFile.getFLocat().getHref();
                File newFile = newGrp.getFileByHref(href);

                Area newArea = new Area();
                newArea.setBegin(oldArea.getBegin());
                newArea.setEnd(oldArea.getEnd());
                newArea.setFileId(newFile.getId());
                newArea.setBetype("IDREF");
                newSeq.getAreaList().add(newArea);
            }
            if (!newSeq.getAreaList().isEmpty()) {
                newFptr.getSeqList().add(newSeq);
            }
        }
        return newFptr.getSeqList().isEmpty() ? Optional.empty() : Optional.of(newFptr);
    }

    /**
     * Fills the structLinkMap for a single logical mets:div using derivate link information.
     *
     * @param logicalDiv the logical div to handle
     * @param mcrObject the mycore object linked in the logical div (mets:div/@ID == mycore object id)
     * @param group the group to search the corresponding File ID. this is usally the MASTER group
     */
    protected void updateStructLinkMapUsingDerivateLinks(LogicalDiv logicalDiv, MCRObject mcrObject, FileGrp group) {
        // get the derivate link
        Optional<String> linkedFileOptional = getLinkedFile(mcrObject);
        // resolve the derivate link using the FileRef API
        Optional<String> resolvedFileHref = linkedFileOptional.map(linkedFile -> {
            FileRef tempRef = buildFileRef(MCRPath.ofPath(getDerivatePath().resolve(linkedFile)), null);
            return tempRef.toFileHref(group);
        });
        // add to struct link map
        resolvedFileHref.flatMap(linkedFile -> this.getFileId(linkedFile, group)).ifPresent(fileId -> {
            PhysicalSubDiv physicalDiv = getPhysicalDiv(fileId);
            addToStructLinkMap(logicalDiv, physicalDiv);
        });
    }

    /**
     * Fills the structLinkMap for a single logical mets:div using mets:area/@FILEID information.
     *
     * @param logicalDiv the logical div to handle
     */
    protected void updateStructLinkMapUsingALTO(final LogicalDiv logicalDiv) {
        logicalDiv.getFptrList()
            .stream()
            .flatMap(fptr -> fptr.getSeqList().stream())
            .flatMap(seq -> seq.getAreaList().stream())
            .map(Area::getFileId)
            .map(this::getPhysicalDiv)
            .forEach(physicalDiv -> addToStructLinkMap(logicalDiv, physicalDiv));
    }

    /**
     * Adds the physical div to the logical div. Required to build the mets:structLink section.
     *
     * @param from logical div
     * @param to physical div
     */
    protected void addToStructLinkMap(LogicalDiv from, PhysicalSubDiv to) {
        if (from == null || to == null) {
            return;
        }
        List<PhysicalSubDiv> physicalSubDivs = this.structLinkMap.getOrDefault(from, new ArrayList<>());
        physicalSubDivs.add(to);
        physicalSubDivs = physicalSubDivs.stream().filter(MCRStreamUtils.distinctByKey(AbstractDiv::getId))
            .sorted((div1, div2) -> {
                if (div1.getOrder() != null && div2.getOrder() != null) {
                    return div1.getOrder().compareTo(div2.getOrder());
                }
                return div1.getId().compareTo(div2.getId());
            }).collect(Collectors.toList());
        this.structLinkMap.put(from, physicalSubDivs);
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
     * Creates the mets:structLink part of the mets.xml
     *
     * @return a newly generated StructLink.
     */
    protected StructLink createStructLink() {
        DefaultStructLinkGenerator structLinkGenerator = new DefaultStructLinkGenerator(this.structLinkMap);
        structLinkGenerator.setFailEasy(false);
        return structLinkGenerator.generate(this.physicalStructMap, this.logicalStructMap);
    }

    /**
     * Runs through all files in the given FileGrp and tries to find the corresponding mets:file @ID.
     *
     * @param group the file group
     * @param uriEncodedLinkedFile the file to find
     * @return the fileSec @ID
     */
    private Optional<String> getFileId(String uriEncodedLinkedFile, FileGrp group) {
        return group.getFileList().stream().filter(file -> {
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
     * @return finds a physical div by the given file id
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

    protected FileRef buildFileRef(MCRPath path, String contentType) {
        return new FileRefImpl(path, contentType);
    }

    public interface FileRef {

        MCRPath getPath();

        String getContentType();

        default String toFileId() {
            return this.toFileId(null);
        }

        String toFileId(FileGrp fileGroup);

        String toFileHref(FileGrp fileGroup);

        String toPhysId();

    }

    public static class FileRefImpl implements FileRef {

        private MCRPath path;

        private String contentType;

        FileRefImpl(MCRPath path, String contentType) {
            this.path = path;
            this.contentType = contentType;
        }

        @Override
        public MCRPath getPath() {
            return this.path;
        }

        @Override
        public String getContentType() {
            return this.contentType;
        }

        @Override
        public String toFileId(FileGrp fileGroup) {
            return MCRMetsSave.getFileId(this.path);
        }

        @Override
        public String toFileHref(FileGrp fileGroup) {
            String path = getPath().getOwnerRelativePath().substring(1);
            try {
                return MCRXMLFunctions.encodeURIPath(path,
                    true);
            } catch (URISyntaxException exc) {
                throw new MCRException("Unable to encode " + path, exc);
            }
        }

        @Override
        public String toPhysId() {
            return PhysicalSubDiv.ID_PREFIX + MCRMetsSave.getFileBase(this.path);
        }

    }

}
