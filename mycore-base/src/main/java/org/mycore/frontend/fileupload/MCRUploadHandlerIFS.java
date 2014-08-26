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
import org.mycore.common.config.MCRConfiguration;
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
 * @author Frank L\u00FCtzenkirchen
 * 
 * @version $Revision$ $Date$
 * 
 * @see MCRUploadHandler
 */
public class MCRUploadHandlerIFS extends MCRUploadHandler {

    private static final Logger LOGGER = Logger.getLogger(MCRUploadHandlerIFS.class);

    private static final MCRConfiguration CONFIG = MCRConfiguration.instance();

    private static final String ID_TYPE = "derivate";

    private static final String ID_PROJECT = MCRConfiguration.instance().getString("MCR.SWF.Project.ID", "MCR");

    protected String documentID;

    protected String derivateID;

    protected MCRDerivate derivate;

    protected MCRDirectory rootDir;

    public MCRUploadHandlerIFS(String documentID, String derivateID, String returnURL) {
        super();
        this.url = returnURL;
        this.derivateID = derivateID;
        this.documentID = documentID;
    }

    @Override
    public void startUpload(int numFiles) throws Exception {
        LOGGER.debug("upload starting, expecting " + numFiles + " files");

        MCRObjectID derivateID = getOrCreateDerivateID();

        if (MCRMetadataManager.exists(derivateID))
            this.derivate = MCRMetadataManager.retrieveMCRDerivate(derivateID);
        else
            this.derivate = createDerivate(derivateID);

        getOrCreateRootDirectory();

        LOGGER.debug("uploading into " + this.derivateID + " of " + this.documentID);
    }

    private MCRObjectID getOrCreateDerivateID() {
        if (derivateID == null) {
            MCRObjectID oid = MCRObjectID.getNextFreeId(ID_PROJECT + '_' + ID_TYPE);
            this.derivateID = oid.toString();
            return oid;
        } else
            return MCRObjectID.getInstance(derivateID);
    }

    private MCRDerivate createDerivate(MCRObjectID derivateID) {
        MCRDerivate derivate = new MCRDerivate();
        derivate.setId(derivateID);
        derivate.setLabel("data object from " + documentID);

        String schema = CONFIG.getString("MCR.Metadata.Config.derivate", "datamodel-derivate.xml").replaceAll(".xml", ".xsd");
        derivate.setSchema(schema);

        MCRMetaLinkID linkId = new MCRMetaLinkID();
        linkId.setSubTag("linkmeta");
        linkId.setReference(documentID, null, null);
        derivate.getDerivate().setLinkMeta(linkId);

        MCRMetaIFS ifs = new MCRMetaIFS();
        ifs.setSubTag("internal");
        ifs.setSourcePath(null);
        derivate.getDerivate().setInternals(ifs);

        LOGGER.debug("Creating new derivate with ID " + this.derivateID);
        MCRMetadataManager.create(derivate);

        setDefaultPermissions(derivateID);

        return derivate;
    }

    protected void setDefaultPermissions(MCRObjectID derivateID) {
        if (CONFIG.getBoolean("MCR.Access.AddDerivateDefaultRule", true)) {
            MCRAccessInterface AI = MCRAccessManager.getAccessImpl();
            Collection<String> configuredPermissions = AI.getAccessPermissionsFromConfiguration();
            for (String permission : configuredPermissions) {
                MCRAccessManager.addRule(derivateID, permission, MCRAccessManager.getTrueRule(), "default derivate rule");
            }
        }
    }

    private void getOrCreateRootDirectory() {
        this.rootDir = (MCRDirectory) (MCRFilesystemNode.getRootNode(derivateID));
        if (rootDir == null)
            this.rootDir = new MCRDirectory(derivateID);
    }

    @Override
    public boolean acceptFile(String path, String checksum, long length) throws Exception {
        LOGGER.debug("incoming acceptFile request: " + path + " " + checksum + " " + length + " bytes");
        boolean shouldAcceptFile = true;
        MCRFilesystemNode child = rootDir.getChildByPath(path);
        if (child instanceof MCRFile) {
            MCRFile file = (MCRFile) child;
            shouldAcceptFile = file.getSize() != length || !(checksum.equals(file.getMD5()) && file.isValid());
        }
        LOGGER.debug("Should the client send this file? " + shouldAcceptFile);
        return shouldAcceptFile;
    }

    @Override
    public synchronized long receiveFile(String path, InputStream in, long length, String checksum) throws Exception {
        LOGGER.debug("incoming receiveFile request: " + path + " " + checksum + " " + length + " bytes");

        try {
            startTransaction();
            MCRFile file = getFile(path);
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

    private MCRFile getFile(String path) {
        MCRDirectory parent = rootDir;
        for (StringTokenizer tokenizer = new StringTokenizer(path, "/"); tokenizer.hasMoreTokens();) {
            String child = tokenizer.nextToken();
            if (parent.hasChild(child)) {
                MCRFilesystemNode childNode = parent.getChild(child);
                if (childNode instanceof MCRDirectory) {
                    parent = (MCRDirectory) childNode;
                } else if (tokenizer.hasMoreTokens()) {
                    throw new RuntimeException("Trying to upload " + path + ", but " + childNode.getAbsolutePath()
                            + " is an already existing file");
                } else {
                    return (MCRFile) childNode;
                }
            } else {
                if (tokenizer.hasMoreTokens()) {
                    parent = new MCRDirectory(child, parent);
                } else {
                    return new MCRFile(child, parent);
                }
            }
        }

        LOGGER.error("Please investigate getNewFile() method in IFS upload handler. Server shouldn't get to this point!");
        return null;
    }

    @Override
    public synchronized void finishUpload() throws Exception {
        if (this.derivate == null)
            return;
        else if (rootDir.hasChildren())
            updateMainFile();
        else
            deleteEmptyDerivate();
    }

    private void deleteEmptyDerivate() {
        LOGGER.warn("No files were uploaded, delete entry in database for " + derivate.getId().toString() + " and return:");
        MCRMetadataManager.deleteMCRDerivate(derivate.getId());
    }

    private void updateMainFile() {
        String mainFile = derivate.getDerivate().getInternals().getMainDoc();
        if ((mainFile == null) || mainFile.trim().isEmpty()) {
            mainFile = getPathOfMainFile(rootDir);
            LOGGER.debug("Setting main file to " + mainFile);
            derivate.getDerivate().getInternals().setMainDoc(mainFile);
            MCRMetadataManager.updateMCRDerivateXML(derivate);
        }
    }

    protected static String getPathOfMainFile(MCRDirectory root) {
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
}
