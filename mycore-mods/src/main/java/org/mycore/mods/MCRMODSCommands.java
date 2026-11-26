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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.MCRRuleAccessInterface;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRPathContent;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.mods.enrichment.MCREnricher;
import org.mycore.mods.rss.MCRRSSFeedImporter;
import org.xml.sax.SAXException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@MCRCommandGroup(name = "MODS Commands")
public class MCRMODSCommands extends MCRAbstractCommands {

    public static final String MODS_V3_XSD_URI = "http://www.loc.gov/standards/mods/v3/mods-3-7.xsd";

    private static final Logger LOGGER = LogManager.getLogger(MCRMODSCommands.class);

    @MCRCommand(syntax = "load all mods documents from directory {0} for project {1}",
        help = "Load all MODS documents as MyCoRe Objects for project {1} from directory {0}",
        order = 10)
    public static List<String> loadFromDirectory(String directory, String projectID) {
        File dir = new File(directory);
        if (!dir.isDirectory()) {
            throw new MCRException(String.format(Locale.ENGLISH, "File %s is not a directory.", directory));
        }
        String[] list = dir.list();
        if (list.length == 0) {
            LOGGER.warn("No files found in directory {}", dir);
            return null;
        }
        return Arrays.stream(list)
            .filter(file -> file.endsWith(".xml"))
            .map(file -> String.format(Locale.ENGLISH,
                "load mods document from file %s for project %s",
                new File(dir, file).getAbsolutePath(), projectID))
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "load mods document from file {0} for project {1}",
        help = "Load MODS document {0} as MyCoRe Object for project {1}",
        order = 20)
    public static void loadFromFile(String modsFileName, String projectID) throws JDOMException, IOException,
        SAXException, MCRPersistenceException, MCRAccessException {
        File modsFile = new File(modsFileName);
        if (!modsFile.isFile()) {
            throw new MCRException(String.format(Locale.ENGLISH, "File %s is not a file.", modsFile.getAbsolutePath()));
        }
        MCRPathContent pathContent = new MCRPathContent(modsFile.toPath());
        Document modsDoc = pathContent.asXML();
        //force validation against MODS XSD
        MCRXMLHelper.validate(modsDoc, MODS_V3_XSD_URI);
        Element modsRoot = modsDoc.getRootElement();
        if (!modsRoot.getNamespace().equals(MCRConstants.MODS_NAMESPACE)) {
            throw new MCRException(
                String.format(Locale.ENGLISH, "File %s is not a MODS document.", modsFile.getAbsolutePath()));
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
        SAXException, MCRPersistenceException, MCRAccessException {
        File modsFile = new File(modsFileName);
        if (!modsFile.isFile()) {
            throw new MCRException(String.format(Locale.ENGLISH, "File %s is not a file.", modsFile.getAbsolutePath()));
        }

        File fileDir = new File(fileDirName);
        if (!fileDir.isDirectory()) {
            throw new MCRException(
                String.format(Locale.ENGLISH, "Directory %s is not a directory.", fileDir.getAbsolutePath()));
        }

        MCRPathContent pathContent = new MCRPathContent(modsFile.toPath());
        Document modsDoc = pathContent.asXML();
        //force validation against MODS XSD
        MCRXMLHelper.validate(modsDoc, MODS_V3_XSD_URI);
        Element modsRoot = modsDoc.getRootElement();
        if (!modsRoot.getNamespace().equals(MCRConstants.MODS_NAMESPACE)) {
            throw new MCRException(
                String.format(Locale.ENGLISH, "File %s is not a MODS document.", modsFile.getAbsolutePath()));
        }
        if (modsRoot.getName().equals("modsCollection")) {
            throw new MCRException(String.format(Locale.ENGLISH,
                "File %s contains a mods collection witch not supported by this command.", modsFile.getAbsolutePath()));
        } else {
            createDerivate(saveAsMyCoReObject(projectID, modsRoot), fileDir);
        }
    }

    private static MCRObjectID saveAsMyCoReObject(String projectID, Element modsRoot)
        throws MCRPersistenceException, MCRAccessException {
        MCRObject mcrObject = MCRMODSWrapper.wrapMODSDocument(modsRoot, projectID);
        mcrObject.setId(MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId(mcrObject.getId().getBase()));
        MCRConfiguration2.getString("MCR.MODS.Import.Object.State")
            .ifPresent(mcrObject.getService()::setState);
        MCRMetadataManager.create(mcrObject);
        return mcrObject.getId();
    }

    @MCRCommand(syntax = "import publications from {0} RSS feed for project {1}",
        help = "Read RSS feed from data source {0}, convert and save new publications as MyCoRe Object for project {1}",
        order = 30)
    public static void importFromRSSFeed(String sourceSystemID, String projectID) throws Exception {
        MCRRSSFeedImporter.importFromFeed(sourceSystemID, projectID);
    }

    @MCRCommand(syntax = "enrich {0} with config {1}",
        help = "Enriches existing MODS metadata {0} with a given enrichment configuration {1}", order = 40)
    public static void enrichMods(String modsId, String configID) {
        try {
            MCRObject obj = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(modsId));
            Element mods = new MCRMODSWrapper(obj).getMODS();
            MCREnricher enricher = new MCREnricher(configID);
            enricher.enrich(mods);
            MCRMetadataManager.update(obj);
        } catch (MCRException | MCRAccessException e) {
            LOGGER.error("Error while trying to enrich {} with configuration {}: ", modsId, configID, e);
        }
    }

    @MCRCommand(syntax = "repair pages of all mods objects",
        help = "Re-parses page information of all MODS objects that is stored only as mods:list, "
            + "e.g. \"275--281\", into mods:start and mods:end",
        order = 50)
    public static List<String> repairPagesOfAllObjects() {
        return MCRConfiguration2.getOrThrow("MCR.MODS.Types", MCRConfiguration2::splitValue)
            .flatMap(type -> MCRXMLMetadataManager.instance().listIDsOfType(type).stream())
            .sorted()
            .map(id -> "repair pages of mods object " + id)
            .collect(Collectors.toList());
    }

    /**
     * Re-parses every mods:part/mods:extent[@unit='pages'] that contains only a mods:list element
     * using {@link MCRMODSPagesHelper}. The extent is only replaced if a start and an end page could be detected,
     * so free text page lists and lists with other information like "S. 123 ff." stay unchanged. The object is
     * only updated if an extent was changed.
     *
     * @param objectID the ID of the MODS object to repair
     * @throws MCRAccessException if the current user may not update the object
     */
    @MCRCommand(syntax = "repair pages of mods object {0}",
        help = "Re-parses page information of MODS object {0} that is stored only as mods:list, "
            + "e.g. \"275--281\", into mods:start and mods:end",
        order = 51)
    public static void repairPages(String objectID) throws MCRAccessException {
        MCRObjectID mcrObjectID = MCRObjectID.getInstance(objectID);
        if (!MCRMODSWrapper.isSupported(mcrObjectID)) {
            LOGGER.warn("Object {} is not a MODS object, skipping.", objectID);
            return;
        }
        MCRObject obj = MCRMetadataManager.retrieveMCRObject(mcrObjectID);
        List<Element> extents = new MCRMODSWrapper(obj)
            .getElements(".//mods:part/mods:extent[@unit='pages'][mods:list][count(*)=1]");
        boolean changed = false;
        for (Element extent : extents) {
            String pages = extent.getChildText("list", MCRConstants.MODS_NAMESPACE);
            Element parsed = MCRMODSPagesHelper.buildExtentPages(pages);
            if (parsed.getChild("start", MCRConstants.MODS_NAMESPACE) == null
                || parsed.getChild("end", MCRConstants.MODS_NAMESPACE) == null) {
                continue;
            }
            LOGGER.info("{}: split pages '{}' into start and end page", objectID, pages);
            extent.setContent(parsed.removeContent());
            changed = true;
        }
        if (changed) {
            MCRMetadataManager.update(obj);
        }
    }

    private static MCRDerivate createDerivate(MCRObjectID documentID, File fileDir)
        throws MCRPersistenceException, IOException, MCRAccessException {
        MCRDerivate derivate = new MCRDerivate();
        derivate.setId(MCRMetadataManager.getMCRObjectIDGenerator()
            .getNextFreeId(documentID.getProjectId(), "derivate"));
        String schema = MCRConfiguration2.getString("MCR.Metadata.Config.derivate")
            .orElse("datamodel-derivate.xml")
            .replaceAll(".xml", ".xsd");
        derivate.setSchema(schema);

        MCRMetaLinkID linkId = new MCRMetaLinkID();
        linkId.setSubTag("linkmeta");
        linkId.setReference(documentID, null, null);
        derivate.getDerivate().setLinkMeta(linkId);

        final Path rootPath = fileDir.toPath();
        try (Stream<Path> streamRootPath = Files.list(rootPath)) {
            final Optional<String> firstRegularFile = streamRootPath.filter(Files::isRegularFile)
                .map(rootPath::relativize)
                .map(Path::toString)
                .findFirst();

            MCRMetaIFS ifs = new MCRMetaIFS();
            ifs.setSubTag("internal");
            ifs.setSourcePath(fileDir.getAbsolutePath());
            firstRegularFile.ifPresent(ifs::setMainDoc);
            derivate.getDerivate().setInternals(ifs);
        }
        MCRConfiguration2.getString("MCR.MODS.Import.Derivate.Categories")
            .map(MCRConfiguration2::splitValue)
            .ifPresent(s -> {
                s.map(MCRCategoryID::fromString)
                    .forEach(categId -> derivate.getDerivate().getClassifications()
                        .add(new MCRMetaClassification("classification", 0, null,
                            categId)));
            });

        LOGGER.debug("Creating new derivate with ID {}", derivate.getId());
        MCRMetadataManager.create(derivate);

        setDefaultPermissions(derivate.getId());

        return derivate;
    }

    protected static void setDefaultPermissions(MCRObjectID derivateID) {
        if (MCRConfiguration2.getBoolean("MCR.Access.AddDerivateDefaultRule").orElse(true)
            && MCRAccessManager.getAccessImpl() instanceof MCRRuleAccessInterface ruleAccess) {
            ruleAccess
                .getAccessPermissionsFromConfiguration()
                .forEach(p -> MCRAccessManager.addRule(derivateID, p, MCRAccessManager.getTrueRule(),
                    "default derivate rule"));
        }
    }
}
