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

package org.mycore.frontend.cli;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMSource;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.MCRRuleAccessInterface;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRPathContent;
import org.mycore.common.content.transformer.MCRXSLTransformer;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.common.xml.MCRXSLTransformerUtils;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRMetaEnrichedLinkID;
import org.mycore.datamodel.metadata.MCRMetaEnrichedLinkIDFactory;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRAbstractFileSystem;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.utils.MCRDerivateUtil;
import org.mycore.datamodel.niofs.utils.MCRTreeCopier;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.frontend.fileupload.MCRUploadHelper;

/**
 * Provides static methods that implement commands for the MyCoRe command line
 * interface.
 *
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 */
@MCRCommandGroup(name = "Derivate Commands")
public class MCRDerivateCommands extends MCRAbstractCommands {

    /** The logger */
    private static final Logger LOGGER = LogManager.getLogger();

    /** The ACL interface */
    private static final MCRAccessInterface ACCESS_IMPL = MCRAccessManager.getAccessImpl();

    /** Default transformer script */
    public static final String DEFAULT_STYLE = "save-derivate.xsl";

    /** Static compiled transformer stylesheets */
    private static final Map<String, Transformer> TRANSFORMER_CACHE = new HashMap<>();

    /**
     * deletes all MCRDerivate from the datastore.
     */
    @MCRCommand(syntax = "delete all derivates", help = "Removes all derivates from the repository", order = 10)
    public static List<String> deleteAllDerivates() {
        return MCRCommandUtils.getIdsForType(MCRDerivate.OBJECT_TYPE)
            .map(id -> "delete derivate " + id)
            .collect(Collectors.toList());
    }

    /**
     * Delete an MCRDerivate from the datastore.
     *
     * @param id
     *            the ID of the MCRDerivate that should be deleted
     * @throws MCRAccessException see {@link MCRMetadataManager#delete(MCRDerivate)}
     */
    @MCRCommand(syntax = "delete derivate {0}",
        help = "The command remove a derivate with the MCRObjectID {0}",
        order = 30)
    public static void delete(String id) throws MCRPersistenceException, MCRAccessException {
        MCRObjectID objectID = MCRObjectID.getInstance(id);
        MCRMetadataManager.deleteMCRDerivate(objectID);
        LOGGER.info("{} deleted.", objectID);
    }

    /**
     * Delete MCRDerivates form ID to ID from the datastore.
     *
     * @param idFrom
     *            the start ID for deleting the MCRDerivate
     * @param idTo
     *            the stop ID for deleting the MCRDerivate
     */
    @MCRCommand(syntax = "delete derivate from {0} to {1}",
        help = "The command remove derivates in the number range between the MCRObjectID {0} and {1}.",
        order = 20)
    public static List<String> delete(String idFrom, String idTo)
        throws MCRPersistenceException {
        return MCRCommandUtils.getIdsFromIdToId(idFrom, idTo)
            .map(id -> "delete derivate " + id)
            .collect(Collectors.toList());
    }

    /**
     * Loads MCRDerivates from all XML files in a directory.
     *
     * @param directory
     *            the directory containing the XML files
     */
    @MCRCommand(syntax = "load all derivates from directory {0}",
        help = "Loads all MCRDerivates from the directory {0} to the system. " +
            "If the numerical part of a provided ID is zero, a new ID with the same project ID and type is assigned.",
        order = 60)
    public static List<String> loadFromDirectory(String directory) {
        return processFromDirectory(directory, false);
    }

    /**
     * Updates MCRDerivates from all XML files in a directory.
     *
     * @param directory
     *            the directory containing the XML files
     */
    @MCRCommand(syntax = "update all derivates from directory {0}",
        help = "The command update all derivates form the directory {0} in the system.",
        order = 70)
    public static List<String> updateFromDirectory(String directory) {
        return processFromDirectory(directory, true);
    }

