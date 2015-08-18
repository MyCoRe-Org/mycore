/*
 * 
 * $Revision: 30735 $ $Date: 2014-09-29 14:22:50 +0200 (Mo, 29. Sep 2014) $
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

// package
package org.mycore.frontend.workflow;

import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRFileContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.validator.MCREditorOutValidator;
import org.mycore.frontend.cli.MCRDerivateCommands;
import org.mycore.frontend.cli.MCRObjectCommands;
import org.xml.sax.SAXParseException;

/**
 * This class holds methods to manage the workflow file system of MyCoRe.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision: 30735 $ $Date: 2014-09-29 14:22:50 +0200 (Mo, 29. Sep 2014) $
 */

public class MCRSimpleWorkflowManager {

    /** The link table manager singleton */
    protected static MCRSimpleWorkflowManager singleton;

    // Configuration
    private static MCRConfiguration config = null;

    // logger
    static Logger logger = Logger.getLogger(MCRSimpleWorkflowManager.class.getName());

    // The mail sender address
    static String sender = "";

    // table of workflow directories mail addresses
    private Hashtable<String, File> ht = null;

    private Hashtable<String, ArrayList<String>> mt = null;

    /**
     * Returns the workflow manager singleton.
     */
    public static synchronized MCRSimpleWorkflowManager instance() {
        if (singleton == null) {
            singleton = new MCRSimpleWorkflowManager();
        }

        return singleton;
    }

    /**
     * The constructor of this class.
     */
    protected MCRSimpleWorkflowManager() {
        config = MCRConfiguration.instance();

        // read mail sender address
        sender = config.getString("MCR.Mail.Sender");

        // int tables
        ht = new Hashtable<String, File>();
        mt = new Hashtable<String, ArrayList<String>>();
    }

    /**
     * The method return the workflow directory path for a given MCRObjectID
     * type.
     * 
     * @param base
     *            the MCRObjectID type
     * @return the string of the workflow directory path
     */
    public final File getDirectoryPath(String base) {
        if (ht.containsKey(base)) {
            return ht.get(base);
        }
        String dirname = config.getString("MCR.SWF.Directory." + base, null);
        if (dirname == null) {
            int ibase = base.indexOf('_');
            if (ibase != -1) {
                String type = base.substring(ibase + 1);
                dirname = config.getString("MCR.SWF.Directory." + type, null);
            }
            if (dirname == null) {
                final File currentDir = new File(".");
                ht.put(base, currentDir);
                logger.warn("No workflow directory path of " + base + " is in the configuration.");
                return currentDir;
            }
        }
        File dir = new File(dirname);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        ht.put(base, dir);
        return dir;
    }

    /**
     * The method return the information mail address for a given MCRObjectID
     * type.
     * 
     * @param base
     *            the MCRObjectID base or MCRObjectID type
     * @param todo
     *            the todo action String from the workflow.
     * @return the List of the information mail addresses
     */
    public final List<String> getMailAddress(String base, String todo) {
        if ((base == null) || ((base = base.trim()).length() == 0)) {
            return new ArrayList<String>();
        }

        if ((todo == null) || ((todo = todo.trim()).length() == 0)) {
            return new ArrayList<String>();
        }

        if (mt.containsKey(base + "_" + todo)) {
            return mt.get(base + "_" + todo);
        }

        String mailaddr = config.getString("MCR.SWF.Mail." + base + "." + todo, "");
        ArrayList<String> li = new ArrayList<String>();

        if ((mailaddr == null) || ((mailaddr = mailaddr.trim()).length() == 0)) {
            int i = base.indexOf('_');
            if (i != -1) {
                String type = base.substring(i + 1);
                mailaddr = config.getString("MCR.SWF.Mail." + type + "." + todo, "");
                if ((mailaddr == null) || ((mailaddr = mailaddr.trim()).length() == 0)) {
                    mt.put(base, li);
                    logger.warn("No mail address for MCR.SWF.Mail." + base + "." + todo + " is in the configuration.");
                    return li;
                }
            } else {
                mt.put(base, li);
                logger.warn("No mail address for MCR.SWF.Mail." + base + "." + todo + " is in the configuration.");
                return li;
            }
        }

        StringTokenizer st = new StringTokenizer(mailaddr, ",");
        while (st.hasMoreTokens()) {
            li.add(st.nextToken());
        }
        mt.put(base, li);

        return li;
    }

    /**
     * The method return the mail sender address form the configuration.
     * 
     * @return the mail sender address
     */
    public final String getMailSender() {
        return sender;
    }

