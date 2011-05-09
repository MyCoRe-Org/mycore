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
import org.jdom.Namespace;
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
import org.mycore.mets.model.struct.Div;
import org.mycore.mets.model.struct.Fptr;
import org.mycore.mets.model.struct.LogicalStructMap;
import org.mycore.mets.model.struct.PhysicalStructMap;
import org.mycore.mets.model.struct.SmLink;
import org.mycore.mets.model.struct.StructLink;
import org.mycore.mets.model.struct.SubDiv;

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
        Div physicalDiv = new Div("phys_dmd_" + dir.getOwnerID(), "physSequence");
        physicalStructMap.setDivContainer(physicalDiv);
        // logical structure
        LogicalStructMap logicalStructMap = new LogicalStructMap();
        Div logicalDiv = new Div("log_" + dir.getOwnerID(), dmdSec.getId(), amdSec.getId(), "monograph", dir.getOwnerID());
        logicalStructMap.setDivContainer(logicalDiv);
        // struct Link
        StructLink structLink = new StructLink();

        // create
        createMets(dir, ignoreNodes, fileGrp, physicalDiv, logicalDiv, null, structLink, 0, 0);

        // add to mets
        Mets mets = new Mets();
        mets.addDmdSec(dmdSec);
        mets.addAmdSec(amdSec);
        mets.setFileSec(fileSec);
        mets.setPysicalStructMap(physicalStructMap);
        mets.setLogicalStructMap(logicalStructMap);
        mets.setStructLink(structLink);

        return mets;
    }

    private void createMets(MCRDirectory dir, Set<MCRFilesystemNode> ignoreNodes, FileGrp fileGrp, Div physicalDiv, Div logicalDiv, SubDiv logicalContainer, StructLink structLink, int logOrder, int physOrder) {
        MCRFilesystemNode[] children = dir.getChildren(MCRDirectory.SORT_BY_NAME_IGNORECASE);
        for (MCRFilesystemNode node : children) {
            if (ignoreNodes.contains(node))
                continue;
            if (node instanceof MCRDirectory) {
                MCRDirectory subDir = (MCRDirectory) node;
                SubDiv section = new SubDiv("log_" + Integer.toString(++logOrder), "section", logOrder, subDir.getName());
                section.setLabel(subDir.getName());
                if(logicalContainer == null) {
                    logicalDiv.addSubDiv(section);
                } else {
                    logicalContainer.addLogicalDiv(section);
                }
                createMets((MCRDirectory)node, ignoreNodes, fileGrp, physicalDiv, logicalDiv, section, structLink, logOrder, physOrder);
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
                SubDiv physPage = new SubDiv(physicalID, SubDiv.TYPE_PAGE, ++physOrder, true);
                Fptr fptr = new Fptr(fileID);
                physPage.addFptr(fptr);
                physicalDiv.addSubDiv(physPage);
                // struct link
                if(logicalContainer == null) {
                    SmLink smLink = new SmLink(logicalDiv.asLogicalSubDiv(), physPage);
                    structLink.addSmLink(smLink);    
                } else {
                    SmLink smLink = new SmLink(logicalContainer, physPage);
                    structLink.addSmLink(smLink);
                }
            }
        }
    }

    private String getMimeType(MCRFile subFile) {
        return MCRFileContentTypeFactory.getType(subFile.getContentTypeID()).getMimeType();
    }

}