    /**
     * Loads or updates MCRDerivates from all XML files in a directory.
     *
     * @param directory
     *            the directory containing the XML files
     * @param update
     *            if true, object will be updated, else object is created
     */
    private static List<String> processFromDirectory(String directory, boolean update) {
        File dir = new File(directory);

        if (!dir.isDirectory()) {
            LOGGER.warn("{} ignored, is not a directory.", directory);
            return null;
        }

        File[] list = dir.listFiles();

        if (list.length == 0) {
            LOGGER.warn("No files found in directory {}", directory);
            return null;
        }

        List<String> cmds = new ArrayList<>();
        for (File file : list) {
            String name = file.getName();
            if (!(name.endsWith(".xml") && name.contains("derivate"))) {
                continue;
            }
            name = name.substring(0, name.length() - 4); // remove ".xml"
            File contentDir = new File(dir, name);
            if (!(contentDir.exists() && contentDir.isDirectory())) {
                continue;
            }
            cmds.add((update ? "update" : "load") + " derivate from file " + file.getAbsolutePath());
        }

        return cmds;
    }

    /**
     * Loads an MCRDerivates from an XML file.
     *
     * @param file
     *            the location of the xml file
     * @throws MCRAccessException see {@link MCRMetadataManager#create(MCRDerivate)}
     */
    @MCRCommand(syntax = "load derivate from file {0}",
        help = "Loads an MCRDerivate from the file {0} to the system. " +
            "If the numerical part of the provided ID is zero, a new ID with the same project ID and type is assigned.",
        order = 40)
    public static boolean loadFromFile(String file)
        throws IOException, MCRPersistenceException, MCRAccessException, JDOMException {
        return loadFromFile(file, true);
    }

    /**
     * Loads an MCRDerivates from an XML file.
     *
     * @param file
     *            the location of the xml file
     * @param importMode
     *            if true, servdates are taken from xml file
     * @throws MCRAccessException see {@link MCRMetadataManager#create(MCRDerivate)}
     */
    public static boolean loadFromFile(String file, boolean importMode)
        throws IOException, MCRPersistenceException, MCRAccessException, JDOMException {
        return processFromFile(new File(file), false, importMode);
    }

    /**
     * Updates an MCRDerivates from an XML file.
     *
     * @param file
     *            the location of the xml file
     * @throws MCRAccessException see {@link MCRMetadataManager#update(MCRDerivate)}
     */

    @MCRCommand(syntax = "update derivate from file {0}",
        help = "The command update a derivate form the file {0} in the system.",
        order = 50)
    public static boolean updateFromFile(String file)
        throws IOException, MCRPersistenceException, MCRAccessException, JDOMException {
        return updateFromFile(file, true);
    }

    /**
     * Updates an MCRDerivates from an XML file.
     *
     * @param file
     *            the location of the xml file
     * @param importMode
     *            if true, servdates are taken from xml file
     * @throws MCRAccessException see {@link MCRMetadataManager#update(MCRDerivate)}
     */
    public static boolean updateFromFile(String file, boolean importMode)
        throws IOException, MCRPersistenceException, MCRAccessException, JDOMException {
        return processFromFile(new File(file), true, importMode);
    }

    /**
     * Load or update an MCRDerivate from an XML file. If the numerical part of the contained ID is zero,
     * a new ID with the same project ID and type is assigned.
     *
     * @param file
     *            the location of the xml file
     * @param update
     *            if true, derivate will be updated, else derivate is created
     * @param importMode
     *            if true, servdates are taken from xml file
     * @throws MCRAccessException see {@link MCRMetadataManager#update(MCRDerivate)}
     */
    private static boolean processFromFile(File file, boolean update, boolean importMode)
        throws IOException, MCRPersistenceException, MCRAccessException, JDOMException {
        if (!file.getName().endsWith(".xml")) {
            LOGGER.warn("{} ignored, does not end with *.xml", file);
            return false;
        }

        if (!file.isFile()) {
            LOGGER.warn("{} ignored, is not a file.", file);
            return false;
        }

        LOGGER.info("Reading file {} …", file);

        MCRDerivate derivate = new MCRDerivate(file.toURI());
        derivate.setImportMode(importMode);

        // Replace relative path with absolute path of files
        if (derivate.getDerivate().getInternals() != null) {
            String path = derivate.getDerivate().getInternals().getSourcePath();
            if (path == null) {
                path = "";
            } else {
                path = path.replace('/', File.separatorChar).replace('\\', File.separatorChar);
            }
            if (path.trim().length() <= 1) {
                // the path is the path name plus the name of the derivate -
                path = derivate.getId().toString();
            }
            File sPath = new File(path);

            if (!sPath.isAbsolute()) {
                // only change path to absolute path when relative
                String prefix = file.getParent();

                if (prefix != null) {
                    path = prefix + File.separator + path;
                }
            }

            derivate.getDerivate().getInternals().setSourcePath(path);
            LOGGER.info("Source path --> {}", path);
        }

        if (update) {
            MCRMetadataManager.update(derivate);
            LOGGER.info("{} updated.", derivate::getId);
        } else {
            MCRMetadataManager.create(derivate);
            LOGGER.info("{} loaded.", derivate::getId);
        }

        return true;
    }

