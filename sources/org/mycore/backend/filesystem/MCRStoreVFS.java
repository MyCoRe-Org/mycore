/**
 * $RCSfile$
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

package org.mycore.backend.filesystem;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.datamodel.ifs.MCRContentStore;
import org.mycore.datamodel.ifs.MCRFileReader;

/**
 * This class implements the MCRContentStore interface to store the content of
 * MCRFile objects on a filesystem supported by Apache Jakarta Commons VFS. The
 * connection parameters are configured in mycore.properties:
 * 
 * <code>
 *   MCR.IFS.ContentStore.<StoreID>.Method         The connection method to connect to the remote server
 *   MCR.IFS.ContentStore.<StoreID>.Hostname       Hostname of remote server
 *   MCR.IFS.ContentStore.<StoreID>.UserID         User ID for FTP connections
 *   MCR.IFS.ContentStore.<StoreID>.Password       Password for this user
 *   MCR.IFS.ContentStore.<StoreID>.BaseDirectory  Directory on server where content will be stored
 *   MCR.IFS.ContentStore.<StoreID>.Domain		   Domain of the server where content will be stored (in case of smb)
 *   MCR.IFS.ContentStore.<StoreID>.buildSlots     If true, a directory structure is build, default is false
 *   MCR.IFS.ContentStore.<StoreID>.Debug  		   If true, debug messages are written to stdout, default is false
 * </code>
 * 
 * @author Werner Greßhoff
 * @author Frank Lützenkirchen
 * 
 * @version $Revision$ $Date$
 */
public class MCRStoreVFS extends MCRContentStore {

    private String method;

    private String host;

    private String user;

    private String password;

    private String baseDirectory;

    private String domain;

    private boolean buildSlots;

    private boolean debug;

    private FileSystemManager fsManager;

    private FileObject baseDir;

    protected String doStoreContent(MCRFileReader file,
            MCRContentInputStream source) throws Exception {
        StringBuffer storageId = new StringBuffer();
        if (buildSlots) {
            String[] slots = buildSlotPath();

            // Recursively create directory name
            for (int i = 0; i < slots.length; i++) {
                storageId.append(slots[i]).append("/");
            }
        }

        String fileId = buildNextID(file);
        storageId.append(fileId);
        FileObject targetObject = fsManager.resolveFile(baseDir, storageId
                .toString());
        FileContent targetContent = targetObject.getContent();
        OutputStream out = targetContent.getOutputStream();
        MCRUtils.copyStream(source, out);
        out.close();

        return storageId.toString();
    }

    protected void doDeleteContent(String storageId) throws Exception {
        FileObject targetObject = fsManager.resolveFile(baseDir, storageId);
        FileObject parent = targetObject.getParent();
        targetObject.delete();
        while (!parent.getName().getPathDecoded().equals(
                baseDir.getName().getPathDecoded())) {
            targetObject = parent;
            parent = targetObject.getParent();
            targetObject.delete();
        }
    }

    protected void doRetrieveContent(MCRFileReader file, OutputStream target)
            throws Exception {
        FileObject targetObject = fsManager.resolveFile(baseDir, file
                .getStorageID());
        FileContent targetContent = targetObject.getContent();
        InputStream in = targetContent.getInputStream();
        MCRUtils.copyStream(in, target);
    }

    protected InputStream doRetrieveContent(MCRFileReader file)
            throws Exception {
        FileObject targetObject = fsManager.resolveFile(baseDir, file
                .getStorageID());
        FileContent targetContent = targetObject.getContent();
        return targetContent.getInputStream();
    }

    public void init(String storeId) {
        super.init(storeId);
        MCRConfiguration config = MCRConfiguration.instance();

        method = config.getString(prefix + "Method");
        host = config.getString(prefix + "Hostname");
        user = config.getString(prefix + "UserID", "");
        password = config.getString(prefix + "Password", "");
        baseDirectory = config.getString(prefix + "BaseDirectory");
        domain = config.getString(prefix + "Domain", "");
        buildSlots = config.getBoolean(prefix + "buildSlots", false);
        debug = config.getBoolean(prefix + "Debug", false);

        try {
            fsManager = VFS.getManager();
            StringBuffer baseDirName = new StringBuffer(method).append("://");
            if (method.equals("smb") && domain.length() > 0) {
                baseDirName.append(domain).append(";");
            }
            baseDirName.append(user);
            if (user.length() > 0) {
                baseDirectory = "@" + baseDirectory;
                if (password.length() > 0) {
                    baseDirName.append(":").append(password);
                }
            }
            baseDirName.append(baseDirectory);
            baseDir = fsManager.resolveFile(baseDirName.toString());
            // Create a folder, if it does not exist or throw an
            // exception, if baseDir is not a folder
            baseDir.createFolder();
            if (!baseDir.isWriteable()) {
                String msg = "Content store base directory must be writable: "
                        + baseDirectory;
                throw new MCRConfigurationException(msg);
            }
        } catch (FileSystemException ignored) {
            ignored.printStackTrace();
            throw new MCRConfigurationException(ignored.getCode());
        }
    }
}