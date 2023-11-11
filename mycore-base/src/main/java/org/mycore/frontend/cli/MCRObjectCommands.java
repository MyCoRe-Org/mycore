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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.FailableBiConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.access.MCRAccessException;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRStreamUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRBaseContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRSourceContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.common.xml.MCREntityResolver;
import org.mycore.common.xml.MCRLayoutTransformerFactory;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.common.xml.MCRXSLTransformerUtils;
import org.mycore.common.xsl.MCRErrorListener;
import org.mycore.datamodel.common.MCRAbstractMetadataVersion;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaEnrichedLinkID;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectUtils;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.tools.MCRTopologicalSort;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * Provides static methods that implement commands for the MyCoRe command line interface. Robert: Ideas for clean-up -
 * "transform ..." and "xslt..." do the same thing and should thereform be named uniquely - "transformm ...." -
 * "delete by Query ..." can be deleted - "select ..." and "delete selected ..." supply the same behaviour in 2 commands
 * - "list objects matching ..." can be deleted - "select ..." and "list selected" supply the same behaviour in 2
 * commands
 *
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * @author Robert Stephan
 */
@MCRCommandGroup(name = "Object Commands")
public class MCRObjectCommands extends MCRAbstractCommands {

    private static final String EXPORT_OBJECT_TO_DIRECTORY_WITH_STYLESHEET_COMMAND
        = "export object {0} to directory {1} with stylesheet {2}";

    /** The logger */
    private static final Logger LOGGER = LogManager.getLogger(MCRObjectCommands.class);

    /** Default transformer script */
    public static final String DEFAULT_STYLE
        = MCRConfiguration2.getStringOrThrow("MCR.Layout.Transformer.Factory.XSLFolder")
            + "/save-object.xsl";

    /** Static compiled transformer stylesheets */
    private static final Map<String, Transformer> TRANSFORMER_CACHE = new HashMap<>();

    public static void setSelectedObjectIDs(List<String> selected) {
        LOGGER.info("{} objects selected", selected.size());
        MCRSessionMgr.getCurrentSession().put("mcrSelectedObjects", selected);
    }

    @SuppressWarnings("unchecked")
    public static List<String> getSelectedObjectIDs() {
        final List<String> list = (List<String>) MCRSessionMgr.getCurrentSession().get("mcrSelectedObjects");
        if (list == null) {
            return Collections.EMPTY_LIST;
        }
        return list;
    }

    @MCRCommand(
        syntax = "select objects with xpath {0}",
        help = "Selects MCRObjects with XPath {0}, if that XPath evaluates to a non-empty result list" +
            " (this command may take a while, use with care in case of a large number of objects)",
        order = 10)
    public static void selectObjectsWithXpath(String xPath) {

        XPathExpression<Object> xPathExpression = XPathFactory
            .instance()
            .compile(xPath, Filters.fpassthrough(), null, MCRConstants.getStandardNamespaces());

        List<String> selectedObjectIds = MCRXMLMetadataManager
            .instance()
            .listIDs()
            .stream()
            .filter(id -> !id.contains("_derivate_"))
            .map(MCRObjectID::getInstance)
            .map(MCRMetadataManager::retrieveMCRObject)
            .filter(mcrObject -> !xPathExpression.evaluate(mcrObject.createXML()).isEmpty())
            .map(MCRObject::getId)
            .map(MCRObjectID::toString)
            .collect(Collectors.toList());

        MCRObjectCommands.setSelectedObjectIDs(selectedObjectIds);

    }

    @MCRCommand(
        syntax = "select descendants of object {0}",
        help = "Selects MCRObjects that are descendants of {0} (children, grandchildren, ...) and {0} itself.",
        order = 15)
    public static void selectDescendantObjects(String id) {
        List<String> descendants = new ArrayList<>();
        if (MCRMetadataManager.exists(MCRObjectID.getInstance(id))) {
            fillWithDescendants(id, descendants);
        }
        MCRObjectCommands.setSelectedObjectIDs(descendants);
    }

    private static void fillWithDescendants(String mcrObjID, List<String> descendants) {
        descendants.add(mcrObjID);
        //add child objects
        for (String childID : MCRLinkTableManager.instance().getSourceOf(mcrObjID,
            MCRLinkTableManager.ENTRY_TYPE_PARENT)) {
            descendants.add(childID);
            fillWithDescendants(childID, descendants);
        }
    }

    /**
     * Delete all MCRObject from the datastore for a given type.
     *
     * @param type
     *            the type of the MCRObjects that should be deleted
     */
    @MCRCommand(
        syntax = "delete all objects of type {0}",
        help = "Removes MCRObjects of type {0}.",
        order = 20)
    public static List<String> deleteAllObjects(String type) {
        return MCRCommandUtils.getIdsForType(type)
            .map(id -> "delete object " + id)
            .collect(Collectors.toList());
    }

    /**
     * Delete all MCRObjects from the datastore in topological order
     *
     */
    @MCRCommand(
        syntax = "delete all objects in topological order",
        help = "Removes all MCRObjects in topological order.",
        order = 25)
    public static List<String> deleteTopologicalAllObjects() {
        final List<String> objectIds = MCRXMLMetadataManager.instance().listIDs();
        String[] objects = objectIds.stream().filter(id -> !id.contains("_derivate_")).toArray(String[]::new);
        MCRTopologicalSort<String> ts = new MCRTopologicalSort<>();
        MCRTopologicalSort.prepareMCRObjects(ts, objects);
        int[] order = ts.doTopoSort();

        List<String> cmds = new ArrayList<>(objectIds.size());
        if (order != null) {
            //delete in reverse order
            for (int o = order.length - 1; o >= 0; o--) {
                cmds.add("delete object " + ts.getNodeName(order[o]));
            }
        }
        return cmds;
    }

    @MCRCommand(
        syntax = "check for circles in topological order",
        help = "Checks if there are circular dependencies in the parent child relationships of MCRObjects.",
        order = 25)
    public static void checkForCircles() {
        final List<String> objectIds = MCRXMLMetadataManager.instance().listIDs();
        String[] objects = objectIds.stream().filter(id -> !id.contains("_derivate_")).toArray(String[]::new);
        MCRTopologicalSort<String> ts = new MCRTopologicalSort<>();
        MCRTopologicalSort.prepareMCRObjects(ts, objects);
        int[] order = ts.doTopoSort();
        if (order != null) {
            LOGGER.info("OK - No circles detected!");
        }
    }

