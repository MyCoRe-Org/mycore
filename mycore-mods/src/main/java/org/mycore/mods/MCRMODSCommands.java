/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.mods;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.mods.rss.MCRRSSFeedImporter;
import org.xml.sax.SAXException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@MCRCommandGroup(name = "MODS Commands")
public class MCRMODSCommands extends MCRAbstractCommands {

    public static final String MODS_V3_XSD_URI = "http://www.loc.gov/standards/mods/v3/mods-3-6.xsd";

    private static final Logger LOGGER = LogManager.getLogger(MCRMODSCommands.class);

    @MCRCommand(syntax = "load all mods documents from directory {0} for project {1}",
        help = "Load all MODS documents as MyCoRe Objects for project {1} from directory {0}",
        order = 10)
    public static List<String> loadFromDirectory(String directory, String projectID) {
        File dir = new File(directory);
        if (!dir.isDirectory()) {
            throw new MCRException(MessageFormat.format("File {0} is not a directory.", directory));
        }
        String[] list = dir.list();
        if (list.length == 0) {
            LOGGER.warn("No files found in directory {}", dir);
            return null;
        }
        return Arrays.stream(list)
            .filter(file -> file.endsWith(".xml"))
            .map(file -> MessageFormat.format(
                "load mods document from file {0} for project {1}",
                new File(dir, file).getAbsolutePath(), projectID))
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "load mods document from file {0} for project {1}",
        help = "Load MODS document {0} as MyCoRe Object for project {1}",
        order = 20)
    public static void loadFromFile(String modsFileName, String projectID) throws JDOMException, IOException,
        MCRActiveLinkException, SAXException, MCRPersistenceException, MCRAccessException {
        File modsFile = new File(modsFileName);
        if (!modsFile.isFile()) {
            throw new MCRException(MessageFormat.format("File {0} is not a file.", modsFile.getAbsolutePath()));
        }
        SAXBuilder s = new SAXBuilder(XMLReaders.NONVALIDATING, null, null);
        Document modsDoc = s.build(modsFile);
        //force validation against MODS XSD
        MCRXMLHelper.validate(modsDoc, MODS_V3_XSD_URI);
        Element modsRoot = modsDoc.getRootElement();
        if (!modsRoot.getNamespace().equals(MCRConstants.MODS_NAMESPACE)) {
            throw new MCRException(
                MessageFormat.format("File {0} is not a MODS document.", modsFile.getAbsolutePath()));
        }
        if (modsRoot.getName().equals("modsCollection")) {
            List<Element> modsElements = modsRoot.getChildren("mods", MCRConstants.MODS_NAMESPACE);
            for (Element mods : modsElements) {
                saveAsMyCoReObject(projectID, mods);
            }
        } else {
            saveAsMyCoReObject(projectID, modsRoot);
        }
    }

    @MCRCommand(syntax = "load mods document from file {0} with files from directory {1} for project {2}",
        help = "Load MODS document {0} as MyCoRe Object with files from direcory {1} for project {2}",
        order = 10)
    public static void loadFromFileWithFiles(String modsFileName, String fileDirName, String projectID)
        throws JDOMException, IOException,
        MCRActiveLinkException, SAXException, MCRPersistenceException, MCRAccessException {
        File modsFile = new File(modsFileName);
        if (!modsFile.isFile()) {
            throw new MCRException(MessageFormat.format("File {0} is not a file.", modsFile.getAbsolutePath()));
        }

        File fileDir = new File(fileDirName);
        if (!fileDir.isDirectory()) {
            throw new MCRException(
                MessageFormat.format("Directory {0} is not a directory.", fileDir.getAbsolutePath()));
        }

        SAXBuilder s = new SAXBuilder(XMLReaders.NONVALIDATING, null, null);
        Document modsDoc = s.build(modsFile);
        //force validation against MODS XSD
        MCRXMLHelper.validate(modsDoc, MODS_V3_XSD_URI);
        Element modsRoot = modsDoc.getRootElement();
        if (!modsRoot.getNamespace().equals(MCRConstants.MODS_NAMESPACE)) {
            throw new MCRException(
                MessageFormat.format("File {0} is not a MODS document.", modsFile.getAbsolutePath()));
        }
        if (modsRoot.getName().equals("modsCollection")) {
            throw new MCRException(
                MessageFormat.format("File {0} contains a mods collection witch not supported by this command.",
                    modsFile.getAbsolutePath()));
        } else {
            createDerivate(saveAsMyCoReObject(projectID, modsRoot), fileDir);
        }
    }

    private static MCRObjectID saveAsMyCoReObject(String projectID, Element modsRoot)
        throws MCRActiveLinkException, MCRPersistenceException, MCRAccessException {
        MCRObject mcrObject = MCRMODSWrapper.wrapMODSDocument(modsRoot, projectID);
        mcrObject.setId(MCRObjectID.getNextFreeId(mcrObject.getId().getBase()));
        MCRMetadataManager.create(mcrObject);
        return mcrObject.getId();
    }

    @MCRCommand(syntax = "import publications from {0} RSS feed for project {1}",
        help = "Read RSS feed from data source {0}, convert and save new publications as MyCoRe Object for project {1}",
        order = 30)
    public static void importFromRSSFeed(String sourceSystemID, String projectID) throws Exception {
        MCRRSSFeedImporter.importFromFeed(sourceSystemID, projectID);
    }

    private static MCRDerivate createDerivate(MCRObjectID documentID, File fileDir)
        throws MCRPersistenceException, IOException, MCRAccessException {
        MCRDerivate derivate = new MCRDerivate();
        derivate.setId(MCRObjectID.getNextFreeId(documentID.getProjectId(), "derivate"));
        derivate.setLabel("data object from " + documentID);

        String schema = CONFIG.getString("MCR.Metadata.Config.derivate", "datamodel-derivate.xml").replaceAll(".xml",
            ".xsd");
        derivate.setSchema(schema);

        MCRMetaLinkID linkId = new MCRMetaLinkID();
        linkId.setSubTag("linkmeta");
        linkId.setReference(documentID, null, null);
        derivate.getDerivate().setLinkMeta(linkId);

        MCRMetaIFS ifs = new MCRMetaIFS();
        ifs.setSubTag("internal");
        ifs.setSourcePath(fileDir.getAbsolutePath());
        derivate.getDerivate().setInternals(ifs);

        LOGGER.debug("Creating new derivate with ID {}", derivate.getId());
        MCRMetadataManager.create(derivate);

        setDefaultPermissions(derivate.getId());

        return derivate;
    }

    protected static void setDefaultPermissions(MCRObjectID derivateID) {
        if (CONFIG.getBoolean("MCR.Access.AddDerivateDefaultRule", true)) {
            MCRAccessInterface ai = MCRAccessManager.getAccessImpl();
            Collection<String> configuredPermissions = ai.getAccessPermissionsFromConfiguration();
            for (String permission : configuredPermissions) {
                MCRAccessManager.addRule(derivateID, permission, MCRAccessManager.getTrueRule(),
                    "default derivate rule");
            }
        }
    }
}
