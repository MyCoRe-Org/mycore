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

package org.mycore.resource;

import java.io.Serial;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A value class that represents a resource path, which inherently is a slash-delimited list of path segments.
 * Resource path point to resources (generally: files) that may or may not exist.
 * <p>
 * A resource path must be non-empty, must not contain segments <code>.</code> or <code>..</code> and must not end
 * with a slash. A resource path without a leading slash is equal to the same resource path with a leading slash.
 * <p>
 * A resource path starting with <code>/META-INF/resources/</code> is considered to be a web resource path.
 * Resources pointed to by a web resource path are considered to be web resources. Web resource paths
 * must not continue with <code>/META-INF/</code> or <code>/WEB-INF/</code> after the <code>/META-INF/resources/</code>
 * prefix because of the way that web containers like <em>Tomcat</em> or <em>Jetty</em> handle resources
 * found in WAR and JAR files.
 * <p>
 * Web containers like <em>Tomcat</em> and <em>Jetty</em> serve the content of the webapp directory (and the content
 * inside <code>/META-INF/resources</code> directories of JAR files) as web content. Conflictingly, they also extract
 * the content of the WAR files <code>/META-INF</code> and <code>/WEB-INF</code> directories into the webapp directory,
 * although the content of these directories is explicitly not meant to be exposed as web content. The content of these
 * directories is therefore not served as web content; deviating from the rule mentioned at the beginning of this
 * paragraph. Consequently, a resource path can not start with <code>/META-INF/resources/META-INF/</code> or
 * <code>/META-INF/resources/WEB-INF/</code>, because such resource paths would be web resource paths that point to
 * resources that are not served by popular web containers.
 */
public sealed abstract class MCRResourcePath {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String WEB_RESOURCE_PREFIX = "/META-INF/resources";

    private static final String WEB_RESOURCE_PREFIX_WITH_TRAILING_SLASH = WEB_RESOURCE_PREFIX + "/";

    /** Returns weather this path is a web resource path. */
    public abstract boolean isWebPath();

    /**
     * Returns the complete path as a {@link String} with a leading slash.
     */
    public abstract String asAbsolutePath();

    /**
     * Returns the complete path as a {@link String} without a leading slash.
     */
    public String asRelativePath() {
        return removeLeadingSlash(asAbsolutePath());
    }

    /**
     * Returns a path beginning with <code>/META-INF/resources</code> as a {@link String} without that prefix
     * and with a leading slash.
     */
    public abstract Optional<String> asAbsoluteWebPath();

    /**
     * Returns a path beginning with <code>/META-INF/resources</code> as a {@link String} without that prefix
     * and without a leading slash.
     */
    public Optional<String> asRelativeWebPath() {
        return asAbsoluteWebPath().map(this::removeLeadingSlash);
    }

    private String removeLeadingSlash(String path) {
        return path.substring("/".length());
    }

    @Override
    public String toString() {
        return asAbsolutePath();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MCRResourcePath && ((MCRResourcePath) other).asAbsolutePath().equals(asAbsolutePath());
    }

    @Override
    public int hashCode() {
        return asAbsolutePath().hashCode();
    }

    /**
     * Creates a {@link MCRResourcePath} that represents the given path as a resource path.
     * <p>
     * Returns an empty {@link Optional} in case of an empty path or in case
     * of a path targeting a directory.
     * <p>
     * Throws a {@link IllegalPathException} in case of a path starting with <code>/META-INF/resources/META-INF</code>
     * or <code>/META-INF/resources/WEB-INF</code> or in case of a path targeting a class.
     */
    public static Optional<MCRResourcePath> ofPath(Optional<String> path) {
        return Objects.requireNonNull(path, "Path must not be null").flatMap(MCRResourcePath::ofPath);
    }