    /**
     * Export an MCRDerivate to a file named <em>MCRObjectID</em>.xml in a
     * directory named <em>dirname</em> and store the derivate files in a
     * nested directory named <em>MCRObjectID</em>. The IFS-Attribute of the
     * derivate files aren't saved, for reloading purpose after deleting a
     * derivate in the datastore
     *
     * @param id
     *            the ID of the MCRDerivate to be save.
     * @param dirname
     *            the dirname to store the derivate
     */
    @MCRCommand(syntax = "show loadable derivate of {0} to directory {1}",
        help = "The command store the derivate with the MCRObjectID {0} to the directory {1}, without ifs-metadata",
        order = 130)
    public static void show(String id, String dirname) {
        exportWithStylesheet(id, id, dirname, "save");
    }

    /**
     * Export an MCRDerivate to a file named <em>MCRObjectID</em>.xml in a directory named <em>dirname</em>
     * and store the derivate files in a nested directory named <em>MCRObjectID</em>.
     * The method uses the converter stylesheet <em>style</em>.xsl.
     *
     * @param id
     *            the ID of the MCRDerivate to save.
     * @param dirname
     *            the dirname to store the derivate
     * @param style
     *            the name of the stylesheet, prefix of <em>style</em>-derivate.xsl
     */
    @MCRCommand(
        syntax = "export derivate {0} to directory {1} with stylesheet {2}",
        help = "Stores the derivate with the MCRObjectID {0} to the directory {1}" +
            " with the stylesheet {2}-derivate.xsl. For {2}, the default is xsl/save.",
        order = 90)
    public static void exportWithStylesheet(String id, String dirname, String style) {
        exportWithStylesheet(id, id, dirname, style);
    }

    /**
     * Export any MCRDerivate's to files named <em>MCRObjectID</em>.xml in a directory named <em>dirname</em>
     * and the derivate files in nested directories named <em>MCRObjectID</em>.
     * Exporting starts with <em>fromID</em> and ends with <em>toID</em>. IDs that aren't found will be skipped.
     * The method use the converter stylesheet <em>style</em>.xsl.
     *
     * @param fromID
     *            the first ID of the MCRDerivate to save.
     * @param toID
     *            the last ID of the MCRDerivate to save.
     * @param dirname
     *            the filename to store the object
     * @param style
     *            the name of the stylesheet, prefix of <em>style</em>-derivate.xsl
     */
    @MCRCommand(syntax = "export derivates from {0} to {1} to directory {2} with stylesheet {3}",
        help = "Stores all derivates with MCRObjectID's between {0} and {1} to the directory {2}"
            + " with the stylesheet {3}-derivate.xsl. For {3}, the default is xsl/save.",
        order = 80)
    public static void exportWithStylesheet(String fromID, String toID, String dirname, String style) {

        // check fromID
        MCRObjectID fid;
        try {
            fid = MCRObjectID.getInstance(fromID);
        } catch (Exception ex) {
            LOGGER.error("FromID : {}", ex::getMessage);
            return;
        }

        // check toID
        MCRObjectID tid;
        try {
            tid = MCRObjectID.getInstance(toID);
        } catch (Exception ex) {
            LOGGER.error("ToID : {}", ex::getMessage);
            return;
        }

        // check dirname
        File dir = new File(dirname);
        if (!dir.isDirectory()) {
            LOGGER.error("{} is not a directory.", dirname);
            return;
        }

        Transformer trans = getTransformer(style != null ? style + "-derivate" : null);
        String extension = MCRXSLTransformerUtils.getFileExtension(trans, "xml");

        int k = 0;
        try {
            for (int i = fid.getNumberAsInteger(); i < tid.getNumberAsInteger() + 1; i++) {
                String id = MCRObjectID.formatID(fid.getProjectId(), fid.getTypeId(), i);
                exportDerivate(dir, trans, extension, id);
                k++;
            }
        } catch (Exception ex) {
            LOGGER.error(() -> "Exception while storing derivate to " + dir.getAbsolutePath(), ex);
            return;
        }
        int numExported = k;
        LOGGER.info("{} Object's stored under {}.", () -> numExported, dir::getAbsolutePath);
    }