    /**
     * The method return a ArrayList of file names from objects they are under
     * .../workflow/ <em>type/...type...</em>.
     * 
     * @param base
     *            the MCRObjectID base attribute
     * @return an ArrayList of file names
     */
    public final ArrayList<String> getAllObjectFileNames(String base) {
        File dir = getDirectoryPath(base);
        ArrayList<String> workfiles = new ArrayList<String>();

        String[] dirl = null;

        if (dir.isDirectory()) {
            dirl = dir.list();
        }

        if (dirl != null) {
            for (String aDirl : dirl) {
                if ((aDirl.contains(base)) && (aDirl.endsWith(".xml"))) {
                    workfiles.add(aDirl);
                }
            }
        }

        java.util.Collections.sort(workfiles);

        return workfiles;
    }

    /**
     * The method return a ArrayList of file names form derivates they are under
     * .../workflow/ <em>type/...derivate...</em>.
     * 
     * @param base
     *            the MCRObjectID type attribute
     * @return an ArrayList of file names
     */
    public final ArrayList<String> getAllDerivateFileNames(String base) {
        File dir = getDirectoryPath(base);
        ArrayList<String> workfiles = new ArrayList<String>();
        String[] dirl = null;

        if (dir.isDirectory()) {
            dirl = dir.list();
        }

        if (dirl != null) {
            for (String aDirl : dirl) {
                if ((aDirl.contains("_derivate_")) && (aDirl.endsWith(".xml"))) {
                    workfiles.add(aDirl);
                }
            }
        }
        java.util.Collections.sort(workfiles);
        return workfiles;
    }

    /**
     * The method read a derivate file with name <em>filename</em> in the
     * workflow directory of <em>type</em> and check that this derivate
     * reference the given <em>ID</em>.
     * 
     * @param filename
     *            the file name of the derivate
     * @param ID
     *            the MCRObjectID of the metadata object
     * @return true if the derivate refernce the metadata object, else return
     *         false
     */
    public final boolean isDerivateOfObject(String filename, MCRObjectID ID) {
        File dir = getDirectoryPath(ID.getProjectId() + "_derivate");
        File fname = new File(dir, filename);
        org.jdom2.Document workflow_in = null;

        try {
            workflow_in = MCRXMLParserFactory.getParser().parseXML(new MCRFileContent(fname));
            logger.debug("Readed from workflow " + fname);
        } catch (Exception ex) {
            logger.error("Error while reading XML workflow file " + filename);
            logger.error(ex.getMessage());

            return false;
        }

        org.jdom2.Element root = workflow_in.getRootElement();
        org.jdom2.Element derivate = root.getChild("derivate");

        if (derivate == null) {
            return false;
        }

        org.jdom2.Element linkmetas = derivate.getChild("linkmetas");

        if (linkmetas == null) {
            return false;
        }

        org.jdom2.Element linkmeta = linkmetas.getChild("linkmeta");

        if (linkmeta == null) {
            return false;
        }

        String DID = linkmeta.getAttributeValue("href", XLINK_NAMESPACE);
        logger.debug("The linked object ID of derivate is " + DID);

        return ID.toString().equals(DID);
    }

    /**
     * The method removes a metadata object with all referenced derivate objects
     * from the workflow.
     * 
     * @param ID
     *            the MCRObjectID of the metadata object
     */
    public final void deleteMetadataObject(MCRObjectID ID) {
        // remove metadate
        String fn = getDirectoryPath(ID.getBase()) + File.separator + ID + ".xml";

        try {
            File fi = new File(fn);

            if (fi.isFile() && fi.canWrite()) {
                fi.delete();
                logger.debug("File " + fn + " removed.");
            } else {
                logger.error("Can't remove file " + fn);
            }
        } catch (Exception ex) {
            logger.error("Can't remove file " + fn);
        }

        // remove derivate
        ArrayList<String> derifiles = getAllDerivateFileNames(ID.getProjectId() + "_derivate");

        for (String dername : derifiles) {
            logger.debug("Check the derivate file " + dername);

            if (isDerivateOfObject(dername, ID)) {
                try {
                    MCRObjectID DID = MCRObjectID.getInstance(dername.substring(0, dername.length() - 4));

                    deleteDerivateObject(ID, DID);
                } catch (MCRException ex) {
                }
            }
        }
    }