    /**
     * Delete a MCRObject from the datastore.
     *
     * @param id
     *            the id of the MCRObject that should be deleted
     * @throws MCRPersistenceException  if a persistence problem is occurred
     * @throws MCRAccessException see {@link MCRMetadataManager#deleteMCRObject(MCRObjectID)}
     * @throws MCRActiveLinkException if object is referenced by other objects
     */
    @MCRCommand(
        syntax = "delete object {0}",
        help = "Removes a MCRObject with the MCRObjectID {0}",
        order = 40)
    public static void delete(String id) throws MCRPersistenceException, MCRActiveLinkException, MCRAccessException {
        MCRObjectID mcrId = MCRObjectID.getInstance(id);
        MCRMetadataManager.deleteMCRObject(mcrId);
        LOGGER.info("{} deleted.", mcrId);
    }

    /**
     * Runs though all mycore objects which are linked with the given object and removes its link. This includes
     * parent/child relations and all {@link MCRMetaLinkID} in the metadata section.
     *
     * @param id
     *            the id of the MCRObject that should be deleted
     * @throws MCRPersistenceException  if a persistence problem is occurred
     */
    @MCRCommand(
        syntax = "clear links of object {0}",
        help = "removes all links of this object, including parent/child relations"
            + " and all MetaLinkID's in the metadata section",
        order = 45)
    public static void clearLinks(String id) throws MCRPersistenceException {
        final MCRObjectID mcrId = MCRObjectID.getInstance(id);
        AtomicInteger counter = new AtomicInteger(0);
        MCRObjectUtils.removeLinks(mcrId).forEach(linkedObject -> {
            try {
                LOGGER.info("removing link '{}' of '{}'.", mcrId, linkedObject.getId());
                MCRMetadataManager.update(linkedObject);
                counter.incrementAndGet();
            } catch (Exception exc) {
                LOGGER.error(String.format(Locale.ROOT, "Unable to update object '%s'", linkedObject), exc);
            }
        });
        LOGGER.info("{} link(s) removed of {}.", counter.get(), mcrId);
    }

    /**
     * Delete MCRObject's form ID to ID from the datastore.
     *
     * @param idFrom
     *            the start ID for deleting the MCRObjects
     * @param idTo
     *            the stop ID for deleting the MCRObjects
     * @return list of delete commands
     */
    @MCRCommand(
        syntax = "delete object from {0} to {1}",
        help = "Removes MCRObjects in the number range between the MCRObjectID {0} and {1}.",
        order = 30)
    public static List<String> deleteFromTo(String idFrom, String idTo) {
        return MCRCommandUtils.getIdsFromIdToId(idFrom, idTo)
            .map(id -> "delete object " + id)
            .collect(Collectors.toList());
    }

    /**
     * Load MCRObject's from all XML files in a directory in proper order (respecting parent-child-relationships).
     *
     * @param directory
     *            the directory containing the XML files
     */
    @MCRCommand(
        syntax = "load all objects in topological order from directory {0}",
        help = "Loads all MCRObjects form the directory {0} to the system "
            + "respecting the order of parents and children.",
        order = 75)
    public static List<String> loadTopologicalFromDirectory(String directory) {
        return processFromDirectory(true, directory, false);
    }

    /**
     * Update MCRObject's from all XML files in a directory in proper order (respecting parent-child-relationships).
     *
     * @param directory
     *            the directory containing the XML files
     */
    @MCRCommand(
        syntax = "update all objects in topological order from directory {0}",
        help = "Updates all MCRObjects from the directory {0} in the system "
            + "respecting the order of parents and children.",
        order = 95)
    public static List<String> updateTopologicalFromDirectory(String directory) {
        return processFromDirectory(true, directory, true);
    }

    /**
     * Load MCRObject's from all XML files in a directory.
     *
     * @param directory
     *            the directory containing the XML files
     */
    @MCRCommand(
        syntax = "load all objects from directory {0}",
        help = "Loads all MCRObjects from the directory {0} to the system. " +
            "If the numerical part of a provided ID is zero, a new ID with the same project ID and type is assigned.",
        order = 70)
    public static List<String> loadFromDirectory(String directory) {
        return processFromDirectory(false, directory, false);
    }

    /**
     * Update MCRObject's from all XML files in a directory.
     *
     * @param directory
     *            the directory containing the XML files
     */
    @MCRCommand(
        syntax = "update all objects from directory {0}",
        help = "Updates all MCRObjects from the directory {0} in the system.",
        order = 90)
    public static List<String> updateFromDirectory(String directory) {
        return processFromDirectory(false, directory, true);
    }

    /**
     * Load or update MCRObject's from all XML files in a directory.
     *
     * @param topological
     *            if true, the dependencies of parent and child objects will be respected
     * @param directory
     *            the directory containing the XML files
     * @param update
     *            if true, object will be updated, else object is created
     */
    private static List<String> processFromDirectory(boolean topological, String directory, boolean update) {
        File dir = new File(directory);

        if (!dir.isDirectory()) {
            LOGGER.warn("{} ignored, is not a directory.", directory);
            return null;
        }

        String[] list = dir.list();
        if (list == null || list.length == 0) {
            LOGGER.warn("No files found in directory {}", directory);
            return null;
        }

        Predicate<String> isMetaXML = file -> file.endsWith(".xml") && !file.contains("derivate");
        Function<String, String> cmdFromFile = file -> (update ? "update" : "load") + " object from file "
            + new File(dir, file).getAbsolutePath();
        if (topological) {
            MCRTopologicalSort<String> ts = new MCRTopologicalSort<>();
            MCRTopologicalSort.prepareData(ts, list, dir.toPath());
            return Optional.ofNullable(ts.doTopoSort())
                .map(Arrays::stream)
                .map(is -> is.mapToObj(i -> list[i]))
                .orElse(Stream.empty())
                .filter(isMetaXML)
                .map(cmdFromFile)
                .collect(Collectors.toList());
        } else {
            return Arrays.stream(list)
                .filter(isMetaXML)
                .sorted()
                .map(cmdFromFile)
                .collect(Collectors.toList());
        }
    }

