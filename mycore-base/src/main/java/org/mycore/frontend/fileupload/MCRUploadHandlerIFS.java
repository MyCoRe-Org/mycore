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
import java.util.Collection;
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
import org.mycore.datamodel.metadata.MCRMetadataManager;
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

    protected String derID, docID;

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
        docID = docId;
        if (derId == null) {
            return;
        }
        MCRObjectID derOID = MCRObjectID.getInstance(derId);
        derID = derOID.toString();
        if (MCRMetadataManager.exists(derOID)) {
            LOGGER.debug("Derivate allready exists: " + derId);
            newDerivate = false;
            derivate = MCRMetadataManager.retrieveMCRDerivate(derOID);
        }
    }

    /**
     * Start Upload for MyCoRe
     */
    @Override
    public void startUpload(int numFiles) throws Exception {
        if (derivate == null) {
            if (derID == null) {
                // create new derivate
                LOGGER.debug("derId=null create derivate with next free ID");
                createNewDerivate(docID, getFreeDerivateID());
            } else {
                // create new derivate with given ID
                LOGGER.debug("derId='" + derID + "' create derivate with that ID");
                createNewDerivate(docID, MCRObjectID.getInstance(derID));
            }
            LOGGER.debug("Create new derivate with id: " + derivate.getId());
            MCRMetadataManager.create(derivate);
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
    @Override
    public boolean acceptFile(String path, String checksum, long length) throws Exception {
        LOGGER.debug("incoming acceptFile request: " + path + " " + checksum + " " + length + " bytes");
        MCRFilesystemNode child = rootDir.getChildByPath(path);
        if (!(child instanceof MCRFile)) {
            return true;
        }
        MCRFile file = (MCRFile) child;
        return file.getSize() != length || !(checksum.equals(file.getMD5()) && file.isValid());
    }

    @Override
    public synchronized long receiveFile(String path, InputStream in, long length, String checksum) throws Exception {
        LOGGER.debug("incoming receiveFile request: " + path + " " + checksum + " " + length + " bytes");
        try {
            LOGGER.debug("adding file: " + path);
            startTransaction();
            MCRFile file = getNewFile(path);
            commitTransaction();
            long sizeDiff = file.setContentFrom(in, false);
            startTransaction();
            file.storeContentChange(sizeDiff);
            commitTransaction();

            long myLength = file.getSize();

            LOGGER.debug("file size expected=" + length + " setContent=" + sizeDiff + " getSize=" + myLength);
            if (myLength >= length) {
                return myLength;
            } else {
                LOGGER.debug("file size < expected size, upload seems canceled or broken, will delete incomplete file");
                startTransaction();
                file.delete();
                commitTransaction();
                return 0;
            }
        } catch (Exception e) {
            LOGGER.error("Error while uploading file: " + path, e);
            try {
                rollbackTransaction();
            } catch (Exception e2) {
                LOGGER.error("Error while rolling back transaction", e);
            }
            return 0;
        }
    }

    /**
     * Finish upload, store derivate
     * 
     */
    @Override
    public void finishUpload() throws Exception {
        // existing files
        if (!rootDir.hasChildren()) {
            MCRMetadataManager.deleteMCRDerivate(derivate.getId());
            LOGGER.warn("No file were uploaded, delete entry in database for " + derivate.getId().toString() + " and return.");
            return;
        }
        String mainfile = getMainFilePath(rootDir);
        if (newDerivate) {
            derivate.getDerivate().getInternals().setMainDoc(mainfile);
            MCRMetadataManager.updateMCRDerivateXML(derivate);
            setDefaultPermissions(derivate.getId());
        } else {
            String mf = derivate.getDerivate().getInternals().getMainDoc();
            if (mf.trim().length() == 0) {
                derivate.getDerivate().getInternals().setMainDoc(mainfile);
                MCRMetadataManager.updateMCRDerivateXML(derivate);
            }
        }
    }

    private static MCRObjectID getFreeDerivateID() {
        return MCRObjectID.getNextFreeId(PROJECT + '_' + ID_TYPE);
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
        if (!path.contains("/")) {
            return new MCRFile(path, rootDir);
        }
        StringTokenizer tok = new StringTokenizer(path, "/");
        MCRDirectory parent = rootDir;
        while (tok.hasMoreTokens()) {
            String child = tok.nextToken();
            if (parent.hasChild(child)) {
                MCRFilesystemNode childNode = parent.getChild(child);
                if (childNode instanceof MCRFile && !tok.hasMoreTokens()) {
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
            root = new MCRDirectory(derID);
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
            for (MCRFilesystemNode element : children) {
                if (element instanceof MCRFile) {
                    return element.getAbsolutePath().substring(1);
                }
            }
        }
        return "";
    }

    protected static void setDefaultPermissions(MCRObjectID derID) {
        if (CONFIG.getBoolean("MCR.Access.AddDerivateDefaultRule", true)) {
            MCRAccessInterface AI = MCRAccessManager.getAccessImpl();
            Collection<String> configuredPermissions = AI.getAccessPermissionsFromConfiguration();
            for (String permission : configuredPermissions) {
                MCRAccessManager.addRule(derID, permission, MCRAccessManager.getTrueRule(), "default derivate rule");
            }
        }
    }

}
