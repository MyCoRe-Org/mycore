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

import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.workflow.MCRSimpleWorkflowManager;

/**
 * handles uploads via the UploadApplet and store files directly into the IFS.
 * 
 * @author Thomas Scheffler (yagee)
 * @author Jens Kupferschmidt
 * 
 * @version $Revision$ $Date$
 * 
 * @see MCRUploadHandler
 */
public class MCRUploadHandlerIFS extends MCRUploadHandler {

    MCRDerivate derivate;

    MCRDirectory rootDir;

    boolean newDerivate;

    private String docId;

    private String derId;

    /**
     * The constructor for this class. It set all data to handle with IFS upload
     * store.
     * 
     * @param docId
     *            the document ID
     * @param derId
     *            the derivate ID
     * @param url
     *            an URL string. Not used in this implementation.
     */
    public MCRUploadHandlerIFS(String docId, String derId, String url) {
        super();
        this.url = url;
        logger.debug("MCRUploadHandlerMyCoRe DocID: " + docId + " DerId: " + derId);

        try {
            new MCRObjectID(docId);
            this.docId = docId;
        } catch (Exception e) {
            logger.debug("Error while creating MCRObjectID : " + docId, e);
        }

        if (derId == null) {
            this.derId = MCRSimpleWorkflowManager.instance().getNextDrivateID(new MCRObjectID(docId)).getId();
        } else {
            try {
                new MCRObjectID(derId);
                this.derId = derId;
            } catch (Exception e) {
                logger.debug("Error while creating MCRObjectID : " + derId, e);
            }
        }

        newDerivate = true;
        if (MCRDerivate.existInDatastore(this.derId)) {
            logger.debug("Derivate allready exists: " + this.derId);
            newDerivate = false;
            derivate = new MCRDerivate();
            derivate.receiveFromDatastore(derId);
        } else {
            // create new derivate with given ID
            logger.debug("Create derivate with that ID" + derId);
            derivate = MCRSimpleWorkflowManager.instance().createDerivate(new MCRObjectID(this.docId), new MCRObjectID(this.derId));
        }
    }

    /**
     * Start Upload for MyCoRe
     */
    public void startUpload(int numFiles) throws Exception {
        if (newDerivate) {
            logger.debug("Create new derivate with id: " + derivate.getId() + "in the server.");
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
     * @param length
     *            the length of the file in bytes (file size)
     * 
     * @return true transfer file false don't send file
     * 
     */
    public boolean acceptFile(String path, String checksum, long length) throws Exception {
        MCRFilesystemNode child = rootDir.getChildByPath(path);
        if (!(child instanceof MCRFile)) {
            return true;
        }
        MCRFile file = (MCRFile) child;
        return !checksum.equals(file.getMD5());
    }

    public synchronized long receiveFile(String path, InputStream in, long length, String md5) throws Exception {
        logger.debug("adding file: " + path);
        MCRFile file = getNewFile(path);
        file.setContentFrom(in);

        long myLength = file.getSize();
        if (myLength >= length)
            return myLength;
        else {
            file.delete(); // Incomplete file transfer, user canceled upload
            return 0;
        }
    }

    /**
     * Finish upload, store derivate
     * 
     */
    public void finishUpload() throws Exception {
        // existing files
        MCRFilesystemNode root = MCRFilesystemNode.getRootNode(derId);
        if (!(root instanceof MCRDirectory) || !((MCRDirectory)root).hasChildren()) {
            derivate.deleteFromDatastore(derivate.getId().getId());
            logger.warn("No file were uploaded, delete entry in database for " + derId + " and return.");
            return;
        }
        // set main file
        if (newDerivate) {
            setDefaultPermissions(derivate.getId());
        }
        String mf = derivate.getDerivate().getInternals().getMainDoc();
        if (mf.trim().length() == 0) {
            String mainfile = getMainFilePath(rootDir);
            derivate.getDerivate().getInternals().setMainDoc(mainfile);
            derivate.updateXMLInDatastore();

        }
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
        logger.error("Please investigate getNewFile() method in IFS upload handler. Server shouldn't get to this point!");
        return null;
    }

    /**
     * This method return the MCRDirectory root element of IFS for the given
     * derivate ID.
     * 
     * @param derID
     *            the derivate ID as IFS owner
     * @return a MCRDirectory instance of the root directory
     */
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
            MCRFilesystemNode[] children = parent.getChildren(MCRDirectory.SORT_BY_NAME);
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
        List<?> configuredPermissions = AI.getAccessPermissionsFromConfiguration();
        for (Iterator <?>it = configuredPermissions.iterator(); it.hasNext();) {
            String permission = (String) it.next();
            MCRAccessManager.addRule(derID, permission, MCRAccessManager.getTrueRule(), "default derivate rule");
        }
    }

}
