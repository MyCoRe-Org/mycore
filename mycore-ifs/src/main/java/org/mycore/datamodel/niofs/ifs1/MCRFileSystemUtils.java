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

package org.mycore.datamodel.niofs.ifs1;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * @author Thomas Scheffler
 *
 */
abstract class MCRFileSystemUtils {

    private static final Logger LOGGER = LogManager.getLogger(MCRFileSystemUtils.class);

    static MCRPath checkPathAbsolute(Path path) {
        MCRPath mcrPath = MCRPath.toMCRPath(path);
        if (!(Objects.requireNonNull(mcrPath.getFileSystem(), "'path' requires a associated filesystem.")
            .provider() instanceof MCRFileSystemProvider)) {
            throw new ProviderMismatchException("Path does not match to this provider: " + path);
        }
        if (!mcrPath.isAbsolute()) {
            throw new InvalidPathException(mcrPath.toString(), "'path' must be absolute.");
        }
        return mcrPath;
    }

    static MCRFile getMCRFile(MCRPath ifsPath, boolean create, boolean createNew) throws IOException {
        if (!ifsPath.isAbsolute()) {
            throw new IllegalArgumentException("'path' needs to be absolute.");
        }
        MCRFile file;
        MCRDirectory root = null;
        boolean rootCreated = false;
        try {
            try {
                root = MCRFileSystemProvider.getRootDirectory(ifsPath);
            } catch (NoSuchFileException e) {
                if (create || createNew) {
                    root = new MCRDirectory(ifsPath.getOwner());
                    rootCreated = true;
                } else {
                    throw e;
                }
            }
            MCRPath relativePath = root.toPath().relativize(ifsPath);
            file = getMCRFile(root, relativePath, create, createNew);
        } catch (Exception e) {
            if (rootCreated) {
                LOGGER.error("Exception while getting MCRFile {}. Removing created filesystem nodes.", ifsPath);
                try {
                    root.delete();
                } catch (Exception de) {
                    LOGGER.fatal("Error while deleting file system node: {}", root.getName(), de);
                }
            }
            throw e;
        }
        return file;
    }

    static MCRFile getMCRFile(MCRDirectory baseDir, MCRPath relativePath, boolean create, boolean createNew)
        throws IOException {
        MCRPath ifsPath = relativePath;
        if (relativePath.isAbsolute()) {
            if (baseDir.getOwnerID().equals(relativePath.getOwner())) {
                ifsPath = baseDir.toPath().relativize(relativePath);
            } else
                throw new IOException(relativePath + " is absolute does not fit to " + baseDir.toPath());
        }
        Deque<MCRFilesystemNode> created = new LinkedList<>();
        MCRFile file;
        try {
            file = (MCRFile) baseDir.getChildByPath(ifsPath.toString());
            if (file != null && createNew) {
                throw new FileAlreadyExistsException(baseDir.toPath().resolve(ifsPath).toString());
            }
            if (file == null & (create || createNew)) {
                Path normalized = ifsPath.normalize();
                MCRDirectory parent = baseDir;
                int nameCount = normalized.getNameCount();
                int directoryCount = nameCount - 1;
                int i = 0;
                while (i < directoryCount) {
                    String curName = normalized.getName(i).toString();
                    MCRDirectory curDir = (MCRDirectory) parent.getChild(curName);
                    if (curDir == null) {
                        curDir = new MCRDirectory(curName, parent);
                        created.addFirst(curDir);
                    }
                    i++;
                    parent = curDir;
                }
                String fileName = normalized.getFileName().toString();
                file = new MCRFile(fileName, parent);
                file.setContentFrom(new ByteArrayInputStream(new byte[0]), false);//false -> no event handler
                created.addFirst(file);
            }
        } catch (Exception e) {
            if (create || createNew) {
                LOGGER.error("Exception while getting MCRFile {}. Removing created filesystem nodes.", ifsPath);
                while (created.peekFirst() != null) {
                    MCRFilesystemNode node = created.pollFirst();
                    try {
                        node.delete();
                    } catch (Exception de) {
                        LOGGER.fatal("Error while deleting file system node: {}", node.getName(), de);
                    }
                }
            }
            throw e;
        }
        return file;
    }

}
