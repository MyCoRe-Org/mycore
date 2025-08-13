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

package org.mycore.resource.common;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.mycore.common.config.MCRConfiguration2;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

/**
 * A utility class that obtains the classpath of a {@link ClassLoader} using {@link ClassGraph} and globally
 * shared {@link ExecutorService}.
 */
public final class MCRClassgraphUtil {

    private static final String CLASS_GRAPH_THREAD_COUNT_NAME = "MCR.Resource.Common.ClassGraph.ThreadCount";

    private static final int CLASS_GRAPH_THREAD_COUNT = MCRConfiguration2.getInt(CLASS_GRAPH_THREAD_COUNT_NAME)
        .orElseThrow(() -> MCRConfiguration2.createConfigurationException(CLASS_GRAPH_THREAD_COUNT_NAME));

    private static final ExecutorService EXECUTOR_SERVICE = new ThreadPoolExecutor(CLASS_GRAPH_THREAD_COUNT,
        CLASS_GRAPH_THREAD_COUNT, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
        new ThreadFactoryBuilder().setNameFormat("MCRClassgraphUtil-worker-%d").setDaemon(true).build());

    private MCRClassgraphUtil() {
    }

    public static List<URI> scanClasspath(ClassLoader classLoader) {

        ClassGraph classGraph = new ClassGraph();
        classGraph.overrideClassLoaders(classLoader);

        List<URI> classpath;
        try (ScanResult scanResult = classGraph.scan(EXECUTOR_SERVICE, CLASS_GRAPH_THREAD_COUNT)) {
            classpath = scanResult.getClasspathURIs();

        }

        return classpath;

    }

}
