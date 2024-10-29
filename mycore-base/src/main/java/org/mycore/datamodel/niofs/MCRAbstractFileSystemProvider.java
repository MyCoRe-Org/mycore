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

package org.mycore.datamodel.niofs;

import java.io.IOException;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.fileupload.MCRUploadHelper;

public abstract class MCRAbstractFileSystemProvider extends FileSystemProvider {

    /**
     * set of supported copy options
     */
    protected static final Set<? extends CopyOption> SUPPORTED_COPY_OPTIONS = Collections.unmodifiableSet(EnumSet.of(
        StandardCopyOption.COPY_ATTRIBUTES,
        StandardCopyOption.REPLACE_EXISTING));

    /**
     * set of supported open options
     */
    protected static final Set<? extends OpenOption> SUPPORTED_OPEN_OPTIONS = Collections.unmodifiableSet(EnumSet.of(
        StandardOpenOption.APPEND,
        StandardOpenOption.DSYNC,
        StandardOpenOption.READ,
        StandardOpenOption.SPARSE,
        StandardOpenOption.SYNC,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE));

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#newFileSystem(java.net.URI, java.util.Map)
     */
    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) {
        throw new FileSystemAlreadyExistsException();
    }

    public abstract URI getURI();

    public abstract MCRAbstractFileSystem getFileSystem();

    /* (non-Javadoc)
     * @see java.nio.file.spi.FileSystemProvider#getPath(java.net.URI)
     */
    @Override
    public MCRPath getPath(final URI uri) {
        PathInformation pathInfo = getPathInformation(uri);
        return getPath(pathInfo.owner, pathInfo.path);
    }

    public MCRPath getPath(String owner, String path) {
        return new MCRPath(owner, path, getFileSystem()) {
        };
    }

    protected PathInformation getPathInformation(URI uri) {
        if (!getURI().getScheme().equals(Objects.requireNonNull(uri).getScheme())) {
            throw new FileSystemNotFoundException("Unknown filesystem: " + uri);
        }
        String path = uri.getPath().substring(1); //URI path is absolute -> remove first slash
        String owner = null;
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == MCRAbstractFileSystem.SEPARATOR) {
                break;
            }
            if (path.charAt(i) == ':') {
                owner = path.substring(0, i);
                path = path.substring(i + 1);
                break;
            }

        }
        return new PathInformation(owner, path);
    }

    protected void checkDirectoryAccessModes(Path directoryPath, AccessMode... modes) throws AccessDeniedException {
        for (AccessMode mode : modes) {
            switch (mode) {
                case READ:
                case WRITE:
                case EXECUTE:
                    break;
                default:
                    throw new AccessDeniedException(directoryPath.toString(), null, "Unsupported AccessMode: " + mode);
            }
        }
    }

    protected void checkFileAccessModes(Path filePath, AccessMode... modes) throws AccessDeniedException {
        for (AccessMode mode : modes) {
            switch (mode) {
                case READ:
                case WRITE:
                    break;
                default:
                    throw new AccessDeniedException(filePath.toString(), null, "Unsupported AccessMode: " + mode);
            }
        }
    }

    protected void checkFileName(String fileName) throws IOException {
        //check property lazy as on initialization of this class MCRConfiguration2 is not ready
        if (MCRConfiguration2.getBoolean("MCR.NIO.PathCreateNameCheck").orElse(true)) {
            try {
                MCRUploadHelper.checkPathName(fileName, true);
            } catch (MCRException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
    }

    protected void checkOpenOption(Set<? extends OpenOption> openOptions) {
        for (OpenOption option : openOptions) {
            checkOpenOption(option);
        }
    }

    protected Set<? extends OpenOption> getOpenOptions(Set<? extends OpenOption> options) {
        return options.stream()
            .filter(option -> !(option == StandardOpenOption.CREATE || option == StandardOpenOption.CREATE_NEW))
            .collect(Collectors.toSet());
    }

    protected void checkOpenOption(OpenOption option) {
        if (!SUPPORTED_OPEN_OPTIONS.contains(option)) {
            throw new UnsupportedOperationException("Unsupported OpenOption: " + option.getClass().getSimpleName()
                + "." + option);
        }
    }

    protected void checkCopyOptions(CopyOption[] options) {
        for (CopyOption option : options) {
            if (!SUPPORTED_COPY_OPTIONS.contains(option)) {
                throw new UnsupportedOperationException("Unsupported copy option: " + option);
            }
        }
    }

    protected String[] splitAttrName(String attribute) {
        String[] s = new String[2];
        int pos = attribute.indexOf(':');
        if (pos == -1) {
            s[0] = "basic";
            s[1] = attribute;
        } else {
            s[0] = attribute.substring(0, pos++);
            s[1] = (pos == attribute.length()) ? "" : attribute.substring(pos);
        }
        return s;
    }

    protected record PathInformation(String owner, String path) {
    }

}
