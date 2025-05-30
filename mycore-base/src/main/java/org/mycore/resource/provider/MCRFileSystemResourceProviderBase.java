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

package org.mycore.resource.provider;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.resource.MCRResourcePath;

/**
 * {@link MCRFileSystemResourceProviderBase} is a base implementation of a {@link MCRResourceProvider} that
 * looks up resources in the file system. Implementors must provide the file system base directories that should
 * be searched when looking up a resource. If multiple directories contain a resource for the same resource path,
 * only the first matching file is considered.
 * <p>
 * Two modes are supported:
 * <ul>
 *     <li>In mode {@link MCRResourceProviderMode#RESOURCES}, the content of the base directories is provided
 *     as resources. The resource path to be looked up is directly matched against the content of a directory.</li>
 *     <li>In mode {@link MCRResourceProviderMode#WEB_RESOURCES}, the content of the base directories is provided
 *     as web resources. The resource path to be looked up is matched against the content of a directory, but
 *     without its <code>/META-INF/resources</code> prefix.</li>
 * </ul>
 * <p>
 * Say, the directory <code>/foo</code> is used as a base directory and has the following content:
 * <pre>
 * foo
 * ├─ META-INF
 * │  └─ resources
 * │     ├─ nested
 * │     │  └─ resources.txt
 * │     └─ resources.txt
 * ├─ nested
 * │  └─ resource.txt
 * └─ resource.txt
 * </pre>
 * <ul>
 *     <li>In mode {@link MCRResourceProviderMode#RESOURCES}
 *       <ul>
 *           <li>resource path <code>/resource.txt</code>
 *           would be resolved as <code>/foo/resource.txt</code>,</li>
 *           <li>resource path <code>/nested/resource.txt</code>
 *           would be resolved as <code>/foo/nested/resource.txt</code>,</li>
 *           <li>resource path <code>/META-INF/resources/resource.txt</code>
 *           (i.e. web resource path <code>/resource.txt</code>)
 *           would be resolved as <code>/foo/META-INF/resources/resource.txt</code> and</li>
 *           <li>resource path <code>/META-INF/resources/nested/resource.txt</code>
 *           (i.e. web resource path <code>/nested/resource.txt</code>)
 *           would be resolved as <code>/foo/META-INF/resources/nested/resource.txt</code></li>
 *       </ul>
 *     <li>In mode {@link MCRResourceProviderMode#WEB_RESOURCES}
 *       <ul>
 *           <li>resource path <code>/resource.txt</code>
 *           would not be resolved because this path doesn't represent a web resource,</li>
 *           <li>resource path <code>/nested/resource.txt</code>
 *           would not be resolved because this path doesn't represent a web resource,</li>
 *           <li>resource path <code>/META-INF/resources/resource.txt</code>
 *           (i.e. web resource path <code>/resource.txt</code>)
 *           would be resolved as <code>/foo/resource.txt</code> and</li>
 *           <li>resource path <code>/META-INF/resources/nested/resource.txt</code>
 *           (i.e. web resource path <code>/nested/resource.txt</code>)
 *           would be resolved as <code>/foo/nested/resource.txt</code></li>
 *       </ul>
 *     </li>
 * </ul>
 */
public abstract class MCRFileSystemResourceProviderBase extends MCRResourceProviderBase {

    private final MCRResourceProviderMode mode;

    public MCRFileSystemResourceProviderBase(String coverage, MCRResourceProviderMode mode) {
        super(coverage);
        this.mode = Objects.requireNonNull(mode, "Mode must not be null");
    }

    @Override
    protected final Optional<URL> doProvide(MCRResourcePath path, MCRHints hints) {
        return getResourceUrls(path, hints).findFirst();
    }

    @Override
    protected final List<ProvidedUrl> doProvideAll(MCRResourcePath path, MCRHints hints) {
        return getResourceUrls(path, hints).map(this::providedURL).toList();
    }

    private Stream<URL> getResourceUrls(MCRResourcePath path, MCRHints hints) {
        return getBaseDirs(hints)
            .filter(this::isUsableBaseDir)
            .map(baseDir -> toSafeFile(baseDir, path))
            .flatMap(Optional::stream)
            .filter(this::isUsableFile)
            .map(this::toUrl);
    }

    protected abstract Stream<File> getBaseDirs(MCRHints hints);

    private boolean isUsableBaseDir(File baseDir) {
        String dirPath = baseDir.getAbsolutePath();
        logger.debug("Looking for directory {}", dirPath);
        if (!baseDir.exists()) {
            logger.debug("{} doesn't exist", dirPath);
            return false;
        }
        if (!baseDir.isDirectory()) {
            logger.debug("{} isn't a directory", dirPath);
            return false;
        }
        if (!baseDir.canRead()) {
            logger.debug("{} can't be read", dirPath);
            return false;
        }
        if (!baseDir.canExecute()) {
            logger.debug("{} can't be opened", dirPath);
            return false;
        }
        return true;
    }

    private Optional<File> toSafeFile(File baseDir, MCRResourcePath path) {
        return getRelativePath(path).map(relativePath -> toSafeFile(baseDir, relativePath));
    }

    private static File toSafeFile(File baseDir, String relativePath) {
        return MCRUtils.safeResolve(baseDir.toPath(), relativePath).toFile();
    }

    private Optional<String> getRelativePath(MCRResourcePath path) {
        return switch (mode) {
            case RESOURCES -> Optional.of(path.asRelativePath());
            case WEB_RESOURCES -> path.asRelativeWebPath();
        };
    }

    private boolean isUsableFile(File file) {
        String filePath = file.getAbsolutePath();
        logger.debug("Looking for file {}", filePath);
        if (!file.exists()) {
            logger.debug("{} doesn't exist", filePath);
            return false;
        }
        if (!file.isFile()) {
            logger.debug("{} isn't a file", filePath);
            return false;
        }
        if (!file.canRead()) {
            logger.debug("{} can't be read", filePath);
            return false;
        }
        return true;
    }

    private URL toUrl(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new MCRException("Failed to convert file to URL: " + file.getAbsolutePath(), e);
        }
    }

    @Override
    public final Stream<PrefixStripper> prefixStrippers(MCRHints hints) {
        return getBaseDirs(hints).map(BaseDirPrefixStripper::new);
    }

    @Override
    public MCRTreeMessage compileDescription(Level level) {
        MCRTreeMessage description = super.compileDescription(level);
        if (!suppressDescriptionDetails() || level.isLessSpecificThan(Level.DEBUG)) {
            description.add("Mode", mode.name());
        }
        return description;
    }

    protected boolean suppressDescriptionDetails() {
        return false;
    }

}
