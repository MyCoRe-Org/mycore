/* $Revision$ 
 * $Date$ 
 * $LastChangedBy$
 * Copyright 2010 - Thüringer Universitäts- und Landesbibliothek Jena
 *  
 * Mets-Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mets-Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mets-Editor.  If not, see http://www.gnu.org/licenses/.
 */

package org.mycore.mets.model;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRContentTypes;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.mets.model.files.FLocat;
import org.mycore.mets.model.files.File;
import org.mycore.mets.model.files.FileGrp;
import org.mycore.mets.model.files.FileSec;
import org.mycore.mets.model.sections.AmdSec;
import org.mycore.mets.model.sections.DmdSec;
import org.mycore.mets.model.simple.MCRMetsFileUse;
import org.mycore.mets.model.struct.Fptr;
import org.mycore.mets.model.struct.LOCTYPE;
import org.mycore.mets.model.struct.LogicalDiv;
import org.mycore.mets.model.struct.LogicalStructMap;
import org.mycore.mets.model.struct.PhysicalDiv;
import org.mycore.mets.model.struct.PhysicalStructMap;
import org.mycore.mets.model.struct.PhysicalSubDiv;
import org.mycore.mets.model.struct.SmLink;
import org.mycore.mets.model.struct.StructLink;
import org.mycore.mets.tools.MCRMetsSave;
import org.mycore.services.i18n.MCRTranslation;

/**
 * @author Thomas Scheffler (yagee)
 * @author Matthias Eichner
 * @author Sebastian Hofmann
 * @author Sebastian Röher (basti890)
 */
public class MCRMETSDefaultGenerator extends MCRMETSAbstractGenerator {

    private static final Logger LOGGER = LogManager.getLogger(MCRMETSGenerator.class);

    private static final List<String> EXCLUDED_ROOT_FOLDERS = Arrays.asList("alto", "tei");

    private HashMap<String, String> hrefIdMap = new HashMap<>();