    /**
     * The method removes a derivate object from the workflow.
     * 
     * @param ID
     *            the MCRObjectID type of the metadata object
     * @param DID
     *            the MCRObjectID of the derivate object as String
     */
    public final void deleteDerivateObject(MCRObjectID ID, MCRObjectID DID) {
        logger.debug("Delete the derivate " + DID.toString());
        // remove the XML file
        String fn = getDirectoryPath(DID.getBase()) + File.separator + DID.toString();
        try {
            File fi = new File(fn + ".xml");

            if (fi.isFile() && fi.canWrite()) {
                fi.delete();
                logger.debug("File " + fn + ".xml removed.");
            } else {
                logger.error("Can't remove file " + fn + ".xml");
            }
        } catch (Exception ex) {
            logger.error("Can't remove file " + fn + ".xml");
        }
        // remove all derivate objects
        try {
            File fi = new File(fn);
            if (fi.isDirectory() && fi.canWrite()) {
                // delete files
                ArrayList<String> dellist = MCRUtils.getAllFileNames(fi);

                for (String aDellist : dellist) {
                    String na = (String) aDellist;
                    File fl = new File(fn + File.separator + na);

                    if (fl.delete()) {
                        logger.debug("File " + na + " removed.");
                    } else {
                        logger.error("Can't remove file " + na);
                    }
                }
                // delete subirectories
                dellist = MCRUtils.getAllDirectoryNames(fi);

                for (int j = dellist.size() - 1; j > -1; j--) {
                    String na = (String) dellist.get(j);
                    File fl = new File(fn + File.separator + na);

                    if (fl.delete()) {
                        logger.debug("Directory " + na + " removed.");
                    } else {
                        logger.error("Can't remove directory " + na);
                    }
                }
                if (fi.delete()) {
                    logger.debug("Directory " + fn + " removed.");
                } else {
                    logger.error("Can't remove directory " + fn);
                }
            } else {
                logger.error("Can't remove directory " + fn);
            }
        } catch (Exception ex) {
            logger.error("Can't remove directory " + fn.substring(0, fn.length() - 4));
        }
    }

