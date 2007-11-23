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

// package
package org.mycore.frontend.workflow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.jaxen.XPath;
import org.jaxen.jdom.JDOMXPath;
import org.jdom.Document;
import org.jdom.Element;

import org.mycore.common.MCRConfiguration;
import static org.mycore.common.MCRConstants.*;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.frontend.cli.MCRDerivateCommands;
import org.mycore.frontend.cli.MCRObjectCommands;

/**
 * This class holds methods to manage the workflow file system of MyCoRe.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
/**
 * @author mcradmin
 *
 */
/**
 * @author mcradmin
 * 
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
    private Hashtable<String, String> ht = null;

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
        sender = config.getString("MCR.Mail.Address", "mcradmin@localhost");

        // int tables
        ht = new Hashtable<String, String>();
        mt = new Hashtable<String, ArrayList<String>>();
    }

    /**
     * The method return the workflow directory path for a given MCRObjectID
     * type.
     * 
     * @param type
     *            the MCRObjectID type
     * @return the string of the workflow directory path
     */
    public final String getDirectoryPath(String type) {
        if (ht.containsKey(type)) {
            return ht.get(type);
        }

        String dirname = config.getString("MCR.SWF.Directory." + type, null);

        if (dirname == null) {
            ht.put(type, ".");
            logger.warn("No workflow directory path of " + type + " is in the configuration.");

            return ".";
        }

        ht.put(type, dirname);

        return dirname;
    }

    /**
     * The method return the information mail address for a given MCRObjectID
     * type.
     * 
     * @param type
     *            the MCRObjectID type
     * @param todo
     *            the todo action String from the workflow.
     * @return the List of the information mail addresses
     */
    public final List<String> getMailAddress(String type, String todo) {
        if ((type == null) || ((type = type.trim()).length() == 0)) {
            return new ArrayList<String>();
        }

        if ((todo == null) || ((todo = todo.trim()).length() == 0)) {
            return new ArrayList<String>();
        }

        if (mt.containsKey(type + "_" + todo)) {
            return mt.get(type + "_" + todo);
        }

        String mailaddr = config.getString("MCR.SWF.Mail." + type + "." + todo, "");
        ArrayList<String> li = new ArrayList<String>();

        if ((mailaddr == null) || ((mailaddr = mailaddr.trim()).length() == 0)) {
            mt.put(type, li);
            logger.warn("No mail address for MCR.SWF.Mail." + type + "." + todo + " is in the configuration.");

            return li;
        }

        StringTokenizer st = new StringTokenizer(mailaddr, ",");

        while (st.hasMoreTokens()) {
            li.add(st.nextToken());
        }

        mt.put(type, li);

        return li;
    }

    /**
     * The method return the mail sender adress form the configuration.
     * 
     * @return the mail sender adress
     */
    public final String getMailSender() {
        return sender;
    }

    /**
     * The method return a ArrayList of file names from objects they are under
     * .../workflow/ <em>type/...type...</em>.
     * 
     * @param type
     *            the MCRObjectID type attribute
     * @return an ArrayList of file names
     */
    public final ArrayList<String> getAllObjectFileNames(String type) {
        String dirname = getDirectoryPath(type);
        ArrayList<String> workfiles = new ArrayList<String>();

        if (!dirname.equals(".")) {
            File dir = new File(dirname);
            String[] dirl = null;

            if (dir.isDirectory()) {
                dirl = dir.list();
            }

            if (dirl != null) {
                for (int i = 0; i < dirl.length; i++) {
                    if ((dirl[i].indexOf(type) != -1) && (dirl[i].endsWith(".xml"))) {
                        workfiles.add(dirl[i]);
                    }
                }
            }

            java.util.Collections.sort(workfiles);
        }

        return workfiles;
    }

    /**
     * The method return a ArrayList of file names form derivates they are under
     * .../workflow/ <em>type/...derivate...</em>.
     * 
     * @param type
     *            the MCRObjectID type attribute
     * @return an ArrayList of file names
     */
    public final ArrayList<String> getAllDerivateFileNames(String type) {
        String dirname = getDirectoryPath(type);
        ArrayList<String> workfiles = new ArrayList<String>();

        if (!dirname.equals(".")) {
            File dir = new File(dirname);
            String[] dirl = null;

            if (dir.isDirectory()) {
                dirl = dir.list();
            }

            if (dirl != null) {
                for (int i = 0; i < dirl.length; i++) {
                    if ((dirl[i].indexOf("_derivate_") != -1) && (dirl[i].endsWith(".xml"))) {
                        workfiles.add(dirl[i]);
                    }
                }
            }

            java.util.Collections.sort(workfiles);
        }

        return workfiles;
    }

    /**
     * The method read a derivate file with name <em>filename</em> in the
     * workflow directory of <em>type</em> and check that this derivate
     * reference the given <em>ID</em>.
     * 
     * @param type
     *            the MCRObjectID type
     * @param filename
     *            the file name of the derivate
     * @param ID
     *            the MCRObjectID of the metadata object
     * @return true if the derivate refernce the metadata object, else return
     *         false
     */
    public final boolean isDerivateOfObject(String type, String filename, String ID) {
        String dirname = getDirectoryPath(type);
        String fname = dirname + File.separator + filename;
        org.jdom.Document workflow_in = null;

        try {
            workflow_in = MCRXMLHelper.parseURI(fname);
            logger.debug("Readed from workflow " + fname);
        } catch (Exception ex) {
            logger.error("Error while reading XML workflow file " + filename);
            logger.error(ex.getMessage());

            return false;
        }

        org.jdom.Element root = workflow_in.getRootElement();
        org.jdom.Element derivate = root.getChild("derivate");

        if (derivate == null) {
            return false;
        }

        org.jdom.Element linkmetas = derivate.getChild("linkmetas");

        if (linkmetas == null) {
            return false;
        }

        org.jdom.Element linkmeta = linkmetas.getChild("linkmeta");

        if (linkmeta == null) {
            return false;
        }

        String DID = linkmeta.getAttributeValue("href", XLINK_NAMESPACE);
        logger.debug("The linked object ID of derivate is " + DID);

        if (!ID.equals(DID)) {
            return false;
        }

        return true;
    }

    /**
     * The method removes a metadata object with all referenced derivate objects
     * from the workflow.
     * 
     * @param type
     *            the MCRObjectID type of the metadata object
     * @param ID
     *            the ID of the metadata object
     */
    public final void deleteMetadataObject(String type, String ID) {
        // remove metadate
        String fn = getDirectoryPath(type) + File.separator + ID + ".xml";

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
        ArrayList<String> derifiles = getAllDerivateFileNames(type);

        for (int i = 0; i < derifiles.size(); i++) {
            String dername = derifiles.get(i);
            logger.debug("Check the derivate file " + dername);

            if (isDerivateOfObject(type, dername, ID)) {
                deleteDerivateObject(type, dername.substring(0, dername.length() - 4));
            }
        }
    }

    /**
     * The method removes a derivate object from the workflow.
     * 
     * @param type
     *            the MCRObjectID type of the metadata object
     * @param id
     *            the MCRObjectID of the derivate object as String
     */
    @SuppressWarnings("unchecked")
    public final void deleteDerivateObject(String type, String id) {
        logger.debug("Delete the derivate " + id);

        // remove the XML file
        String fn = getDirectoryPath(type) + File.separator + id;

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
                ArrayList dellist = MCRUtils.getAllFileNames(fi);

                for (int j = 0; j < dellist.size(); j++) {
                    String na = (String) dellist.get(j);
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
     * @param type
     *            the MCRObjectID type of the metadata object
     * @param ID
     *            the ID of the metadata object
     * @throws MCRActiveLinkException
     *             if links to the object exist prior loading
     */
    public final boolean commitMetadataObject(String type, String ID) throws MCRActiveLinkException {
        // commit metadata
        String fn = getDirectoryPath(type) + File.separator + ID + ".xml";

        if (MCRObject.existInDatastore(ID)) {
            MCRObjectCommands.updateFromFile(fn, false);
        } else {
            MCRObjectCommands.loadFromFile(fn, false);
        }

        logger.info("The metadata objekt was " + fn + " loaded.");
        // commit derivates
        if (!MCRObject.existInDatastore(ID)) {
            return false;
        }

        ArrayList<String> derifiles = getAllDerivateFileNames(type);

        for (int i = 0; i < derifiles.size(); i++) {
            String dername = derifiles.get(i);
            logger.debug("Check the derivate file " + dername);

            if (isDerivateOfObject(type, dername, ID)) {
                fn = getDirectoryPath(type) + File.separator + dername;

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
     * @param type
     *            the MCRObjectID type of the metadata object
     * @param ID
     *            the ID as String of the derivate object
     */
    public final boolean commitDerivateObject(String type, String ID) {
        String fn = getDirectoryPath(type) + File.separator + ID + ".xml";

        return loadDerivate(ID, fn);
    }

    private boolean loadDerivate(String ID, String filename) {
        if (MCRDerivate.existInDatastore(ID)) {
            MCRDerivateCommands.updateFromFile(filename, false);
        } else {
            MCRDerivateCommands.loadFromFile(filename, false);
        }

        if (!MCRDerivate.existInDatastore(ID)) {
            return false;
        }

        logger.debug("Commit the derivate " + filename);

        return true;
    }

    /**
     * The method return the next free derivate ID. It looks in the current
     * workflow directory and in the server.
     */
    private synchronized final MCRObjectID getNextDrivateID(MCRObjectID ID) {
        String myproject = ID.getProjectId() + "_derivate";
        MCRObjectID dmcridnext = new MCRObjectID();
        dmcridnext.setNextFreeId(myproject);

        String workdir = getDirectoryPath(ID.getTypeId());
        File workf = new File(workdir);
        if (workf.isDirectory()) {
            String[] list = workf.list();
            for (int i = 0; i < list.length; i++) {
                if (!list[i].startsWith(myproject)) {
                    continue;
                }
                if (!list[i].endsWith(".xml")) {
                    continue;
                }
                try {
                    MCRObjectID dmcriddir = new MCRObjectID(list[i].substring(0, list[i].length() - 4));
                    if (dmcridnext.getNumberAsInteger() <= dmcriddir.getNumberAsInteger()) {
                        dmcriddir.setNumber(dmcriddir.getNumberAsInteger() + 1);
                        dmcridnext = dmcriddir;
                    }
                } catch (Exception e) {
                }
            }
        }
        return dmcridnext;
    }

    /**
     * The method create a new MCRDerivate and store them to the directory of
     * the workflow that correspons with the type of the given object
     * MCRObjectID with the name of itseslf. Also it create a directory with
     * the same new name. This new derivate ID will return.
     * 
     * @param objmcrid
     *            the MCRObjectID of the related object
     * @return the MCRObjectID of the derivate
     */
    public final String createDerivateInWorkflow(String objmcrid) {
        // prepare the derivate MCRObjectID
        MCRObjectID ID = new MCRObjectID(objmcrid);
        MCRObjectID DD = getNextDrivateID(ID);
        logger.debug("New derivate ID " + DD.getId());
        String workdir = getDirectoryPath(ID.getTypeId());

        // create a new directory
        String dirname = workdir + File.separator + DD.getId();
        File dir = new File(dirname);
        dir.mkdir();
        logger.debug("Directory " + dirname + " created.");

        // get derivate xml object
        MCRDerivate der = createDerivate(ID, DD);
        byte[] outxml = MCRUtils.getByteArray(der.createXML());
        String fullname = workdir + File.separator + DD.getId() + ".xml";
        try {
            FileOutputStream out = new FileOutputStream(fullname);
            out.write(outxml);
            out.flush();
        } catch (IOException ex) {
            logger.error(ex.getMessage());
            logger.error("Exception while store to file " + fullname);
            return "";
        }
        logger.info("Derivate " + DD.getId() + " stored under " + fullname + ".");
        return DD.getId();
    }

    /**
     * The method create a new MCRDerivate and store them in the server. This new derivate ID will return.
     * 
     * @param objmcrid
     *            the MCRObjectID of the related object
     * @return the MCRObjectID of the derivate
     */
    public final String createDerivateInServer(String objmcrid) {
        // prepare the derivate MCRObjectID
        MCRObjectID ID = new MCRObjectID(objmcrid);
        MCRObjectID DD = getNextDrivateID(ID);
        logger.debug("New derivate ID " + DD.getId());

        // get derivate xml object
        MCRDerivate der = createDerivate(ID, DD);
        der.updateInDatastore();
        logger.info("Derivate " + DD.getId() + " stored in server.");
        return DD.getId();
    }

    /**
     * The method create a new MCRDerivate and store them to the directory of
     * the workflow that correspons with the type of the given object
     * MCRObjectID with the name of itseslf. Also ti create a ne directory with
     * the same new name. This new derivate ID was returned.
     * 
     * @param objmcrid
     *            the MCRObjectID of the related object
     * @param server
     *            the flag to show that the data came from a server
     * @return the MCRObjectID of the derivate
     */
    private final MCRDerivate createDerivate(MCRObjectID ID, MCRObjectID DD) {
        // build the derivate XML file
        MCRDerivate der = new MCRDerivate();
        der.setId(DD);
        der.setLabel("Dataobject from " + ID.getId());
        der.setSchema("datamodel-derivate.xsd");

        MCRMetaLinkID link = new MCRMetaLinkID("linkmetas", "linkmeta", "de", 0);
        link.setReference(ID.getId(), "", "");
        der.getDerivate().setLinkMeta(link);

        MCRMetaIFS internal = new MCRMetaIFS("internals", "internal", "de", DD.getId());
        internal.setMainDoc("");
        der.getDerivate().setInternals(internal);

        MCRObject obj = new MCRObject();

        try {
            obj.receiveFromDatastore(ID);
            MCRObjectService serv = obj.getService();
            der.setService(serv);
        } catch (Exception e) {
            String workdir = getDirectoryPath(ID.getTypeId());
            try {
                obj.setFromURI(workdir + File.separator + ID.getId() + ".xml");
                MCRObjectService serv = obj.getService();
                der.setService(serv);
            } catch (Exception e2) {
                logger.warn("Read error of " + workdir + File.separator + ID.getId() + ".xml");
            }
        }
        MCRObjectService service = new MCRObjectService();
        org.jdom.Element elm = service.createXML();
        MCREditorOutValidator.setDefaultDerivateACLs(elm);
        service.setFromDOM(elm);
        der.setService(service);

        return der;
    }

    /**
     * The method return the conditione XML tree from a XML file in the workflow
     * for a given permission.
     * 
     * @param id
     *            the MCRObjectID as string
     * @param permission
     *            the permission for the ACL system
     * @return the XML tree of the condition
     */
    @SuppressWarnings("unchecked")
    public final org.jdom.Element getRuleFromFile(String id, String permission) {
        // read data
        MCRObjectID mcrid = new MCRObjectID(id);
        String fn = getDirectoryPath(mcrid.getTypeId()) + File.separator + id + ".xml";
        try {
            File fi = new File(fn);
            if (fi.isFile() && fi.canRead()) {
                Document wfDoc=MCRXMLHelper.parseURI(fn, false);
                XPath path = new JDOMXPath("/*/service/servacls/servacl[@permission='"+permission+"']/condition");
                List results = path.selectNodes(wfDoc);
                if (results.size()>0){
                    return (Element)((Element)results.get(0)).detach();
                }
            } else {
                logger.error("Can't read file " + fn);
            }
        } catch (Exception ex) {
            logger.error("Can't read file " + fn);
        }
        return new org.jdom.Element("condition").setAttribute("format", "xml");
    }
}
