/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.frontend.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;

import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.common.MCRXMLTableManager;

/**
 * Provides static methods that implement commands for the MyCoRe command line
 * interface.
 * 
 * @author Jens Kupferschmidt
 * @author Frank Luetzenkirchen
 * @version $Revision$ $Date$
 */
public class MCRObjectCommands extends MCRAbstractCommands {
    /** The logger */
    private static Logger LOGGER = Logger.getLogger(MCRObjectCommands.class.getName());

    /** Default transformer script */
    public static final String DEFAULT_TRANSFORMER = "save-object.xsl";

    /** static compiled transformer stylesheets */
    private static Hashtable<String, javax.xml.transform.Transformer> translist = new Hashtable<String, javax.xml.transform.Transformer>();

    /**
     * The empty constructor.
     */
    public MCRObjectCommands() {
        super();

        MCRCommand com = null;

        com = new MCRCommand("delete all objects of type {0}", "org.mycore.frontend.cli.MCRObjectCommands.deleteAllObjects String", "Removes MCRObjects in the number range between the MCRObjectID {0} and {1}.");
        command.add(com);

        com = new MCRCommand("delete object from {0} to {1}", "org.mycore.frontend.cli.MCRObjectCommands.deleteFromTo String String", "Removes MCRObjects in the number range between the MCRObjectID {0} and {1}.");
        command.add(com);

        com = new MCRCommand("delete object {0}", "org.mycore.frontend.cli.MCRObjectCommands.delete String", "Removes a MCRObject with the MCRObjectID {0}");
        command.add(com);

        com = new MCRCommand("load object from file {0}", "org.mycore.frontend.cli.MCRObjectCommands.loadFromFile String", "Adds a MCRObject form the file {0} to the system.");
        command.add(com);

        com = new MCRCommand("load all objects from directory {0}", "org.mycore.frontend.cli.MCRObjectCommands.loadFromDirectory String", "Loads all MCRObjects form the directory {0} to the system.");
        command.add(com);

        com = new MCRCommand("update object from file {0}", "org.mycore.frontend.cli.MCRObjectCommands.updateFromFile String", "Updates a MCRObject form the file {0} in the system.");
        command.add(com);

        com = new MCRCommand("update all objects from directory {0}", "org.mycore.frontend.cli.MCRObjectCommands.updateFromDirectory String", "Updates all MCRObjects form the directory {0} in the system.");
        command.add(com);

        com = new MCRCommand("export object from {0} to {1} to directory {2} with {3}", "org.mycore.frontend.cli.MCRObjectCommands.export String String String String", "Stores all MCRObjects with MCRObjectID's between {0} and {1} to the directory {2} with the stylesheet {3}-object.xsl. For {3} save is the default.");
        command.add(com);

        com = new MCRCommand("export object {0} to directory {1} with {2}", "org.mycore.frontend.cli.MCRObjectCommands.export String String String", "Stores the MCRObject with the MCRObjectID {0} to the directory {1} with the stylesheet {2}-object.xsl. For {2} save is the default.");
        command.add(com);

        com = new MCRCommand("export all objects of type {0} to directory {1} with {2}", "org.mycore.frontend.cli.MCRObjectCommands.exportAllObjects String String String", "Stores all MCRObjects of type {0} to directory {1} with the stylesheet mcr_{2}-object.xsl. For {2} save is the default.");
        command.add(com);

        com = new MCRCommand("get last ID for base {0}", "org.mycore.frontend.cli.MCRObjectCommands.getLastID String", "Returns the last used MCRObjectID for the ID base {0}.");
        command.add(com);

        com = new MCRCommand("get next ID for base {0}", "org.mycore.frontend.cli.MCRObjectCommands.getNextID String", "Returns the next free MCRObjectID for the ID base {0}.");
        command.add(com);

        com = new MCRCommand("check file {0}", "org.mycore.frontend.cli.MCRObjectCommands.checkXMLFile String", "Checks the data file {0} against the XML Schema.");
        command.add(com);

        com = new MCRCommand("repair metadata search of type {0}", "org.mycore.frontend.cli.MCRObjectCommands.repairMetadataSearch String", "Reads the SQL store table of MCRObject XML files for the type {0} and restore them to the search store.");
        command.add(com);

        com = new MCRCommand("repair metadata search of ID {0}", "org.mycore.frontend.cli.MCRObjectCommands.repairMetadataSearchForID String", "Read the SQL store table of MCRObject XML files with MCRObjectID {0} and restore them to the search store.");
        command.add(com);
    }