    /**
     * This command looks for all derivates in the application and builds export
     * commands.
     *
     * @param dirname
     *            the filename to store the object
     * @param style
     *            the name of the stylesheet, prefix of <em>style</em>-derivate.xsl
     * @return a list of export commands, one for each derivate
     */
    @MCRCommand(syntax = "export all derivates to directory {0} with stylesheet {1}",
        help = "Stores all derivates to the directory {0} with the stylesheet {1}-derivate.xsl."
            + " For {1}, the default is xsl/save.",
        order = 100)
    public static List<String> exportAllDerivatesWithStylesheet(String dirname, String style) {
        return MCRCommandUtils.getIdsForType(MCRDerivate.OBJECT_TYPE)
            .map(id -> "export derivate " + id + " to directory " + dirname + " with stylesheet " + style)
            .collect(Collectors.toList());
    }

    /**
     * This command looks for all derivates starting with project name in the
     * application and builds export commands.
     *
     * @param dirname
     *            the filename to store the object
     * @param style
     *            the name of the stylesheet, prefix of <em>style</em>-derivate.xsl
     * @return a list of export commands, one for each derivate
     */
    @MCRCommand(syntax = "export all derivates of project {0} to directory {1} with stylesheet {2}",
        help = "Stores all derivates of project {0} to the directory {1} with the stylesheet {2}-derivate.xsl."
            + " For {2}, the default is xsl/save.",
        order = 110)
    public static List<String> exportAllDerivatesOfProjectWithStylesheet(String project, String dirname, String style) {
        return MCRCommandUtils.getIdsForProjectAndType(project, MCRDerivate.OBJECT_TYPE)
            .map(id -> "export derivate " + id + " to directory " + dirname + " with stylesheet " + style)
            .collect(Collectors.toList());
    }

    private static void exportDerivate(File dir, Transformer transformer, String extension, String nid)
        throws TransformerException, IOException {
        // store the XML file
        Document xml;
        MCRDerivate obj;

        MCRObjectID derivateID = MCRObjectID.getInstance(nid);
        try {
            obj = MCRMetadataManager.retrieveMCRDerivate(derivateID);
            String path = obj.getDerivate().getInternals().getSourcePath();
            // reset from the absolute to relative path, for later reload
            LOGGER.info("Old Internal Path ====>{}", path);
            obj.getDerivate().getInternals().setSourcePath(nid);
            LOGGER.info("New Internal Path ====>{}", nid);
            // add ACL's
            if (ACCESS_IMPL instanceof MCRRuleAccessInterface ruleAccessInterface) {
                Collection<String> l = ruleAccessInterface.getPermissionsForID(nid);
                for (String permission : l) {
                    Element rule = ruleAccessInterface.getRule(nid, permission);
                    obj.getService().addRule(permission, rule);
                }
            }

            // build JDOM
            xml = obj.createXML();

        } catch (IllegalArgumentException ex) {
            LOGGER.warn("Could not read {}, continue with next ID", nid);
            return;
        }
        File xmlOutput = new File(dir, derivateID + "." + extension);
        File directoryFile = new File(dir, derivateID.toString());
        try(OutputStream fileOutputStream = Files.newOutputStream(xmlOutput.toPath())) {
            if (transformer != null) {
                transformer.setParameter("dirname", directoryFile.getPath());
                StreamResult sr = new StreamResult(fileOutputStream);
                transformer.transform(new JDOMSource(xml), sr);
            } else {
                new XMLOutputter().output(xml, fileOutputStream);
            }
        }

        LOGGER.info("Object {} stored under {}.", nid, xmlOutput);

        // store the derivate file under dirname
        if (!directoryFile.isDirectory()) {
            directoryFile.mkdir();
        }
        MCRPath rootPath = MCRPath.getPath(derivateID.toString(), "/");
        Files.walkFileTree(rootPath, new MCRTreeCopier(rootPath, directoryFile.toPath()));

        LOGGER.info("Derivate {} saved under {} and {}.", nid, directoryFile, xmlOutput);
    }

