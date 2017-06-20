/*
 * $Id$
 * $Revision: 5697 $ $Date: Jun 27, 2014 $
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

package org.mycore.datamodel.niofs.ifs1;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.MCRException;
import org.mycore.datamodel.ifs.MCRContentStoreFactory;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFileMetadataManager;
import org.mycore.datamodel.niofs.MCRAbstractFileSystem;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRIFSFileSystem extends MCRAbstractFileSystem {

    private MCRFileSystemProvider provider;

    MCRIFSFileSystem(MCRFileSystemProvider provider) {
        super();
        this.provider = provider;
    }

    /* (non-Javadoc)
     * @see java.nio.file.FileSystem#provider()
     */
    @Override
    public MCRFileSystemProvider provider() {
        return provider;
    }

    /* (non-Javadoc)
     * @see java.nio.file.FileSystem#getRootDirectories()
     */
    @Override
    public Iterable<Path> getRootDirectories() {
        return StreamSupport
            .stream(MCRFileMetadataManager.instance().getOwnerIDs().spliterator(), false)
            .map(owner -> (Path) getPath(owner, "", this))::iterator;
    }

    /* (non-Javadoc)
     * @see java.nio.file.FileSystem#getFileStores()
     */
    @Override
    public Iterable<FileStore> getFileStores() {
        return MCRContentStoreFactory
            .getAvailableStores()
            .keySet()
            .stream()
            .map(MCRIFSFileSystem::getFileStore)::iterator;
    }

    private static FileStore getFileStore(String id) {
        try {
            return MCRFileStore.getInstance(id);
        } catch (IOException e) {
            throw new MCRException(e);
        }
    }

    @Override
    public void createRoot(String owner) throws FileSystemException {
        MCRDirectory rootDirectory = MCRDirectory.getRootDirectory(owner);
        MCRPath rootPath = getPath(owner, "", this);
        if (rootDirectory != null) {
            throw new FileAlreadyExistsException(rootPath.toString());
        }
        try {
            rootDirectory = new MCRDirectory(owner);
        } catch (RuntimeException e) {
            LogManager.getLogger(getClass()).warn("Catched run time exception while creating new root directory.", e);
            throw new FileSystemException(rootPath.toString(), null, e.getMessage());
        }
        LogManager.getLogger(getClass()).info("Created root directory: " + rootPath);
    }

    @Override
    public void removeRoot(String owner) throws FileSystemException {
        MCRPath rootPath = getPath(owner, "", this);
        MCRDirectory rootDirectory = MCRDirectory.getRootDirectory(owner);
        if (rootDirectory == null) {
            throw new NoSuchFileException(rootPath.toString());
        }
        if (rootDirectory.isDeleted()) {
            return;
        }
        if (rootDirectory.hasChildren()) {
            throw new DirectoryNotEmptyException(rootPath.toString());
        }
        try {
            rootDirectory.delete();
        } catch (RuntimeException e) {
            LogManager.getLogger(getClass()).warn("Catched run time exception while removing root directory.", e);
            throw new FileSystemException(rootPath.toString(), null, e.getMessage());
        }
        LogManager.getLogger(getClass()).info("Removed root directory: " + rootPath);
    }

}
