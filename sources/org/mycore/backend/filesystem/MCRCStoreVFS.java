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

package org.mycore.backend.filesystem;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.datamodel.ifs.MCRContentStore;
import org.mycore.datamodel.ifs.MCRFileReader;

/**
 * This class implements the MCRContentStore interface to store the content of
 * MCRFile objects on any filesystem supported by the Apache Jakarta Commons
 * VFS. The connection URI is configured in mycore.properties:
 * 
 * <code>
 *   MCR.IFS.ContentStore.<StoreID>.URI   the base directory in Apache Commons VFS syntax
 *   
 *   Local filesystem:
 *     [file://]/absolute-path
 *   FTP Server:
 *     ftp://[username[:password]@]hostname[:port][/absolute-path]
 *   SFTP / SCP / SSH Server:
 *     sftp://[username[:password]@]hostname[:port][/absolute-path]
 *   CIFS / Samba / Windows share:
 *     smb://[username[:password]@]hostname[:port][/absolute-path]
 * </code>
 * 
 * @author Werner Greßhoff
 * @author Frank Lützenkirchen
 * 
 * @version $Revision$ $Date$
 */
public class MCRCStoreVFS extends MCRContentStore {

    private FileSystemManager fsManager;

    private FileObject baseDir;

    protected String doStoreContent(MCRFileReader file, MCRContentInputStream source) throws Exception {
        StringBuffer storageId = new StringBuffer();

        String[] slots = buildSlotPath();
        // Recursively create directory name
        for (int i = 0; i < slots.length; i++) {
            storageId.append(slots[i]).append("/");
        }

        String fileId = buildNextID(file);
        storageId.append(fileId);

        FileObject targetObject = fsManager.resolveFile(baseDir, storageId.toString());
        FileContent targetContent = targetObject.getContent();
        OutputStream out = new BufferedOutputStream(targetContent.getOutputStream());
        MCRUtils.copyStream(source, out);
        out.close();

        return storageId.toString();
    }

    protected void doDeleteContent(String storageId) throws Exception {
        FileObject targetObject = fsManager.resolveFile(baseDir, storageId);
        FileObject parent = targetObject.getParent();
        targetObject.delete();

        // Delete parent slot directories, if empty
        String baseDirPath = baseDir.getName().getPathDecoded();
        while (!parent.getName().getPathDecoded().equals(baseDirPath)) {
            if (parent.getChildren().length > 0)
                break;

            targetObject = parent;
            parent = targetObject.getParent();
            if (!targetObject.delete())
                break;
        }
    }

    protected void doRetrieveContent(MCRFileReader file, OutputStream target) throws Exception {
        FileObject targetObject = fsManager.resolveFile(baseDir, file.getStorageID());
        FileContent targetContent = targetObject.getContent();
        InputStream in = new BufferedInputStream(targetContent.getInputStream());
        MCRUtils.copyStream(in, target);
    }

    protected InputStream doRetrieveContent(MCRFileReader file) throws Exception {
        FileObject targetObject = fsManager.resolveFile(baseDir, file.getStorageID());
        FileContent targetContent = targetObject.getContent();
        return new BufferedInputStream(targetContent.getInputStream());
    }

    public void init(String storeId) {
        super.init(storeId);

        String uri = MCRConfiguration.instance().getString(prefix + "URI");

        try {
            fsManager = VFS.getManager();
            baseDir = fsManager.resolveFile(uri);

            // Create a folder, if it does not exist or throw an
            // exception, if baseDir is not a folder
            baseDir.createFolder();

            if (!baseDir.isWriteable()) {
                String msg = "Content store base directory is not writeable: " + uri;
                throw new MCRConfigurationException(msg);
            }
        } catch (FileSystemException ex) {
            throw new MCRException(ex.getCode(), ex);
        }
    }
}
