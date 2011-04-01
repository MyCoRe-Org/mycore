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

import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileContentTypeFactory;
import org.mycore.datamodel.ifs.MCRFilesystemNode;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRMETSDefaultGenerator extends MCRMETSGenerator {

    private static final Logger LOGGER = Logger.getLogger(MCRMETSGenerator.class);

    private static final Namespace METS = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");

    public Document getMETS(MCRDirectory dir, Set<MCRFilesystemNode> ignoreNodes) {
        Document mets = new Document();
        Element root = new Element("mets", METS);
        mets.setRootElement(root);
        root.addNamespaceDeclaration(XLINK_NAMESPACE);
        root.setAttribute("schemaLocation",
            "http://www.loc.gov/METS/ http://www.loc.gov/mets/mets.xsd http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-2.xsd",
            XSI_NAMESPACE);
        //dmdSec
        /*
         * stylesheets my rely on the fact that derivate ID is part of dmdSec/@ID
         */
        root.addContent(new Element("dmdSec", METS).setAttribute("ID", "dmd_" + dir.getOwnerID()));
        //amdSec
        root.addContent(new Element("amdSec", METS).setAttribute("ID", "amd_" + dir.getOwnerID()));
        //file section
        Element fileSec = new Element("fileSec", METS);
        root.addContent(fileSec);
        Element fileGrp = new Element("fileGrp", METS);
        fileSec.addContent(fileGrp);
        fileGrp.setAttribute("USE", "MASTER");
        //physical structure
        Element physicalMap = new Element("structMap", METS);
        root.addContent(physicalMap);
        physicalMap.setAttribute("TYPE", "PHYSICAL");
        Element physSequence = new Element("div", METS);
        physicalMap.addContent(physSequence);
        physSequence.setAttribute("ID", "phys_dmd_" + dir.getOwnerID());
        physSequence.setAttribute("TYPE", "physSequence");
        //logical structure
        Element logicalMap = new Element("structMap", METS);
        root.addContent(logicalMap);
        logicalMap.setAttribute("TYPE", "LOGICAL");
        Element logContainer = new Element("div", METS);
        logicalMap.addContent(logContainer);
        logContainer.setAttribute("ID", "log_" + dir.getOwnerID());
        logContainer.setAttribute("DMDID", "dmd_" + dir.getOwnerID());
        logContainer.setAttribute("ADMID", "amd_" + dir.getOwnerID());
        logContainer.setAttribute("TYPE", "unit");
        logContainer.setAttribute("LABEL", dir.getOwnerID());
        //structure Links
        Element structLink = new Element("structLink", METS);
        root.addContent(structLink);
        addFolder(dir, ignoreNodes, fileGrp, physSequence, logContainer, structLink, 0);
        return mets;
    }

    private void addFolder(MCRDirectory dir, Set<MCRFilesystemNode> ignoreNodes, Element fileGrp, Element physSequence, Element logicalContainer,
        Element structLink, int logCounter) {
        MCRFilesystemNode[] children = dir.getChildren(MCRDirectory.SORT_BY_NAME_IGNORECASE);
        for (MCRFilesystemNode node : children) {
            if (ignoreNodes.contains(node))
                continue;
            if (node instanceof MCRDirectory) {
                MCRDirectory subDir = (MCRDirectory) node;
                Element section = new Element("div", METS);
                logicalContainer.addContent(section);
                section.setAttribute("TYPE", "section");
                section.setAttribute("ID", "log" + Integer.toString(++logCounter));
                section.setAttribute("LABEL", subDir.getName());
                addFolder(subDir, ignoreNodes, fileGrp, physSequence, section, structLink, logCounter);
            } else {
                // node is a file
                MCRFile subFile = (MCRFile) node;
                // add to fileGrp
                Element file = new Element("file", METS);
                fileGrp.addContent(file);
                final UUID uuid = UUID.randomUUID();
                final String fileID = "master_" + uuid.toString();
                final String physicalID = "phys_" + uuid.toString();
                file.setAttribute("ID", fileID);
                file.setAttribute("MIMETYPE", getMimeType(subFile));
                Element fLocat = new Element("FLocat", METS);
                file.addContent(fLocat);
                fLocat.setAttribute("LOCTYPE", "URL");
                try {
                    final String href = new URI(null, subFile.getAbsolutePath().substring(1), null).toString();
                    fLocat.setAttribute("href", href, XLINK_NAMESPACE);
                } catch(URISyntaxException uriSyntaxException) {
                    LOGGER.error("invalid href",uriSyntaxException);
                    continue;
                }
                //add to physical structMap
                Element physPage = new Element("div", METS);
                physSequence.addContent(physPage);
                physPage.setAttribute("TYPE", "page");
                physPage.setAttribute("ID", physicalID);
                physPage.setAttribute("ORDER", Integer.toString(physSequence.getContentSize()));
                Element fptr = new Element("fptr", METS);
                physPage.addContent(fptr);
                fptr.setAttribute("FILEID", fileID);
                //add to logical structMap and structLink
                //add to StructLink
                Element smLink = new Element("smLink", METS);
                structLink.addContent(smLink);
                smLink.setAttribute("from", logicalContainer.getAttributeValue("ID"), XLINK_NAMESPACE);
                smLink.setAttribute("to", physicalID, XLINK_NAMESPACE);
            }
        }
    }

    private String getMimeType(MCRFile subFile) {
        return MCRFileContentTypeFactory.getType(subFile.getContentTypeID()).getMimeType();
    }

}