    /**
     * This method searches for the stylesheet <em>style</em>.xsl and builds the transformer. Default is
     * <em>save-derivate.xsl</em> if no stylesheet is given or the stylesheet couldn't be resolved.
     *
     * @param style
     *            the name of the style to be used when resolving the stylesheet
     * @return the transformer
     */
    private static Transformer getTransformer(String style) {
        return MCRCommandUtils.getTransformer(style, DEFAULT_STYLE, TRANSFORMER_CACHE);
    }

    /**
     * The method start the repair the content search index for all derivates.
     */
    @MCRCommand(syntax = "repair derivate search of type derivate",
        help = "The command read the Content store and reindex the derivate search stores.",
        order = 140)
    public static List<String> repairDerivateSearch() {
        LOGGER.info("Start the repair for type derivate.");
        return MCRCommandUtils.getIdsForType(MCRDerivate.OBJECT_TYPE)
            .map(id -> "repair derivate search of ID " + id)
            .collect(Collectors.toList());
    }

    /**
     * Repairing the content search index for all derivates in project {0}.
     *
     * @param project
     *            the project part of a MCRObjectID e.g. *DocPortal*_derivate
     */
    @MCRCommand(syntax = "repair derivate search of project {0}",
        help = "Reads the Content store for project {0} and reindexes the derivate search stores.",
        order = 141)
    public static List<String> repairDerivateSearchForBase(String project) {
        LOGGER.info("Start the repair for project {}.", project);
        return MCRCommandUtils.getIdsForProjectAndType(project, MCRDerivate.OBJECT_TYPE)
            .map(id -> "repair derivate search of ID " + id)
            .collect(Collectors.toList());
    }

    /**
     * The method start the repair the content search index for one.
     *
     * @param id
     *            the MCRObjectID as String
     */
    @MCRCommand(syntax = "repair derivate search of ID {0}",
        help = "The command read the Content store for MCRObjectID {0} and reindex the derivate search store.",
        order = 150)
    public static void repairDerivateSearchForID(String id) throws IOException {
        LOGGER.info("Start the repair for the ID {}", id);
        doForChildren(MCRPath.getPath(id, "/"));
        LOGGER.info("Repaired {}", id);
    }