    @Override
    public Mets generate() throws MCRException {
        try {
            Mets mets = createMets();
            MCRDerivate owner = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(getOwner()));

            mets.getLogicalStructMap().getDivContainer().setLabel(
                MCRTranslation.exists("MCR.Mets.LogicalStructMap.Default.Label")
                    ? MCRTranslation.translate("MCR.Mets.LogicalStructMap.Default.Label")
                    : owner.getId().toString());

            Map<String, String> urnFileMap = owner.getUrnMap();
            if (urnFileMap.size() > 0) {
                try {
                    MCRMetsSave.updateURNsInMetsDocument(mets, urnFileMap);
                } catch (Exception e) {
                    LOGGER.error("error while adding urn´s to new Mets file", e);
                }
            }
            return mets;
        } catch (Exception ioExc) {
            throw new MCRException("Unable to create mets.xml of " + getOwner(), ioExc);
        }
    }

    private Mets createMets() throws IOException {
        Mets mets = new Mets();
        String owner = getOwner();

        // add dmdsec
        DmdSec dmdSec = new DmdSec("dmd_" + owner);
        // add amdsec
        AmdSec amdSec = new AmdSec("amd_" + owner);
        // file sec
        FileSec fileSec = new FileSec();
        for (MCRMetsFileUse fileUse : MCRMetsFileUse.values()) {
            FileGrp fileGrp = new FileGrp(fileUse.toString());
            fileSec.addFileGrp(fileGrp);
        }

        // physical structure
        PhysicalStructMap physicalStructMap = new PhysicalStructMap();
        PhysicalDiv physicalDiv = new PhysicalDiv("phys_" + owner, "physSequence");
        physicalStructMap.setDivContainer(physicalDiv);

        // logical structure
        MCRILogicalStructMapTypeProvider typeProvider = getTypeProvider();
        LogicalStructMap logicalStructMap = new LogicalStructMap();

        LogicalDiv logicalDiv = new LogicalDiv("log_" + owner, typeProvider.getType(MCRObjectID.getInstance(owner)),
            owner, amdSec.getId(), dmdSec.getId());
        logicalDiv.setDmdId(dmdSec.getId());
        logicalStructMap.setDivContainer(logicalDiv);
        // struct Link
        StructLink structLink = new StructLink();

        // create internal structure
        structureMets(getDerivatePath(), getIgnorePaths(), fileSec, physicalDiv, logicalDiv, structLink, 0);
        hrefIdMap.clear();

        // add to mets
        mets.addDmdSec(dmdSec);
        mets.addAmdSec(amdSec);
        mets.setFileSec(fileSec);
        mets.addStructMap(physicalStructMap);
        mets.addStructMap(logicalStructMap);
        mets.setStructLink(structLink);

        return mets;
    }

    private void structureMets(MCRPath dir, Set<MCRPath> ignoreNodes, FileSec fileSec, PhysicalDiv physicalDiv,
        LogicalDiv logicalDiv, StructLink structLink, int logOrder) throws IOException {
        SortedMap<MCRPath, BasicFileAttributes> files = new TreeMap<>(), directories = new TreeMap<>();

        fillFileMap(ignoreNodes, files, directories, dir);

        for (Map.Entry<MCRPath, BasicFileAttributes> file : files.entrySet()) {
            createStructure(dir, fileSec, physicalDiv, logicalDiv, structLink, file);
        }
        for (Map.Entry<MCRPath, BasicFileAttributes> directory : directories.entrySet()) {
            String dirName = directory.getKey().getFileName().toString();
            if (isInExcludedRootFolder(directory.getKey())) {
                structureMets(directory.getKey(), ignoreNodes, fileSec, physicalDiv, logicalDiv, structLink, logOrder);
            } else {
                LogicalDiv section = new LogicalDiv("log_" + Integer.toString(++logOrder), "section", dirName);
                logicalDiv.add(section);
                structureMets(directory.getKey(), ignoreNodes, fileSec, physicalDiv, section, structLink, logOrder);
            }
        }
    }

    private void createStructure(MCRPath dir, FileSec fileSec, PhysicalDiv physicalDiv, LogicalDiv logicalDiv,
        StructLink structLink, Map.Entry<MCRPath, BasicFileAttributes> file) throws IOException {
        String baseID = MCRMetsSave.getFileBase(file.getKey());
        final String physicalID = "phys_" + baseID;
        final String href;
        String path = file.getKey().getOwnerRelativePath().substring(1);
        try {
            href = MCRXMLFunctions.encodeURIPath(path, true);
        } catch (URISyntaxException uriSyntaxException) {
            LOGGER.error("invalid href {}", path, uriSyntaxException);
            return;
        }
        int beginIndex = href.lastIndexOf("/") == -1 ? 0 : href.lastIndexOf("/") + 1;
        int endIndex = (href.lastIndexOf(".") == -1 || href.lastIndexOf(".") <= beginIndex) ?
                href.length() :
                href.lastIndexOf(".");
        String fileName = href.substring(beginIndex, endIndex);
        LOGGER.debug("Created fileName: {}", fileName);

        if (!(hrefIdMap.containsKey(fileName) || hrefIdMap.containsValue(baseID)
            && isInExcludedRootFolder(dir))) {
            hrefIdMap.put(fileName, baseID);
        }

        //files
        MCRMetsFileUse fileUse = MCRMetsFileUse.get(href);
        String fileID = MCRMetsFileUse.getIdPrefix(href) + "_" + baseID;
        sortFileToGrp(fileSec, file, fileID, href, fileUse);

        // physical
        buildPhysDivs(dir, physicalDiv, fileID, physicalID, fileName);

        // struct link
        if (!isInExcludedRootFolder(dir)) {
            SmLink smLink = new SmLink(logicalDiv.getId(), physicalID);
            structLink.addSmLink(smLink);
        }
    }

    private void fillFileMap(Set<MCRPath> ignoreNodes, SortedMap<MCRPath, BasicFileAttributes> files,
        SortedMap<MCRPath, BasicFileAttributes> directories, Path dir) throws IOException {
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
                    files.put(path, attrs);
                }
            }
        }
    }

    private void buildPhysDivs(MCRPath dir, PhysicalDiv physicalDiv, String fileID, final String physicalID,
        String fileName) {
        if (!fileName.isEmpty() && hrefIdMap.containsKey(fileName) && isInExcludedRootFolder(dir)) {
            for (PhysicalSubDiv physSubDiv : physicalDiv.getChildren()) {
                if (physSubDiv.getId().contains(hrefIdMap.get(fileName))) {
                    physSubDiv.add(new Fptr(fileID));
                }
            }
        } else {
            PhysicalSubDiv pyhsicalPage = new PhysicalSubDiv(physicalID, "page");
            Fptr fptr = new Fptr(fileID);
            pyhsicalPage.add(fptr);
            physicalDiv.add(pyhsicalPage);
        }
    }

    private void sortFileToGrp(FileSec fileSec, Map.Entry<MCRPath, BasicFileAttributes> file, String fileID,
            final String href, MCRMetsFileUse fileUse) throws IOException {
        // file
        File metsFile = new File(fileID, MCRContentTypes.probeContentType(file.getKey()));
        FLocat fLocat = new FLocat(LOCTYPE.URL, href);
        metsFile.setFLocat(fLocat);

        for (FileGrp fileGrp : fileSec.getFileGroups()) {
            if (fileGrp.getUse().equalsIgnoreCase(fileUse.toString())) {
                fileGrp.addFile(metsFile);
            }
        }
    }

    /**
     * Checks if a root directory should be included in mets.xml
     *
     * @param directory the directory to check
     * @return true if the directory should be excluded
     */
    private boolean isInExcludedRootFolder(MCRPath directory) {
        for (String excludedRoot : EXCLUDED_ROOT_FOLDERS) {
            String path = directory.toString().substring(directory.toString().indexOf(":/") + 2);
            if (path.startsWith(excludedRoot))
                return true;
        }
        return false;
    }

    private MCRILogicalStructMapTypeProvider getTypeProvider() {
        try {
            return MCRConfiguration.instance().getClass("MCR.Component.MetsMods.LogicalStructMapTypeProvider",
                MCRDefaultLogicalStructMapTypeProvider.class).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            LOGGER.warn("Could not load class", e);
            return new MCRDefaultLogicalStructMapTypeProvider();
        }
    }

}
