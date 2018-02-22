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

package org.mycore.frontend.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRPathContent;
import org.mycore.common.content.transformer.MCRXSLTransformer;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRAbstractFileSystem;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.utils.MCRTreeCopier;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Provides static methods that implement commands for the MyCoRe command line
 * interface.
 *
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date: 2010-10-29 15:17:03 +0200 (Fri, 29 Oct
 *          2010) $
 */
@MCRCommandGroup(name = "Derivate Commands")
public class MCRDerivateCommands extends MCRAbstractCommands {
    /** The logger */
    private static Logger LOGGER = LogManager.getLogger(MCRDerivateCommands.class.getName());

    /** The ACL interface */
    private static final MCRAccessInterface ACCESS_IMPL = MCRAccessManager.getAccessImpl();

    /** Default transformer script */
    public static final String DEFAULT_TRANSFORMER = "save-derivate.xsl";

    /**
     * deletes all MCRDerivate from the datastore.
     */
    @MCRCommand(syntax = "delete all derivates", help = "Removes all derivates from the repository", order = 10)
    public static List<String> deleteAllDerivates() {
        List<String> ids = MCRXMLMetadataManager.instance().listIDsOfType("derivate");
        List<String> cmds = new ArrayList<>(ids.size());
        for (String id : ids) {
            cmds.add("delete derivate " + id);
        }
        return cmds;
    }

    /**
     * Delete an MCRDerivate from the datastore.
     *
     * @param ID
     *            the ID of the MCRDerivate that should be deleted
     * @throws MCRActiveLinkException
     * @throws MCRAccessException see {@link MCRMetadataManager#delete(MCRDerivate)}
     */
    @MCRCommand(syntax = "delete derivate {0}",
        help = "The command remove a derivate with the MCRObjectID {0}",
        order = 30)
    public static void delete(String ID) throws MCRPersistenceException, MCRActiveLinkException, MCRAccessException {
        MCRObjectID objectID = MCRObjectID.getInstance(ID);
        MCRMetadataManager.deleteMCRDerivate(objectID);
        LOGGER.info("{} deleted.", objectID);
    }

    /**
     * Delete MCRDerivates form ID to ID from the datastore.
     *
     * @param IDfrom
     *            the start ID for deleting the MCRDerivate
     * @param IDto
     *            the stop ID for deleting the MCRDerivate
     * @throws MCRAccessException see {@link MCRMetadataManager#delete(MCRDerivate)}
     */
    @MCRCommand(syntax = "delete derivate from {0} to {1}",
        help = "The command remove derivates in the number range between the MCRObjectID {0} and {1}.",
        order = 20)
    public static void delete(String IDfrom, String IDto)
        throws MCRPersistenceException, MCRActiveLinkException, MCRAccessException {
        int from_i = 0;
        int to_i = 0;

        MCRObjectID from = MCRObjectID.getInstance(IDfrom);
        MCRObjectID to = MCRObjectID.getInstance(IDto);
        from_i = from.getNumberAsInteger();
        to_i = to.getNumberAsInteger();

        if (from_i > to_i) {
            throw new MCRException("The from-to-interval is false.");
        }

        for (int i = from_i; i < to_i + 1; i++) {

            String id = MCRObjectID.formatID(from.getProjectId(), from.getTypeId(), i);
            if (MCRMetadataManager.exists(MCRObjectID.getInstance(id))) {
                delete(id);
            }
        }
    }

