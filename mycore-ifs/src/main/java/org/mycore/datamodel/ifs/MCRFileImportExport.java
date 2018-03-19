/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.datamodel.ifs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.content.streams.MCRDevNull;
import org.mycore.common.content.streams.MCRMD5InputStream;

/**
 * Imports or exports complete directory trees with all contained files and subdirectories between the local host's filesystem and the internal MCRDirectory
 * structures.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRFileImportExport {

    private static Logger LOGGER = LogManager.getLogger(MCRFileImportExport.class);

    /**
     * Imports the contents of a local file or directory into a newly created MCRDirectory that is owned by the given owner ID. The new MCRDirectory will have
     * the same name as the owner ID. If the local object is a file, a MCRFile with the same name will be created or updated in that MCRDirectory. If the local
     * object is a directory, all contained subdirectories and files will be imported into the newly created MCRDirectory. That means that after finishing this
     * method, the complete directory structure will have been imported and mapped from the local filesystem's structure. The method checks the contents of each
     * local file to be imported. If the file's content has not changed for existing files, the internal MCRFile will not be updated. If there is any exception
     * while importing the local contents, the system will try to undo this operation by completely deleting all content that was imported so far.
     * 
     * @param local
     *            the local file or directory to be imported
     * @param ownerID
     *            the ID of the logical owner of the content that will be stored
     * @return a new MCRDirectory that will contain all imported files and directories as instances of MCRFilesystemNode children.
     */
    public static MCRDirectory importFiles(File local, String ownerID) {
        if (Objects.requireNonNull(ownerID, "owner ID" + " is null").trim().isEmpty()) {
            throw new MCRUsageException("owner ID" + " is an empty String");
        }

        // Create new parent directory
        MCRDirectory dir = new MCRDirectory(ownerID);

        try // Try to import local content into this new directory
        {
            importFiles(local, dir);
        } catch (Exception ex) // If anything goes wrong
        {
            try {
                dir.delete();
            } // Try to delete all content stored so far
            catch (Exception ignored) {
                LOGGER.error("Exception while deleting MCRDirectory for derivate: {}", ownerID, ignored);
            }
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            throw new MCRException(ex);
        }

        return dir;
    }

    /**
     * Imports the contents of a local file or directory into an existing MCRDirectory that is owned by the given owner ID. The new MCRDirectory will have the
     * same name as the owner ID. If the local object is a file, a MCRFile with the same name will be created or updated in that MCRDirectory. If the local
     * object is a directory, all contained subdirectories and files will be imported into the newly created MCRDirectory. That means that after finishing this
     * method, the complete directory structure will have been imported and mapped from the local filesystem's structure. The method checks the contents of each
     * local file to be imported. If the file's content has not changed for existing files, the internal MCRFile will not be updated. If there is any exception
     * while importing the local contents, the system will stop with the last state and break the work.
     * 
     * @param local
     *            the local file or directory to be imported
     * @param ownerID
     *            the ID of the logical owner of the content that will be stored
     * @return a new MCRDirectory that will contain all imported files and directories as instances of MCRFilesystemNode children.
     */
    public static MCRDirectory addFiles(File local, String ownerID) throws IOException {
        if (Objects.requireNonNull(ownerID, "owner ID" + " is null").trim().isEmpty()) {
            throw new MCRUsageException("owner ID" + " is an empty String");
        }

        // Get the existing parent directory
        MCRDirectory dir = MCRDirectory.getRootDirectory(ownerID);
        importFiles(local, dir);
        return dir;
    }

    /**
     * Imports the contents of a local file or directory into the MyCoRe Internal Filesystem. If the local object is a file, a MCRFile with the same name will
     * be created or updated in the given MCRDirectory. If the local object is a directory, all contained subdirectories and files will be imported into the
     * given MCRDirectory. That means that after finishing this method, the complete directory structure will have been imported and mapped from the local
     * filesystem's structure. The method checks the contents of each local file to be imported. If the file's content has not changed for existing files, the
     * internal MCRFile will not be updated. If an internal directory is updated from a local directory, new files will be added, existing files will be updated
     * if necessary, but files that already exist in the given MCRDirectory but not in the local filesystem will be kept and will not be deleted.
     * 
     * @param local
     *            the local file or directory
     * @param dir
     *            an existing MCRDirectory where to store the imported contents of the local filesystem.
     */
    public static void importFiles(File local, MCRDirectory dir) throws IOException {
        Objects.requireNonNull(local, "local file is null");

        String path = local.getPath();
        String name = local.getName();

        if (!local.exists()) {
            throw new MCRUsageException("Not found: " + path);
        }
        if (!local.canRead()) {
            throw new MCRUsageException("Not readable: " + path);
        }

        // Import a local file
        if (local.isFile()) {
            MCRFilesystemNode existing = dir.getChild(name);
            MCRFile file = null;

            // If internal directory with same name exists
            if (existing instanceof MCRDirectory) {
                existing.delete(); // delete it
                existing = null;
            }

            if (existing == null) { // Create new, empty MCRFile
                file = new MCRFile(name, dir, false);
            } else {
                file = (MCRFile) existing; // Update existing MCRFile

                // Determine MD5 checksum of local file
                try(FileInputStream fin = new FileInputStream(local); MCRMD5InputStream cis = new MCRMD5InputStream(fin)) {
                    IOUtils.copy(cis, new MCRDevNull());
                    String local_md5 = cis.getMD5String();

                    // If file content of local file has not changed, do not load it again
                    if (file.getMD5().equals(local_md5)) {
                        return;
                    }
                }
            }

            // Store file content
            file.setContentFrom(local);
        } else {
            File[] files = local.listFiles();

            // For each local child node
            for (File file : files) {
                local = file;
                name = local.getName();

                MCRDirectory internalDir = dir;

                if (local.isDirectory()) {
                    MCRFilesystemNode existing = dir.getChild(name);

                    if (existing instanceof MCRFile) { // If there is an

                        // existing MCRFile with
                        // same name
                        existing.delete(); // delete that existing MCRFile
                        existing = null;
                    }

                    if (existing == null) { // Create new directory
                        internalDir = new MCRDirectory(name, dir, false);
                    } else {
                        internalDir = (MCRDirectory) existing;
                    }
                }

                importFiles(local, internalDir); // Recursively import
            }
        }
    }

    /**
     * Exports all contents of the given MCRDirectory to the local filesystem, including all subdirectories and stored files. If the local object is a file, the
     * parent directory of that file will be used for exporting.
     * 
     * @param local
     *            the local directory where to export the contents to
     * @param dir
     *            the directory thats contents should be exported
     */
    public static void exportFiles(MCRDirectory dir, File local) throws MCRException {
        Objects.requireNonNull(dir, "internal directory is null");
        Objects.requireNonNull(local, "local file is null");

        String path = local.getPath();
        if (!local.canWrite()) {
            throw new MCRUsageException("Not writeable: " + path);
        }

        // If local is file, use its parent instead
        if (local.isFile()) {
            local = local.getParentFile();
        }

        MCRFilesystemNode[] children = dir.getChildren();

        for (MCRFilesystemNode element : children) {
            if (element instanceof MCRFile) {
                MCRFile internalFile = (MCRFile) element;
                String name = internalFile.getName();

                File localFile = new File(local, name);

                try {
                    internalFile.getContentTo(localFile);
                } catch (Exception ex) {
                    throw new MCRException("Can't get file content.", ex);
                }
            } else {
                MCRDirectory internalDir = (MCRDirectory) element;
                String name = internalDir.getName();

                File localDir = new File(local, name);

                if (!localDir.exists()) {
                    localDir.mkdir();
                }

                exportFiles(internalDir, localDir);
            }
        }
    }
}
