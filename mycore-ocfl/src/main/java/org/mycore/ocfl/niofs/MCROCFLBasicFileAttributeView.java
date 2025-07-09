/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.ocfl.niofs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.Map;

import org.mycore.datamodel.niofs.MCRBasicFileAttributeViewProperties;
import org.mycore.datamodel.niofs.MCRVersionedPath;

import io.ocfl.api.exception.NotFoundException;
import org.mycore.ocfl.niofs.storage.MCROCFLTransactionalStorage;

/**
 * Implementation of {@link BasicFileAttributeView} for OCFL-managed files.
 * This class provides a view of the basic file attributes for files in an OCFL repository.
 */
public class MCROCFLBasicFileAttributeView implements BasicFileAttributeView {

    private final MCRVersionedPath path;

    private final MCRBasicFileAttributeViewProperties<MCROCFLBasicFileAttributeView> properties;

    private MCROCFLFileAttributes fileAttributes;

    /**
     * Constructs a new {@code MCROCFLBasicFileAttributeView} for the specified versioned path.
     *
     * @param path the versioned path.
     */
    public MCROCFLBasicFileAttributeView(MCRVersionedPath path) {
        this.path = path;
        this.properties = new MCRBasicFileAttributeViewProperties<>(this);
    }

    /**
     * Returns the name of the attribute view, which is "ocfl".
     *
     * @return the name of the attribute view.
     */
    @Override
    public String name() {
        return "ocfl";
    }

    /**
     * Reads the basic file attributes for the specified versioned path.
     *
     * @return the {@link MCROCFLFileAttributes} for the versioned path.
     * @throws IOException if an I/O error occurs or if the file does not exist.
     */
    @Override
    public MCROCFLFileAttributes readAttributes() throws IOException {
        if (this.fileAttributes != null) {
            return this.fileAttributes;
        }
        MCROCFLFileSystemProvider fileSystemProvider = MCROCFLFileSystemProvider.get();
        MCROCFLVirtualObject virtualObject;
        try {
            virtualObject = fileSystemProvider.virtualObjectProvider().get(path);
            if (!virtualObject.exists(path)) {
                throw new NoSuchFileException(path.toString());
            }
        } catch (NotFoundException notFoundException) {
            NoSuchFileException noSuchFileException = new NoSuchFileException(path.toString());
            noSuchFileException.initCause(notFoundException);
            throw noSuchFileException;
        }
        this.fileAttributes = new MCROCFLFileAttributes(virtualObject, path);
        return this.fileAttributes;
    }

    /**
     * Sets the file times for the specified versioned path.
     * This method only allows setting times for locally modified files.
     *
     * @param lastModifiedTime the new last modified time, or {@code null} to leave unchanged.
     * @param lastAccessTime the new last access time, or {@code null} to leave unchanged.
     * @param createTime the new creation time, or {@code null} to leave unchanged.
     * @throws IOException if an I/O error occurs or if the file does not exist or is not locally modified.
     */
    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
        MCROCFLVirtualObject virtualObject = MCROCFLFileSystemProvider.get().virtualObjectProvider().get(path);
        if (!virtualObject.exists(path)) {
            throw new NoSuchFileException(path.toString());
        }
        MCROCFLTransactionalStorage localStorage = MCROCFLFileSystemProvider.get().transactionalStorage();
        boolean exists = localStorage.exists(path);
        if (!exists) {
            throw new IOException("Setting times directly to ocfl repository is not allowed!");
        }
        Path physicalPath = localStorage.toPhysicalPath(path);
        BasicFileAttributeView view = Files.getFileAttributeView(physicalPath, BasicFileAttributeView.class);
        view.setTimes(lastModifiedTime, lastAccessTime, createTime);
    }

    /**
     * Gets a map of the specified attributes for the file.
     *
     * @param attributes the attributes to retrieve.
     * @return a map of attribute names to their values.
     * @throws IOException if an I/O error occurs or if an attribute is not recognized.
     */
    public Map<String, Object> getAttributeMap(String... attributes) throws IOException {
        return this.properties.getAttributeMap(attributes);
    }

}
