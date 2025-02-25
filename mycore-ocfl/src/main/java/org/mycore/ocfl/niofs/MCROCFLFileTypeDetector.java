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
import java.nio.file.spi.FileTypeDetector;

import org.mycore.datamodel.niofs.MCRVersionedPath;

/**
 * A file type detector for OCFL-managed files.
 * <p>
 * This class extends {@link FileTypeDetector} to provide content type detection for files managed by an OCFL file
 * system. It leverages the underlying physical path of the OCFL virtual object to determine the content type.
 * </p>
 */
public class MCROCFLFileTypeDetector extends FileTypeDetector {

    /**
     * Probes the content type of a file specified by a {@link Path}.
     * <p>
     * This method checks if the path belongs to an OCFL file system and if it is not a directory.
     * It retrieves the underlying physical path of the OCFL virtual object and uses the
     * {@link Files#probeContentType(Path)} method to determine the content type.
     * </p>
     *
     * @param path the path to the file.
     * @return the content type of the file, or {@code null} if the path does not belong to an OCFL file system.
     * @throws IOException if an I/O error occurs or if the path is a directory or the file does not exist.
     */
    @Override
    public String probeContentType(Path path) throws IOException {
        if (!(path.getFileSystem() instanceof MCROCFLFileSystem)) {
            return null;
        }
        if (Files.isDirectory(path)) {
            throw new NoSuchFileException(path.toString());
        }
        MCRVersionedPath versionedPath = MCRVersionedPath.toVersionedPath(path);
        MCROCFLFileSystemProvider.get().virtualObjectProvider();
        MCROCFLVirtualObject virtualObject = MCROCFLFileSystemProvider.get().virtualObjectProvider().get(versionedPath);
        if (virtualObject == null) {
            throw new NoSuchFileException(path.toString());
        }
        return virtualObject.probeContentType(versionedPath);
    }

}
