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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.mycore.access.MCRAccessException;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.backend.jpa.MCRStreamQuery;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRStreamUtils;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.common.xml.MCREntityResolver;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.common.xsl.MCRErrorListener;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs2.MCRMetadataVersion;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectUtils;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.utils.MCRRecursiveDeleter;
import org.mycore.datamodel.niofs.utils.MCRTreeCopier;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.tools.MCRTopologicalSort;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

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
 * @version $Revision$ $Date$
 */
@MCRCommandGroup(
    name = "Object Commands")
public class MCRObjectCommands extends MCRAbstractCommands {
    private static final String EXPORT_OBJECT_TO_DIRECTORY_COMMAND = "export object {0} to directory {1} with {2}";

    /** The logger */
    private static Logger LOGGER = LogManager.getLogger(MCRObjectCommands.class);

    /** Default transformer script */
    public static final String DEFAULT_TRANSFORMER = "save-object.xsl";

    /** static compiled transformer stylesheets */
    private static Hashtable<String, Transformer> translist = new Hashtable<>();

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
        final List<String> objectIds = MCRXMLMetadataManager.instance().listIDsOfType(type);
        List<String> cmds = new ArrayList<>(objectIds.size());
        for (String id : objectIds) {
            cmds.add("delete object " + id);
        }
        return cmds;
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
        MCRTopologicalSort ts = new MCRTopologicalSort();
        ts.prepareMCRObjects(objects);
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

    /**
     * Delete a MCRObject from the datastore.
     *
     * @param ID
     *            the ID of the MCRObject that should be deleted
     * @throws MCRPersistenceException  if a persistence problem is occurred
     * @throws MCRAccessException see {@link MCRMetadataManager#deleteMCRObject(MCRObjectID)}
     * @throws MCRActiveLinkException if object is referenced by other objects
     */
    @MCRCommand(
        syntax = "delete object {0}",
        help = "Removes a MCRObject with the MCRObjectID {0}",
        order = 40)
    public static void delete(String ID) throws MCRPersistenceException, MCRActiveLinkException, MCRAccessException {
        MCRObjectID mcrId = MCRObjectID.getInstance(ID);
        MCRMetadataManager.deleteMCRObject(mcrId);
        LOGGER.info("{} deleted.", mcrId);
    }

    /**
     * Delete MCRObject's form ID to ID from the datastore.
     *
     * @param IDfrom
     *            the start ID for deleting the MCRObjects
     * @param IDto
     *            the stop ID for deleting the MCRObjects
     * @return list of delete commands
     */
    @MCRCommand(
        syntax = "delete object from {0} to {1}",
        help = "Removes MCRObjects in the number range between the MCRObjectID {0} and {1}.",
        order = 30)
    public static List<String> deleteFromTo(String IDfrom, String IDto) {
        MCRObjectID from = MCRObjectID.getInstance(IDfrom);
        MCRObjectID to = MCRObjectID.getInstance(IDto);
        int from_i = from.getNumberAsInteger();
        int to_i = to.getNumberAsInteger();

        if (from_i > to_i) {
            throw new MCRException("The from-to-interval is false.");
        }
        List<String> cmds = new ArrayList<>(to_i - from_i);

        for (int i = from_i; i < to_i + 1; i++) {
            String id = MCRObjectID.formatID(from.getProjectId(), from.getTypeId(), i);
            if (MCRMetadataManager.exists(MCRObjectID.getInstance(id))) {
                cmds.add("delete object " + id);
            }
        }
        return cmds;
    }

