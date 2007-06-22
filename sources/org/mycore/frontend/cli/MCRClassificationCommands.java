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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.classifications.MCRClassification;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.common.MCRXMLTableManager;

/**
 * Provides static methods that implement commands for the MyCoRe command line
 * interface for classifications.
 * 
 * @author Jens Kupferschmidt
 * @author Frank Luetzenkirchen
 * @version $Revision$ $Date$
 */
public class MCRClassificationCommands extends MCRAbstractCommands {
    /** The logger */
    private static Logger LOGGER = Logger.getLogger(MCRClassificationCommands.class.getName());

    /** Default transformer script */
    public static final String DEFAULT_TRANSFORMER = "save-classification.xsl";

    /**
     * The empty constructor.
     */
    public MCRClassificationCommands() {
        super();

        MCRCommand com = null;

        com = new MCRCommand("delete classification {0}", "org.mycore.frontend.cli.MCRClassificationCommands.delete String", "The command remove the classification with MCRObjectID {0} from the system.");
        command.add(com);

        com = new MCRCommand("load classification from file {0}", "org.mycore.frontend.cli.MCRClassificationCommands.loadFromFile String", "The command add a new classification form file {0} to the system.");
        command.add(com);

        com = new MCRCommand("update classification from file {0}", "org.mycore.frontend.cli.MCRClassificationCommands.updateFromFile String", "The command update a classification form file {0} in the system.");
        command.add(com);

        com = new MCRCommand("load all classifications from directory {0}", "org.mycore.frontend.cli.MCRClassificationCommands.loadFromDirectory String", "The command add all classifications in the directory {0} to the system.");
        command.add(com);

        com = new MCRCommand("update all classifications from directory {0}", "org.mycore.frontend.cli.MCRClassificationCommands.updateFromDirectory String", "The command update all classifications in the directory {0} to the system.");
        command.add(com);

        com = new MCRCommand("export classification {0} to {1} with {2}", "org.mycore.frontend.cli.MCRClassificationCommands.export String String String", "The command store the classification with MCRObjectID {0} to the file named {1} with the stylesheet {2}-object.xsl. For {2} save is the default..");
        command.add(com);

        com = new MCRCommand("export all classifications to {0} with {1}", "org.mycore.frontend.cli.MCRClassificationCommands.exportAll String String", "The command store all classifications to the directory with name {0} with the stylesheet {1}-object.xsl. For {1} save is the default.");
        command.add(com);

        com = new MCRCommand("repair all classifications", "org.mycore.frontend.cli.MCRClassificationCommands.repairAll", "The command repair all classifications in SQL tables with BLOB data from XML table.");
        command.add(com);
    }

    /**
     * Deletes an MCRClassification from the datastore.
     * 
     * @param ID
     *            the ID of the MCRClassification that should be deleted
     */
    public static void delete(String ID) throws MCRActiveLinkException {
        MCRObjectID mcr_id = new MCRObjectID(ID);
        MCRClassification.deleteFromDatastore(mcr_id);
        LOGGER.info(ID + " deleted.");
    }

    /**
     * Loads MCRClassification from all XML files in a directory.
     * 
     * @param directory
     *            the directory containing the XML files
     * @throws MCRActiveLinkException
     */
    public static List<String> loadFromDirectory(String directory) throws MCRActiveLinkException {
        return processFromDirectory(directory, false);
    }

    /**
     * Updates MCRClassification from all XML files in a directory.
     * 
     * @param directory
     *            the directory containing the XML files
     * @throws MCRActiveLinkException
     */
    public static List<String> updateFromDirectory(String directory) throws MCRActiveLinkException {
        return processFromDirectory(directory, true);
    }

    /**
     * Loads or updates MCRClassification from all XML files in a directory.
     * 
     * @param directory
     *            the directory containing the XML files
     * @param update
     *            if true, classification will be updated, else Classification
     *            is created
     * @throws MCRActiveLinkException
     */
    private static List<String> processFromDirectory(String directory, boolean update) throws MCRActiveLinkException {
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
            if ( file.endsWith(".xml") )
                cmds.add( ( update ? "update" : "load" ) + " classification from file " + new File( dir, file ).getAbsolutePath() );

        return cmds;
    }

    /**
     * Loads an MCRClassification from an XML file.
     * 
     * @param file
     *            the location of the xml file
     * @throws MCRActiveLinkException
     */
    public static boolean loadFromFile(String file) throws MCRActiveLinkException {
        return processFromFile(new File(file), false);
    }

    /**
     * Updates an MCRClassification from an XML file.
     * 
     * @param file
     *            the location of the xml file
     * @throws MCRActiveLinkException
     */
    public static boolean updateFromFile(String file) throws MCRActiveLinkException {
        return processFromFile(new File(file), true);
    }