    /**
     * Delete all MCRObject from the datastore for a given type.
     * 
     * @param type
     *            the type of the MCRObjects that should be deleted
     */
    public static final void deleteAllObjects(String type) throws MCRActiveLinkException {
        MCRObject mycore_obj = new MCRObject();
        MCRXMLTableManager tm = MCRXMLTableManager.instance();
        for (String id : tm.retrieveAllIDs(type)) {
            try {
                mycore_obj.deleteFromDatastore(id);
                LOGGER.info(mycore_obj.getId() + " deleted.");
            } catch (MCRException ex) {
                LOGGER.error("Can't delete " + mycore_obj.getId().getId() + ".");
                LOGGER.error(ex.getMessage());
                LOGGER.debug(ex.getStackTraceAsString());
            }
        }
    }

    /**
     * Delete a MCRObject from the datastore.
     * 
     * @param ID
     *            the ID of the MCRObject that should be deleted
     */
    public static final void delete(String ID) throws MCRActiveLinkException {
        MCRObject mycore_obj = new MCRObject();

        try {
            mycore_obj.deleteFromDatastore(ID);
            LOGGER.info(mycore_obj.getId().getId() + " deleted.");
        } catch (MCRException ex) {
            LOGGER.debug(ex.getStackTraceAsString());
            LOGGER.error(ex.getMessage());
            LOGGER.error("Can't delete " + mycore_obj.getId().getId() + ".");
            LOGGER.error("");
        }
    }

    /**
     * Delete MCRObject's form ID to ID from the datastore.
     * 
     * @param IDfrom
     *            the start ID for deleting the MCRObjects
     * @param IDto
     *            the stop ID for deleting the MCRObjects
     */
    public static final void deleteFromTo(String IDfrom, String IDto) throws MCRActiveLinkException {
        int from_i = 0;
        int to_i = 0;

        try {
            MCRObjectID from = new MCRObjectID(IDfrom);
            MCRObjectID to = new MCRObjectID(IDto);
            MCRObjectID now = new MCRObjectID(IDfrom);
            from_i = from.getNumberAsInteger();
            to_i = to.getNumberAsInteger();

            if (from_i > to_i) {
                throw new MCRException("The from-to-interval is false.");
            }

            for (int i = from_i; i < (to_i + 1); i++) {
                now.setNumber(i);
                if (MCRObject.existInDatastore(now)) {
                    delete(now.getId());
                }
            }
        } catch (MCRException ex) {
            LOGGER.debug(ex.getStackTraceAsString());
            LOGGER.error(ex.getMessage());
            LOGGER.error("");
        }
    }

    /**
     * Load MCRObject's from all XML files in a directory.
     * 
     * @param directory
     *            the directory containing the XML files
     * @throws MCRActiveLinkException
     */
    public static final List<String> loadFromDirectory(String directory) throws MCRActiveLinkException {
        return processFromDirectory(directory, false);
    }

    /**
     * Update MCRObject's from all XML files in a directory.
     * 
     * @param directory
     *            the directory containing the XML files
     * @throws MCRActiveLinkException
     */
    public static final List<String> updateFromDirectory(String directory) throws MCRActiveLinkException {
        return processFromDirectory(directory, true);
    }

    /**
     * Load or update MCRObject's from all XML files in a directory.
     * 
     * @param directory
     *            the directory containing the XML files
     * @param update
     *            if true, object will be updated, else object is created
     * @throws MCRActiveLinkException
     */
    private static final List<String> processFromDirectory(String directory, boolean update) throws MCRActiveLinkException {
        File dir = new File(directory);

        if (!dir.isDirectory()) {
            LOGGER.warn(directory + " ignored, is not a directory.");
            return null;
        }

        String[] list = dir.list();

        if (list.length == 0) {
            LOGGER.warn("No files found in directory " + directory);
            return null;
        }

        List<String> cmds = new ArrayList<String>();
        for (String file : list)
            if ( file.endsWith(".xml") && ( ! file.contains( "derivate" ) ) )
                cmds.add( ( update ? "update" : "load" ) + " object from file " + new File( dir, file ).getAbsolutePath() );

        return cmds;
    }

