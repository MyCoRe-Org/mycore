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

package org.mycore.frontend.fileupload;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * handles uploads via the UploadApplet and store files directly into the IFS.
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * 
 * @see MCRUploadHandler
 */
public class MCRUploadHandlerIFS extends MCRUploadHandler {

    MCRDerivate derivate;

    MCRDirectory rootDir;

    boolean newDerivate;

    private static final String ID_TYPE = "derivate";

    private static final String PROJECT = MCRConfiguration.instance().getString("MCR.default_project_id", "MCR");

    private static final Logger LOGGER = Logger.getLogger(MCRUploadHandlerIFS.class);

    private static final MCRConfiguration CONFIG = MCRConfiguration.instance();

    public MCRUploadHandlerIFS(String docId, String derId, String url) {
        super();
        this.url = url;

        if (derId == null) {
            // create new derivate
            createNewDerivate(docId, getFreeDerivateID());
        } else {
            if (MCRDerivate.existInDatastore(derId)) {
                newDerivate = false;
                derivate = new MCRDerivate();
                derivate.receiveFromDatastore(derId);
            } else {
                // create new derivate with given ID
                createNewDerivate(docId, new MCRObjectID(derId));
            }
        }
    }

    /**
     * Start Upload for MyCoRe
     */
    public void startUpload(int numFiles) throws Exception {
        if (newDerivate) {
            LOGGER.debug("Create new derivate with id: " + derivate.getId());
            derivate.createInDatastore();
        }
        rootDir = getRootDir(derivate.getId().toString());
    }

    /**
     * Message from UploadApplet If you want all files transfered omit this
     * method
     * 
     * @param path
     *            file name
     * @param checksum
     *            md5 checksum of of file
     * 
     * @return true transfer file false don't send file
     * 
     */
    public boolean acceptFile(String path, String checksum) throws Exception {
        MCRFilesystemNode child = rootDir.getChildByPath(path);
        if (!(child instanceof MCRFile)) {
            return true;
        }
        MCRFile file = (MCRFile) child;
        return !checksum.equals(file.getMD5());
    }

    /**
     * Store file in data store
     * 
     * @param path
     *            file name
     * @param in
     *            InputStream belongs to socket, do not close!
     * 
     */
    public synchronized void receiveFile(String path, InputStream in) throws Exception {
        LOGGER.debug("adding file: " + path);
        MCRFile file = getNewFile(path);
        file.setContentFrom(in);
    }

    /**
     * Finish upload, store derivate
     * 
     */
    public void finishUpload() throws Exception {
        String mainfile = getMainFilePath(rootDir);
        if (newDerivate) {
            MCRDerivate derivate = new MCRDerivate();
            derivate.receiveFromDatastore(this.derivate.getId());
            derivate.getDerivate().getInternals().setMainDoc(mainfile);
            derivate.updateInDatastore();
            setDefaultPermissions(derivate.getId());
        } else {
            String mf = derivate.getDerivate().getInternals().getMainDoc();
            if (mf.trim().length() == 0) {
                derivate.getDerivate().getInternals().setMainDoc(mainfile);
                derivate.updateXMLInDatastore();
            }
        }
    }

    private static MCRObjectID getFreeDerivateID() {
        MCRObjectID derivateID = new MCRObjectID();
        derivateID.setNextFreeId(PROJECT + '_' + ID_TYPE);
        return derivateID;
    }

    private void createNewDerivate(String docId, MCRObjectID newDerID) {
        newDerivate = true;
        derivate = new MCRDerivate();
        derivate.setId(newDerID);
        String schema = CONFIG.getString("MCR.Metadata.Config.derivate", "datamodel-derivate.xml").replaceAll(".xml", ".xsd");
        derivate.setSchema(schema);
        derivate.setLabel("data object from " + docId);
        // set link to Object
        MCRMetaLinkID linkId = new MCRMetaLinkID();
        linkId.setSubTag("linkmetas");
        linkId.setReference(docId, null, null);
        MCRMetaIFS ifs = new MCRMetaIFS();
        ifs.setSubTag("internal");
        ifs.setSourcePath(null);
        derivate.getDerivate().setInternals(ifs);
        derivate.getDerivate().setLinkMeta(linkId);
    }

    private MCRFile getNewFile(String path) {
        if (path.indexOf("/") == -1) {
            return new MCRFile(path, rootDir);
        }
        StringTokenizer tok = new StringTokenizer(path, "/");
        MCRDirectory parent = rootDir;
        while (tok.hasMoreTokens()) {
            String child = tok.nextToken();
            if (parent.hasChild(child)) {
                MCRFilesystemNode childNode = parent.getChild(child);
                if ((childNode instanceof MCRFile) && !tok.hasMoreTokens()) {
                    return (MCRFile) childNode;
                } else if (childNode instanceof MCRDirectory) {
                    parent = (MCRDirectory) childNode;
                } else {
                    // obviously a file should not contain any other files
                    return null;
                }
            } else {
                if (tok.hasMoreTokens()) {
                    parent = new MCRDirectory(child, parent);
                } else {
                    // NOTE: How should we handle empty directories?
                    return new MCRFile(child, parent);
                }
            }
        }
        LOGGER.error("Please investigate getNewFile() method in IFS upload handler. Server shouldn't get to this point!");
        return null;
    }

    private static MCRDirectory getRootDir(String derID) {
        MCRFilesystemNode root = MCRFilesystemNode.getRootNode(derID);
        if (!(root instanceof MCRDirectory)) {
            root = new MCRDirectory(derID, derID);
        }
        MCRDirectory rootDir = (MCRDirectory) root;
        return rootDir;
    }

    private static String getMainFilePath(MCRDirectory root) {
        MCRDirectory parent = root;
        while (parent.hasChildren()) {
            MCRFilesystemNode[] children = parent.getChildren();
            if (children[0] instanceof MCRDirectory) {
                parent = (MCRDirectory) children[0];
            }
            for (int i = 0; i < children.length; i++) {
                if (children[i] instanceof MCRFile)
                    return children[i].getAbsolutePath().substring(1);
            }
        }
        return "";
    }

    private static void setDefaultPermissions(MCRObjectID derID) {
        MCRAccessInterface AI = MCRAccessManager.getAccessImpl();
        List configuredPermissions = AI.getAccessPermissionsFromConfiguration();
        for (Iterator it = configuredPermissions.iterator(); it.hasNext();) {
            String permission = (String) it.next();
            MCRAccessManager.addRule(derID, permission, MCRAccessManager.getTrueRule(), "default derivate rule");
        }
    }

}
