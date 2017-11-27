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

package org.mycore.datamodel.niofs;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.config.MCRConfiguration;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
final class MCRPaths {

    static final String DEFAULT_SCHEME_PROPERTY = "MCR.NIO.DefaultScheme";

    static List<FileSystemProvider> webAppProvider = new ArrayList<>(4);

    private MCRPaths() {
    }

    static URI getURI(String owner, String path) throws URISyntaxException {
        String scheme = MCRConfiguration.instance().getString(DEFAULT_SCHEME_PROPERTY, "ifs");
        return getURI(scheme, owner, path);
    }

    static URI getURI(String scheme, String owner, String path) throws URISyntaxException {
        boolean needSeperator = Objects.requireNonNull(path, "No 'path' specified.").isEmpty()
            || path.charAt(0) != MCRAbstractFileSystem.SEPARATOR;
        String aPath = needSeperator ? (MCRAbstractFileSystem.SEPARATOR_STRING + path) : path;
        String finalPath = MCRAbstractFileSystem.SEPARATOR_STRING
            + Objects.requireNonNull(owner, "No 'owner' specified.") + ":" + aPath;
        return new URI(Objects.requireNonNull(scheme, "No 'scheme' specified."), null, finalPath, null);
    }

    static Path getPath(String owner, String path) {
        String scheme = MCRConfiguration.instance().getString(DEFAULT_SCHEME_PROPERTY, "ifs");
        return getPath(scheme, owner, path);
    }

    static Path getPath(String scheme, String owner, String path) {
        URI uri;
        try {
            uri = getURI(scheme, owner, path);
            LogManager.getLogger(MCRPaths.class).debug("Generated path URI:{}", uri);
        } catch (URISyntaxException e) {
            throw new InvalidPathException(path, "URI syntax error (" + e.getMessage() + ") for path");
        }
        try {
            return Paths.get(uri);
        } catch (FileSystemNotFoundException e) {
            for (FileSystemProvider provider : webAppProvider) {
                if (provider.getScheme().equals(uri.getScheme())) {
                    return provider.getPath(uri);
                }
            }
            throw e;
        }
    }

    static void addFileSystemProvider(FileSystemProvider provider) {
        webAppProvider.add(provider);
    }
}