    /**
     * Loads MCRDerivates from all XML files in a directory.
     *
     * @param directory
     *            the directory containing the XML files
     */
    @MCRCommand(syntax = "load all derivates from directory {0}",
        help = "The command load all derivates form the directory {0} to the system.",
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
        help = "The command add a derivate form the file {0} to the system.",
        order = 40)
    public static boolean loadFromFile(String file)
        throws SAXParseException, IOException, MCRPersistenceException, MCRAccessException {
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
     * @throws MCRPersistenceException
     */
    public static boolean loadFromFile(String file, boolean importMode)
        throws SAXParseException, IOException, MCRPersistenceException, MCRAccessException {
        return processFromFile(new File(file), false, importMode);
    }

    /**
     * Updates an MCRDerivates from an XML file.
     *
     * @param file
     *            the location of the xml file
     * @throws MCRAccessException see {@link MCRMetadataManager#update(MCRDerivate)}
     * @throws MCRPersistenceException
     */

    @MCRCommand(syntax = "update derivate from file {0}",
        help = "The command update a derivate form the file {0} in the system.",
        order = 50)
    public static boolean updateFromFile(String file)
        throws SAXParseException, IOException, MCRPersistenceException, MCRAccessException {
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
        throws SAXParseException, IOException, MCRPersistenceException, MCRAccessException {
        return processFromFile(new File(file), true, importMode);
    }

    /**
     * Loads or updates an MCRDerivates from an XML file.
     *
     * @param file
     *            the location of the xml file
     * @param update
     *            if true, object will be updated, else object is created
     * @param importMode
     *            if true, servdates are taken from xml file
     * @throws SAXParseException
     * @throws MCRAccessException see {@link MCRMetadataManager#update(MCRDerivate)}
     * @throws MCRPersistenceException
     */
    private static boolean processFromFile(File file, boolean update, boolean importMode) throws SAXParseException,
        IOException, MCRPersistenceException, MCRAccessException {
        if (!file.getName().endsWith(".xml")) {
            LOGGER.warn("{} ignored, does not end with *.xml", file);
            return false;
        }

        if (!file.isFile()) {
            LOGGER.warn("{} ignored, is not a file.", file);
            return false;
        }

        LOGGER.info("Reading file {} ...", file);

        MCRDerivate derivate = new MCRDerivate(file.toURI());
        derivate.setImportMode(importMode);

        // Replace relative path with absolute path of files
        if (derivate.getDerivate().getInternals() != null) {
            String path = derivate.getDerivate().getInternals().getSourcePath();
            path = path.replace('/', File.separatorChar).replace('\\', File.separatorChar);
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

        LOGGER.info("Label --> {}", derivate.getLabel());

        if (update) {
            MCRMetadataManager.update(derivate);
            LOGGER.info("{} updated.", derivate.getId());
            LOGGER.info("");
        } else {
            MCRMetadataManager.create(derivate);
            LOGGER.info("{} loaded.", derivate.getId());
            LOGGER.info("");
        }

        return true;
    }

    /**
     * Save an MCRDerivate to a file named <em>MCRObjectID</em> .xml in a
     * directory with <em>dirname</em> and store the derivate objects in a
     * directory under them named <em>MCRObjectID</em>. The IFS-Attribute of the
     * derivate files aren't saved, for reloading purpose after deleting a
     * derivate in the datastore
     *
     * @param ID
     *            the ID of the MCRDerivate to be save.
     * @param dirname
     *            the dirname to store the derivate
     */
    @MCRCommand(syntax = "show loadable derivate of {0} to directory {1}",
        help = "The command store the derivate with the MCRObjectID {0} to the directory {1}, without ifs-metadata",
        order = 130)
    public static void show(String ID, String dirname) {
        export(ID, ID, dirname, "save");
    }

    /**
     * Save an MCRDerivate to a file named <em>MCRObjectID</em> .xml in a
     * directory with <em>dirname</em> and store the derivate objects in a
     * directory under them named <em>MCRObjectID</em>. The method use the
     * converter stylesheet mcr_<em>style</em>_object.xsl.
     *
     * @param ID
     *            the ID of the MCRDerivate to be save.
     * @param dirname
     *            the dirname to store the derivate
     * @param style
     *            the type of the stylesheet
     */
    @MCRCommand(syntax = "export derivate {0} to directory {1} with {2}",
        help = "The command store the derivate with the MCRObjectID {0} to the directory {1} with the stylesheet {2}-object.xsl. For {2} save is the default.",
        order = 90)
    public static void export(String ID, String dirname, String style) {
        export(ID, ID, dirname, style);
    }

    /**
     * Export any MCRDerivate's to files named <em>MCRObjectID</em> .xml in a
     * directory and the objects under them named <em>MCRObjectID</em>. The
     * saving starts with fromID and runs to toID. ID's they was not found will
     * skiped. The method use the converter stylesheet mcr_<em>style</em>
     * _object.xsl.
     *
     * @param fromID
     *            the ID of the MCRObject from be save.
     * @param toID
     *            the ID of the MCRObject to be save.
     * @param dirname
     *            the filename to store the object
     * @param style
     *            the type of the stylesheet
     */

    @MCRCommand(syntax = "export derivate from {0} to {1} to directory {2} with {3}",
        help = "The command store all derivates with MCRObjectID's between {0} and {1} to the directory {2} with the stylesheet {3}-object.xsl. For {3} save is the default.",
        order = 80)
    public static void export(String fromID, String toID, String dirname, String style) {
        // check fromID and toID
        MCRObjectID fid = null;
        MCRObjectID tid = null;

        try {
            fid = MCRObjectID.getInstance(fromID);
        } catch (Exception ex) {
            LOGGER.error("FromID : {}", ex.getMessage());

            return;
        }

        try {
            tid = MCRObjectID.getInstance(toID);
        } catch (Exception ex) {
            LOGGER.error("ToID : {}", ex.getMessage());

            return;
        }

        // check dirname
        File dir = new File(dirname);

        if (dir.isFile()) {
            LOGGER.error("{} is not a dirctory.", dirname);

            return;
        }

        Transformer trans = getTransformer(style);

        int k = 0;

        try {
            for (int i = fid.getNumberAsInteger(); i < tid.getNumberAsInteger() + 1; i++) {

                exportDerivate(dir, trans, MCRObjectID.formatID(fid.getProjectId(), fid.getTypeId(), i));

                k++;
            }
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
            LOGGER.error("Exception while store file or objects to {}", dir.getAbsolutePath(), ex);

            return;
        }

        LOGGER.info("{} Object's stored under {}.", k, dir.getAbsolutePath());
    }

    /**
     * The command look for all derivates in the application and build export
     * commands.
     *
     * @param dirname
     *            the filename to store the object
     * @param style
     *            the type of the stylesheet
     * @return a list of export commands for each derivate
     */
    @MCRCommand(syntax = "export all derivates to directory {0} with {1}",
        help = "Stores all derivates to the directory {0} with the stylesheet mcr_{1}-derivate.xsl. For {1} save is the default.",
        order = 100)
    public static List<String> exportAllDerivates(String dirname, String style) {
        // check dirname
        File dir = new File(dirname);

        if (dir.isFile()) {
            throw new MCRException(dirname + " is not a dirctory.");
        }

        List<String> ids = MCRXMLMetadataManager.instance().listIDsOfType("derivate");
        List<String> cmds = new ArrayList<>(ids.size());
        for (String id : ids) {
            cmds.add("export derivate " + id + " to directory " + dirname + " with " + style);
        }
        return cmds;
    }

    /**
     * The command look for all derivates starts with project name in the
     * application and build export commands.
     *
     * @param dirname
     *            the filename to store the object
     * @param style
     *            the type of the stylesheet
     * @return a list of export commands for derivates with project name
     */
    @MCRCommand(syntax = "export all derivates of project {0} to directory {1} with {2}",
        help = "Stores all derivates of project {0} to the directory {1} with the stylesheet mcr_{2}-derivate.xsl. For {2} save is the default.",
        order = 110)
    public static List<String> exportAllDerivatesOfProject(String project, String dirname, String style) {
        // check dirname
        File dir = new File(dirname);

        if (dir.isFile()) {
            throw new MCRException(dirname + " is not a dirctory.");
        }

        List<String> ids = MCRXMLMetadataManager.instance().listIDsOfType("derivate");
        List<String> cmds = new ArrayList<>(ids.size());
        for (String id : ids) {
            if (!id.startsWith(project))
                continue;
            cmds.add("export derivate " + id + " to directory " + dirname + " with " + style);
        }
        return cmds;
    }

    /**
     * @param dir
     * @param trans
     * @param nid
     * @throws FileNotFoundException
     * @throws TransformerException
     * @throws IOException
     */
    private static void exportDerivate(File dir, Transformer trans, String nid)
        throws TransformerException, IOException {
        // store the XML file
        Document xml = null;
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
            Collection<String> l = ACCESS_IMPL.getPermissionsForID(nid);
            for (String permission : l) {
                Element rule = ACCESS_IMPL.getRule(nid, permission);
                obj.getService().addRule(permission, rule);
            }
            // build JDOM
            xml = obj.createXML();

        } catch (MCRException ex) {
            LOGGER.warn("Could not read {}, continue with next ID", nid);
            return;
        }
        File xmlOutput = new File(dir, derivateID + ".xml");
        FileOutputStream out = new FileOutputStream(xmlOutput);
        dir = new File(dir, derivateID.toString());

        if (trans != null) {
            trans.setParameter("dirname", dir.getPath());
            StreamResult sr = new StreamResult(out);
            trans.transform(new org.jdom2.transform.JDOMSource(xml), sr);
        } else {
            new org.jdom2.output.XMLOutputter().output(xml, out);
            out.flush();
            out.close();
        }

        LOGGER.info("Object {} stored under {}.", nid, xmlOutput);

        // store the derivate file under dirname
        if (!dir.isDirectory()) {
            dir.mkdir();
        }
        MCRPath rootPath = MCRPath.getPath(derivateID.toString(), "/");
        Files.walkFileTree(rootPath, new MCRTreeCopier(rootPath, dir.toPath()));

        LOGGER.info("Derivate {} saved under {} and {}.", nid, dir, xmlOutput);
    }

    /**
     * @param style
     * @return
     * @throws TransformerFactoryConfigurationError
     */
    private static Transformer getTransformer(String style) throws TransformerFactoryConfigurationError {
        String xslfile = DEFAULT_TRANSFORMER;
        if (style != null && style.trim().length() != 0) {
            xslfile = style + "-derivate.xsl";
        }
        Transformer trans = null;

        try {
            URL xslURL = MCRDerivateCommands.class.getResource("/" + xslfile);

            if (xslURL != null) {
                StreamSource source = new StreamSource(xslURL.toURI().toASCIIString());
                TransformerFactory transfakt = TransformerFactory.newInstance();
                transfakt.setURIResolver(MCRURIResolver.instance());
                trans = transfakt.newTransformer(source);
            }
        } catch (Exception e) {
            LOGGER.debug("Cannot build Transformer.", e);
        }
        return trans;
    }

    /**
     * The method start the repair the content search index for all derivates.
     */
    @MCRCommand(syntax = "repair derivate search of type derivate",
        help = "The command read the Content store and reindex the derivate search stores.",
        order = 140)
    public static List<String> repairDerivateSearch() {
        LOGGER.info("Start the repair for type derivate.");
        List<String> ids = MCRXMLMetadataManager.instance().listIDsOfType("derivate");
        if (ids.size() == 0) {
            LOGGER.warn("No ID's was found for type derivate.");
            return Collections.emptyList();
        }
        List<String> cmds = new ArrayList<>(ids.size());
        for (String id : ids) {
            cmds.add("repair derivate search of ID " + id);
        }
        return cmds;
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
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // handle events
                MCREvent evt = new MCREvent(MCREvent.PATH_TYPE, MCREvent.REPAIR_EVENT);
                evt.put(MCREvent.PATH_KEY, file);
                evt.put(MCREvent.FILEATTR_KEY, attrs);
                MCREventManager.instance().handleEvent(evt);
                LOGGER.debug("repaired file {}", file);
                return super.visitFile(file, attrs);
            }

        });
    }

    /**
     * The method start the repair the content search index for all derivates.
     */
    @MCRCommand(syntax = "synchronize all derivates",
        help = "The command read each derivate and synchronize the xlink:label with the derivate entry of the mycoreobject.",
        order = 160)
    public static List<String> synchronizeAllDerivates() {
        LOGGER.info("Start the synchronization for derivates.");
        List<String> ids = MCRXMLMetadataManager.instance().listIDsOfType("derivate");
        if (ids.size() == 0) {
            LOGGER.warn("No ID's was found for type derivate.");
            return Collections.emptyList();
        }
        List<String> cmds = new ArrayList<>(ids.size());
        for (String id : ids) {
            cmds.add("synchronize derivate with ID " + id);
        }
        return cmds;
    }

    /**
     * The method sychronize the xlink:label of the mycorederivate with the
     * xlink:label of the derivate reference of mycoreobject.
     *
     * @param id
     *            the MCRObjectID as String
     */
    @MCRCommand(syntax = "synchronize derivate with ID {0}",
        help = "The command read a derivate with the MCRObjectID {0} and synchronize the xlink:label with the derivate entry of the mycoreobject.",
        order = 170)
    public static void synchronizeDerivateForID(String id) {
        MCRObjectID mid = null;
        try {
            mid = MCRObjectID.getInstance(id);
        } catch (Exception e) {
            LOGGER.error("The String {} is not a MCRObjectID.", id);
            return;
        }

        // set mycoreobject
        MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(mid);
        String label = der.getLabel();
        String href = der.getDerivate().getMetaLink().getXLinkHref();
        MCRObject obj = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(href));
        int size = obj.getStructure().getDerivates().size();
        boolean isset = false;
        for (int i = 0; i < size; i++) {
            MCRMetaLinkID link = obj.getStructure().getDerivates().get(i);
            if (link.getXLinkHref().equals(mid.toString())) {
                String oldlabel = link.getXLinkLabel();
                if (oldlabel != null && !oldlabel.trim().equals(label)) {
                    obj.getStructure().getDerivates().get(i).setXLinkTitle(label);
                    isset = true;
                }
                break;
            }
        }
        // update mycoreobject
        if (isset) {
            MCRMetadataManager.fireUpdateEvent(obj);
            LOGGER.info("Synchronized {}", mid);
        }
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
            throw new Exception("The object with id " + objID + " does not exist");
        }

        if (!MCRMetadataManager.exists(derID)) {
            throw new Exception("The derivate with id " + derID + " does not exist");
        }

        MCRDerivate derObj = MCRMetadataManager.retrieveMCRDerivate(derID);
        MCRMetaLinkID oldDerivateToObjectLink = derObj.getDerivate().getMetaLink();
        MCRObjectID oldOwnerId = oldDerivateToObjectLink.getXLinkHrefID();

        /* set link to new parent in the derivate object */
        LOGGER.info("Setting {} as parent for derivate {}", objID, derID);
        derObj.getDerivate().getMetaLink()
            .setReference(objID, oldDerivateToObjectLink.getXLinkLabel(), oldDerivateToObjectLink.getXLinkTitle());
        derObj.setLabel("data object from " + objectId + " (prev. owner was " + oldOwnerId);
        MCRMetadataManager.updateMCRDerivateXML(derObj);

        /* set link to derivate in the new parent */
        MCRObject oldOwner = MCRMetadataManager.retrieveMCRObject(oldOwnerId);
        List<MCRMetaLinkID> derivates = oldOwner.getStructure().getDerivates();
        MCRMetaLinkID oldObjectToDerivateLink = null;
        for (MCRMetaLinkID derivate : derivates) {
            if (derivate.getXLinkHrefID().equals(derID)) {
                oldObjectToDerivateLink = derivate;
            }
        }
        if (oldObjectToDerivateLink == null) {
            oldObjectToDerivateLink = new MCRMetaLinkID();
        }
        LOGGER.info("Linking derivate {} to {}", derID, objID);
        MCRMetaLinkID derivateLink = new MCRMetaLinkID();
        derivateLink.setReference(derID, oldObjectToDerivateLink.getXLinkLabel(),
            oldObjectToDerivateLink.getXLinkTitle());
        derivateLink.setSubTag("derobject");
        MCRMetadataManager.addOrUpdateDerivateToObject(objID, derivateLink);

        /* removing link from old parent */
        boolean flag = oldOwner.getStructure().removeDerivate(derID);
        LOGGER.info("Unlinking derivate {} from object {}. Success={}", derID, oldOwnerId, flag);
        MCRMetadataManager.fireUpdateEvent(oldOwner);
    }

    /**
     * Check the object links in derivates of MCR base ID for existing. It looks to the XML store on the disk to get all object IDs.
     *
     * @param base_id
     *            the base part of a MCRObjectID e.g. DocPortal_derivate
     */
    @MCRCommand(syntax = "check object entries in derivates for base {0}",
        help = "check in all derivates of MCR base ID {0} for existing linked objects",
        order = 400)
    public static void checkObjectsInDerivates(String base_id) throws IOException {
        if (base_id == null || base_id.length() == 0) {
            LOGGER.error("Base ID missed for check object entries in derivates for base {0}");
            return;
        }
        int project_part_position = base_id.indexOf('_');
        if (project_part_position == -1) {
            LOGGER.error("The given base ID {} has not the syntax of project_type", base_id);
            return;
        }
        MCRXMLMetadataManager mgr = MCRXMLMetadataManager.instance();
        List<String> id_list = mgr.listIDsForBase(base_id.substring(0, project_part_position + 1) + "derivate");
        int counter = 0;
        int maxresults = id_list.size();
        for (String derid : id_list) {
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
        LOGGER.info("Check done for {} entries", Integer.toString(counter));
    }

    @MCRCommand(syntax = "transform xml matching file name pattern {0} in derivate {1} with stylesheet {2}",
        help = "Finds all files in Derivate {1} which match the pattern {0} (the complete path with regex: or glob:*.xml syntax) and transforms them with stylesheet {2}")
    public static void transformXMLMatchingPatternWithStylesheet(String pattern, String derivate, String stylesheet)
        throws IOException {
        MCRXSLTransformer transformer = new MCRXSLTransformer(stylesheet);
        MCRPath derivateRoot = MCRPath.getPath(derivate, "/");
        PathMatcher matcher = derivateRoot.getFileSystem().getPathMatcher(pattern);

        Files.walkFileTree(derivateRoot, new SimpleFileVisitor<Path>() {
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

                    } catch (JDOMException | SAXException e) {
                        throw new IOException("Error while processing file : " + file, e);
                    }

                }

                return FileVisitResult.CONTINUE;
            }
        });

    }

    @MCRCommand(syntax = "set main file of {0} to {1}", help = "Sets the main file of the derivate with the id {0} to "
        + "the file with the path {1}")
    public static void setMainFile(final String derivateIDString, final String filePath) {
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
            cleanPath = filePath.substring(1, filePath.length());
        }

        // check for file exist
        final MCRPath path = MCRPath.getPath(derivateID.toString(), cleanPath);
        if (!Files.exists(path)) {
            LOGGER.error("File {} does not exist!", cleanPath);
            return;
        }

        final MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateID);
        derivate.getDerivate().getInternals().setMainDoc(cleanPath);
        MCRMetadataManager.updateMCRDerivateXML(derivate);
        LOGGER.info("The main file of {} is now '{}'!", derivateIDString, cleanPath);
    }

}
