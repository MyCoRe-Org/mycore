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

    private static HashMap<String, String> HREF_ID_MAP = new HashMap<String, String>();

    public Mets getMETS(MCRPath dir, Set<MCRPath> ignoreNodes) throws IOException {
        // add dmdsec
        DmdSec dmdSec = new DmdSec("dmd_" + dir.getOwner());
        // add amdsec
        AmdSec amdSec = new AmdSec("amd_" + dir.getOwner());
        // file sec
        FileSec fileSec = new FileSec();
        FileGrp fileGrp = new FileGrp(FileGrp.USE_MASTER);
        FileGrp transcritionGrp = new FileGrp("TRANSCRIPTION");
        FileGrp translationGrp = new FileGrp("TRANSLATION");
        FileGrp altoGrp = new FileGrp("ALTO");
        fileSec.addFileGrp(fileGrp);
        fileSec.addFileGrp(altoGrp);
        fileSec.addFileGrp(transcritionGrp);
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

        // create
        createMets(dir, ignoreNodes, fileGrp, altoGrp, transcritionGrp, translationGrp, physicalDiv, logicalDiv,
            structLink, 0);

        // add to mets
        Mets mets = new Mets();
        mets.addDmdSec(dmdSec);
        mets.addAmdSec(amdSec);
        mets.setFileSec(fileSec);
        sortByOrder(physicalStructMap.getDivContainer());
        mets.addStructMap(physicalStructMap);
        mets.addStructMap(logicalStructMap);
        mets.setStructLink(structLink);

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

    private void createMets(MCRPath dir, Set<MCRPath> ignoreNodes, FileGrp fileGrp, FileGrp altoGrp,
        FileGrp transcritionGrp, FileGrp translationGrp, PhysicalDiv physicalDiv, AbstractLogicalDiv logicalDiv,
        StructLink structLink, int logOrder) throws IOException {
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
                    files.put(path, attrs);
                }
            }
        }
        for (Map.Entry<MCRPath, BasicFileAttributes> file : files.entrySet()) {
            final UUID uuid = UUID.randomUUID();
            final UUID uuid2 = UUID.randomUUID();
            String fileID = "master_" + uuid.toString();
            final String physicalID = "phys_" + uuid2.toString();
            String href2 = "";
            try {
                final String href = MCRXMLFunctions.encodeURIPath(file.getKey().getOwnerRelativePath().substring(1));
                int beginIndex = href.lastIndexOf("/") == -1 ? 0 : href.lastIndexOf("/") + 1;
                href2 = href.substring(beginIndex, href.length() - 4);
                LOGGER.debug("Created href2: " + href2);
                if (!(HREF_ID_MAP.containsKey(href2) || HREF_ID_MAP.containsValue(uuid2.toString())
                    && isExcludedRootFolder(dir))) {
                    HREF_ID_MAP.put(href2, uuid2.toString());
                }

                if (href.contains(TRANSLATION)) {
                    fileID = TRANSLATION + "_" + uuid.toString();
                } else if (href.contains(TRANSCRIPTION)) {
                    fileID = TRANSCRIPTION + "_" + uuid.toString();
                } else if (href.contains("alto")) {
                    fileID = "alto_" + uuid.toString();
                }

                // file
                File metsFile = new File(fileID, MCRContentTypes.probeContentType(file.getKey()));
                FLocat fLocat = new FLocat(LOCTYPE.URL, href);
                metsFile.setFLocat(fLocat);
                if (href.contains(TRANSLATION)) {
                    translationGrp.addFile(metsFile);
                } else if (href.contains(TRANSCRIPTION)) {
                    transcritionGrp.addFile(metsFile);
                } else if (href.contains("alto")) {
                    altoGrp.addFile(metsFile);
                } else {
                    fileGrp.addFile(metsFile);
                }
            } catch (URISyntaxException uriSyntaxException) {
                LOGGER.error("invalid href", uriSyntaxException);
                continue;
            }
            // physical
            if (!href2.isEmpty() && HREF_ID_MAP.containsKey(href2)
                && (isExcludedRootFolder(dir) || isExcludedRootFolder(dir.getParent()))) {
                for (PhysicalSubDiv physSubDiv : physicalDiv.getChildren()) {
                    if (physSubDiv.getId().contains(HREF_ID_MAP.get(href2))) {
                        physicalDiv.remove(physSubDiv);
                        Fptr fptr = new Fptr(fileID);
                        physSubDiv.add(fptr);
                        physicalDiv.add(physSubDiv);
                    }
                }
            } else {
                PhysicalSubDiv pyhsicalPage = new PhysicalSubDiv(physicalID, "page",
                    physicalDiv.getChildren().size() + 1);
                Fptr fptr = new Fptr(fileID);
                pyhsicalPage.add(fptr);
                physicalDiv.add(pyhsicalPage);
            }
            // struct link
            if (!(isExcludedRootFolder(dir) || isExcludedRootFolder(dir.getParent()))) {
            SmLink smLink = new SmLink(logicalDiv.getId(), physicalID);
                structLink.addSmLink(smLink);
            }
        }
        for (Map.Entry<MCRPath, BasicFileAttributes> directory : directories.entrySet()) {
            String dirName = directory.getKey().getFileName().toString();
            if (isExcludedRootFolder(directory.getKey()) || isExcludedRootFolder(directory.getKey().getParent())) {
                createMets(directory.getKey(), ignoreNodes, fileGrp, altoGrp, transcritionGrp, translationGrp,
                    physicalDiv, logicalDiv, structLink, logOrder);
            } else {
                LogicalSubDiv section = new LogicalSubDiv("log_" + Integer.toString(++logOrder), "section", dirName,
                    logOrder);
                logicalDiv.add(section);
                createMets(directory.getKey(), ignoreNodes, fileGrp, altoGrp, transcritionGrp, translationGrp,
                    physicalDiv, section, structLink, logOrder);
            }
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
