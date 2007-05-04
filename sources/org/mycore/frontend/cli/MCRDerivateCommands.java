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
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;

import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.access.MCRAccessInterface;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.ifs.MCRFileImportExport;
import org.mycore.datamodel.metadata.MCRDerivate;
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
public class MCRDerivateCommands extends MCRAbstractCommands {
    /** The logger */
    private static Logger LOGGER = Logger.getLogger(MCRDerivateCommands.class.getName());

    /** The ACL interface */
    private static final MCRAccessInterface ACCESS_IMPL = (MCRAccessInterface) MCRConfiguration.instance().getInstanceOf("MCR.Access.Class", MCRAccessBaseImpl.class.getName());

    /** Default transformer script */
    public static final String DEFAULT_TRANSFORMER = "save-derivate.xsl";

    /**
     * The constructor.
     */
    public MCRDerivateCommands() {
        super();

        MCRCommand com = null;

        com = new MCRCommand("delete all derivates", "org.mycore.frontend.cli.MCRDerivateCommands.deleteAllDerivates", "Removes all derivates from the repository");
        command.add(com);

        com = new MCRCommand("delete derivate from {0} to {1}", "org.mycore.frontend.cli.MCRDerivateCommands.delete String String", "The command remove derivates in the number range between the MCRObjectID {0} and {1}.");
        command.add(com);

        com = new MCRCommand("delete derivate {0}", "org.mycore.frontend.cli.MCRDerivateCommands.delete String", "The command remove a derivate with the MCRObjectID {0}");
        command.add(com);

        com = new MCRCommand("load derivate from file {0}", "org.mycore.frontend.cli.MCRDerivateCommands.loadFromFile String", "The command add a derivate form the file {0} to the system.");
        command.add(com);

        com = new MCRCommand("update derivate from file {0}", "org.mycore.frontend.cli.MCRDerivateCommands.updateFromFile String", "The command update a derivate form the file {0} in the system.");
        command.add(com);

        com = new MCRCommand("load all derivates from directory {0}", "org.mycore.frontend.cli.MCRDerivateCommands.loadFromDirectory String", "The command load all derivates form the directory {0} to the system.");
        command.add(com);

        com = new MCRCommand("update all derivates from directory {0}", "org.mycore.frontend.cli.MCRDerivateCommands.updateFromDirectory String", "The command update all derivates form the directory {0} in the system.");
        command.add(com);

        com = new MCRCommand("export derivate {0} to directory {1} with {2}", "org.mycore.frontend.cli.MCRDerivateCommands.export String String String", "The command store the derivate with the MCRObjectID {0} to the directory {1} with the stylesheet {2}-object.xsl. For {2} save is the default.");
        command.add(com);

        com = new MCRCommand("export derivate from {0} to {1} to directory {2} with {3}", "org.mycore.frontend.cli.MCRDerivateCommands.export String String String String", "The command store all derivates with MCRObjectID's between {0} and {1} to the directory {2} with the stylesheet {3}-object.xsl. For {3} save is the default.");
        command.add(com);

        com = new MCRCommand("export all derivates to directory {0} with {1}", "org.mycore.frontend.cli.MCRDerivateCommands.exportAllDerivates String String", "Stores all derivates to the directory {0} with the stylesheet mcr_{1}-derivate.xsl. For {1} save is the default.");
        command.add(com);

        com = new MCRCommand("show loadable derivate of {0} to directory {1}", "org.mycore.frontend.cli.MCRDerivateCommands.show String String", "The command store the derivate with the MCRObjectID {0} to the directory {1}, without ifs-metadata");
        command.add(com);

        com = new MCRCommand("get next derivate ID for base {0}", "org.mycore.frontend.cli.MCRDerivateCommands.getNextID String", "The command return the next free MCRObjectID for the ID base.");
        command.add(com);

        com = new MCRCommand("repair derivate search of type derivate", "org.mycore.frontend.cli.MCRDerivateCommands.repairDerivateSearch", "The command read the Content store and reindex the derivate search stores.");
        command.add(com);

        com = new MCRCommand("repair derivate search of ID {0}", "org.mycore.frontend.cli.MCRDerivateCommands.repairDerivateSearchForID String", "The command read the Content store for MCRObjectID {0} and reindex the derivate search store.");
        command.add(com);
    }