    /**
     * Creates a {@link MCRResourcePath} that represents the given path as a resource path.
     * <p>
     * Returns an empty {@link Optional} in case of a missing or empty path or in case
     * of a path targeting a directory.
     * <p>
     * Throws a {@link IllegalPathException} in case of a path starting with <code>/META-INF/resources/META-INF</code>
     * or <code>/META-INF/resources/WEB-INF</code> or in case of a path targeting a class.
     */
    public static Optional<MCRResourcePath> ofPath(String path) {
        return Optional.ofNullable(cleanAndSafePath(path, false)).map(MCRResourcePath::createResourcePath);
    }

    /**
     * Creates a {@link MCRResourcePath} that represents the given path as a resource path.
     * <p>
     * Throws a {@link IllegalPathException} in case of a missing or empty path or in case
     * of a path targeting a directory or in case of a path starting with <code>/META-INF/resources/META-INF</code>
     * or <code>/META-INF/resources/WEB-INF</code> or in case of a path targeting a class.
     */
    public static MCRResourcePath ofPathOrThrow(String path) {
        return createResourcePath(cleanAndSafePath(path, true));
    }

    private static MCRResourcePath createResourcePath(String path) {
        if (path.startsWith(WEB_RESOURCE_PREFIX_WITH_TRAILING_SLASH)) {
            return createWebResourcePath(path.substring(WEB_RESOURCE_PREFIX.length()));
        }
        if (path.endsWith(".class")) {
            throw IllegalPathException.Code.PATH_POINTS_TO_CLASS_RESOURCE.createException(path);
        }
        LOGGER.debug("Creating resource path {}", path);
        return new ResourcePath(path);
    }

    /**
     * Creates a {@link MCRResourcePath} that represents the given path as a web resource path,
     * i.e. a resource path that implicitly starts with <code>/META-INF/resources/</code>.
     * <p>
     * Returns an empty {@link Optional} in case of an empty path or in case
     * of a path targeting a directory.
     * <p>
     * Throws a {@link IllegalPathException} in case of a path starting with <code>/META-INF</code>
     * or <code>/WEB-INF</code> or in case of a path targeting a class.
     */
    public static Optional<MCRResourcePath> ofWebPath(Optional<String> path) throws IllegalPathException {
        return Objects.requireNonNull(path, "Path must not be null").flatMap(MCRResourcePath::ofWebPath);
    }

    /**
     * Creates a {@link MCRResourcePath} that represents the given path as a web resource path,
     * i.e. a resource path that implicitly starts with <code>/META-INF/resources/</code>.
     * <p>
     * Returns an empty {@link Optional} in case of a missing or empty path or in case
     * of a path targeting a directory.
     * <p>
     * Throws a {@link IllegalPathException} in case of a path starting with <code>/META-INF</code>
     * or <code>/WEB-INF</code> or in case of a path targeting a class.
     */
    public static Optional<MCRResourcePath> ofWebPath(String path) {
        return Optional.ofNullable(cleanAndSafePath(path, false)).map(MCRResourcePath::createWebResourcePath);
    }

    /**
     * Creates a {@link MCRResourcePath} that represents the given path as a web resource path,
     * i.e. a resource path that implicitly starts with <code>/META-INF/resources/</code>.
     * <p>
     * Throws a {@link IllegalPathException} in case of a missing or empty path or in case
     * of a path targeting a directory or in case of a path starting with <code>/META-INF</code>
     * or <code>/WEB-INF</code> path or in case of a path targeting a class.
     */
    public static MCRResourcePath ofWebPathOrThrow(String path) {
        return createWebResourcePath(cleanAndSafePath(path, true));
    }

    private static WebResourcePath createWebResourcePath(String path) {
        if (path.startsWith("/META-INF/")) {
            throw IllegalPathException.Code.PATH_POINTS_TO_META_INF_RESOURCE.createException(path);
        }
        if (path.startsWith("/WEB-INF/")) {
            throw IllegalPathException.Code.PATH_POINTS_TO_WEB_INF_RESOURCE.createException(path);
        }
        if (path.endsWith(".class")) {
            throw IllegalPathException.Code.PATH_POINTS_TO_CLASS_RESOURCE.createException(path);
        }
        LOGGER.debug("Creating web resource path {}", path);
        return new WebResourcePath(path);
    }