    /**
     * Load MCRObject's from all XML files in a directory in proper order (respecting parent-child-relationships).
     *
     * @param directory
     *            the directory containing the XML files
     */
    @MCRCommand(
        syntax = "load all objects in topological order from directory {0}",
        help = "Loads all MCRObjects form the directory {0} to the system respecting the order of parents and children.",
        order = 75)
    public static List<String> loadTopologicalFromDirectory(String directory) throws MCRActiveLinkException {
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
        help = "Updates all MCRObjects from the directory {0} in the system respecting the order of parents and children.",
        order = 95)
    public static List<String> updateTopologicalFromDirectory(String directory) throws MCRActiveLinkException {
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
        help = "Loads all MCRObjects from the directory {0} to the system.",
        order = 70)
    public static List<String> loadFromDirectory(String directory) throws MCRActiveLinkException {
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
    public static List<String> updateFromDirectory(String directory) throws MCRActiveLinkException {
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
            MCRTopologicalSort ts = new MCRTopologicalSort();
            ts.prepareData(list, dir);
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
        help = "Adds a MCRObject from the file {0} to the system.",
        order = 60)
    public static boolean loadFromFile(String file) throws MCRActiveLinkException, MCRException, SAXParseException,
        IOException, MCRAccessException {
        return loadFromFile(file, true);
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
    public static boolean loadFromFile(String file, boolean importMode) throws MCRActiveLinkException, MCRException,
        SAXParseException, IOException, MCRAccessException {
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
    public static boolean updateFromFile(String file) throws MCRActiveLinkException, MCRException, SAXParseException,
        IOException, MCRAccessException {
        return updateFromFile(file, true);
    }

    /**
     * Update a MCRObject's from an XML file.
     *
     * @param file
     *            the location of the xml file
     * @param importMode
     *            if true, servdates are taken from xml file
     * @throws MCRAccessException see {@link MCRMetadataManager#update(MCRObject)}
     */
    public static boolean updateFromFile(String file, boolean importMode) throws MCRActiveLinkException, MCRException,
        SAXParseException, IOException, MCRAccessException {
        return processFromFile(new File(file), true, importMode);
    }

    /**
     * Load or update an MCRObject's from an XML file.
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
     */
    private static boolean processFromFile(File file, boolean update, boolean importMode)
        throws MCRException, SAXParseException, IOException, MCRAccessException {
        if (!file.getName().endsWith(".xml")) {
            LOGGER.warn("{} ignored, does not end with *.xml", file);
            return false;
        }

        if (!file.isFile()) {
            LOGGER.warn("{} ignored, is not a file.", file);
            return false;
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

        return true;
    }

    /**
     * Shows the next free MCRObjectIDs.
     *
     * @param base
     *            the base String of the MCRObjectID
     */
    public static void showNextID(String base) {

        try {
            LOGGER.info("The next free ID  is {}", MCRObjectID.getNextFreeId(base));
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
            LOGGER.info("The last used ID  is {}", MCRObjectID.getLastID(base));
        } catch (MCRException ex) {
            LOGGER.error(ex.getMessage());
        }
    }

    /**
     * Export an MCRObject to a file named <em>MCRObjectID</em> .xml in a directory. The method use the converter
     * stylesheet mcr_<em>style</em>_object.xsl.
     *
     * @param ID
     *            the ID of the MCRObject to be save.
     * @param dirname
     *            the dirname to store the object
     * @param style
     *            the type of the stylesheet
     */
    @MCRCommand(
        syntax = EXPORT_OBJECT_TO_DIRECTORY_COMMAND,
        help = "Stores the MCRObject with the MCRObjectID {0} to the directory {1} with the stylesheet {2}-object.xsl. For {2} save is the default.",
        order = 110)
    public static void export(String ID, String dirname, String style) {
        export(ID, ID, dirname, style);
    }

    /**
     * Save any MCRObject's to files named <em>MCRObjectID</em> .xml in a directory. The saving starts with fromID and
     * runs to toID. ID's they was not found will skiped. The method use the converter stylesheet mcr_<em>style</em>
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
    @MCRCommand(
        syntax = "export object from {0} to {1} to directory {2} with {3}",
        help = "Stores all MCRObjects with MCRObjectID's between {0} and {1} to the directory {2} with the stylesheet {3}-object.xsl. For {3} save is the default.",
        order = 100)
    public static void export(String fromID, String toID, String dirname, String style) {
        MCRObjectID fid, tid;

        // check fromID and toID
        try {
            fid = MCRObjectID.getInstance(fromID);
            tid = MCRObjectID.getInstance(toID);
        } catch (Exception ex) {
            LOGGER.error("FromID : {}", ex.getMessage());
            return;
        }
        // check dirname
        File dir = new File(dirname);
        if (!dir.isDirectory()) {
            LOGGER.error("{} is not a dirctory.", dirname);
            return;
        }

        int k = 0;
        try {
            Transformer trans = getTransformer(style);
            for (int i = fid.getNumberAsInteger(); i < tid.getNumberAsInteger() + 1; i++) {
                String id = MCRObjectID.formatID(fid.getProjectId(), fid.getTypeId(), i);
                if (!MCRMetadataManager.exists(MCRObjectID.getInstance(id))) {
                    continue;
                }
                if (!exportMCRObject(dir, trans, id)) {
                    continue;
                }
                k++;
            }
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
            LOGGER.error("Exception while store file to {}", dir.getAbsolutePath());
            return;
        }
        LOGGER.info("{} Object's stored under {}.", k, dir.getAbsolutePath());
    }

    /**
     * Save all MCRObject's to files named <em>MCRObjectID</em> .xml in a <em>dirname</em>directory for the data type
     * <em>type</em>. The method use the converter stylesheet mcr_<em>style</em>_object.xsl.
     *
     * @param type
     *            the MCRObjectID type
     * @param dirname
     *            the filename to store the object
     * @param style
     *            the type of the stylesheet
     */
    @MCRCommand(
        syntax = "export all objects of type {0} to directory {1} with {2}",
        help = "Stores all MCRObjects of type {0} to directory {1} with the stylesheet mcr_{2}-object.xsl. For {2} save is the default.",
        order = 120)
    public static List<String> exportAllObjectsOfType(String type, String dirname, String style) {
        List<String> objectIds = MCRXMLMetadataManager.instance().listIDsOfType(type);
        return buildExportCommands(new File(dirname), style, objectIds);
    }

    /**
     * Save all MCRObject's to files named <em>MCRObjectID</em> .xml in a <em>dirname</em>directory for the data base
     * <em>project_type</em>. The method use the converter stylesheet mcr_<em>style</em>_object.xsl.
     *
     * @param base
     *            the MCRObjectID base
     * @param dirname
     *            the filename to store the object
     * @param style
     *            the type of the stylesheet
     */
    @MCRCommand(
        syntax = "export all objects of base {0} to directory {1} with {2}",
        help = "Stores all MCRObjects of base {0} to directory {1} with the stylesheet mcr_{2}-object.xsl. For {2} save is the default.",
        order = 130)
    public static List<String> exportAllObjectsOfBase(String base, String dirname, String style) {
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
            String command = MessageFormat.format(EXPORT_OBJECT_TO_DIRECTORY_COMMAND, id, dir.getAbsolutePath(), style);
            cmds.add(command);
        }
        return cmds;
    }

    /**
     * The method search for a stylesheet mcr_<em>style</em>_object.xsl and build the transformer. Default is
     * <em>mcr_save-object.xsl</em>.
     *
     * @param style
     *            the style attribute for the transformer stylesheet
     * @return the transformer
     */
    private static Transformer getTransformer(String style) {
        String xslfile = DEFAULT_TRANSFORMER;
        if (style != null && style.trim().length() != 0) {
            xslfile = style + "-object.xsl";
        }
        Transformer trans = translist.get(xslfile);
        if (trans != null) {
            return trans;
        }
        LOGGER.debug("Will load transformer stylesheet {}for export.", xslfile);

        URL xslURL = MCRObjectCommands.class.getResource("/" + xslfile);
        if (xslURL == null) {
            xslURL = MCRObjectCommands.class.getResource("/xsl/" + DEFAULT_TRANSFORMER);
        }
        try {
            if (xslURL != null) {
                StreamSource source = new StreamSource(xslURL.toURI().toASCIIString());
                TransformerFactory transfakt = TransformerFactory.newInstance();
                transfakt.setURIResolver(MCRURIResolver.instance());
                trans = transfakt.newTransformer(source);
                translist.put(xslfile, trans);
                return trans;
            } else {
                LOGGER.warn("Can't load transformer ressource {} or " + DEFAULT_TRANSFORMER + ".", xslfile);
            }
        } catch (Exception e) {
            LOGGER.warn("Error while load transformer ressource {} or " + DEFAULT_TRANSFORMER + ".", xslfile);
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * The method read a MCRObject and use the transformer to write the data to a file. They are any steps to handel
     * errors and save the damaged data.
     * <ul>
     * <li>Read data for object ID in the MCRObject, add ACL's and store it as checked and transformed XML. Return true.
     * </li>
     * <li>If it can't find a transformer instance (no script file found) it store the checked data with ACL's native in
     * the file. Warning and return true.</li>
     * <li>If it get an exception while build the MCRObject, it try to read the XML blob and stor it without check and
     * ACL's to the file. Warning and return true.</li>
     * <li>If it get an exception while store the native data without check, ACÖ's and transformation it return a
     * warning and false.</li>
     * </ul>
     *
     * @param dir
     *            the file instance to store
     * @param trans
     *            the XML transformer
     * @param nid
     *            the MCRObjectID
     * @return true if the store was okay (see description), else return false
     * @throws TransformerException
     * @throws IOException
     * @throws MCRException
     * @throws SAXParseException
     */
    private static boolean exportMCRObject(File dir, Transformer trans, String nid)
        throws TransformerException, IOException, MCRException, SAXParseException {
        MCRContent content;
        try {
            // if object do'snt exist - no exception is catched!
            content = MCRXMLMetadataManager.instance().retrieveContent(MCRObjectID.getInstance(nid));
        } catch (MCRException ex) {
            return false;
        }

        File xmlOutput = new File(dir, nid + ".xml");

        if (trans != null) {
            FileOutputStream out = new FileOutputStream(xmlOutput);
            StreamResult sr = new StreamResult(out);
            Document doc = MCRXMLParserFactory.getNonValidatingParser().parseXML(content);
            trans.transform(new org.jdom2.transform.JDOMSource(doc), sr);
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
            LOGGER.info(MCRObjectID.getNextFreeId(base));
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
        LOGGER.info(MCRObjectID.getLastID(base));
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
            LOGGER.info("No Resultset to work with, use command \"select objects with query {0}\" to build one");
            return;
        }
        StringBuilder out = new StringBuilder();
        for (String id : getSelectedObjectIDs()) {
            out.append(id).append(" ");
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
            List<MCRMetadataVersion> revisions = MCRXMLMetadataManager.instance().listRevisions(mcrId);
            for (MCRMetadataVersion revision : revisions) {
                log.append(revision.getRevision()).append(" ");
                log.append(revision.getType()).append(" ");
                log.append(sdf.format(revision.getDate())).append(" ");
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
    public static void restoreToRevision(String id, long revision) {
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
     * @throws URISyntaxException if xslFilePath is not a valid file or URL
     * @throws MCRPersistenceException see {@link MCRMetadataManager#update(MCRObject)}
     * @throws MCRAccessException see {@link MCRMetadataManager#update(MCRObject)}
     */
    @MCRCommand(
        syntax = "xslt {0} with file {1}",
        help = "transforms a mycore object {0} with the given file or URL {1}",
        order = 280)
    public static void xslt(String objectId, String xslFilePath) throws IOException, JDOMException, SAXException,
        URISyntaxException, TransformerException, MCRPersistenceException, MCRAccessException,
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
     * @throws URISyntaxException if xslFilePath is not a valid file or URL
     * @throws MCRPersistenceException see {@link MCRMetadataManager#update(MCRObject)}
     * @throws MCRAccessException see {@link MCRMetadataManager#update(MCRObject)}
     */
    @MCRCommand(
            syntax = "force xslt {0} with file {1}",
            help = "transforms a mycore object {0} with the given file or URL {1}. Overwrites anyway if original "
                    + "root name and result root name are different.",
            order = 285)
    public static void forceXSLT(String objectId, String xslFilePath) throws IOException, JDOMException, SAXException,
        URISyntaxException, TransformerException, MCRPersistenceException, MCRAccessException,
        ParserConfigurationException {
        xslt(objectId, xslFilePath, true);
    }

    private static void xslt(String objectId, String xslFilePath, boolean force)
        throws IOException, JDOMException, SAXException,
        URISyntaxException, TransformerException, MCRPersistenceException, MCRAccessException,
        ParserConfigurationException {
        File xslFile = new File(xslFilePath);
        URL xslURL;
        if (!xslFile.exists()) {
            try {
                xslURL = new URL(xslFilePath);
            } catch (MalformedURLException e) {
                LOGGER.error("XSL parameter is not a file or URL: {}", xslFilePath);
                return;
            }
        } else {
            xslURL = xslFile.toURI().toURL();
        }
        MCRObjectID mcrId = MCRObjectID.getInstance(objectId);
        Document document = MCRXMLMetadataManager.instance().retrieveXML(mcrId);
        // do XSL transform
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setErrorListener(MCRErrorListener.getInstance());
        transformerFactory.setURIResolver(MCRURIResolver.instance());
        XMLReader xmlReader = MCRXMLParserFactory.getNonValidatingParser().getXMLReader();
        xmlReader.setEntityResolver(MCREntityResolver.instance());
        SAXSource styleSource = new SAXSource(xmlReader, new InputSource(xslURL.toURI().toString()));
        Transformer transformer = transformerFactory.newTransformer(styleSource);
        for (Entry<String, String> property : MCRConfiguration.instance().getPropertiesMap().entrySet()) {
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
        if(resultName.equals(MCRObject.ROOT_NAME)) {
            MCRMetadataManager.update(new MCRObject(resultDocument));
        } else if(resultName.equals(MCRDerivate.ROOT_NAME)) {
            MCRMetadataManager.update(new MCRDerivate(resultDocument));
        } else {
            LOGGER.error("Unable to transform '{}' because unknown result root name '{}'.", objectId, resultName);
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
        MCRActiveLinkException, MCRAccessException {
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
     * @param base_id
     *            the base part of a MCRObjectID e.g. DocPortal_document
     */
    @MCRCommand(
        syntax = "check derivate entries in objects for base {0}",
        help = "check in all objects with MCR base ID {0} for existing linked derivates",
        order = 400)
    public static void checkDerivatesInObjects(String base_id) throws IOException {
        if (base_id == null || base_id.length() == 0) {
            LOGGER.error("Base ID missed for check derivate entries in objects for base {0}");
            return;
        }
        MCRXMLMetadataManager mgr = MCRXMLMetadataManager.instance();
        List<String> id_list = mgr.listIDsForBase(base_id);
        int counter = 0;
        int maxresults = id_list.size();
        for (String objid : id_list) {
            counter++;
            LOGGER.info("Processing dataset {} from {} with ID: {}", counter, maxresults, objid);
            // get from data
            MCRObjectID mcrobjid = MCRObjectID.getInstance(objid);
            MCRObject obj = MCRMetadataManager.retrieveMCRObject(mcrobjid);
            List<MCRMetaLinkID> derivate_entries = obj.getStructure().getDerivates();
            for (MCRMetaLinkID derivate_entry : derivate_entries) {
                String derid = derivate_entry.getXLinkHref();
                if (!mgr.exists(MCRObjectID.getInstance(derid))) {
                    LOGGER.error("   !!! Missing derivate {} in database for base ID {}", derid, base_id);
                }
            }
        }
        LOGGER.info("Check done for {} entries", Integer.toString(counter));
    }

    @MCRCommand(
        syntax = "execute for selected {0}",
        help = "Calls the given command multiple times for all selected objects." +
                " The replacement is defined by an {x}.E.g. 'execute for selected set" +
                " parent of {x} to myapp_container_00000001'",
        order = 450)
    public static List<String> executeForSelected(String command) throws Exception {
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
        help = "Scans the metadata store for MCRObjects of type {0} and restore them in the search store.",
        order = 170)
    public static List<String> repairMetadataSearch(String type) {
        LOGGER.info("Start the repair for type {}", type);
        String typetest = CONFIG.getString("MCR.Metadata.Type." + type, "");

        if (typetest.length() == 0) {
            LOGGER.error("The type {} was not found.", type);
            return Collections.emptyList();
        }
        List<String> ar = MCRXMLMetadataManager.instance().listIDsOfType(type);
        if (ar.size() == 0) {
            LOGGER.warn("No ID's was found for type {}.", type);
            return Collections.emptyList();
        }

        List<String> cmds = new ArrayList<>(ar.size());

        for (String stid : ar) {
            cmds.add("repair metadata search of ID " + stid);
        }
        return cmds;
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
        if(!MCRObjectID.isValid(id)) {
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
        MCRStreamQuery<String> fromQuery = MCRStreamQuery
                .getInstance(em, "SELECT DISTINCT m.key.mcrfrom FROM MCRLINKHREF m", String.class);
        MCRStreamQuery<String> toQuery = MCRStreamQuery
                .getInstance(em, "SELECT DISTINCT m.key.mcrto FROM MCRLINKHREF m", String.class);
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
            order = 188
    )
    public static void rebuildMCRLinkHrefTableForObject(String objectId) {
        MCRLinkTableManager.instance().update(MCRObjectID.getInstance(objectId));
    }

    @MCRCommand(syntax = "merge derivates of object {0}",
        help = "Retrieves the MCRObject with the MCRObjectID {0} and if it has more then one MCRDerivate, then all" +
            " Files will be copied to the first Derivate and all other will be deleted.",
        order = 190)
    public static void mergeDerivatesOfObject(String id) {
        MCRObjectID objectID = MCRObjectID.getInstance(id);
        if (!MCRMetadataManager.exists(objectID)) {
            LOGGER.error("The object with the id {} does not exist!", id);
            return;
        }

        MCRObject object = MCRMetadataManager.retrieveMCRObject(objectID);

        List<MCRMetaLinkID> derivateLinkIDs = object.getStructure().getDerivates();
        List<MCRObjectID> derivateIDs = derivateLinkIDs.stream().map(MCRMetaLinkID::getXLinkHrefID)
            .collect(Collectors.toList());

        if (derivateIDs.size() <= 1) {
            LOGGER.error("The object with the id {} has no Derivates to merge!", id);
            return;
        }

        String mainID = derivateIDs.get(0).toString();
        MCRPath mainDerivateRootPath = MCRPath.getPath(mainID, "/");

        derivateIDs.stream().skip(1).forEach(derivateID -> {
            LOGGER.info("Merge {} into {}...", derivateID, mainID);
            MCRPath copyRootPath = MCRPath.getPath(derivateID.toString(), "/");
            try {
                MCRTreeCopier treeCopier = new MCRTreeCopier(copyRootPath, mainDerivateRootPath);
                Files.walkFileTree(copyRootPath, treeCopier);
                Files.walkFileTree(copyRootPath, MCRRecursiveDeleter.instance());
                MCRMetadataManager.deleteMCRDerivate(derivateID);
            } catch (IOException | MCRAccessException e) {
                throw new MCRException(e);
            }
        });

    }
}
