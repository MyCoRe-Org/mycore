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

package org.mycore.resource.hint;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRClassTools;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.hint.MCRHint;
import org.mycore.common.hint.MCRHintKey;
import org.mycore.resource.common.ClasspathSupplier;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

@MCRConfigurationProxy(proxyClass = MCRClasspathResourceHint.Factory.class)
public sealed abstract class MCRClasspathResourceHint implements MCRHint<ClasspathSupplier> {

    public static final String MODE_KEY = "Mode";

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CLASS_GRAPH_THREAD_COUNT_NAME = "MCR.Resource.Provider.ClassGraph.ThreadCount";

    private static final int CLASS_GRAPH_THREAD_COUNT = MCRConfiguration2.getInt(CLASS_GRAPH_THREAD_COUNT_NAME)
        .orElseThrow(() -> MCRConfiguration2.createConfigurationException(CLASS_GRAPH_THREAD_COUNT_NAME));

    private static final ExecutorService EXECUTOR_SERVICE = new ThreadPoolExecutor(CLASS_GRAPH_THREAD_COUNT,
        CLASS_GRAPH_THREAD_COUNT, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
        new ThreadFactoryBuilder().setNameFormat("ClassLoaderPrefixStripper-worker-%d").setDaemon(true).build());

    private MCRClasspathResourceHint() {
    }

    @Override
    public final MCRHintKey<ClasspathSupplier> key() {
        return MCRResourceHintKeys.CLASSPATH;
    }

    @Override
    public final Optional<ClasspathSupplier> value() {
        ClassLoader classLoader = MCRClassTools.getClassLoader();
        return Optional.of(new ClasspathSupplier(classLoader.getName(), () -> obtainClasspath(classLoader)));
    }

    protected abstract List<URI> obtainClasspath(ClassLoader classLoader);

    protected final List<URI> scanClassLoaderForClasspath(ClassLoader classLoader) {
        if(LOGGER.isInfoEnabled()) {
            LOGGER.info("Scanning class loader {} ({}) for classpath entries", classLoader.getName(), classLoader);
        }
        ClassGraph classGraph = new ClassGraph();
        classGraph.overrideClassLoaders(classLoader);
        try (ScanResult scanResult = classGraph.scan(EXECUTOR_SERVICE, CLASS_GRAPH_THREAD_COUNT)) {
            return scanResult.getClasspathURIs();
        }
    }

    private static final class NonCaching extends MCRClasspathResourceHint {

        @Override
        protected List<URI> obtainClasspath(ClassLoader classLoader) {
            return scanClassLoaderForClasspath(classLoader);
        }

    }

    private static final class Caching extends MCRClasspathResourceHint {

        private static final Map<ClassLoader, List<URI>> CLASSPATHS = Collections.synchronizedMap(new WeakHashMap<>());

        @Override
        protected List<URI> obtainClasspath(ClassLoader classLoader) {
            return CLASSPATHS.computeIfAbsent(classLoader, this::scanClassLoaderForClasspath);
        }

    }

    public enum Mode {

        NON_CACHING(NonCaching::new),

        CACHING(Caching::new);

        private final Supplier<MCRClasspathResourceHint> supplier;

        Mode(Supplier<MCRClasspathResourceHint> supplier) {
            this.supplier = supplier;
        }

    }

    public static class Factory implements Supplier<MCRClasspathResourceHint> {

        @MCRProperty(name = MODE_KEY)
        public String mode;

        @Override
        public MCRClasspathResourceHint get() {
            return Mode.valueOf(mode).supplier.get();
        }

    }

}