    /**
     * Load a MCRObjects from an XML file.
     *
     * @param file
     *            the location of the xml file
     * @throws MCRAccessException see {@link MCRMetadataManager#create(MCRObject)}
     */
    @MCRCommand(
        syntax = "load object from file {0}",
        help = "Loads an MCRObject from the file {0} to the system. " +
            "If the numerical part of the provided ID is zero, a new ID with the same project ID and type is assigned.",
        order = 60)
    public static boolean loadFromFile(String file) throws MCRException, IOException, MCRAccessException,
        JDOMException {
        return loadFromFile(file, true) != null;
    }

    /**
     * Load a MCRObjects from an XML file.
     *
     * @param file
     *            the location of the xml file
     * @param importMode
     *            if true, servdates are taken from xml file
     * @throws MCRAccessException see {@link MCRMetadataManager#update(MCRObject)}
     */
    public static MCRObject loadFromFile(String file, boolean importMode)
        throws MCRException, IOException, MCRAccessException, JDOMException {
        return processFromFile(new File(file), false, importMode);
    }

    /**
     * Update a MCRObject's from an XML file.
     *
     * @param file
     *            the location of the xml file
     * @throws MCRAccessException see {@link MCRMetadataManager#update(MCRObject)}
     */
    @MCRCommand(
        syntax = "update object from file {0}",
        help = "Updates a MCRObject from the file {0} in the system.",
        order = 80)
    public static boolean updateFromFile(String file)
        throws MCRException, IOException, MCRAccessException, JDOMException {
        return updateFromFile(file, true) != null;
    }

    /**
     * Update a MCRObject's from an XML file.
     *
     * @param file
     *            the location of the xml file
     * @param importMode
     *            if true, servdates are taken from xml file
     * @throws MCRAccessException see {@link MCRMetadataManager#update(MCRObject)}
     * @return the updated object
     */
    public static MCRObject updateFromFile(String file, boolean importMode)
        throws MCRException, IOException, MCRAccessException, JDOMException {
        return processFromFile(new File(file), true, importMode);
    }

    /**
     * Load or update an MCRObject from an XML file. If the numerical part of the contained ID is zero,
     * a new ID with the same project ID and type is assigned.
     *
     * @param file
     *            the location of the xml file
     * @param update
     *            if true, object will be updated, else object is created
     * @param importMode
     *            if true, servdates are taken from xml file
     * @throws SAXParseException
     *            unable to build the mycore object from the file's URI
     * @throws MCRException
     *            the parent of the given object does not exists
     * @throws MCRAccessException
     *            if write permission is missing
     * @return the created or updated object
     */
    private static MCRObject processFromFile(File file, boolean update, boolean importMode)
        throws MCRException, IOException, MCRAccessException, JDOMException {
        if (!file.getName().endsWith(".xml")) {
            LOGGER.warn("{} ignored, does not end with *.xml", file);
            return null;
        }

        if (!file.isFile()) {
            LOGGER.warn("{} ignored, is not a file.", file);
            return null;
        }

        LOGGER.info("Reading file {} ...", file);

        MCRObject mcrObject = new MCRObject(file.toURI());
        if (mcrObject.hasParent()) {
            MCRObjectID parentID = mcrObject.getStructure().getParentID();
            if (!MCRMetadataManager.exists(mcrObject.getStructure().getParentID())) {
                throw new MCRException("The parent object " + parentID + "does not exist for " + mcrObject + ".");
            }
        }
        mcrObject.setImportMode(importMode);
        LOGGER.debug("Label --> {}", mcrObject.getLabel());

        if (update) {
            MCRMetadataManager.update(mcrObject);
            LOGGER.info("{} updated.", mcrObject.getId());
        } else {
            MCRMetadataManager.create(mcrObject);
            LOGGER.info("{} loaded.", mcrObject.getId());
        }

        return mcrObject;
    }

    /**
     * Shows the next free MCRObjectIDs.
     *
     * @param base
     *            the base String of the MCRObjectID
     */
    public static void showNextID(String base) {

        try {
            LOGGER.info("The next free ID  is {}", MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId(base));
        } catch (MCRException ex) {
            LOGGER.error(ex.getMessage());
        }
    }

    /**
     * Shows the last used MCRObjectIDs.
     *
     * @param base
     *            the base String of the MCRObjectID
     */
    public static void showLastID(String base) {
        try {
            LOGGER.info("The last used ID  is {}",
                MCRMetadataManager.getMCRObjectIDGenerator().getLastID(base));
        } catch (MCRException ex) {
            LOGGER.error(ex.getMessage());
        }
    }

    /**
     * Export an MCRObject to a file named <em>MCRObjectID</em>.xml in a directory named <em>dirname</em>.
     * The method uses the converter stylesheet <em>style</em>.xsl.
     *
     * @param id
     *            the id of the MCRObject to save.
     * @param dirname
     *            the dirname to store the object
     * @param style
     *            the name of the stylesheet, prefix of <em>style</em>-object.xsl
     */
    @MCRCommand(
        syntax = EXPORT_OBJECT_TO_DIRECTORY_WITH_STYLESHEET_COMMAND,
        help = "Stores the object with the MCRObjectID {0} to the directory {1}" +
            " with the stylesheet {2}. "
            + "The default for {2} is '{MCR.Layout.Transformer.Factory.XSLFolder}/save-object.xsl'.",
        order = 110)
    public static void exportWithStylesheet(String id, String dirname, String style) {
        exportWithStylesheet(id, id, dirname, style);
    }

    /**
     * Export an MCRObject to a file named <em>MCRObjectID</em>.xml in a directory named <em>dirname</em>.
     * The method use the content transformer <em>transname</em>xsl.
     *
     * @param id
     *            the id of the MCRObject to save.
     * @param dirname
     *            the dirname to store the object
     * @param transname
     *            the name of the transformer
     */
    @MCRCommand(
        syntax = "export object {0} to directory {1} with transformer {2}",
        help = "Stores the object with the MCRObjectID {0} to the directory {1}" +
            " with the transformer {2}.",
        order = 110)
    public static void exportWithTransformer(String id, String dirname, String transname) {
        exportWithTransformer(id, id, dirname, transname);
    }

