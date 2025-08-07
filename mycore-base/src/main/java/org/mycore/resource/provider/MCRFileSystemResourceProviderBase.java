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

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.mycore.common.MCRUtils;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.common.MCRResourceTracer;
import org.mycore.resource.common.MCRResourceUtils;

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
    protected final Optional<URL> doProvide(MCRResourcePath path, MCRHints hints, MCRResourceTracer tracer) {
        return getResourceUrls(path, hints, tracer).findFirst();
    }

    @Override
    protected final List<ProvidedUrl> doProvideAll(MCRResourcePath path, MCRHints hints, MCRResourceTracer tracer) {
        return getResourceUrls(path, hints, tracer).map(this::providedUrl).toList();
    }

    private Stream<URL> getResourceUrls(MCRResourcePath path, MCRHints hints, MCRResourceTracer tracer) {
        LinkOption[] linkOptions = MCRResourceUtils.linkOptions();
        return getBaseDirs(hints)
            .map(Path::toAbsolutePath)
            .filter(baseDir -> isUsableBaseDir(baseDir, linkOptions, tracer))
            .map(baseDir -> resolveSafeFile(baseDir, path))
            .flatMap(Optional::stream)
            .filter(file -> isUsableFile(file, linkOptions, tracer))
            .map(MCRResourceUtils::toFileUrl);
    }

    protected abstract Stream<Path> getBaseDirs(MCRHints hints);

    private boolean isUsableBaseDir(Path baseDir, LinkOption[] linkOptions, MCRResourceTracer tracer) {
        tracer.trace(() -> "Looking for directory " + baseDir);
        if (!Files.exists(baseDir, linkOptions)) {
            tracer.trace(() -> baseDir + " doesn't exist");
            return false;
        }
        if (!Files.isDirectory(baseDir, linkOptions)) {
            tracer.trace(() -> baseDir + " isn't a directory");
            return false;
        }
        if (!Files.isReadable(baseDir)) {
            tracer.trace(() -> baseDir + " can't be read");
            return false;
        }
        if (!Files.isExecutable(baseDir)) {
            tracer.trace(() -> baseDir + " can't be opened");
            return false;
        }
        return true;
    }

    private Optional<Path> resolveSafeFile(Path baseDir, MCRResourcePath path) {
        return getRelativePath(path).map(relativePath -> MCRUtils.safeResolve(baseDir, relativePath));
    }

    private Optional<String> getRelativePath(MCRResourcePath path) {
        return switch (mode) {
            case RESOURCES -> Optional.of(path.asRelativePath());
            case WEB_RESOURCES -> path.asRelativeWebPath();
        };
    }

    private boolean isUsableFile(Path file, LinkOption[] linkOptions, MCRResourceTracer tracer) {
        tracer.trace(() -> "Looking for file " + file);
        if (!Files.exists(file, linkOptions)) {
            tracer.trace(() -> file + " doesn't exist");
            return false;
        }
        if (!Files.isRegularFile(file, linkOptions)) {
            tracer.trace(() -> file + " isn't a file");
            return false;
        }
        if (!Files.isReadable(file)) {
            tracer.trace(() -> file + " can't be read");
            return false;
        }
        return true;
    }

    @Override
    public final Stream<PrefixStripper> prefixStrippers(MCRHints hints) {
        return getBaseDirs(hints).map(MCRResourceUtils::toFileUrl).map(URL::toString).map(PrefixPrefixStripper::new);
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
