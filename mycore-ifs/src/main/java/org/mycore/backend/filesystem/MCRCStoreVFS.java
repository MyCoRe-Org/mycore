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

package org.mycore.backend.filesystem;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRVFSContent;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.datamodel.ifs.MCRContentStore;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileReader;

/**
 * This class implements the MCRContentStore interface to store the content of
 * MCRFile objects on any filesystem supported by the Apache Jakarta Commons
 * VFS. The connection URI is configured in mycore.properties:
 * 
 * <code>
 *   MCR.IFS.ContentStore.&lt;StoreID&gt;.URI   the base directory in Apache Commons VFS syntax
 *   
 *   Local filesystem:
 *     [file://]/absolute-path
 *   FTP Server:
 *     ftp://[username[:password]@]hostname[:port][/absolute-path]
 *   SFTP / SCP / SSH Server:
 *     sftp://[username[:password]@]hostname[:port][/absolute-path]
 *   CIFS / Samba / Windows share:
 *     smb://[username[:password]@]hostname[:port][/absolute-path]
 *     
 *    MCR.IFS.ContentStore.&lt;StoreID&gt;.StrictHostKeyChecking=yes|no 
 *      for SFTP: controls the use of known_hosts file, default is "no"
 * </code>
 * 
 * @author Werner Greßhoff
 * @author Frank Lützenkirchen
 * 
 * @version $Revision$ $Date$
 */
public class MCRCStoreVFS extends MCRContentStore {

    private FileSystemManager fsManager;

    private FileSystemOptions opts;

    private String uri;

    private static final Logger LOGGER = LogManager.getLogger(MCRCStoreVFS.class);

    @Override
    protected String doStoreContent(MCRFileReader file, MCRContentInputStream source) throws Exception {
        StringBuilder storageId = new StringBuilder();

        String[] slots = buildSlotPath();
        // Recursively create directory name
        for (String slot : slots) {
            storageId.append(slot).append("/");
        }

        String fileId = buildNextID(file);
        storageId.append(fileId);

        FileObject targetObject = fsManager.resolveFile(getBase(), storageId.toString());
        FileContent targetContent = targetObject.getContent();
        try (OutputStream out = targetContent.getOutputStream()) {
            IOUtils.copy(source, out);
        } finally {
            targetContent.close();
        }
        return storageId.toString();
    }

    @Override
    protected void doDeleteContent(String storageId) throws Exception {
        FileObject targetObject = fsManager.resolveFile(getBase(), storageId);
        LOGGER.debug("Delete fired on: " + targetObject);
        LOGGER.debug("targetObject.class = " + targetObject.getClass().getName());
        if (targetObject.delete()) {
            LOGGER.debug("Delete of " + targetObject + " was successful.");
        } else {
            LOGGER.warn("Delete of " + targetObject + " was NOT successful (w/o errors given).");
        }
    }

    protected MCRContent doRetrieveMCRContent(MCRFileReader file) throws IOException {
        FileObject targetObject = fsManager.resolveFile(getBase(), file.getStorageID());
        MCRVFSContent content = new MCRVFSContent(targetObject);
        if (file instanceof MCRFile) {
            content.setName(((MCRFile) file).getName());
        }
        return content;
    }

    @Override
    public File getLocalFile(String storageId) throws IOException {
        FileObject fileObject = fsManager.resolveFile(getBase(), storageId);
        return fileObject.getFileSystem().replicateFile(fileObject, Selectors.SELECT_SELF);
    }

    protected FileObject getBase() throws FileSystemException {
        return fsManager.resolveFile(uri, opts);
    }

    @Override
    public File getBaseDir() throws IOException {
        URL baseURL = getBase().getURL();
        if ("file".equals(baseURL.getProtocol())) {
            try {
                File baseDir = new File(baseURL.toURI());
                return baseDir;
            } catch (URISyntaxException e) {
                throw new IOException("baseURI for content store " + getID() + " is invalid: " + baseURL, e);
            }
        } else {
            LOGGER.warn("Base URI is not a file URI: " + baseURL.toString());
            return null;
        }
    }

    @Override
    public void init(String storeId) {
        super.init(storeId);

        uri = MCRConfiguration.instance().getString(storeConfigPrefix + "URI");
        String check = MCRConfiguration.instance().getString(storeConfigPrefix + "StrictHostKeyChecking", "no");

        try {
            fsManager = VFS.getManager();

            opts = new FileSystemOptions();
            SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(opts, check);

            FileObject baseDir = getBase();

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

    @Override
    protected boolean exists(MCRFileReader file) {
        try {
            FileObject targetObject = fsManager.resolveFile(getBase(), file.getStorageID());
            return targetObject.exists();
        } catch (FileSystemException e) {
            LOGGER.error(e);
            return false;
        }
    }
}