    /**
     * Export any MCRObject's to files named <em>MCRObjectID</em>.xml in a directory named <em>dirname</em>.
     * Exporting starts with <em>fromID</em> and ends with <em>toID</em>. IDs that aren't found will be skipped.
     * The method uses the converter stylesheet <em>style</em>.xsl.
     *
     * @param fromID
     *            the first ID of the MCRObjects to save.
     * @param toID
     *            the last ID of the MCRObjects to save.
     * @param dirname
     *            the filename to store the object
     * @param style
     *            the name of the stylesheet, prefix of <em>style</em>-object.xsl
     */
    @MCRCommand(
        syntax = "export objects from {0} to {1} to directory {2} with stylesheet {3}",
        help = "Stores all objects with MCRObjectID's between {0} and {1} to the directory {2} "
            + "with the stylesheet {3}. "
            + "The default for {3} is '{MCR.Layout.Transformer.Factory.XSLFolder}/save-object.xsl'.",
        order = 100)
    public static void exportWithStylesheet(String fromID, String toID, String dirname, String style) {
        Transformer transformer
            = MCRCommandUtils.getTransformer(StringUtils.defaultIfEmpty(style, DEFAULT_STYLE), TRANSFORMER_CACHE);
        String extension = MCRXSLTransformerUtils.getFileExtension(transformer, "xml");
        exportWith(fromID, toID, dirname, extension, (content, out) -> {
            StreamResult sr = new StreamResult(out);
            JDOMSource doc = new JDOMSource(MCRXMLParserFactory.getNonValidatingParser().parseXML(content));
            transformer.transform(doc, sr);
        });
    }

    /**
     * Export any MCRObject's to files named <em>MCRObjectID</em>.xml in a directory named <em>dirname</em>.
     * Exporting starts with <em>fromID</em> and ends with <em>toID</em>. IDs that aren't found will be skipped.
     * The method use the content transformer <em>transname</em>xsl.
     *
     * @param fromID
     *            the first ID of the MCRObjects to save.
     * @param toID
     *            the last ID of the MCRObjects to save.
     * @param dirname
     *            the filename to store the object
     * @param transname
     *            the name of the transformer
     */
    @MCRCommand(
        syntax = "export objects from {0} to {1} to directory {2} with transformer {3}",
        help = "Stores all objects with MCRObjectID's between {0} and {1} to the directory {2} "
            + "with the transformer {3}.",
        order = 100)
    public static void exportWithTransformer(String fromID, String toID, String dirname, String transname) {
        MCRContentTransformer transformer = MCRContentTransformerFactory.getTransformer(transname);
        exportWith(fromID, toID, dirname, getExtension(transformer), transformer::transform);
    }

    private static String getExtension(MCRContentTransformer transformer) {
        try {
            return transformer.getFileExtension();
        } catch (Exception e) {
            throw new MCRException(e);
        }
    }

    private static void exportWith(String fromID, String toID, String dirname, String extension,
        FailableBiConsumer<MCRContent, OutputStream, Exception> trans) {

        // check fromID
        MCRObjectID fid;
        try {
            fid = MCRObjectID.getInstance(fromID);
        } catch (Exception ex) {
            LOGGER.error("FromID : {}", ex.getMessage());
            return;
        }

        // check toID
        MCRObjectID tid;
        try {
            tid = MCRObjectID.getInstance(toID);
        } catch (Exception ex) {
            LOGGER.error("ToID : {}", ex.getMessage());
            return;
        }

        // check dirname
        File dir = new File(dirname);
        if (!dir.isDirectory()) {
            LOGGER.error("{} is not a directory.", dirname);
            return;
        }

        int k = 0;
        try {
            for (int i = fid.getNumberAsInteger(); i < tid.getNumberAsInteger() + 1; i++) {
                String id = MCRObjectID.formatID(fid.getProjectId(), fid.getTypeId(), i);
                if (!MCRMetadataManager.exists(MCRObjectID.getInstance(id))) {
                    continue;
                }
                if (!exportObject(dir, extension, trans, id)) {
                    continue;
                }
                k++;
            }
        } catch (Exception ex) {
            LOGGER.error("Exception while storing object to " + dir.getAbsolutePath(), ex);
            return;
        }
        LOGGER.info("{} Object's stored under {}.", k, dir.getAbsolutePath());
    }

    /**
     * Export all MCRObject's with data type <em>type</em> to files named <em>MCRObjectID</em>.xml in a directory
     * named <em>dirname</em>. The method uses the converter stylesheet <em>style</em>.xsl.
     *
     * @param type
     *            the MCRObjectID type
     * @param dirname
     *            the filename to store the object
     * @param style
     *            the name of the stylesheet, prefix of <em>style</em>-object.xsl
     * @return a list of export commands, one for each object
     */
    @MCRCommand(
        syntax = "export all objects of type {0} to directory {1} with stylesheet {2}",
        help = "Stores all objects of type {0} to directory {1} with the stylesheet {2} ."
            + "The default for {2} is '{MCR.Layout.Transformer.Factory.XSLFolder}/save-object.xsl'.",
        order = 120)
    public static List<String> exportAllObjectsOfTypeWithStylesheet(String type, String dirname, String style) {
        List<String> objectIds = MCRXMLMetadataManager.instance().listIDsOfType(type);
        return buildExportCommands(new File(dirname), style, objectIds);
    }

    /**
     * Export all MCRObject's with data base <em>base</em> to files named <em>MCRObjectID</em>.xml in a directory
     * named <em>dirname</em>. The method uses the converter stylesheet <em>style</em>.xsl.
     *
     * @param base
     *            the MCRObjectID base
     * @param dirname
     *            the filename to store the object
     * @param style
     *            the name of the stylesheet, prefix of <em>style</em>-object.xsl
     * @return a list of export commands, one for each object
     */
    @MCRCommand(
        syntax = "export all objects of base {0} to directory {1} with stylesheet {2}",
        help = "Stores all objects of base {0} to directory {1} with the stylesheet {2} ."
            + "The default for {2} is '{MCR.Layout.Transformer.Factory.XSLFolder}/save-object.xsl'.",
        order = 130)
    public static List<String> exportAllObjectsOfBaseWithStylesheet(String base, String dirname, String style) {
        List<String> objectIds = MCRXMLMetadataManager.instance().listIDsForBase(base);
        return buildExportCommands(new File(dirname), style, objectIds);
    }

    private static List<String> buildExportCommands(File dir, String style, List<String> objectIds) {
        if (dir.isFile()) {
            LOGGER.error("{} is not a dirctory.", dir);
            return Collections.emptyList();
        }
        List<String> cmds = new ArrayList<>(objectIds.size());
        for (String id : objectIds) {
            String command = new MessageFormat(EXPORT_OBJECT_TO_DIRECTORY_WITH_STYLESHEET_COMMAND, Locale.ROOT)
                .format(new Object[] { id, dir.getAbsolutePath(), style });
            cmds.add(command);
        }
        return cmds;
    }