    /**
     * Loads or updates an MCRClassification from an XML file.
     * 
     * @param file
     *            the location of the xml file
     * @param update
     *            if true, classification will be updated, else classification
     *            is created
     * @throws MCRActiveLinkException
     */
    private static boolean processFromFile(File file, boolean update) throws MCRActiveLinkException {
        if (!file.getName().endsWith(".xml")) {
            LOGGER.warn(file + " ignored, does not end with *.xml");
            return false;
        }

        if (!file.isFile()) {
            LOGGER.warn(file + " ignored, is not a file.");
            return false;
        }

        LOGGER.info("Reading file " + file + " ...\n");

        MCRClassification cl = new MCRClassification();
        cl.setFromURI(file.getAbsolutePath());

        if (update) {
            cl.updateInDatastore();
            LOGGER.info(cl.getId() + " updated.\n");
        } else {
            cl.createInDatastore();
            LOGGER.info(cl.getId() + " loaded.\n");
        }

        return true;
    }

    /**
     * Save a MCRClassification.
     * 
     * @param ID
     *            the ID of the MCRClassification to be save.
     * @param filename
     *            the filename to store the classification
     * @param style
     *            the name part of the stylesheet like <em>style</em>-classification.xsl
     * @return false if an error was occured, else true
     */
    public static boolean export(String ID, String dirname, String style) throws Exception {
        String dname = "";
        if (dirname.length() != 0) {
            try {
                File dir = new File(dirname);
                if (!dir.isDirectory()) {
                    dir.mkdir();
                }
                if (!dir.isDirectory()) {
                    LOGGER.error("Can't find or create directory " + dirname);
                    LOGGER.info("");
                    return false;
                } else {
                    dname = dirname;
                }
            } catch (Exception e) {
                LOGGER.error("Can't find or create directory " + dirname);
                LOGGER.info("");
                return false;
            }
        }
        MCRObjectID mcr_id = new MCRObjectID(ID);
        byte[] xml = MCRClassification.retrievClassificationAsXML(mcr_id.getId());

        Transformer trans = getTransformer(style);
        File xmlOutput = new File(dname, ID + ".xml");
        FileOutputStream out = new FileOutputStream(xmlOutput);
        if (trans != null) {
            StreamResult sr = new StreamResult(out);
            trans.transform(new org.jdom.transform.JDOMSource(MCRXMLHelper.parseXML(xml, false)), sr);
        } else {
            out.write(xml);
            out.flush();
        }
        LOGGER.info("Classifcation " + ID + " saved to " + xmlOutput.getCanonicalPath() + ".");
        LOGGER.info("");
        return true;
    }

    /**
     * Save all MCRClassifications.
     * 
     * @param dirname
     *            the directory name to store all classifications
     * @param style
     *            the name part of the stylesheet like <em>style</em>-classification.xsl
     * @return false if an error was occured, else true
     */
    public static boolean exportAll(String dirname, String style) throws Exception {
        List li = MCRXMLTableManager.instance().retrieveAllIDs("class");
        boolean ret = false;
        for (int i = 0; i < li.size(); i++) {
            String id = (String) li.get(i);
            ret = ret & export(id, dirname, style);
        }
        return ret;
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
            xslfile = style + "-classification.xsl";
        }
        Transformer trans = null;

        InputStream in = MCRClassificationCommands.class.getResourceAsStream("/" + xslfile);
        if (in == null) {
            in = MCRClassificationCommands.class.getResourceAsStream(DEFAULT_TRANSFORMER);
        }
        if (in != null) {
            StreamSource source = new StreamSource(in);
            TransformerFactory transfakt = TransformerFactory.newInstance();
            transfakt.setURIResolver(MCRURIResolver.instance());
            trans = transfakt.newTransformer(source);
        }
        return trans;
    }

    /**
     * The method read all classifications from the MCRXMLTableManager and
     * repair the other SQL tables. If no ACL entry exist it set a default
     * entry.
     */
    public static void repairAll() {
        MCRXMLTableManager TM = MCRXMLTableManager.instance();
        List<String> list = TM.retrieveAllIDs("class");
        for (int i = 0; i < list.size(); i++) {
            String mcrid = (String) list.get(i);
            byte[] xml = TM.retrieveAsXML(new MCRObjectID(mcrid));
            MCRClassification cl = new MCRClassification();
            cl.setFromXML(xml);
            cl.repairInDatastore();
            LOGGER.info("Classification " + mcrid + " repaired.");
            LOGGER.info("");
        }
    }
}
