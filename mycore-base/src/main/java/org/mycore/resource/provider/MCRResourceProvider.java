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

package org.mycore.resource.provider;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.resource.MCRResourcePath;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import nonapi.io.github.classgraph.concurrency.AutoCloseableExecutorService;

/**
 * A {@link MCRResourceProvider} implements a resource lookup strategy.
 */
public interface MCRResourceProvider {

    /**
     * Resolves a {@link MCRResourcePath} using the given hints.
     */
    Optional<URL> provide(MCRResourcePath path, MCRHints hints);

    /**
     * Resolves a {@link MCRResourcePath}, returning all alternatives (i.e. because one module
     * overrides a resource that is also provided by another module). Intended for introspective purposes only.
     */
    List<ProvidedUrl> provideAll(MCRResourcePath path, MCRHints hints);

    Stream<PrefixStripper> prefixStrippers(MCRHints hints);

    /**
     * Returns a description of this {@link MCRResourceProvider}.
     */
    MCRTreeMessage compileDescription(Level level);

    final class ProvidedUrl {

        public final URL url;

        public final String origin;

        public ProvidedUrl(URL url, String origin) {
            this.url = url;
            this.origin = origin;
        }

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

    final class BaseDirPrefixStripper extends PrefixStripperBase {

        private final String prefix;

        public BaseDirPrefixStripper(File baseDir) {
            this.prefix = Objects.requireNonNull(baseDir).toURI().toString();
        }

        @Override
        public List<String> getStrippedPaths(String value) {
            if (value.startsWith(prefix)) {
                return List.of(value.substring(prefix.length()));
            }
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return prefix;
        }

    }

    final class ClassLoaderPrefixStripper extends PrefixStripperBase {

        private static final int CLASS_GRAPH_THREADS = 4;

        private static final ExecutorService EXECUTOR_SERVICE = new AutoCloseableExecutorService(CLASS_GRAPH_THREADS);

        private final ClassLoader classLoader;

        public ClassLoaderPrefixStripper(ClassLoader classLoader) {
            this.classLoader = Objects.requireNonNull(classLoader);
        }

        @Override
        public List<String> getStrippedPaths(String value) {
            List<String> potentialPaths = new LinkedList<>();
            ClassGraph classGraph = new ClassGraph();
            classGraph.overrideClassLoaders(classLoader);
            try (ScanResult scanResult = classGraph.scan(EXECUTOR_SERVICE, CLASS_GRAPH_THREADS)) {
                List<URI> classpath = scanResult.getClasspathURIs();
                for (URI uri : classpath) {
                    if (uri.getScheme().equals("file")) {
                        File file = new File(uri.getPath());
                        if (file.isDirectory()) {
                            String prefix = file.toURI().toString();
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
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return "jar:.*!";
        }

    }

}