    /**
     * Load a MCRObjects from an XML file.
     * 
     * @param file
     *            the location of the xml file
     * @throws MCRActiveLinkException
     */
    public static final boolean loadFromFile(String file) throws MCRActiveLinkException {
        return loadFromFile(file, true);
    }

    /**
     * Load a MCRObjects from an XML file.
     * 
     * @param file
     *            the location of the xml file
     * @param importMode
     *            if true, servdates are taken from xml file
     * @throws MCRActiveLinkException
     */
    public static final boolean loadFromFile(String file, boolean importMode) throws MCRActiveLinkException {
        return processFromFile(new File(file), false, importMode);
    }

    /**
     * Update a MCRObject's from an XML file.
     * 
     * @param file
     *            the location of the xml file
     * @throws MCRActiveLinkException
     */
    public static final boolean updateFromFile(String file) throws MCRActiveLinkException {
        return updateFromFile(file, true);
    }

    /**
     * Update a MCRObject's from an XML file.
     * 
     * @param file
     *            the location of the xml file
     * @param importMode
     *            if true, servdates are taken from xml file
     * @throws MCRActiveLinkException
     */
    public static final boolean updateFromFile(String file, boolean importMode) throws MCRActiveLinkException {
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
     * @throws MCRActiveLinkException
     */
    private static final boolean processFromFile(File file, boolean update, boolean importMode) throws MCRActiveLinkException {
        if (!file.getName().endsWith(".xml")) {
            LOGGER.warn(file + " ignored, does not end with *.xml");
            return false;
        }

        if (!file.isFile()) {
            LOGGER.warn(file + " ignored, is not a file.");
            return false;
        }

        LOGGER.info("Reading file " + file + " ...");

        MCRObject mycore_obj = new MCRObject();
        mycore_obj.setImportMode(importMode);
        mycore_obj.setFromURI(file.getAbsolutePath());
        LOGGER.info("Label --> " + mycore_obj.getLabel());

        if (update) {
            mycore_obj.updateInDatastore();
            LOGGER.info(mycore_obj.getId().getId() + " updated.");
            LOGGER.info("");
        } else {
            mycore_obj.createInDatastore();
            LOGGER.info(mycore_obj.getId().getId() + " loaded.");
            LOGGER.info("");
        }

        return true;
    }

    /**
     * Shows the next free MCRObjectIDs.
     * 
     * @param base
     *            the base String of the MCRObjectID
     */
    public static final void showNextID(String base) {
        MCRObjectID mcr_id = new MCRObjectID();

        try {
            mcr_id.setNextFreeId(base);
            LOGGER.info("The next free ID  is " + mcr_id.getId());
        } catch (MCRException ex) {
            LOGGER.error(ex.getMessage());
            LOGGER.error("");
        }
    }

    /**
     * Shows the last used MCRObjectIDs.
     * 
     * @param base
     *            the base String of the MCRObjectID
     */
    public static final void showLastID(String base) {
        MCRObjectID mcr_id = new MCRObjectID();

        try {
            mcr_id.setNextFreeId(base);
            mcr_id.setNumber(mcr_id.getNumberAsInteger() - 1);
            LOGGER.info("The last used ID  is " + mcr_id.getId());
        } catch (MCRException ex) {
            LOGGER.error(ex.getMessage());
            LOGGER.error("");
        }
    }

    /**
     * Export an MCRObject to a file named <em>MCRObjectID</em> .xml in a
     * directory. The method use the converter stylesheet mcr_<em>style</em>_object.xsl.
     * 
     * @param ID
     *            the ID of the MCRObject to be save.
     * @param dirname
     *            the dirname to store the object
     * @param style
     *            the type of the stylesheet
     */
    public static final void export(String ID, String dirname, String style) {
        export(ID, ID, dirname, style);
    }

    /**
     * Save any MCRObject's to files named <em>MCRObjectID</em> .xml in a
     * directory. The saving starts with fromID and runs to toID. ID's they was
     * not found will skiped. The method use the converter stylesheet mcr_<em>style</em>_object.xsl.
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
    public static final void export(String fromID, String toID, String dirname, String style) {
        MCRObjectID fid, tid;

        // check fromID and toID
        try {
            fid = new MCRObjectID(fromID);
            tid = new MCRObjectID(toID);
        } catch (Exception ex) {
            LOGGER.error("FromID : " + ex.getMessage());
            return;
        }
        // check dirname
        File dir = new File(dirname);
        if (!dir.isDirectory()) {
            LOGGER.error(dirname + " is not a dirctory.");
            return;
        }

        MCRObjectID nid = fid;
        int k = 0;
        try {
            Transformer trans = getTransformer(style);
            for (int i = fid.getNumberAsInteger(); i < (tid.getNumberAsInteger() + 1); i++) {
                nid.setNumber(i);
                if (!MCRObject.existInDatastore(nid)) {
                    continue;
                }
                if (!exportMCRObject(dir, trans, nid)) {
                    continue;
                }
                k++;
            }
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
            LOGGER.error("Exception while store file to " + dir.getAbsolutePath());
            return;
        }
        LOGGER.info(k + " Object's stored under " + dir.getAbsolutePath() + ".");
    }

    /**
     * Save all MCRObject's to files named <em>MCRObjectID</em> .xml in a
     * <em>dirname</em>directory for the data type <em>type</em>. The
     * method use the converter stylesheet mcr_<em>style</em>_object.xsl.
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
    public static final void exportAllObjects(String type, String dirname, String style) {
        // check dirname
        File dir = new File(dirname);

        if (dir.isFile()) {
            LOGGER.error(dirname + " is not a dirctory.");
            return;
        }
        Transformer trans;
        try {
            trans = getTransformer(style);
        } catch (Exception e) {
            LOGGER.error("Error while getting transformer:", e);
            return;
        }
        MCRXMLTableManager tm = MCRXMLTableManager.instance();
        for (String id : tm.retrieveAllIDs(type)) {
            MCRObjectID oid = new MCRObjectID(id);
            try {
                exportMCRObject(dir, trans, oid);
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage());
                LOGGER.error("Exception while store file to " + dir.getAbsolutePath());
                return;
            }
        }
    }

    /**
     * The method search for a stylesheet mcr_<em>style</em>_object.xsl and
     * build the transformer. Default is <em>mcr_save-object.xsl</em>.
     * 
     * @param style
     *            the style attribute for the transformer stylesheet
     * @return the transformer
     * @throws TransformerFactoryConfigurationError
     * @throws TransformerConfigurationException
     */
    private static final Transformer getTransformer(String style) throws TransformerFactoryConfigurationError, TransformerConfigurationException {
        String xslfile = DEFAULT_TRANSFORMER;
        if ((style != null) && (style.trim().length() != 0)) {
            xslfile = style + "-object.xsl";
        }
        Transformer trans = translist.get(xslfile);
        if (trans != null) {
            return trans;
        }
        LOGGER.debug("Will load transformer stylesheet " + xslfile + "for export.");

        InputStream in = MCRObjectCommands.class.getResourceAsStream("/" + xslfile);
        if (in == null) {
            in = MCRObjectCommands.class.getResourceAsStream(DEFAULT_TRANSFORMER);
        }
        try {
            if (in != null) {
                StreamSource source = new StreamSource(in);
                TransformerFactory transfakt = TransformerFactory.newInstance();
                transfakt.setURIResolver(MCRURIResolver.instance());
                trans = transfakt.newTransformer(source);
                translist.put(xslfile, trans);
                return trans;
            } else {
                LOGGER.warn("Can't load transformer ressource " + xslfile + " or " + DEFAULT_TRANSFORMER + ".");
            }
        } catch (Exception e) {
            LOGGER.warn("Error while load transformer ressource " + xslfile + " or " + DEFAULT_TRANSFORMER + ".");
            if (LOGGER.isDebugEnabled()) {
            e.printStackTrace();}
        }
        return null;
    }

    /**
     * The method read a MCRObject and use the transformer to write the data to
     * a file. They are any steps to handel errors and save the damaged data.
     * <ul>
     * <li> Read data for object ID in the MCRObject, add ACL's and store it as
     * checked and transformed XML. Return true.</li>
     * <li> If it can't find a transformer instance (no script file found) it
     * store the checked data with ACL's native in the file. Warning and return
     * true.</li>
     * <li> If it get an exception while build the MCRObject, it try to read the
     * XML blob and stor it without check and ACL's to the file. Warning and
     * return true.</li>
     * <li> If it get an exception while store the native data without check,
     * ACÖ's and transformation it return a warning and false.</li>
     * </ul>
     * 
     * @param dir
     *            the file instance to store
     * @param trans
     *            the XML transformer
     * @param nid
     *            the MCRObjectID
     * @return true if the store was okay (see description), else return false
     * @throws FileNotFoundException
     * @throws TransformerException
     * @throws IOException
     */
    private static final boolean exportMCRObject(File dir, Transformer trans, MCRObjectID nid) throws FileNotFoundException, TransformerException, IOException {
        byte[] xml = null;
        try {
            // if object do'snt exist - no exception is catched!
            xml = MCRObject.receiveXMLFromDatastore(nid.toString());
        } catch (MCRException ex) {
            return false;
        }

        File xmlOutput = new File(dir, nid.toString() + ".xml");
        FileOutputStream out = new FileOutputStream(xmlOutput);

        if (trans != null) {
            StreamResult sr = new StreamResult(out);
            trans.transform(new org.jdom.transform.JDOMSource(MCRXMLHelper.parseXML(xml, false)), sr);
        } else {
            out.write(xml);
            out.flush();
        }
        LOGGER.info("Object " + nid.toString() + " saved to " + xmlOutput.getCanonicalPath() + ".");
        LOGGER.info("");
        return true;
    }

    /**
     * Get the next free MCRObjectID for the given MCRObjectID base.
     * 
     * @param base
     *            the MCRObjectID base string
     */
    public static final void getNextID(String base) {
        MCRObjectID id = new MCRObjectID();

        try {
            id.setNextFreeId(base);
            LOGGER.info(id.getId());
        } catch (MCRException ex) {
            LOGGER.error(ex.getMessage());
            LOGGER.error("");
        }
    }

    /**
     * Get the last used MCRObjectID for the given MCRObjectID base.
     * 
     * @param base
     *            the MCRObjectID base string
     */
    public static final void getLastID(String base) {
        MCRObjectID mcr_id = new MCRObjectID();

        try {
            mcr_id.setNextFreeId(base);
            mcr_id.setNumber(mcr_id.getNumberAsInteger() - 1);
            LOGGER.info(mcr_id.getId());
        } catch (MCRException ex) {
            LOGGER.error(ex.getMessage());
            LOGGER.error("");
        }
    }

    /**
     * The method parse and check an XML file.
     * 
     * @param file
     *            the location of the xml file
     */
    public static final boolean checkXMLFile(String file) {
        if (!file.endsWith(".xml")) {
            LOGGER.warn(file + " ignored, does not end with *.xml");

            return false;
        }

        if (!new File(file).isFile()) {
            LOGGER.warn(file + " ignored, is not a file.");

            return false;
        }

        LOGGER.info("Reading file " + file + " ...");

        if (MCRXMLHelper.parseURI(file) != null) {
            LOGGER.info("The file has no XML errors.");
        }

        return true;
    }

    /**
     * The method start the repair of the metadata search for a given
     * MCRObjectID type.
     * 
     * @param type
     *            the MCRObjectID type
     */
    public static final void repairMetadataSearch(String type) {
        LOGGER.info("Start the repair for type " + type);
        String typetest = CONFIG.getString("MCR.Metadata.Type." + type, "");

        if (typetest.length() == 0) {
            LOGGER.error("The type " + type + " was not found.");
            return;
        }
        List<String> ar = (List<String>) MCRXMLTableManager.instance().retrieveAllIDs(type);
        if (ar.size() == 0) {
            LOGGER.warn("No ID's was found for type " + type + ".");
            return;
        }
        for (String stid : ar) {
            MCRObject obj = new MCRObject();
            obj.repairPersitenceDatastore(stid);
            LOGGER.info("Repaired " + stid);
        }
    }

    /**
     * The method start the repair of the metadata search for a given
     * MCRObjectID as String.
     * 
     * @param id
     *            the MCRObjectID as String
     */
    public static final void repairMetadataSearchForID(String id) {
        LOGGER.info("Start the repair for the ID " + id);

        MCRObjectID mid = null;

        try {
            mid = new MCRObjectID(id);
        } catch (Exception e) {
            LOGGER.error("The String " + id + " is not a MCRObjectID.");
            LOGGER.info(" ");

            return;
        }

        MCRObject obj = new MCRObject();
        obj.repairPersitenceDatastore(mid);
        LOGGER.info("Repaired " + mid.getId());
        LOGGER.info(" ");
    }
}
