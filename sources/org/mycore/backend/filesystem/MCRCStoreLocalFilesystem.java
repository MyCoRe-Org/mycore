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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.datamodel.ifs.MCRContentStore;
import org.mycore.datamodel.ifs.MCRFileReader;

/**
 * This class implements the MCRContentStore interface to store the content of
 * MCRFile objects on a local filesystem. The filesystem parameters are
 * configured in mycore.properties:
 * 
 * <code>
 *   MCR.IFS.ContentStore.<StoreID>.BaseDirectory  Directory on filesystem where content will be stored
 * </code>
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRCStoreLocalFilesystem extends MCRContentStore {
    /** Base directory on local filesystem where content is stored */
    protected File baseDir;

    public void init(String storeID) {
        super.init(storeID);

        MCRConfiguration config = MCRConfiguration.instance();
        baseDir = new File(config.getString(prefix + "BaseDirectory"));

        if (!baseDir.exists()) {
            baseDir.mkdirs();

            if (!baseDir.exists()) {
                String msg = "Could not create content store base directory: " + baseDir.getPath();
                throw new MCRConfigurationException(msg);
            }
        } else if (!baseDir.isDirectory()) {
            String msg = "Content store base must be a directory, but is not: " + baseDir.getPath();
            throw new MCRConfigurationException(msg);
        } else if (!baseDir.canRead()) {
            String msg = "Content store base directory must be readable: " + baseDir.getPath();
            throw new MCRConfigurationException(msg);
        }
    }

    private void ensureCanWrite() {
        if (!baseDir.canWrite()) {
            String msg = "Content store base directory must be writeable: " + baseDir.getPath();
            throw new MCRConfigurationException(msg);
        }
    }

    protected String doStoreContent(MCRFileReader file, MCRContentInputStream source) throws Exception {
        ensureCanWrite();

        StringBuffer storageID = new StringBuffer();
        String[] slots = buildSlotPath();

        for (int i = 0; i < slots.length; i++)
            storageID.append(slots[i]).append(File.separator);

        File dir = new File(baseDir, storageID.toString());

        synchronized (this) {
            if (!dir.exists()) {
                dir.mkdirs();

                if (!dir.exists()) {
                    String msg = "Could not create content store slot directory: " + dir.getPath();
                    throw new MCRPersistenceException(msg);
                }
            }
        }

        String fileID = buildNextID(file);
        storageID.append(fileID);

        File local = new File(dir, fileID);
        OutputStream out = new BufferedOutputStream(new FileOutputStream(local));
        MCRUtils.copyStream(source, out);
        out.close();

        return storageID.toString();
    }

    protected void doDeleteContent(String storageID) throws Exception {
        ensureCanWrite();

        File local = new File(baseDir, storageID);
        local.delete();

        // Recursively remove all directories that have been created, if empty:
        StringTokenizer st = new StringTokenizer(storageID, File.separator);
        int numDirs = st.countTokens() - 1;
        String[] dirs = new String[numDirs];

        for (int i = 0; i < numDirs; i++) {
            dirs[i] = st.nextToken();

            if (i > 0) {
                dirs[i] = dirs[i - 1] + File.separator + dirs[i];
            }
        }

        for (int i = numDirs; i > 0; i--) {
            File dir = new File(baseDir, dirs[i - 1]);
            if (dir.isDirectory()) {
                // dir is directory and exists
                // ensures that listFiles below does not throw NPE
                if (dir.listFiles().length > 0) {
                    break;
                }
                dir.delete();
            }
        }
    }

    protected void doRetrieveContent(MCRFileReader file, OutputStream target) throws Exception {
        MCRUtils.copyStream(doRetrieveContent(file), target);
    }

    protected InputStream doRetrieveContent(MCRFileReader file) throws Exception {
        File local = new File(baseDir, file.getStorageID());

        return new BufferedInputStream(new FileInputStream(local));
    }
}