    private static String cleanAndSafePath(String path, boolean orThrow) {
        if (path == null || path.isEmpty()) {
            if (orThrow) {
                throw IllegalPathException.Code.MISSING_OR_EMPTY_PATH.createException(path);
            } else {
                return null;
            }
        }
        if (path.endsWith("/")) {
            if (orThrow) {
                throw IllegalPathException.Code.DIRECTORY_PATH.createException(path);
            } else {
                return null;
            }
        }
        if (!path.startsWith("/")) {
            return safePath("/" + path);
        }
        return safePath(path);
    }

    private static String safePath(String path) {
        if (path.contains("//")) {
            throw IllegalPathException.Code.PATH_CONTAINS_EMPTY_SEGMENT.createException(path);
        }
        if (path.contains("/./")) {
            throw IllegalPathException.Code.PATH_CONTAINS_LINK_TO_SELF.createException(path);
        }
        if (path.contains("/../")) {
            throw IllegalPathException.Code.PATH_CONTAINS_LINK_TO_PARENT.createException(path);
        }
        return path;
    }

    /**
     * Represents a path that doesn't start with <code>/META-INF/resources/</code>
     */
    private static final class ResourcePath extends MCRResourcePath {

        private final String path;

        private ResourcePath(String path) {
            assert !path.startsWith(WEB_RESOURCE_PREFIX);
            this.path = path;
        }

        @Override
        public boolean isWebPath() {
            return false;
        }

        @Override
        public String asAbsolutePath() {
            return path;
        }

        @Override
        public Optional<String> asAbsoluteWebPath() {
            return Optional.empty();
        }

    }

    /**
     * Represents a path that start with <code>/META-INF/resources/</code>
     */
    private static final class WebResourcePath extends MCRResourcePath {

        private final String path;

        private final Optional<String> webResourcePath;

        private WebResourcePath(String path) {
            this.path = WEB_RESOURCE_PREFIX + path;
            this.webResourcePath = Optional.of(path);
        }

        @Override
        public boolean isWebPath() {
            return true;
        }

        @Override
        public String asAbsolutePath() {
            return path;
        }

        @Override
        public Optional<String> asAbsoluteWebPath() {
            return webResourcePath;
        }

    }

    /**
     * A {@link IllegalPathException} indicates that an illegal path is used as a resource or web resource path.
     * <p>
     * The {@link #code} specifies the nature of the violation.
     */
    public static class IllegalPathException extends IllegalArgumentException {

        @Serial
        private static final long serialVersionUID = 1L;

        public final Code code;

        IllegalPathException(Code code, String path) {
            super(code.description + ": " + path);
            this.code = code;
        }

        public enum Code {

            PATH_POINTS_TO_META_INF_RESOURCE("Path points to META-INF resource (i.e. beginning with /META-INF/...)"),

            PATH_POINTS_TO_WEB_INF_RESOURCE("Path points to META-INF resource (i.e. beginning with /WEB-INF/...)"),

            PATH_POINTS_TO_CLASS_RESOURCE("Path points to META-INF resource (i.e. ending with .class)"),

            PATH_CONTAINS_EMPTY_SEGMENT("Path contains empty segment (i.e. '//')"),

            PATH_CONTAINS_LINK_TO_SELF("Path contains segment link to self (i.e. '/./')"),

            PATH_CONTAINS_LINK_TO_PARENT("Path contains segment link to parent  (i.e. '/../')"),

            MISSING_OR_EMPTY_PATH("Path is null or empty"),

            DIRECTORY_PATH("Path point to a directory");

            private final String description;

            Code(String description) {
                this.description = description;
            }

            private IllegalPathException createException(String path) {
                return new IllegalPathException(this, path);
            }

        }

    }

}