    /**
     * The method read a MCRObject and use a transformation to write the data to a file. There aren't any steps to
     * handel errors and save the damaged data.
     * <ul>
     * <li>Read data for object ID in the MCRObject, add ACLs and store it as checked and transformed XML. Return true.
     * </li>
     * <li>If it can't find a transformer instance (no script file found) it store the checked data with ACLs native in
     * the file. Warning and return true.</li>
     * <li>If it get an exception while build the MCRObject, it try to read the XML blob and store it without check and
     * ACLs to the file. Warning and return true.</li>
     * <li>If it get an exception while store the native data without check, ACLs and transformation it return a
     * warning and false.</li>
     * </ul>
     *
     * @param dir
     *            the file instance to store
     * @param extension
     *            the file extension to use
     * @param trans
     *            the transformation
     * @param nid
     *            the MCRObjectID
     * @return true if the store was okay (see description), else return false
     */
    private static boolean exportObject(File dir, String extension,
        FailableBiConsumer<MCRContent, OutputStream, Exception> trans,
        String nid) throws IOException, MCRException {
        MCRContent content;
        try {
            // if object doesn't exist - no exception is caught!
            content = MCRXMLMetadataManager.instance().retrieveContent(MCRObjectID.getInstance(nid));
        } catch (MCRException ex) {
            return false;
        }

        File xmlOutput = new File(dir, nid + "." + extension);

        if (trans != null) {
            FileOutputStream out = new FileOutputStream(xmlOutput);
            try {
                trans.accept(content, out);
            } catch (UncheckedIOException e) {
                throw e.getCause();
            } catch (IOException | RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new MCRException(e);
            }
        } else {
            content.sendTo(xmlOutput);
        }
        LOGGER.info("Object {} saved to {}.", nid, xmlOutput.getCanonicalPath());
        return true;
    }

    /**
     * Get the next free MCRObjectID for the given MCRObjectID base.
     *
     * @param base
     *            the MCRObjectID base string
     */
    @MCRCommand(
        syntax = "get next ID for base {0}",
        help = "Returns the next free MCRObjectID for the ID base {0}.",
        order = 150)
    public static void getNextID(String base) {
        try {
            LOGGER.info(MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId(base));
        } catch (MCRException ex) {
            LOGGER.error(ex.getMessage());
        }
    }

    /**
     * Get the last used MCRObjectID for the given MCRObjectID base.
     *
     * @param base
     *            the MCRObjectID base string
     */

    @MCRCommand(
        syntax = "get last ID for base {0}",
        help = "Returns the last used MCRObjectID for the ID base {0}.",
        order = 140)
    public static void getLastID(String base) {
        LOGGER.info(MCRMetadataManager.getMCRObjectIDGenerator().getLastID(base));
    }

    /**
     * List all selected MCRObjects.
     */
    @MCRCommand(
        syntax = "list selected",
        help = "Prints the id of selected objects",
        order = 190)
    public static void listSelected() {
        LOGGER.info("List selected MCRObjects");
        if (getSelectedObjectIDs().isEmpty()) {
            LOGGER.info("No Resultset to work with, use command \"select objects with solr query {0} in core {1}\"" +
                " or \"select objects with xpath {0}\" to build one");
            return;
        }
        StringBuilder out = new StringBuilder();
        for (String id : getSelectedObjectIDs()) {
            out.append(id).append(' ');
        }
        LOGGER.info(out.toString());
    }

    /**
     * List revisions of an MyCoRe Object.
     *
     * @param id
     *            id of MyCoRe Object
     */
    @MCRCommand(
        syntax = "list revisions of {0}",
        help = "List revisions of MCRObject.",
        order = 260)
    public static void listRevisions(String id) {
        MCRObjectID mcrId = MCRObjectID.getInstance(id);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
        try {
            StringBuilder log = new StringBuilder("Revisions:\n");
            List<? extends MCRAbstractMetadataVersion<?>> revisions = MCRXMLMetadataManager.instance()
                .listRevisions(mcrId);
            for (MCRAbstractMetadataVersion<?> revision : revisions) {
                log.append(revision.getRevision()).append(' ');
                log.append(revision.getType()).append(' ');
                log.append(sdf.format(revision.getDate())).append(' ');
                log.append(revision.getUser());
                log.append("\n");
            }
            LOGGER.info(log.toString());
        } catch (Exception exc) {
            LOGGER.error("While print revisions.", exc);
        }
    }

    /**
     * This method restores a MyCoRe Object to the selected revision. Please note that children and derivates are not
     * deleted or reverted!
     *
     * @param id
     *            id of MyCoRe Object
     * @param revision
     *            revision to restore
     */
    @MCRCommand(
        syntax = "restore {0} to revision {1}",
        help = "Restores the selected MCRObject to the selected revision.",
        order = 270)
    public static void restoreToRevision(String id, String revision) {
        LOGGER.info("Try to restore object {} with revision {}", id, revision);
        MCRObjectID mcrId = MCRObjectID.getInstance(id);
        try {
            MCRObjectUtils.restore(mcrId, revision);
            LOGGER.info("Object {} successfully restored!", id);
        } catch (Exception exc) {
            LOGGER.error("While retrieving object {} with revision {}", id, revision, exc);
        }
    }

    /**
     * Does a xsl transform with the given mycore object.
     * <p>
     * To use this command create a new xsl file and copy following xslt code into it.
     * </p>
     *
     * <pre>
     * {@code
     * <?xml version="1.0" encoding="utf-8"?>
     * <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
     *
     *   <xsl:template match='@*|node()'>
     *     <!-- default template: just copy -->
     *     <xsl:copy>
     *       <xsl:apply-templates select='@*|node()' />
     *     </xsl:copy>
     *   </xsl:template>
     *
     * </xsl:stylesheet>
     * }
     * </pre>
     * <p>
     * Insert a new template match, for example:
     * </p>
     *
     * <pre>
     * {@code
     * <xsl:template match="metadata/mainTitle/@heritable">
     *   <xsl:attribute name="heritable"><xsl:value-of select="'true'"/></xsl:attribute>
     * </xsl:template>
     * }
     * </pre>
     *
     * @param objectId
     *            object to transform
     * @param xslFilePath
     *            path to xsl file
     * @throws MCRPersistenceException see {@link MCRMetadataManager#update(MCRObject)}
     * @throws MCRAccessException see {@link MCRMetadataManager#update(MCRObject)}
     */
    @MCRCommand(
        syntax = "xslt {0} with file {1}",
        help = "transforms a mycore object {0} with the given file or URI {1}",
        order = 280)
    public static void xslt(String objectId, String xslFilePath) throws IOException, JDOMException, SAXException,
        TransformerException, MCRPersistenceException, MCRAccessException,
        ParserConfigurationException {
        xslt(objectId, xslFilePath, false);
    }

