/*
 * 
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
import java.io.StringWriter;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

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

    protected MCRDerivate derivate;

    protected MCRDirectory rootDir;

    protected boolean newDerivate = true;

    private static final String ID_TYPE = "derivate";

    private static final String PROJECT = MCRConfiguration.instance().getString("MCR.SWF.Project.ID", "MCR");

    private static final Logger LOGGER = Logger.getLogger(MCRUploadHandlerIFS.class);

    private static final MCRConfiguration CONFIG = MCRConfiguration.instance();

    public MCRUploadHandlerIFS(String docId, String derId, String url) {
        super();
        this.url = url;
        init(docId, derId);
    }

    protected void init(String docId, String derId) {
        if (derId == null) {
            // create new derivate
            LOGGER.debug("derId=null create derivate with next free ID");
            createNewDerivate(docId, getFreeDerivateID());
        } else {
            if (MCRDerivate.existInDatastore(derId)) {
                LOGGER.debug("Derivate allready exists: " + derId);
                newDerivate = false;
                derivate = new MCRDerivate();
                derivate.receiveFromDatastore(derId);
            } else {
                // create new derivate with given ID
                LOGGER.debug("derId='" + derId + "' create derivate with that ID");
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
        LOGGER.debug("adding file: " + path);
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
        String mainfile = getMainFilePath(rootDir);
        if (newDerivate) {
            derivate.getDerivate().getInternals().setMainDoc(mainfile);
            derivate.updateXMLInDatastore();
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

    protected void createNewDerivate(String docId, MCRObjectID newDerID) {
        newDerivate = true;
        derivate = new MCRDerivate();
        derivate.setId(newDerID);
        String schema = CONFIG.getString("MCR.Metadata.Config.derivate", "datamodel-derivate.xml").replaceAll(".xml", ".xsd");
        derivate.setSchema(schema);
        derivate.setLabel("data object from " + docId);
        // set link to Object
        MCRMetaLinkID linkId = new MCRMetaLinkID();
        linkId.setSubTag("linkmeta");
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

    protected static String getMainFilePath(MCRDirectory root) {
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

    @SuppressWarnings("unchecked")
    protected static void setDefaultPermissions(MCRObjectID derID) {
        MCRAccessInterface AI = MCRAccessManager.getAccessImpl();
        List<String> configuredPermissions = AI.getAccessPermissionsFromConfiguration();
        for (String permission : configuredPermissions) {
            MCRAccessManager.addRule(derID, permission, MCRAccessManager.getTrueRule(), "default derivate rule");
        }
    }

}
