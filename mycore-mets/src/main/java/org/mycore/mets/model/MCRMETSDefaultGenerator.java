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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileContentTypeFactory;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.mets.model.files.FLocat;
import org.mycore.mets.model.files.File;
import org.mycore.mets.model.files.FileGrp;
import org.mycore.mets.model.files.FileSec;
import org.mycore.mets.model.sections.AmdSec;
import org.mycore.mets.model.sections.DmdSec;
import org.mycore.mets.model.struct.AbstractLogicalDiv;
import org.mycore.mets.model.struct.Fptr;
import org.mycore.mets.model.struct.LogicalDiv;
import org.mycore.mets.model.struct.LogicalStructMap;
import org.mycore.mets.model.struct.LogicalSubDiv;
import org.mycore.mets.model.struct.PhysicalDiv;
import org.mycore.mets.model.struct.PhysicalStructMap;
import org.mycore.mets.model.struct.PhysicalSubDiv;
import org.mycore.mets.model.struct.SmLink;
import org.mycore.mets.model.struct.StructLink;

/**
 * 
 * @author Thomas Scheffler (yagee)
 * @author Matthias Eichner
 */
public class MCRMETSDefaultGenerator extends MCRMETSGenerator {

    private static final Logger LOGGER = Logger.getLogger(MCRMETSGenerator.class);

    public Mets getMETS(MCRDirectory dir, Set<MCRFilesystemNode> ignoreNodes) {
        // add dmdsec
        DmdSec dmdSec = new DmdSec("dmd_" + dir.getOwnerID());
        // add amdsec
        AmdSec amdSec = new AmdSec("amd_" + dir.getOwnerID());
        // file sec
        FileSec fileSec = new FileSec();
        FileGrp fileGrp = new FileGrp(FileGrp.USE_MASTER);
        fileSec.addFileGrp(fileGrp);
        // physical structure
        PhysicalStructMap physicalStructMap = new PhysicalStructMap();
        PhysicalDiv physicalDiv = new PhysicalDiv("phys_dmd_" + dir.getOwnerID(), "physSequence");
        physicalStructMap.setDivContainer(physicalDiv);
        // logical structure
        LogicalStructMap logicalStructMap = new LogicalStructMap();
        LogicalDiv logicalDiv = new LogicalDiv("log_" + dir.getOwnerID(), "monograph", dir.getOwnerID(), 1, amdSec.getId(), dmdSec.getId());
        logicalDiv.setDmdId(dmdSec.getId());
        logicalStructMap.setDivContainer(logicalDiv);
        // struct Link
        StructLink structLink = new StructLink();

        // create
        createMets(dir, ignoreNodes, fileGrp, physicalDiv, logicalDiv, structLink, 0, 0);

        // add to mets
        Mets mets = new Mets();
        mets.addDmdSec(dmdSec);
        mets.addAmdSec(amdSec);
        mets.setFileSec(fileSec);
        mets.addStructMap(physicalStructMap);
        mets.addStructMap(logicalStructMap);
        mets.setStructLink(structLink);

        return mets;
    }

    private void createMets(MCRDirectory dir, Set<MCRFilesystemNode> ignoreNodes, FileGrp fileGrp, PhysicalDiv physicalDiv, AbstractLogicalDiv logicalDiv, StructLink structLink, int logOrder, int physOrder) {
        MCRFilesystemNode[] children = dir.getChildren(MCRDirectory.SORT_BY_NAME_IGNORECASE);
        for (MCRFilesystemNode node : children) {
            if (ignoreNodes.contains(node))
                continue;
            if (node instanceof MCRDirectory) {
                MCRDirectory subDir = (MCRDirectory) node;
                LogicalSubDiv section = new LogicalSubDiv("log_" + Integer.toString(++logOrder), "section", subDir.getName(), logOrder);
                logicalDiv.add(section);
                createMets((MCRDirectory)node, ignoreNodes, fileGrp, physicalDiv, section, structLink, logOrder, physOrder);
            } else {
                MCRFile mcrFile = (MCRFile) node;
                final UUID uuid = UUID.randomUUID();
                final String fileID = "master_" + uuid.toString();
                final String physicalID = "phys_" + uuid.toString();
                try {
                    final String href = new URI(null, mcrFile.getAbsolutePath().substring(1), null).toString();
                    // file
                    File file = new File(fileID, getMimeType(mcrFile));
                    FLocat fLocat = new FLocat(FLocat.LOCTYPE_URL, href);
                    file.setFLocat(fLocat);
                    fileGrp.addFile(file);
                } catch(URISyntaxException uriSyntaxException) {
                    LOGGER.error("invalid href", uriSyntaxException);
                    continue;
                }
                // physical
                PhysicalSubDiv pyhsicalPage = new PhysicalSubDiv(physicalID, "page",  ++physOrder);
                Fptr fptr = new Fptr(fileID);
                pyhsicalPage.add(fptr);
                physicalDiv.add(pyhsicalPage);
                // struct link
                SmLink smLink = new SmLink(logicalDiv.getId(), physicalID);
                structLink.addSmLink(smLink);    
            }
        }
    }

    private String getMimeType(MCRFile subFile) {
        return MCRFileContentTypeFactory.getType(subFile.getContentTypeID()).getMimeType();
    }

}
