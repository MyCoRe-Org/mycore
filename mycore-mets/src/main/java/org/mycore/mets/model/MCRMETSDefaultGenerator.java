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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.log4j.Logger;
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
import org.mycore.mets.tools.MCRMetsSave;

/**
 * @author Thomas Scheffler (yagee)
 * @author Matthias Eichner
 * @author Sebastian Hofmann
 */
public class MCRMETSDefaultGenerator extends MCRMETSGenerator {

    private static final String TRANSCRIPTION = "transcription";

    private static final String TRANSLATION = "translation";

    private static final Logger LOGGER = Logger.getLogger(MCRMETSGenerator.class);

    private static final List<String> EXCLUDED_ROOT_FOLDERS = Arrays.asList(new String[] { "alto", "tei" });

    private HashMap<String, String> hrefIdMap = new HashMap<String, String>();

    private enum FileStatus {
        TRANSLATION, TRANSCRIPTION, ALTO, DEFAULT
    }

    public Mets getMETS(MCRPath dir, Set<MCRPath> ignoreNodes) throws IOException {
        Mets mets = new Mets();
        createMets(dir, ignoreNodes, mets);

        MCRDerivate owner = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(dir.getOwner()));
        Map<String, String> urnFileMap = owner.getUrnMap();
        if (urnFileMap.size() > 0) {
            try {
                MCRMetsSave.updateURNsInMetsDocument(mets, urnFileMap);
            } catch (Exception e) {
                LOGGER.error("error while adding urn´s to new Mets file", e);
            }
        }
        return mets;
    }

    private void createMets(MCRPath dir, Set<MCRPath> ignoreNodes, Mets mets) throws IOException {
        // add dmdsec
        DmdSec dmdSec = new DmdSec("dmd_" + dir.getOwner());
        // add amdsec
        AmdSec amdSec = new AmdSec("amd_" + dir.getOwner());
        // file sec
        FileSec fileSec = new FileSec();
        FileGrp fileGrp = new FileGrp(FileGrp.USE_MASTER);
        FileGrp transcriptionGrp = new FileGrp("TRANSCRIPTION");
        FileGrp translationGrp = new FileGrp("TRANSLATION");
        FileGrp altoGrp = new FileGrp("ALTO");
        fileSec.addFileGrp(fileGrp);
        fileSec.addFileGrp(altoGrp);
        fileSec.addFileGrp(transcriptionGrp);
        fileSec.addFileGrp(translationGrp);

        // physical structure
        PhysicalStructMap physicalStructMap = new PhysicalStructMap();
        PhysicalDiv physicalDiv = new PhysicalDiv("phys_dmd_" + dir.getOwner(), "physSequence");
        physicalStructMap.setDivContainer(physicalDiv);

        // logical structure
        MCRILogicalStructMapTypeProvider typeProvider = getTypeProvider();
        LogicalStructMap logicalStructMap = new LogicalStructMap();

        LogicalDiv logicalDiv = new LogicalDiv("log_" + dir.getOwner(), typeProvider.getType(MCRObjectID
            .getInstance(dir.getOwner())), dir.getOwner(), 1, amdSec.getId(), dmdSec.getId());
        logicalDiv.setDmdId(dmdSec.getId());
        logicalStructMap.setDivContainer(logicalDiv);
        // struct Link
        StructLink structLink = new StructLink();

        // create internal structure
        structureMets(dir, ignoreNodes, fileGrp, altoGrp, transcriptionGrp, translationGrp, physicalDiv, logicalDiv,
            structLink, 0);
        hrefIdMap.clear();
        sortByOrder(physicalStructMap.getDivContainer());

        // add to mets
        mets.addDmdSec(dmdSec);
        mets.addAmdSec(amdSec);
        mets.setFileSec(fileSec);
        mets.addStructMap(physicalStructMap);
        mets.addStructMap(logicalStructMap);
        mets.setStructLink(structLink);
    }

    private void structureMets(MCRPath dir, Set<MCRPath> ignoreNodes, FileGrp fileGrp, FileGrp altoGrp,
        FileGrp transcriptionGrp, FileGrp translationGrp, PhysicalDiv physicalDiv, AbstractLogicalDiv logicalDiv,
        StructLink structLink, int logOrder) throws IOException {
        SortedMap<MCRPath, BasicFileAttributes> files = new TreeMap<>(), directories = new TreeMap<>();

        fillFileMap(ignoreNodes, files, directories, dir);

        for (Map.Entry<MCRPath, BasicFileAttributes> file : files.entrySet()) {
            final UUID fileUUID = UUID.randomUUID();
            final UUID physUUID = UUID.randomUUID();
            final String physicalID = "phys_" + physUUID.toString();
            String fileID = "master_" + fileUUID.toString();
            String fileName = "";

            try {
                final String href = MCRXMLFunctions.encodeURIPath(file.getKey().getOwnerRelativePath().substring(1));
                int beginIndex = href.lastIndexOf("/") == -1 ? 0 : href.lastIndexOf("/") + 1;
                int endIndex = (href.lastIndexOf(".") == -1 || href.lastIndexOf(".") <= beginIndex) ? href.length()
                    : href.lastIndexOf(".");
                fileName = href.substring(beginIndex, endIndex);
                LOGGER.debug("Created href2: " + fileName);

                if (!(hrefIdMap.containsKey(fileName) || hrefIdMap.containsValue(physUUID.toString())
                    && isExcludedRootFolder(dir))) {
                    hrefIdMap.put(fileName, physUUID.toString());
                }

                FileStatus fStatus = getFileStatus(href);

                switch (fStatus) {
                    case TRANSLATION:
                        fileID = TRANSLATION + "_" + fileUUID.toString();
                        break;
                    case TRANSCRIPTION:
                        fileID = TRANSCRIPTION + "_" + fileUUID.toString();
                        break;
                    case ALTO:
                        fileID = "alto_" + fileUUID.toString();
                    default:
                        break;
                }

                //files
                sortFileToGrp(fileGrp, altoGrp, transcriptionGrp, translationGrp, file, fileID, href, fStatus);
            } catch (URISyntaxException uriSyntaxException) {
                LOGGER.error("invalid href", uriSyntaxException);
                continue;
            }

            // physical
            buldPhysDivs(dir, physicalDiv, fileID, physicalID, fileName);

            // struct link
            if (!(isExcludedRootFolder(dir) || isExcludedRootFolder(dir.getParent()))) {
                SmLink smLink = new SmLink(logicalDiv.getId(), physicalID);
                structLink.addSmLink(smLink);
            }
        }
        for (Map.Entry<MCRPath, BasicFileAttributes> directory : directories.entrySet()) {
            String dirName = directory.getKey().getFileName().toString();

            if (isExcludedRootFolder(directory.getKey()) || isExcludedRootFolder(directory.getKey().getParent())) {
                structureMets(directory.getKey(), ignoreNodes, fileGrp, altoGrp, transcriptionGrp, translationGrp,
                    physicalDiv, logicalDiv, structLink, logOrder);
            } else {
                LogicalSubDiv section = new LogicalSubDiv("log_" + Integer.toString(++logOrder), "section", dirName,
                    logOrder);
                logicalDiv.add(section);
                structureMets(directory.getKey(), ignoreNodes, fileGrp, altoGrp, transcriptionGrp, translationGrp,
                    physicalDiv, section, structLink, logOrder);
            }
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

    private void buldPhysDivs(MCRPath dir, PhysicalDiv physicalDiv, String fileID, final String physicalID,
        String fileName) {
        if (!fileName.isEmpty() && hrefIdMap.containsKey(fileName)
            && (isExcludedRootFolder(dir) || isExcludedRootFolder(dir.getParent()))) {
            for (PhysicalSubDiv physSubDiv : physicalDiv.getChildren()) {
                if (physSubDiv.getId().contains(hrefIdMap.get(fileName))) {
                    physicalDiv.remove(physSubDiv);
                    Fptr fptr = new Fptr(fileID);
                    physSubDiv.add(fptr);
                    physicalDiv.add(physSubDiv);
                }
            }
        } else {
            PhysicalSubDiv pyhsicalPage = new PhysicalSubDiv(physicalID, "page", physicalDiv.getChildren().size() + 1);
            Fptr fptr = new Fptr(fileID);
            pyhsicalPage.add(fptr);
            physicalDiv.add(pyhsicalPage);
        }
    }

    private void sortFileToGrp(FileGrp fileGrp, FileGrp altoGrp, FileGrp transcriptionGrp, FileGrp translationGrp,
        Map.Entry<MCRPath, BasicFileAttributes> file, String fileID, final String href, FileStatus fStatus)
        throws IOException {
        // file
        File metsFile = new File(fileID, MCRContentTypes.probeContentType(file.getKey()));
        FLocat fLocat = new FLocat(LOCTYPE.URL, href);
        metsFile.setFLocat(fLocat);

        switch (fStatus) {
            case TRANSLATION:
                translationGrp.addFile(metsFile);
                break;
            case TRANSCRIPTION:
                transcriptionGrp.addFile(metsFile);
                break;
            case ALTO:
                altoGrp.addFile(metsFile);
                break;
            default:
                fileGrp.addFile(metsFile);
        }
    }

    private FileStatus getFileStatus(final String href) {
        if (href.startsWith("tei/" + TRANSLATION))
            return FileStatus.TRANSLATION;
        if (href.startsWith("tei/" + TRANSCRIPTION))
            return FileStatus.TRANSCRIPTION;
        if (href.startsWith("alto"))
            return FileStatus.ALTO;
        return FileStatus.DEFAULT;
    }

    private void sortByOrder(PhysicalDiv physicalDiv) {
        List<PhysicalSubDiv> sortedSubDivs = new ArrayList<PhysicalSubDiv>(physicalDiv.getChildren());
        for (PhysicalSubDiv physSubDiv : physicalDiv.getChildren()) {
            sortedSubDivs.set(physSubDiv.getOrder() - 1, physSubDiv);
            physicalDiv.remove(physSubDiv);
        }
        for (PhysicalSubDiv physicalSubDiv : sortedSubDivs) {
            physicalDiv.add(physicalSubDiv);
        }
    }

    /**
     * Checks if a root directory should be included in mets.xml
     *
     * @param directory
     * @return true if the directory should be excluded
     */
    private boolean isExcludedRootFolder(MCRPath directory) {
        String fileName = directory == null || directory.getFileName() == null ? "" : directory.getFileName()
            .toString();
        boolean isExcluded = MCRMETSDefaultGenerator.EXCLUDED_ROOT_FOLDERS.contains(fileName);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format(Locale.ROOT, "%s excluded : %s", fileName, isExcluded));
        }

        return isExcluded;
    }

    private MCRILogicalStructMapTypeProvider getTypeProvider() {
        String className = MCRConfiguration.instance().getString("MCR.Component.MetsMods.LogicalStructMapTypeProvider",
            MCRDefaultLogicalStructMapTypeProvider.class.getName());

        MCRILogicalStructMapTypeProvider typeProvider = null;
        try {
            typeProvider = (MCRILogicalStructMapTypeProvider) Class.forName(className).newInstance();
        } catch (Exception e) {
            LOGGER.warn("Could not load class " + className);
            return new MCRDefaultLogicalStructMapTypeProvider();
        }

        return typeProvider;
    }
}