    /**
     * deletes all MCRDerivate from the datastore.
     */
    public static void deleteAllDerivates() {
        MCRDerivate der = new MCRDerivate();
        MCRXMLTableManager tm = MCRXMLTableManager.instance();
        List<String> ids = tm.retrieveAllIDs("derivate");
        for (String id : ids) {
            try {
                der.deleteFromDatastore(id);
                LOGGER.info(der.getId().getId() + " deleted.");
            } catch (MCRException ex) {
                LOGGER.error("Can't delete " + der.getId() + ".");
                LOGGER.error(ex.getMessage());
                LOGGER.debug(ex.getStackTraceAsString());
            }
        }
    }

    /**
     * Delete an MCRDerivate from the datastore.
     * 
     * @param ID
     *            the ID of the MCRDerivate that should be deleted
     */
    public static void delete(String ID) {
        MCRDerivate mycore_obj = new MCRDerivate();

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
     * Delete MCRDerivates form ID to ID from the datastore.
     * 
     * @param IDfrom
     *            the start ID for deleting the MCRDerivate
     * @param IDto
     *            the stop ID for deleting the MCRDerivate
     */
    public static void delete(String IDfrom, String IDto) {
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
     * Loads MCRDerivates from all XML files in a directory.
     * 
     * @param directory
     *            the directory containing the XML files
     */
    public static void loadFromDirectory(String directory) {
        processFromDirectory(directory, false);
    }

    /**
     * Updates MCRDerivates from all XML files in a directory.
     * 
     * @param directory
     *            the directory containing the XML files
     */
    public static void updateFromDirectory(String directory) {
        processFromDirectory(directory, true);
    }

    /**
     * Loads or updates MCRDerivates from all XML files in a directory.
     * 
     * @param directory
     *            the directory containing the XML files
     * @param update
     *            if true, object will be updated, else object is created
     */
    private static void processFromDirectory(String directory, boolean update) {
        File dir = new File(directory);

        if (!dir.isDirectory()) {
            LOGGER.warn(directory + " ignored, is not a directory.");

            return;
        }

        String[] list = dir.list();

        if (list.length == 0) {
            LOGGER.warn("No files found in directory " + directory);

            return;
        }

        int numProcessed = 0;

        for (int i = 0; i < list.length; i++) {
            if (!list[i].endsWith(".xml")) {
                continue;
            }

            if (list[i].indexOf("derivate") == -1) {
                continue;
            }

            if (processFromFile(new File(dir, list[i]), update, true)) {
                numProcessed++;
            }
        }

        LOGGER.info("Processed " + numProcessed + " files.");
    }

    /**
     * Loads an MCRDerivates from an XML file.
     * 
     * @param file
     *            the location of the xml file
     */
    public static boolean loadFromFile(String file) {
        return loadFromFile(file, true);
    }

    /**
     * Loads an MCRDerivates from an XML file.
     * 
     * @param file
     *            the location of the xml file
     * @param importMode
     *            if true, servdates are taken from xml file
     */
    public static boolean loadFromFile(String file, boolean importMode) {
        return processFromFile(new File(file), false, importMode);
    }

    /**
     * Updates an MCRDerivates from an XML file.
     * 
     * @param file
     *            the location of the xml file
     */
    public static boolean updateFromFile(String file) {
        return updateFromFile(file, true);
    }

    /**
     * Updates an MCRDerivates from an XML file.
     * 
     * @param file
     *            the location of the xml file
     * @param importMode
     *            if true, servdates are taken from xml file
     */
    public static boolean updateFromFile(String file, boolean importMode) {
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
     */
    private static boolean processFromFile(File file, boolean update, boolean importMode) {
        if (!file.getName().endsWith(".xml")) {
            LOGGER.warn(file + " ignored, does not end with *.xml");

            return false;
        }

        if (!file.isFile()) {
            LOGGER.warn(file + " ignored, is not a file.");

            return false;
        }

        LOGGER.info("Reading file " + file + " ...");

        try {
            MCRDerivate mycore_obj = new MCRDerivate();
            mycore_obj.setImportMode(importMode);
            mycore_obj.setFromURI(file.getAbsolutePath());

            // Replace relative path with absolute path of files
            if (mycore_obj.getDerivate().getInternals() != null) {
                String path = mycore_obj.getDerivate().getInternals().getSourcePath();
                path = path.replace('/', File.separatorChar).replace('\\', File.separatorChar);
                if (path.trim().length() <= 1)
                    // the path is the path name plus the name of the derivate -
                    path = mycore_obj.getId().getId().toLowerCase();

                File sPath = new File(path);

                if (!sPath.isAbsolute()) {
                    // only change path to absolute path when relative
                    String prefix = file.getParent();

                    if (prefix != null) {
                        path = prefix + File.separator + path;
                    }
                }

                mycore_obj.getDerivate().getInternals().setSourcePath(path);
                LOGGER.info("Source path --> " + path);
            }

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
        } catch (MCRException ex) {
            LOGGER.error("Exception while loading from file " + file, ex);

            return false;
        }
    }

    /**
     * Shows a list of next MCRObjectIDs.
     */
    public static void getNextID(String base) {
        MCRObjectID mcr_id = new MCRObjectID();

        try {
            mcr_id.setNextFreeId(base);
            LOGGER.info(mcr_id.getId());
        } catch (MCRException ex) {
            LOGGER.error(ex.getMessage());
            LOGGER.error("");
        }
    }

    /**
     * Save an MCRDerivate to a file named <em>MCRObjectID</em> .xml in a
     * directory with <em>dirname</em> and store the derivate objects in a
     * directory under them named <em>MCRObjectID</em>. The IFS-Attribute of
     * the derivate files aren't saved, for reloading purpose after deleting a
     * derivate in the datastore
     * 
     * @param ID
     *            the ID of the MCRDerivate to be save.
     * @param dirname
     *            the dirname to store the derivate
     */
    public static void show(String ID, String dirname) {
        export(ID, ID, dirname, "save", false);
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
    public static void export(String ID, String dirname, String style) {
        export(ID, ID, dirname, style, true);
    }

    /**
     * Save any MCRDerivate's to files named <em>MCRObjectID</em> .xml in a
     * directory and the objects under them named <em>MCRObjectID</em>. The
     * saving starts with fromID and runs to toID. ID's they was not found will
     * skiped. The method use the converter stylesheet mcr_<em>style</em>_object.xsl.
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
    public static void export(String fromID, String toID, String dirname, String style) {
        export(fromID, toID, dirname, style, false); // :NOTE: false was the
        // default, but
        // is not equal to "single" save
    }

    /**
     * Export any MCRDerivate's to files named <em>MCRObjectID</em> .xml in a
     * directory and the objects under them named <em>MCRObjectID</em>. The
     * saving starts with fromID and runs to toID. ID's they was not found will
     * skiped. The method use the converter stylesheet mcr_<em>style</em>_object.xsl.
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
    public static void export(String fromID, String toID, String dirname, String style, boolean withIfsID) {
        LOGGER.debug("withIfsID (" + withIfsID + ") will never used"); // FIXME:
        // use or
        // remove
        // withIfsID
        // check fromID and toID
        MCRObjectID fid = null;
        MCRObjectID tid = null;

        try {
            fid = new MCRObjectID(fromID);
        } catch (Exception ex) {
            LOGGER.error("FromID : " + ex.getMessage());

            return;
        }

        try {
            tid = new MCRObjectID(toID);
        } catch (Exception ex) {
            LOGGER.error("ToID : " + ex.getMessage());

            return;
        }

        // check dirname
        File dir = new File(dirname);

        if (dir.isFile()) {
            LOGGER.error(dirname + " is not a dirctory.");

            return;
        }

        Transformer trans = getTransformer(style);

        MCRObjectID nid = fid;
        int k = 0;

        try {
            for (int i = fid.getNumberAsInteger(); i < (tid.getNumberAsInteger() + 1); i++) {
                nid.setNumber(i);

                exportDerivate(dir, trans, nid);

                k++;
            }
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
            LOGGER.error("Exception while store file or objects to " + dir.getAbsolutePath(), ex);

            return;
        }

        LOGGER.info(k + " Object's stored under " + dir.getAbsolutePath() + ".");
    }

    public static void exportAllDerivates(String dirname, String style) {
        // check dirname
        File dir = new File(dirname);

        if (dir.isFile()) {
            LOGGER.error(dirname + " is not a dirctory.");

            return;
        }

        Transformer trans = getTransformer(style);

        MCRXMLTableManager tm = MCRXMLTableManager.instance();
        List<String> ids = tm.retrieveAllIDs("derivate");
        for (String id : ids) {
            MCRObjectID oid = new MCRObjectID(id);
            try {
                exportDerivate(dir, trans, oid);
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage());
                LOGGER.error("Exception while store file to " + dir.getAbsolutePath());
                return;
            }
        }
    }

    /**
     * @param dirname
     * @param dir
     * @param trans
     * @param nid
     * @throws FileNotFoundException
     * @throws TransformerException
     * @throws IOException
     */
    private static void exportDerivate(File dir, Transformer trans, MCRObjectID nid) throws FileNotFoundException, TransformerException, IOException {
        // store the XML file
        MCRDerivate obj = new MCRDerivate();
        Document xml = null;

        try {
            obj.receiveFromDatastore(nid.toString());
            String path = obj.getDerivate().getInternals().getSourcePath();
            // reset from the absolute to relative path, for later reload
            LOGGER.info("Old Internal Path ====>" + path);
            obj.getDerivate().getInternals().setSourcePath(nid.toString());
            LOGGER.info("New Internal Path ====>" + nid.toString());
            // add ACL's
            List l = ACCESS_IMPL.getPermissionsForID(nid.toString());
            for (int i = 0; i < l.size(); i++) {
                Element rule = ACCESS_IMPL.getRule(nid.toString(), (String) l.get(i));
                obj.getService().addRule((String) l.get(i), rule);
            }
            // build JDOM
            xml = obj.createXML();

        } catch (MCRException ex) {
            LOGGER.warn("Could not read " + nid.toString() + ", continue with next ID");
            return;
        }
        File xmlOutput = new File(dir, nid.toString() + ".xml");
        FileOutputStream out = new FileOutputStream(xmlOutput);
        dir = new File(dir, nid.toString());

        if (trans != null) {
            trans.setParameter("dirname", dir.getPath());
            StreamResult sr = new StreamResult(out);
            trans.transform(new org.jdom.transform.JDOMSource(xml), sr);
        } else {
            new org.jdom.output.XMLOutputter().output(xml, out);
            out.flush();
            out.close();
        }

        LOGGER.info("Object " + nid.toString() + " stored under " + xmlOutput + ".");

        // store the derivate file under dirname
        try {

            if (!dir.isDirectory()) {
                dir.mkdir();
            }

            MCRFileImportExport.exportFiles(obj.receiveDirectoryFromIFS(nid.toString()), dir);
        } catch (MCRException ex) {
            LOGGER.error(ex.getMessage());
            LOGGER.error("Exception while store to object in " + dir.getAbsolutePath());
            return;
        }

        LOGGER.info("Derivate " + nid.toString() + " saved under " + dir.toString() + " and " + xmlOutput.toString() + ".");
    }

    /**
     * @param style
     * @return
     * @throws TransformerFactoryConfigurationError
     */
    private static Transformer getTransformer(String style) throws TransformerFactoryConfigurationError {
        String xslfile = DEFAULT_TRANSFORMER;
        if ((style != null) && (style.trim().length() != 0)) {
            xslfile = style + "-derivate.xsl";
        }
        Transformer trans = null;

        try {
            InputStream in = MCRDerivateCommands.class.getResourceAsStream("/" + xslfile);

            if (in != null) {
                StreamSource source = new StreamSource(in);
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
    public static void repairDerivateSearch() {
        LOGGER.info("Start the repair for type derivate.");
        List<String> ar = MCRXMLTableManager.instance().retrieveAllIDs("derivate");
        if (ar.size() == 0) {
            LOGGER.warn("No ID's was found for type derivate.");
            return;
        }
        for (String stid : ar) {
            MCRDerivate der = new MCRDerivate();
            der.repairPersitenceDatastore(stid);
            LOGGER.info("Repaired " + stid);
        }
    }

    /**
     * The method start the repair the content search index for one.
     * 
     * @param id
     *            the MCRObjectID as String
     */
    public static void repairDerivateSearchForID(String id) {
        LOGGER.info("Start the repair for the ID " + id);

        MCRObjectID mid = null;

        try {
            mid = new MCRObjectID(id);
        } catch (Exception e) {
            LOGGER.error("The String " + id + " is not a MCRObjectID.");
            LOGGER.info(" ");

            return;
        }

        MCRDerivate der = new MCRDerivate();
        der.repairPersitenceDatastore(mid);
        LOGGER.info("Repaired " + mid.getId());
        LOGGER.info(" ");
    }

}