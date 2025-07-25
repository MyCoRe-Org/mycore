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

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.common.MCRNoOpResourceTracer;
import org.mycore.resource.common.MCRResourceUtils;
import org.mycore.resource.common.MCRResourceTracer;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

/**
 * A {@link MCRResourceProvider} implements a resource lookup strategy.
 */
public interface MCRResourceProvider {

    /**
     * Resolves a {@link MCRResourcePath} using the given hints.
     */
    default Optional<URL> provide(MCRResourcePath path, MCRHints hints) {
        return provide(path, hints, new MCRNoOpResourceTracer());
    }

    /**
     * Resolves a {@link MCRResourcePath} using the given hints.
     */
    Optional<URL> provide(MCRResourcePath path, MCRHints hints, MCRResourceTracer tracer);

    /**
     * Resolves a {@link MCRResourcePath}, returning all alternatives (i.e. because one module
     * overrides a resource that is also provided by another module). Intended for introspective purposes only.
     */
    default List<ProvidedUrl> provideAll(MCRResourcePath path, MCRHints hints) {
        return provideAll(path, hints, new MCRNoOpResourceTracer());
    }

    /**
     * Resolves a {@link MCRResourcePath}, returning all alternatives (i.e. because one module
     * overrides a resource that is also provided by another module). Intended for introspective purposes only.
     */
    List<ProvidedUrl> provideAll(MCRResourcePath path, MCRHints hints, MCRResourceTracer tracer);

    /**
     * Returns a stream of {@link PrefixStripper} using the given hints, each of which can remove multiple prefixes
     * from a given resource URL in order to facilitate the reversal of resource path resolution.
     * ({@link org.mycore.resource.MCRResourceResolver#reverse(URL, MCRHints)}).
     */
    Stream<PrefixStripper> prefixStrippers(MCRHints hints);

    /**
     * Returns a description of this {@link MCRResourceProvider}.
     */
    MCRTreeMessage compileDescription(Level level);

    String coverage();

    record ProvidedUrl(URL url, String origin) {

        @Override
        public String toString() {
            return origin + ": " + url;
        }

    }

    interface PrefixStripper {

        Supplier<List<MCRResourcePath>> strip(URL url);

    }

    abstract class PrefixStripperBase implements PrefixStripper {

        @Override
        public final Supplier<List<MCRResourcePath>> strip(URL url) {
            return () -> getStrippedPaths(url.toString())
                .stream()
                .map(MCRResourcePath::ofPath)
                .flatMap(Optional::stream)
                .toList();
        }

        protected abstract List<String> getStrippedPaths(String value);

    }

    class PrefixPrefixStripper extends PrefixStripperBase {

        private final String prefix;

        public PrefixPrefixStripper(String prefix) {
            this.prefix = Objects.requireNonNull(prefix, "Prefix must not be null");
        }

        @Override
        public final List<String> getStrippedPaths(String value) {
            if (value.startsWith(prefix)) {
                return List.of(value.substring(prefix.length()));
            }
            return List.of();
        }

        @Override
        public final String toString() {
            return prefix;
        }

    }

    final class ClassLoaderPrefixStripper extends PrefixStripperBase {

        private static final String CLASS_GRAPH_THREAD_COUNT_NAME = "MCR.Resource.Provider.ClassGraph.ThreadCount";

        private static final int CLASS_GRAPH_THREAD_COUNT = MCRConfiguration2.getInt(CLASS_GRAPH_THREAD_COUNT_NAME)
            .orElseThrow(() -> MCRConfiguration2.createConfigurationException(CLASS_GRAPH_THREAD_COUNT_NAME));

        private static final ExecutorService EXECUTOR_SERVICE = new ThreadPoolExecutor(CLASS_GRAPH_THREAD_COUNT,
            CLASS_GRAPH_THREAD_COUNT, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("ClassLoaderPrefixStripper-worker-%d").setDaemon(true).build());

        private final ClassLoader classLoader;

        public ClassLoaderPrefixStripper(ClassLoader classLoader) {
            this.classLoader = Objects.requireNonNull(classLoader, "Class loader must not be null");
        }

        @Override
        public List<String> getStrippedPaths(String value) {
            List<String> potentialPaths = new LinkedList<>();
            ClassGraph classGraph = new ClassGraph();
            classGraph.overrideClassLoaders(classLoader);
            try (ScanResult scanResult = classGraph.scan(EXECUTOR_SERVICE, CLASS_GRAPH_THREAD_COUNT)) {
                List<URI> classpath = scanResult.getClasspathURIs();
                for (URI uri : classpath) {
                    if (uri.getScheme().equals("file")) {
                        Path path = Path.of(uri);
                        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                            String prefix = MCRResourceUtils.toFileUrl(path).toString();
                            if (value.startsWith(prefix)) {
                                potentialPaths.add(value.substring(prefix.length()));
                            }
                        }
                    }
                }
            }
            return potentialPaths;
        }

        @Override
        public String toString() {
            return classLoader.getName();
        }

    }

    final class JarUrlPrefixStripper extends PrefixStripperBase {

        public static final PrefixStripper INSTANCE = new JarUrlPrefixStripper();

        private JarUrlPrefixStripper() {
        }

        @Override
        public List<String> getStrippedPaths(String value) {
            if (value.startsWith("jar:")) {
                int index = value.indexOf('!');
                if (-1 != index) {
                    return List.of(value.substring(index + 1));
                }
            }
            return List.of();
        }

        @Override
        public String toString() {
            return "jar:.*!";
        }

    }

}