    /**
     * @see #xslt(String, String)
     *
     * Forces the xml to overwrite even if the root name of the original and the result differs.
     *
     * @param objectId
     *            object to transform
     * @param xslFilePath
     *            path to xsl file
     * @throws MCRPersistenceException see {@link MCRMetadataManager#update(MCRObject)}
     * @throws MCRAccessException see {@link MCRMetadataManager#update(MCRObject)}
     */
    @MCRCommand(
        syntax = "force xslt {0} with file {1}",
        help = "transforms a mycore object {0} with the given file or URI {1}. Overwrites anyway if original "
            + "root name and result root name are different.",
        order = 285)
    public static void forceXSLT(String objectId, String xslFilePath) throws IOException, JDOMException, SAXException,
        TransformerException, MCRPersistenceException, MCRAccessException,
        ParserConfigurationException {
        xslt(objectId, xslFilePath, true);
    }

    private static void xslt(String objectId, String xslFilePath, boolean force) throws IOException, JDOMException,
        SAXException, TransformerException, MCRPersistenceException, MCRAccessException, ParserConfigurationException {
        File xslFile = new File(xslFilePath);
        Source xslSource;
        if (xslFile.exists()) {
            xslSource = new StreamSource(xslFile);
        } else {
            xslSource = MCRURIResolver.instance().resolve(xslFilePath, null);
            if (xslSource == null) {
                xslSource = new StreamSource(xslFilePath);
            }
        }
        MCRSourceContent style = new MCRSourceContent(xslSource);
        MCRObjectID mcrId = MCRObjectID.getInstance(objectId);
        Document document = MCRXMLMetadataManager.instance().retrieveXML(mcrId);
        // do XSL transform
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setErrorListener(MCRErrorListener.getInstance());
        transformerFactory.setURIResolver(MCRURIResolver.instance());
        XMLReader xmlReader = MCRXMLParserFactory.getNonValidatingParser().getXMLReader();
        xmlReader.setEntityResolver(MCREntityResolver.instance());
        SAXSource styleSource = new SAXSource(xmlReader, style.getInputSource());
        Transformer transformer = transformerFactory.newTransformer(styleSource);
        for (Entry<String, String> property : MCRConfiguration2.getPropertiesMap().entrySet()) {
            transformer.setParameter(property.getKey(), property.getValue());
        }
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        JDOMResult result = new JDOMResult();
        transformer.transform(new JDOMSource(document), result);
        Document resultDocument = Objects.requireNonNull(result.getDocument(), "Could not get transformation result");

        String originalName = document.getRootElement().getName();
        String resultName = resultDocument.getRootElement().getName();
        if (!force && !originalName.equals(resultName)) {
            LOGGER.error("{}: root name '{}' does not match result name '{}'.", objectId, originalName, resultName);
            return;
        }

        // update on diff
        if (MCRXMLHelper.deepEqual(document, resultDocument)) {
            return;
        }
        switch (resultName) {
            case MCRObject.ROOT_NAME -> MCRMetadataManager.update(new MCRObject(resultDocument));
            case MCRDerivate.ROOT_NAME -> MCRMetadataManager.update(new MCRDerivate(resultDocument));
            default -> LOGGER.error("Unable to transform '{}' because unknown result root name '{}'.", objectId,
                resultName);
        }
    }

    @MCRCommand(syntax = "transform object {0} with transformer {1}",
        help = "Transforms the object with the id {0} using the transformer with the id {1} and" +
            " updates the object with the result")
    public static void transformObject(String objectIDStr, String transformer)
        throws IOException, JDOMException, MCRAccessException {
        MCRObjectID objectID = MCRObjectID.getInstance(objectIDStr);
        if (!MCRMetadataManager.exists(objectID)) {
            LOGGER.error("The object {} does not exist!", objectID);
            return;
        }

        MCRObject mcrObject = MCRMetadataManager.retrieveMCRObject(objectID);

        MCRBaseContent baseContent = new MCRBaseContent(mcrObject);
        MCRContent result = new MCRLayoutTransformerFactory().getTransformer(transformer).transform(baseContent);
        Document resultXML = result.asXML();
        MCRObject resulting = new MCRObject(resultXML);

        if (!MCRXMLHelper.deepEqual(mcrObject.createXML(), resultXML)) {
            LOGGER.info("The Object changed with the transformation. Execute Update.");
            MCRMetadataManager.update(resulting);
        } else {
            LOGGER.info("The Object did not change with the transformation. Skip Update.");
        }
    }

    /**
     * Moves object to new parent.
     *
     * @param sourceId
     *            object that should be attached to new parent
     * @param newParentId
     *            the ID of the new parent
     * @throws MCRAccessException see {@link MCRMetadataManager#update(MCRObject)}
     */
    @MCRCommand(
        syntax = "set parent of {0} to {1}",
        help = "replaces a parent of an object (first parameter) to the given new one (second parameter)",
        order = 300)
    public static void replaceParent(String sourceId, String newParentId) throws MCRPersistenceException,
        MCRAccessException {
        // child
        MCRObject sourceMCRObject = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(sourceId));
        // old parent
        MCRObjectID oldParentId = sourceMCRObject.getStructure().getParentID();
        MCRObjectID newParentObjectID = MCRObjectID.getInstance(newParentId);

        if (newParentObjectID.equals(oldParentId)) {
            LOGGER.info("Object {} is already child of {}", sourceId, newParentId);
            return;
        }

        MCRObject oldParentMCRObject = null;

        if (oldParentId != null) {
            try {
                oldParentMCRObject = MCRMetadataManager.retrieveMCRObject(oldParentId);
            } catch (Exception exc) {
                LOGGER.error("Unable to get old parent object {}, its probably deleted.", oldParentId, exc);
            }
        }

