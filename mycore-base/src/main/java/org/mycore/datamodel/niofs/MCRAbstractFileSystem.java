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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public abstract class MCRAbstractFileSystem extends FileSystem {

    public static final char SEPARATOR = '/';

    public static final String SEPARATOR_STRING = String.valueOf(SEPARATOR);

    private final LoadingCache<String, MCRPath> rootDirectoryCache = CacheBuilder.newBuilder().weakValues()
        .build(new CacheLoader<String, MCRPath>() {

            @Override
            public MCRPath load(final String owner) throws Exception {
                return getPath(owner, "/", instance());
            }
        });

    private final MCRPath emptyPath;

    public MCRAbstractFileSystem() {
        super();
        emptyPath = getPath(null, "", this);
    }

    /**
     * Returns any subclass that implements and handles the given scheme.
     * @param scheme a valid {@link URI} scheme
     * @see FileSystemProvider#getScheme()
     * @throws FileSystemNotFoundException if no filesystem handles this scheme
     */
    public static MCRAbstractFileSystem getInstance(String scheme) {
        URI uri;
        try {
            uri = MCRPaths.getURI(scheme, "helper", SEPARATOR_STRING);
        } catch (URISyntaxException e) {
            throw new MCRException(e);
        }
        for (FileSystemProvider provider : Iterables.concat(MCRPaths.webAppProvider,
            FileSystemProvider.installedProviders())) {
            if (provider.getScheme().equals(scheme)) {
                return (MCRAbstractFileSystem) provider.getFileSystem(uri);
            }
        }
        throw new FileSystemNotFoundException("Provider \"" + scheme + "\" not found");
    }

    public static MCRPath getPath(final String owner, final String path, final MCRAbstractFileSystem fs) {
        Objects.requireNonNull(fs, MCRAbstractFileSystem.class.getSimpleName() + " instance may not be null.");
        return new MCRPath(owner, path) {

            @Override
            public MCRAbstractFileSystem getFileSystem() {
                return fs;
            }
        };
    }

    /**
     * Creates a new root under the given name.
     * 
     * After calling this method the implementing FileSystem should
     * be ready to accept data for this root.
     * 
     * @param owner ,e.g. derivate ID
     * @throws FileSystemException if creating the root directory fails
     * @throws FileAlreadyExistsException more specific, if the directory already exists
     */
    public abstract void createRoot(String owner) throws FileSystemException;

    /**
     * Checks if the file for given Path is still valid.
     * 
     * This should check if the file is still completely readable and the MD5 sum still matches the recorded value.
     * @param path Path to the file to check
     * @return if the file is still in good condition
     */
    public boolean verifies(MCRPath path) throws NoSuchFileException {
        try {
            return verifies(path, Files.readAttributes(path, MCRFileAttributes.class));
        } catch (IOException e) {
            if (e instanceof NoSuchFileException) {
                throw (NoSuchFileException) e;
            }
            return false;
        }
    }

    /**
     * Checks if the file for given Path is still valid.
     * 
     * This should check if the file is still completely readable and the MD5 sum still matches the recorded value.
     * This method does the same as {@link #verifies(MCRPath)} but uses the given attributes to save a file access.
     * @param path Path to the file to check
     * @param attrs matching attributes to file
     */
    public boolean verifies(MCRPath path, MCRFileAttributes<?> attrs) throws NoSuchFileException {
        if (Files.notExists(Objects.requireNonNull(path, "Path may not be null."))) {
            throw new NoSuchFileException(path.toString());
        }
        Objects.requireNonNull(attrs, "attrs may not be null");
        String md5Sum;
        try {
            md5Sum = MCRUtils.getMD5Sum(Files.newInputStream(path));
        } catch (IOException e) {
            LogManager.getLogger(getClass()).error("Could not verify path: {}", path, e);
            return false;
        }
        boolean returns = md5Sum.matches(attrs.md5sum());
        if (!returns) {
            LogManager.getLogger(getClass()).warn("MD5sum does not match: {}", path);
        }
        return returns;
    }

    /**
     * Removes a root with the given name.
     * 
     * Call this method if you want to remove a stalled directory that is not in use anymore.
     * 
     * @param owner ,e.g. derivate ID
     * @throws FileSystemException if removing the root directory fails
     * @throws DirectoryNotEmptyException more specific, if the directory is not empty
     * @throws NoSuchFileException more specific, if the directory does not exist
     */
    public abstract void removeRoot(String owner) throws FileSystemException;

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException();
    }

    public MCRPath emptyPath() {
        return emptyPath;
    }

    @Override
    public Path getPath(final String first, final String... more) {
        String root = null;
        final StringBuilder path = new StringBuilder();
        if (!first.isEmpty() && first.charAt(first.length() - 1) == ':') {
            //we have a root;
            root = first;
            path.append(SEPARATOR);
        } else {
            path.append(first);
        }
        boolean addSep = path.length() > 0;
        for (final String element : more) {
            if (!element.isEmpty()) {
                if (addSep) {
                    path.append(SEPARATOR);
                } else {
                    addSep = true;
                }
                path.append(element);
            }
        }
        return getPath(root, path.toString(), this);
    }

    @Override
    public PathMatcher getPathMatcher(final String syntaxAndPattern) {
        final int pos = syntaxAndPattern.indexOf(':');
        if (pos <= 0 || pos == syntaxAndPattern.length()) {
            throw new IllegalArgumentException();
        }
        final String syntax = syntaxAndPattern.substring(0, pos);
        final String pattern = syntaxAndPattern.substring(pos + 1);
        switch (syntax) {
            case "glob":
                return new MCRGlobPathMatcher(pattern);
            case "regex":
                return new MCRPathMatcher(pattern);
            default:
                throw new UnsupportedOperationException("If the pattern syntax '" + syntax + "' is not known.");
        }
    }

    public MCRPath getRootDirectory(final String owner) {
        return rootDirectoryCache.getUnchecked(owner);
    }

    @Override
    public String getSeparator() {
        return SEPARATOR_STRING;
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return Collections.unmodifiableSet(Sets.newHashSet("basic", "mcrifs"));
    }

    public MCRPath toThisFileSystem(final MCRPath other) {
        if (Objects.requireNonNull(other).getFileSystem() == this) {
            return other;
        }
        return getPath(other.root, other.path, this);
    }

    private MCRAbstractFileSystem instance() {
        return this;
    }

}
