package org.mycore.datamodel.niofs;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;

public abstract class MCRAbstractFileSystem extends FileSystem {

    public static final char SEPARATOR = '/';

    public static final String SEPARATOR_STRING = String.valueOf(SEPARATOR);

    private final LoadingCache<String, MCRPath> rootDirectoryCache = CacheBuilder.newBuilder().weakValues()
        .build(new CacheLoader<String, MCRPath>() {

            @Override
            public MCRPath load(final String owner) throws Exception {
                return getPath(owner, "", instance());
            }
        });

    private final MCRPath emptyPath;

    public MCRAbstractFileSystem() {
        super();
        emptyPath = getPath(null, "", this);
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