    /**
     * The method commit a metadata object with all referenced derivate objects
     * from the workflow to the data store.
     * 
     * @param ID
     *            the ID of the metadata object
     * @throws MCRActiveLinkException
     *             if links to the object exist prior loading
     */
    public final boolean commitMetadataObject(MCRObjectID ID) throws MCRActiveLinkException, MCRException,
        SAXParseException, IOException {
        // commit metadata
        String fn = getDirectoryPath(ID.getBase()) + File.separator + ID + ".xml";

        if (MCRMetadataManager.exists(ID)) {
            MCRObjectCommands.updateFromFile(fn, false);
        } else {
            MCRObjectCommands.loadFromFile(fn, false);
        }

        logger.info("The metadata objekt was " + fn + " loaded.");
        // commit derivates
        if (!MCRMetadataManager.exists(ID)) {
            return false;
        }

        String derivateBase = ID.getProjectId() + "_derivate";
        ArrayList<String> derifiles = getAllDerivateFileNames(derivateBase);

        for (String dername : derifiles) {
            logger.debug("Check the derivate file " + dername);

            if (isDerivateOfObject(dername, ID)) {
                fn = getDirectoryPath(derivateBase) + File.separator + dername;

                if (!loadDerivate(ID, fn)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * The method commit a derivate object with update method from the workflow
     * to the data store.
     * 
     * @param ID
     *            the MCRObjectID as String of the derivate object
     */
    public final boolean commitDerivateObject(MCRObjectID ID) throws SAXParseException, IOException {
        String fn = getDirectoryPath(ID.getBase()) + File.separator + ID.toString() + ".xml";

        return loadDerivate(ID, fn);
    }

    private boolean loadDerivate(MCRObjectID objectID, String filename) throws SAXParseException, IOException {
        if (MCRMetadataManager.exists(objectID)) {
            MCRDerivateCommands.updateFromFile(filename, false);
        } else {
            MCRDerivateCommands.loadFromFile(filename, false);
        }

        if (!MCRMetadataManager.exists(objectID)) {
            return false;
        }

        logger.debug("Commit the derivate " + filename);

        return true;
    }

    /**
     * The method return the next free object ID. It looks in the current workflow directory and in the server.
     */
    public synchronized final MCRObjectID getNextObjectID(MCRObjectID ID) {
        return getNextID(ID.getBase());
    }

    /**
     * The method return the next free derivate ID. It looks in the current workflow directory and in the server.
     */
    public synchronized final MCRObjectID getNextDrivateID(MCRObjectID ID) {
        String myproject = ID.getProjectId() + "_derivate";
        return getNextID(myproject);
    }

    /**
     * The method return the next free derivate ID. It looks in the current workflow directory and in the server.
     */
    public synchronized final MCRObjectID getNextID(String base_id) {
        final String myproject = base_id;
        Set<File> workdirs = new HashSet<File>();
        workdirs.add(getDirectoryPath(base_id));
        Map<String, String> propsWD = config.getPropertiesMap("MCR.SWF.Directory.");
        for (String key : propsWD.keySet()) {
            File dir = new File(propsWD.get(key));
            if (!dir.exists()) {
                dir.mkdirs();
            }
            workdirs.add(dir);
        }

        FilenameFilter derivateFilenameFilter = (dir, name) -> name.startsWith(myproject) && name.endsWith(".xml");

        String max = myproject + "_0.xml";
        for (File workdir : workdirs) {
            for (String file : workdir.list(derivateFilenameFilter)) {
                if (file.compareTo(max) > 0) {
                    max = file;
                }
            }
        }
        int maxIDinWorkflow = Integer.parseInt(max.substring(max.lastIndexOf("_") + 1, max.length() - 4));

        MCRObjectID mcridnext = MCRObjectID.getNextFreeId(myproject, maxIDinWorkflow);
        return mcridnext;
    }

    /**
     * The method create a new MCRDerivate and store them to the directory of
     * the workflow that correspons with the type of the given object
     * MCRObjectID with the name of itseslf. Also ti create a ne directory with
     * the same new name. This new derivate ID was returned.
     * 
     * @param ID
     *            the MCRObjectID of the related object
     * @param DD
     *            the MCRObjectID of the related derivate
     * @return the MCRObjectID of the derivate
     */
    public final MCRDerivate createDerivate(MCRObjectID ID, MCRObjectID DD) {
        // build the derivate XML file
        MCRDerivate der = new MCRDerivate();
        der.setId(DD);
        der.setLabel("Dataobject from " + ID.toString());
        der.setSchema("datamodel-derivate.xsd");

        MCRMetaLinkID link = new MCRMetaLinkID("linkmeta", 0);
        link.setReference(ID.toString(), null, null);
        der.getDerivate().setLinkMeta(link);

        MCRMetaIFS internal = new MCRMetaIFS("internal", DD.toString());
        internal.setMainDoc("");
        der.getDerivate().setInternals(internal);

        org.jdom2.Element elm = der.getService().createXML();
        MCREditorOutValidator.setDefaultDerivateACLs(elm);
        der.getService().setFromDOM(elm);

        return der;
    }

    /**
     * The method return the conditione XML tree from a XML file in the workflow
     * for a given permission.
     * 
     * @param mcrid
     *            the MCRObjectID 
     * @param permission
     *            the permission for the ACL system
     * @return the XML tree of the condition or null if the permission is not defined
     */
    public final org.jdom2.Element getRuleFromFile(MCRObjectID mcrid, String permission) {
        // read data
        String fn = getDirectoryPath(mcrid.getBase()) + File.separator + mcrid.toString() + ".xml";
        try {
            File fi = new File(fn);
            if (fi.isFile() && fi.canRead()) {
                Document wfDoc = MCRXMLParserFactory.getNonValidatingParser().parseXML(new MCRFileContent(fi));
                XPathExpression<Element> path = XPathFactory.instance().compile(
                    "/*/service/servacls/servacl[@permission='" + permission + "']/condition", Filters.element());
                List<Element> results = path.evaluate(wfDoc);
                if (results.size() > 0) {
                    return (Element) ((Element) results.get(0)).detach();
                }
            } else {
                logger.error("Can't read file " + fn);
            }
        } catch (Exception ex) {
            logger.error("Can't read file " + fn);
        }
        return null;
    }

    /**
     * The method return page name of the next URL of the workflow.
     * 
     * @param context the servlet context
     * @param pagedir the base directory of the WEB application
     * @param base the MCRObjectID base ID
     * @return the workflow URL
     */
    public final String getWorkflowFile(ServletContext context, String pagedir, String base) {
        StringBuffer sb = new StringBuffer();
        sb.append(pagedir).append("editor_").append(base).append("_editor.xml");
        String file_path = context.getRealPath(sb.toString());
        if (file_path == null || !(new File(file_path).exists())) {
            sb = new StringBuffer();
            int i = base.indexOf('_');
            if (i > -1) {
              sb.append(pagedir).append("editor_").append(base.substring(i + 1)).append("_editor.xml");
              file_path = context.getRealPath(sb.toString());
              if (file_path == null || !new File(file_path).exists()) {
                sb = new StringBuffer("");
              }
           } else {
               sb = new StringBuffer("");
           }
        }
        return sb.toString();
    }
}
