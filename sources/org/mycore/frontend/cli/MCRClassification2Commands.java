/**
 * 
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
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
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImpl;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.datamodel.common.MCRActiveLinkException;

/**
 * Commands for the classification system.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRClassification2Commands extends MCRAbstractCommands {
    private static Logger LOGGER = Logger.getLogger(MCRClassification2Commands.class);

    private static final MCRCategoryDAO DAO = new MCRCategoryDAOImpl();

    /** Default transformer script */
    public static final String DEFAULT_TRANSFORMER = "save-classification.xsl";

    public MCRClassification2Commands() {
        command.add(new MCRCommand("load classification from file {0}", "org.mycore.frontend.cli.MCRClassification2Commands.loadFromFile String",
                "The command add a new classification form file {0} to the system."));
        command.add(new MCRCommand("update classification from file {0}", "org.mycore.frontend.cli.MCRClassification2Commands.updateFromFile String",
                "The command add a new classification form file {0} to the system."));
        command.add(new MCRCommand("delete classification {0}", "org.mycore.frontend.cli.MCRClassification2Commands.delete String",
                "The command remove the classification with MCRObjectID {0} from the system."));
        command.add(new MCRCommand("load all classifications from directory {0}",
                "org.mycore.frontend.cli.MCRClassification2Commands.loadFromDirectory String",
                "The command add all classifications in the directory {0} to the system."));
        command.add(new MCRCommand("update all classifications from directory {0}",
                "org.mycore.frontend.cli.MCRClassification2Commands.updateFromDirectory String",
                "The command update all classifications in the directory {0} to the system."));
        command
                .add(new MCRCommand("export classification {0} to {1} with {2}",
                        "org.mycore.frontend.cli.MCRClassification2Commands.export String String String",
                        "The command store the classification with MCRObjectID {0} to the file named {1} with the stylesheet {2}-object.xsl. For {2} save is the default.."));
        command.add(new MCRCommand("export all classifications to {0} with {1}", "org.mycore.frontend.cli.MCRClassification2Commands.exportAll String String",
                "The command store all classifications to the directory with name {0} with the stylesheet {1}-object.xsl. For {1} save is the default."));
        command.add(new MCRCommand("count classification children of {0}", "org.mycore.frontend.cli.MCRClassification2Commands.countChildren String",
                "The command remove the classification with MCRObjectID {0} from the system."));
    }

    /**
     * Deletes a classification
     * 
     * @param classID
     * @see MCRCategoryDAO#deleteCategory(MCRCategoryID)
     */
    public static void delete(String classID) {
        DAO.deleteCategory(MCRCategoryID.rootID(classID));
    }

    /**
     * Deletes a classification
     * 
     * @param classID
     * @see MCRCategoryDAO#deleteCategory(MCRCategoryID)
     */
    public static void countChildren(String classID) {
        MCRCategory category = DAO.getCategory(MCRCategoryID.rootID(classID), -1);
        System.out.printf("%s has %d children", category.getId(), category.getChildren().size());
    }

    /**
     * Adds a classification.
     * 
     * Classification is built from a file.
     * 
     * @param filname
     *            file in mcrclass xml format
     * @see MCRCategoryDAO#addCategory(MCRCategoryID, MCRCategory)
     */
    public static void loadFromFile(String filename) {
        File file = new File(filename);
        Document xml = MCRXMLHelper.parseURI(file.getPath());
        MCRCategory category = MCRXMLTransformer.getCategory(xml);
        DAO.addCategory(null, category);
    }

    /**
     * Replaces a classification with a new version
     * 
     * @param filename
     *            file in mcrclass xml format
     * @see MCRCategoryDAO#replaceCategory(MCRCategory)
     */
    public static void updateFromFile(String filename) {
        File file = new File(filename);
        Document xml = MCRXMLHelper.parseURI(file.getPath());
        MCRCategory category = MCRXMLTransformer.getCategory(xml);
        if (DAO.exist(category.getId())) {
            DAO.replaceCategory(category);
        } else {
            // add if classification does not exist
            DAO.addCategory(null, category);
        }
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
            if (file.endsWith(".xml"))
                cmds.add((update ? "update" : "load") + " classification from file " + new File(dir, file).getAbsolutePath());

        return cmds;
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
                    LOGGER.error("Can't find or create directory " + dir.getAbsolutePath());
                    return false;
                } else {
                    dname = dirname;
                }
            } catch (Exception e) {
                LOGGER.error("Can't find or create directory " + dirname, e);
                return false;
            }
        }
        MCRCategory cl = DAO.getCategory(MCRCategoryID.rootID(ID), -1);
        Document classDoc = MCRCategoryTransformer.getMetaDataDocument(cl, false);

        Transformer trans = getTransformer(style);
        File xmlOutput = new File(dname, ID + ".xml");
        FileOutputStream out = new FileOutputStream(xmlOutput);
        if (trans != null) {
            StreamResult sr = new StreamResult(out);
            trans.transform(new org.jdom.transform.JDOMSource(classDoc), sr);
        } else {
            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            xout.output(classDoc, out);
            out.flush();
        }
        LOGGER.info("Classifcation " + ID + " saved to " + xmlOutput.getCanonicalPath() + ".");
        return true;
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

        InputStream in = MCRClassification2Commands.class.getResourceAsStream("/" + xslfile);
        if (in == null) {
            in = MCRClassification2Commands.class.getResourceAsStream(DEFAULT_TRANSFORMER);
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
     * Save all MCRClassifications.
     * 
     * @param dirname
     *            the directory name to store all classifications
     * @param style
     *            the name part of the stylesheet like <em>style</em>-classification.xsl
     * @return false if an error was occured, else true
     */
    public static boolean exportAll(String dirname, String style) throws Exception {
        List<MCRCategoryID> allClassIds = DAO.getRootCategoryIDs();
        boolean ret = false;
        for (MCRCategoryID id : allClassIds) {
            ret = ret & export(id.getRootID(), dirname, style);
        }
        return ret;
    }
}