    /**
     * This is a recursive method to start an event handler for each file.
     *
     * @param rootPath
     *            a IFS node (file or directory)
     */
    private static void doForChildren(Path rootPath) throws IOException {
        Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // handle events
                MCREvent evt = new MCREvent(MCREvent.ObjectType.PATH, MCREvent.EventType.REPAIR);
                evt.put(MCREvent.PATH_KEY, file);
                evt.put(MCREvent.FILEATTR_KEY, attrs);
                MCREventManager.getInstance().handleEvent(evt);
                LOGGER.debug("repaired file {}", file);
                return super.visitFile(file, attrs);
            }

        });
    }

    /**
     * The method start the repair the content search index for all derivates.
     */
    @MCRCommand(syntax = "synchronize all derivates",
        help = "The command read each derivate and synchronize the xlink:label with "
            + "the derivate entry of the mycoreobject.",
        order = 160)
    public static List<String> synchronizeAllDerivates() {
        LOGGER.info("Start the synchronization for derivates.");
        return MCRCommandUtils.getIdsForType(MCRDerivate.OBJECT_TYPE)
            .map(id -> "synchronize derivate with ID " + id)
            .collect(Collectors.toList());
    }

    /**
     * Links the given derivate to the given object.
     */
    @MCRCommand(syntax = "link derivate {0} to {1}",
        help = "links the given derivate {0} to the given mycore object {1}",
        order = 180)
    public static void linkDerivateToObject(String derivateId, String objectId) throws Exception {
        if (derivateId == null || objectId == null) {
            LOGGER.error("Either derivate id or object id is null. Derivate={}, object={}", derivateId, objectId);
            return;
        }
        MCRObjectID derID = MCRObjectID.getInstance(derivateId);
        MCRObjectID objID = MCRObjectID.getInstance(objectId);

        if (!MCRMetadataManager.exists(objID)) {
            throw new NoSuchElementException("The object with id " + objID + " does not exist");
        }

        if (!MCRMetadataManager.exists(derID)) {
            throw new NoSuchElementException("The derivate with id " + derID + " does not exist");
        }

        MCRDerivate derObj = MCRMetadataManager.retrieveMCRDerivate(derID);
        MCRMetaLinkID oldDerivateToObjectLink = derObj.getDerivate().getMetaLink();
        MCRObjectID oldOwnerId = oldDerivateToObjectLink.getXLinkHrefID();

        /* set link to new parent in the derivate object */
        LOGGER.info("Setting {} as parent for derivate {}", objID, derID);
        derObj.getDerivate().getMetaLink()
            .setReference(objID, oldDerivateToObjectLink.getXLinkLabel(), oldDerivateToObjectLink.getXLinkTitle());
        MCRMetadataManager.update(derObj);

        /* set link to derivate in the new parent */
        MCRObject oldOwner = MCRMetadataManager.retrieveMCRObject(oldOwnerId);

        LOGGER.info("Linking derivate {} to {}", derID, objID);
        MCRMetaEnrichedLinkID derivateLink = MCRMetaEnrichedLinkIDFactory.obtainInstance().getDerivateLink(derObj);
        MCRMetadataManager.addOrUpdateDerivateToObject(objID, derivateLink, derObj.isImportMode());

        /* removing link from old parent */
        boolean flag = oldOwner.getStructure().removeDerivate(derID);
        LOGGER.info("Unlinking derivate {} from object {}. Success={}", derID, oldOwnerId, flag);
        MCRMetadataManager.fireUpdateEvent(oldOwner);
    }

    /**
     * Check the object links in derivates of MCR base ID for existing.
     * It looks to the XML store on the disk to get all object IDs.
     *
     * @param baseId
     *            the base part of a MCRObjectID e.g. DocPortal_derivate
     */
    @MCRCommand(syntax = "check object entries in derivates for base {0}",
        help = "check in all derivates of MCR base ID {0} for existing linked objects",
        order = 400)
    public static void checkObjectsInDerivates(String baseId) {
        if (baseId == null || baseId.isEmpty()) {
            LOGGER.error("Base ID missed for check object entries in derivates for base {0}");
            return;
        }
        int projectPartPosition = baseId.indexOf('_');
        if (projectPartPosition == -1) {
            LOGGER.error("The given base ID {} has not the syntax of project_type", baseId);
            return;
        }
        MCRXMLMetadataManager mgr = MCRXMLMetadataManager.getInstance();
        String project = baseId.substring(0, projectPartPosition + 1);
        List<String> idList = mgr.listIDsForBase(project + MCRDerivate.OBJECT_TYPE);
        int counter = 0;
        int maxresults = idList.size();
        for (String derid : idList) {
            counter++;
            LOGGER.info("Processing dataset {} from {} with ID: {}", counter, maxresults, derid);
            // get from data
            MCRObjectID mcrderid = MCRObjectID.getInstance(derid);
            MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(mcrderid);
            MCRObjectID objid = der.getOwnerID();
            if (!mgr.exists(objid)) {
                LOGGER.error("   !!! Missing object {} in database for derivate ID {}", objid, mcrderid);
            }
        }
        LOGGER.info("Check done for {} entries", counter);
    }

    @MCRCommand(syntax = "transform xml matching file name pattern {0} in derivate {1} with stylesheet {2}",
        help = "Finds all files in Derivate {1} which match the pattern {0} "
            + "(the complete path with regex: or glob:*.xml syntax) and transforms them with stylesheet {2}")
    public static void transformXMLMatchingPatternWithStylesheet(String pattern, String derivate, String stylesheet)
        throws IOException {
        MCRXSLTransformer transformer = new MCRXSLTransformer(stylesheet);
        MCRPath derivateRoot = MCRPath.getPath(derivate, "/");
        PathMatcher matcher = derivateRoot.getFileSystem().getPathMatcher(pattern);

        Files.walkFileTree(derivateRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
                if (matcher.matches(file)) {
                    LOGGER.info("The file {} matches the pattern {}", file, pattern);
                    MCRContent sourceContent = new MCRPathContent(file);

                    MCRContent resultContent = transformer.transform(sourceContent);
                    try {
                        Document source = sourceContent.asXML();
                        Document result = resultContent.asXML();
                        LOGGER.info("Transforming complete!");

                        if (!MCRXMLHelper.deepEqual(source, result)) {
                            LOGGER.info("Writing result..");
                            resultContent.sendTo(file, StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            LOGGER.info("Result and Source is the same..");
                        }

                    } catch (JDOMException e) {
                        throw new IOException("Error while processing file : " + file, e);
                    }

                }

                return FileVisitResult.CONTINUE;
            }
        });

    }

    @MCRCommand(syntax = "set main file of {0} to {1}",
        help = "Sets the main file of the derivate with the id {0} to "
            + "the file with the path {1}")
    public static void setMainFile(final String derivateIDString, final String filePath) throws MCRAccessException {
        if (!MCRObjectID.isValid(derivateIDString)) {
            LOGGER.error("{} is not valid. ", derivateIDString);
            return;
        }

        // check for derivate exist
        final MCRObjectID derivateID = MCRObjectID.getInstance(derivateIDString);
        if (!MCRMetadataManager.exists(derivateID)) {
            LOGGER.error("{} does not exist!", derivateIDString);
            return;
        }

        // remove leading slash
        String cleanPath = filePath;
        if (filePath.startsWith(String.valueOf(MCRAbstractFileSystem.SEPARATOR))) {
            cleanPath = filePath.substring(1);
        }

        // check for file exist
        final MCRPath path = MCRPath.getPath(derivateID.toString(), cleanPath);
        if (!Files.exists(path)) {
            LOGGER.error("File {} does not exist!", cleanPath);
            return;
        }

        final MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateID);
        derivate.getDerivate().getInternals().setMainDoc(cleanPath);
        MCRMetadataManager.update(derivate);
        LOGGER.info("The main file of {} is now '{}'!", derivateIDString, cleanPath);
    }

    @MCRCommand(syntax = "rename files from derivate {0} with {1} to {2}",
        help = "Renames multiple files in one Derivate with the ID {0} the given RegEx pattern {1} and the replacement"
            + " {2}. You can try out your pattern with the command: 'test rename file {0} with {1} to {2}'.")
    public static void renameFiles(String derivate, String pattern, String newName)
        throws IOException {
        MCRDerivateUtil.renameFiles(derivate, pattern, newName);
    }

    @MCRCommand(syntax = "test rename file {0} with {1} to {2}",
        help = "Tests the rename pattern {1} on one file {0} and replaces it with {2}, so you can try the rename"
            + " before renaming all files. This command does not change any files.")
    public static void testRenameFile(String filename, String pattern, String newName) {
        MCRDerivateUtil.testRenameFile(filename, pattern, newName);
    }

    @MCRCommand(syntax = "set order of derivate {0} to {1}",
        help = "Sets the order of derivate {0} to the number {1} see also MCR-2003")
    public static void setOrderOfDerivate(String derivateIDStr, String orderStr) throws MCRAccessException {
        final int order = Integer.parseInt(orderStr);

        final MCRObjectID derivateID = MCRObjectID.getInstance(derivateIDStr);

        if (!MCRMetadataManager.exists(derivateID)) {
            throw new MCRException("The derivate " + derivateIDStr + " does not exist!");
        }

        final MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateID);
        derivate.setOrder(order);
        MCRMetadataManager.update(derivate);

    }

    @MCRCommand(syntax = "set classification of derivate {0} to {1}",
        help = "Sets the classification of derivate {0} to the categories {1} (comma separated) "
            + "of classification 'derivate_types' or any fully qualified category, removing any previous definition.")
    public static void setClassificationOfDerivate(String derivateIDStr, String categoriesCommaList)
        throws MCRAccessException {
        final MCRCategoryDAO categoryDAO = MCRCategoryDAOFactory.obtainInstance();
        final List<MCRCategoryID> derivateTypes = Stream.of(categoriesCommaList.split(","))
            .map(String::trim)
            .map(category -> category.contains(":") ? MCRCategoryID.ofString(category)
                : new MCRCategoryID("derivate_types", category))
            .collect(Collectors.toList());

        final String nonExistingCategoriesCommaList = derivateTypes.stream()
            .filter(Predicate.not(categoryDAO::exist))
            .map(MCRCategoryID::getId)
            .collect(Collectors.joining(", "));
        if (!nonExistingCategoriesCommaList.isEmpty()) {
            throw new MCRPersistenceException("Categories do not exist: " + nonExistingCategoriesCommaList);
        }

        final MCRObjectID derivateID = MCRObjectID.getInstance(derivateIDStr);
        final MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateID);
        derivate.getDerivate().getClassifications().clear();
        derivate.getDerivate().getClassifications()
            .addAll(
                derivateTypes.stream()
                    .map(categoryID -> new MCRMetaClassification("classification", 0, null, categoryID.getRootID(),
                        categoryID.getId()))
                    .collect(Collectors.toList()));
        MCRMetadataManager.update(derivate);
    }

    @MCRCommand(syntax = "set default classification of derivate {0} to {1}",
        help = "Sets the classification of derivate {0} to the categories {1} (comma separated) "
            + "of classification 'derivate_types' or any fully qualified category, if no classification is present.")
    public static void setClassificationOfDerivateIfNotPresent(String derivateIDStr, String categoriesCommaList)
        throws MCRAccessException {
        final MCRObjectID derivateID = MCRObjectID.getInstance(derivateIDStr);

        if (!MCRMetadataManager.exists(derivateID)) {
            throw new MCRException("The derivate " + derivateIDStr + " does not exist!");
        }

        final MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateID);
        final List<MCRMetaClassification> classifications = derivate.getDerivate().getClassifications();
        if (classifications.isEmpty()) {
            setClassificationOfDerivate(derivateIDStr, categoriesCommaList);
        } else {
            LOGGER.info("Derivate {} already has classifications, skipping.", derivateIDStr);
        }
    }

    @MCRCommand(syntax = "set default main file of derivate {0}",
        help = "Sets the main file of the derivate {0} to the first file found in the derivate")
    public static void setDefaultMainFile(String derID) throws IOException {
        MCRObjectID derivateID = MCRObjectID.getInstance(derID);

        if (!MCRMetadataManager.exists(derivateID)) {
            LOGGER.error("Derivate with ID {} does not exist", derID);
            return;
        }
        MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateID);
        String mainDoc = derivate.getDerivate().getInternals().getMainDoc();
        if (mainDoc != null && !mainDoc.isEmpty()) {
            LOGGER.info("{} already has a main file set to {}", derID, mainDoc);
            return;
        }

        MCRPath path = MCRPath.getPath(derivateID.toString(), "/");
        MCRUploadHelper.detectMainFile(path).ifPresent(file -> {
            LOGGER.info("Setting main file of {} to {}", () -> derID, file::toUri);
            derivate.getDerivate().getInternals().setMainDoc(file.getOwnerRelativePath());
            try {
                MCRMetadataManager.update(derivate);
            } catch (MCRPersistenceException | MCRAccessException e) {
                LOGGER.error("Could not set main file!", e);
            }
        });
    }

    @MCRCommand(syntax = "clear derivate export transformer cache",
        help = "Clears the derivate export transformer cache",
        order = 200)
    public static void clearExportTransformerCache() {
        TRANSFORMER_CACHE.clear();
    }

}
