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

import static org.mycore.resource.common.MCRTraceLoggingHelper.trace;

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
 * <li>In mode {@link MCRResourceProviderMode#RESOURCES}, the content of the base directories is provided
 * as resources. Resource paths are appended to base directory paths to find possible resources.
 * <li>In mode {@link MCRResourceProviderMode#WEB_RESOURCES}, the content of the base directories is provided
 * as web resources. Web resource paths, without the leading <code>META-INF/resources</code> are appended
 * to base directory paths to find possible web resources.
 * </ul>
 * <p>
 * Say, the directory <code>/foo</code> is used as a base directory and has the following content:
 * <pre><code>
 * foo
 * ├─ META-INF
 * │  └─ resources
 * │     ├─ nested
 * │     │  └─ resource.txt
 * │     └─ resource.txt
 * ├─ nested
 * │  └─ resource.txt
 * └─ resource.txt
 * </code></pre>
 * <ul>
 * <li>In mode {@link MCRResourceProviderMode#RESOURCES}
 * <ul>
 * <li>resource path <code>/resource.txt</code> resolves to
 * <br>file path <code>/foo/resource.txt</code>,
 * <li>resource path <code>/nested/resource.txt</code> resolves to
 * <br>file path <code>/foo/nested/resource.txt</code>,
 * <li>web resource path <code>/META-INF/resources/resource.txt</code> resolves to
 * <br>file path <code>/foo/META-INF/resources/resource.txt</code> and
 * <li>web resource path <code>/META-INF/resources/nested/resource.txt</code> resolves to
 * <br>file path <code>/foo/META-INF/resources/nested/resource.txt</code>
 * </ul>
 * <li>In mode {@link MCRResourceProviderMode#WEB_RESOURCES}
 * <ul>
 * <li>resource path <code>/resource.txt</code>
 * <br>does not resolve to a file path,
 * <li>resource path <code>/nested/resource.txt</code>
 * <br>does not resolve to a file path,
 * <li>web resource path <code>/META-INF/resources/resource.txt</code>
 * <br>resolves to file path <code>/foo/resource.txt</code> and
 * <li>web resource path <code>/META-INF/resources/nested/resource.txt</code>
 * <br>resolves to file path <code>/foo/nested/resource.txt</code>
 * </ul>
 * </ul>
 */
public abstract class MCRFileSystemResourceProviderBase extends MCRResourceProviderBase {

    public static final String MODE_KEY = "Mode";

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
        return getResourceUrls(path, hints).map(this::providedUrl).toList();
    }

    private Stream<URL> getResourceUrls(MCRResourcePath path, MCRHints hints) {
        return getBaseDirs(hints)
            .filter(baseDir -> isUsableBaseDir(baseDir, hints))
            .map(baseDir -> toSafeFile(baseDir, path))
            .flatMap(Optional::stream)
            .filter(file -> isUsableFile(file, hints))
            .map(this::toUrl);
    }

    protected abstract Stream<File> getBaseDirs(MCRHints hints);

    private boolean isUsableBaseDir(File baseDir, MCRHints hints) {
        String dirPath = baseDir.getAbsolutePath();
        trace(hints, () -> "Looking for directory " + dirPath);
        if (!baseDir.exists()) {
            trace(hints, () -> dirPath + " doesn't exist");
            return false;
        }
        if (!baseDir.isDirectory()) {
            trace(hints, () -> dirPath + " isn't a directory");
            return false;
        }
        if (!baseDir.canRead()) {
            trace(hints, () -> dirPath + " can't be read");
            return false;
        }
        if (!baseDir.canExecute()) {
            trace(hints, () -> dirPath + " can't be opened");
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

    private boolean isUsableFile(File file, MCRHints hints) {
        String filePath = file.getAbsolutePath();
        trace(hints, () -> "Looking for file " + filePath);
        if (!file.exists()) {
            trace(hints, () -> filePath + " doesn't exist");
            return false;
        }
        if (!file.isFile()) {
            trace(hints, () -> filePath + " isn't a file");
            return false;
        }
        if (!file.canRead()) {
            trace(hints, () -> filePath + " can't be read");
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