        // change href to new parent
        LOGGER.info("Setting link in \"{}\" to parent \"{}\"", sourceId, newParentObjectID);
        MCRMetaLinkID parentLinkId = new MCRMetaLinkID("parent", 0);
        parentLinkId.setReference(newParentObjectID, null, null);
        sourceMCRObject.getStructure().setParent(parentLinkId);

        if (oldParentMCRObject != null) {
            // remove Child in old parent
            LOGGER.info("Remove child \"{}\" in old parent \"{}\"", sourceId, oldParentId);
            oldParentMCRObject.getStructure().removeChild(sourceMCRObject.getId());

            LOGGER.info("Update old parent \"{}\n", oldParentId);
            MCRMetadataManager.update(oldParentMCRObject);
        }

        LOGGER.info("Update \"{}\" in datastore (saving new link)", sourceId);
        MCRMetadataManager.update(sourceMCRObject);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Structure: {}", sourceMCRObject.getStructure().isValid());
            LOGGER.debug("Object: {}", sourceMCRObject.isValid());
        }
    }

    /**
     * Check the derivate links in objects of MCR base ID for existing. It looks to the XML store on the disk to get all
     * object IDs.
     *
     * @param baseId
     *            the base part of a MCRObjectID e.g. DocPortal_document
     */
    @MCRCommand(
        syntax = "check derivate entries in objects for base {0}",
        help = "check in all objects with MCR base ID {0} for existing linked derivates",
        order = 400)
    public static void checkDerivatesInObjects(String baseId) {
        if (baseId == null || baseId.length() == 0) {
            LOGGER.error("Base ID missed for check derivate entries in objects for base {0}");
            return;
        }
        MCRXMLMetadataManager mgr = MCRXMLMetadataManager.instance();
        List<String> idList = mgr.listIDsForBase(baseId);
        int counter = 0;
        int maxresults = idList.size();
        for (String objid : idList) {
            counter++;
            LOGGER.info("Processing dataset {} from {} with ID: {}", counter, maxresults, objid);
            // get from data
            MCRObjectID mcrobjid = MCRObjectID.getInstance(objid);
            MCRObject obj = MCRMetadataManager.retrieveMCRObject(mcrobjid);
            List<MCRMetaEnrichedLinkID> derivateEntries = obj.getStructure().getDerivates();
            for (MCRMetaLinkID derivateEntry : derivateEntries) {
                String derid = derivateEntry.getXLinkHref();
                if (!mgr.exists(MCRObjectID.getInstance(derid))) {
                    LOGGER.error("   !!! Missing derivate {} in database for base ID {}", derid, baseId);
                }
            }
        }
        LOGGER.info("Check done for {} entries", Integer.toString(counter));
    }

    /**
     * Checks objects of the specified base ID for validity against their specified schemas.
     *
     * @param baseID
     *            the base part of a MCRObjectID e.g. DocPortal_document
     */
    @MCRCommand(
        syntax = "validate object schema for base {0}",
        help = "Validates all objects of base {0} against their specified schema.",
        order = 401)
    public static List<String> validateObjectsOfBase(String baseID) {
        return MCRCommandUtils.getIdsForBaseId(baseID)
            .map(id -> "validate object schema for ID " + id)
            .collect(Collectors.toList());
    }

    /**
     * Checks objects of the specified type for validity against their specified schemas.
     *
     * @param type
     *            the type of a MCRObjectID e.g. document
     */
    @MCRCommand(
        syntax = "validate object schema for type {0}",
        help = "Validates all object of type {0} against their specified schema.",
        order = 402)
    public static List<String> validateObjectsOfType(String type) {
        return MCRCommandUtils.getIdsForType(type)
            .map(id -> "validate object schema for ID " + id)
            .collect(Collectors.toList());
    }

    /**
     * Check if an object validates against its specified schema.
     *
     * @param objectID
     *            the ID of an object to check
     */
    @MCRCommand(
        syntax = "validate object schema for ID {0}",
        help = "Checks if object {0} validates against its specified schema.",
        order = 404)
    public static void validateObject(String objectID) {
        validateObjectWithTransformer(objectID, null);
    }

    /**
     * Check if an object validates against its specified schema.
     *
     * @param objectID
     *            the ID of an object to check
     * @param style
     *            the name of a stylesheet that the object should be transformed with before validation
     */
    @MCRCommand(
        syntax = "validate object schema for ID {0} after transformation with stylesheet {1}",
        help = "Checks if object {0} validates against its specified schema, "
            + "after being transformed through stylesheet {1}",
        order = 403)
    public static void validateObjectWithTransformer(String objectID, String style) {
        if (objectID == null || objectID.length() == 0) {
            throw new MCRException("ID of an object required to check its schema validity.");
        }
        LOGGER.info("validate object schema for ID " + objectID);
        Transformer trafo = null;
        if (style != null) {
            // getTransformer with non-existent input successfully returns a working transformer
            // that "successfully transforms", an error would be preferable 
            trafo = MCRCommandUtils.getTransformer(StringUtils.defaultIfEmpty(style, DEFAULT_STYLE), TRANSFORMER_CACHE);
            LOGGER.debug("Transformer for stylesheet {} has been loaded.", style);
        }
        MCRObjectID objID = MCRObjectID.getInstance(objectID);
        try {
            doValidateObjectAgainstSchema(objID, trafo);
            LOGGER.info("Object {} successfully validated.", objectID);
        } catch (MCRException e) {
            LOGGER.error("Object {} failed its validation!", objectID);
            throw e;
        }
    }

    private static void doValidateObjectAgainstSchema(MCRObjectID objID, Transformer trans) {
        // MCRMetadataManager -> retrieveMCRObject() -> MCRObject.createXML already validates the contents
        // we need to offer transformation first though, so manual talking to MCRXMLMetadataManager
        // for the object contents, then manually using a validating XML parser later
        MCRXMLMetadataManager mgr = MCRXMLMetadataManager.instance();
        Document doc;
        try {
            doc = mgr.retrieveXML(objID);
        } catch (IOException | JDOMException e) {
            throw new MCRException(
                "Object " + objID.toString() + " could not be retrieved, unable to validate against schema!", e);
        }
        if (doc == null) {
            throw new MCRException("Could not get object " + objID.toString() + " from XML store");
        }
        MCRObject object = new MCRObject(doc);
        try {
            object.validate();
        } catch (MCRException e) {
            throw new MCRException(
                "Object " + objID.toString()
                    + " does not pass basic self-validation, unable to validate against schema!",
                e);
        }
        String schema = object.getSchema();
        if (schema == null) {
            throw new MCRException(
                "Object " + objID.toString() + " has no assigned schema, unable to validate against it!");
        }
        if (trans != null) {
            JDOMResult res = new JDOMResult();
            try {
                trans.transform(new JDOMSource(doc), res);
                doc = Objects.requireNonNull(res.getDocument(), "Could not get transformation result");
            } catch (TransformerException | MCRException | NullPointerException e) {
                throw new MCRException("Object " + objID.toString()
                    + " could not be transformed, unable to validate against schema!",
                    e);
            }
            LOGGER.info("Object {} successfully transformed.", objID.toString());
        }
        try {
            MCRXMLParserFactory.getValidatingParser().parseXML(new MCRJDOMContent(doc));
        } catch (MCRException | JDOMException | IOException e) {
            throw new MCRException("Object " + objID.toString() + " failed to parse against its schema!", e);
        }
    }

    @MCRCommand(
        syntax = "execute for selected {0}",
        help = "Calls the given command multiple times for all selected objects." +
            " The replacement is defined by an {x}.E.g. 'execute for selected set" +
            " parent of {x} to myapp_container_00000001'",
        order = 450)
    public static List<String> executeForSelected(String command) {
        if (!command.contains("{x}")) {
            LOGGER.info("No replacement defined. Use the {x} variable in order to execute your command with all "
                + "selected objects.");
            return Collections.emptyList();
        }
        return getSelectedObjectIDs().stream()
            .map(objID -> command.replaceAll("\\{x}", objID))
            .collect(Collectors.toList());
    }

    /**
     * The method start the repair of the metadata search for a given MCRObjectID type.
     *
     * @param type
     *            the MCRObjectID type
     */
    @MCRCommand(
        syntax = "repair metadata search of type {0}",
        help = "Scans the metadata store for MCRObjects of type {0} and restores them in the search store.",
        order = 170)
    public static List<String> repairMetadataSearch(String type) {
        LOGGER.info("Start the repair for type {}", type);
        return MCRCommandUtils.getIdsForType(type)
            .map(id -> "repair metadata search of ID " + id)
            .collect(Collectors.toList());
    }

    /**
     * The method start the repair of the metadata search for a given MCRObjectID base.
     *
     * @param baseID
     *            the base part of a MCRObjectID e.g. DocPortal_document
     */
    @MCRCommand(
        syntax = "repair metadata search of base {0}",
        help = "Scans the metadata store for MCRObjects of base {0} and restores them in the search store.",
        order = 171)
    public static List<String> repairMetadataSearchForBase(String baseID) {
        LOGGER.info("Start the repair for base {}", baseID);
        return MCRCommandUtils.getIdsForBaseId(baseID)
            .map(id -> "repair metadata search of ID " + id)
            .collect(Collectors.toList());
    }

    @MCRCommand(
        syntax = "repair shared metadata for the ID {0}",
        help = "Retrieves the MCRObject with the MCRObjectID {0} and repairs the shared metadata.",
        order = 172)
    public static void repairSharedMetadata(String id) throws MCRAccessException {
        if (!MCRObjectID.isValid(id)) {
            LOGGER.error("The String {} is not a MCRObjectID.", id);
            return;
        }
        MCRObjectID mid = MCRObjectID.getInstance(id);
        MCRObject obj = MCRMetadataManager.retrieveMCRObject(mid);
        MCRMetadataManager.repairSharedMetadata(obj);
    }

    /**
     * The method start the repair of the metadata search for a given MCRObjectID as String.
     *
     * @param id
     *            the MCRObjectID as String
     */
    @MCRCommand(
        syntax = "repair metadata search of ID {0}",
        help = "Retrieves the MCRObject with the MCRObjectID {0} and restores it in the search store.",
        order = 180)
    public static void repairMetadataSearchForID(String id) {
        LOGGER.info("Start the repair for the ID {}", id);
        if (!MCRObjectID.isValid(id)) {
            LOGGER.error("The String {} is not a MCRObjectID.", id);
            return;
        }
        MCRObjectID mid = MCRObjectID.getInstance(id);
        MCRBase obj = MCRMetadataManager.retrieve(mid);
        MCRMetadataManager.fireRepairEvent(obj);
        LOGGER.info("Repaired {}", mid);
    }

    @MCRCommand(
        syntax = "repair mcrlinkhref table",
        help = "Runs through the whole table and checks for already deleted mcr objects and deletes them.",
        order = 185)
    public static void repairMCRLinkHrefTable() {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<String> fromQuery = em.createQuery("SELECT DISTINCT m.key.mcrfrom FROM MCRLINKHREF m", String.class);
        TypedQuery<String> toQuery = em.createQuery("SELECT DISTINCT m.key.mcrto FROM MCRLINKHREF m", String.class);
        String query = "DELETE FROM MCRLINKHREF m WHERE m.key.mcrfrom IN (:invalidIds) or m.key.mcrto IN (:invalidIds)";
        // open streams
        try (Stream<String> fromStream = fromQuery.getResultStream()) {
            try (Stream<String> toStream = toQuery.getResultStream()) {
                List<String> invalidIds = Stream.concat(fromStream, toStream)
                    .distinct()
                    .filter(MCRObjectID::isValid)
                    .map(MCRObjectID::getInstance)
                    .filter(MCRStreamUtils.not(MCRMetadataManager::exists))
                    .map(MCRObjectID::toString)
                    .collect(Collectors.toList());
                // delete
                em.createQuery(query).setParameter("invalidIds", invalidIds).executeUpdate();
            }
        }
    }

    @MCRCommand(
        syntax = "rebuild mcrlinkhref table for object {0}",
        help = "Rebuilds (remove/create) all entries of the link href table for the given object id.",
        order = 188)
    public static void rebuildMCRLinkHrefTableForObject(String objectId) {
        MCRLinkTableManager.instance().update(MCRObjectID.getInstance(objectId));
    }

    @MCRCommand(syntax = "clear object export transformer cache",
        help = "Clears the object export transformer cache",
        order = 200)
    public static void clearExportTransformerCache() {
        TRANSFORMER_CACHE.clear();
    }

}
