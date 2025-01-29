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

package org.mycore.common.config;

import java.io.File;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.web.Log4jServletContainerInitializer;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.events.MCRStartupHandler;
import org.mycore.common.events.MCRStartupHandler.AutoExecutable;
import org.mycore.common.log4j2.MCRSessionThreadContext;
import org.mycore.resource.MCRResourceHelper;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

/**
 * Called by {@link MCRStartupHandler} on start up to set up {@link MCRConfiguration2}.
 *
 * @author Thomas Scheffler (yagee)
 * @since 2013.12
 */
public class MCRConfigurationDirSetup implements AutoExecutable {

    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    public static void loadExternalLibs() {
        File resourceDir = MCRConfigurationDir.getConfigFile("resources");
        if (resourceDir == null) {
            //no configuration dir exists
            return;
        }
        Optional<URLClassLoader> classLoaderOptional = Stream
            .of(MCRClassTools.getClassLoader(), Thread.currentThread().getContextClassLoader())
            .filter(URLClassLoader.class::isInstance)
            .map(URLClassLoader.class::cast)
            .findFirst();
        if (classLoaderOptional.isEmpty()) {
            LOGGER.error(() -> classLoaderOptional.getClass() +
                " is unsupported for adding extending CLASSPATH at runtime.");
            return;
        }
        File libDir = MCRConfigurationDir.getConfigFile("lib");
        URLClassLoader urlClassLoader = classLoaderOptional.get();
        List<URL> currentCPElements = Stream.of(urlClassLoader.getURLs()).toList();
        Class<? extends ClassLoader> classLoaderClass = urlClassLoader.getClass();
        try {
            BiConsumer<ClassLoader, URL> addUrlMethod = addToClassPath(classLoaderClass);
            getFileStream(resourceDir, libDir)
                .filter(Objects::nonNull)
                .map(File::toURI)
                .map(u -> {
                    try {
                        return u.toURL();
                    } catch (Exception e) {
                        // should never happen for "file://" URIS
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(u -> !currentCPElements.contains(u))
                .peek(u -> LOGGER.info("Adding to CLASSPATH: {}", u))
                .forEach(url -> addUrlMethod.accept(urlClassLoader, url));
        } catch (InaccessibleObjectException | ReflectiveOperationException | SecurityException e) {
            LOGGER.warn("{} does not support adding additional JARs at runtime", classLoaderClass, e);
        }
    }

    /**
     * Returns a BiConsumer that adds a URL to the classpath of a ClassLoader instance of <code>classLoaderClass</code>.
     */
    private static BiConsumer<ClassLoader, URL> addToClassPath(Class<? extends ClassLoader> classLoaderClass)
        throws NoSuchMethodException {
        Method method;
        Function<URL, ?> argumentMapper;
        final Method addURL = getDeclaredMethod(classLoaderClass, "addURL", URL.class);
        //URLClassLoader does not allow to setAccessible(true) anymore in java >=16
        if (addURL.trySetAccessible()) {
            //works well in Tomcat
            LOGGER.info("Using {} to modify classpath.", addURL);
            method = addURL;
            argumentMapper = u -> u;
        } else {
            final Method jettyFallback = getDeclaredMethod(classLoaderClass, "addClassPath", String.class);
            LOGGER.info("Using {} to modify classpath.", jettyFallback);
            argumentMapper = URL::toString;
            method = jettyFallback;
        }
        Method finalMethod = method;
        Function<URL, ?> finalArgumentMapper = argumentMapper;
        return (cl, url) -> {
            try {
                finalMethod.invoke(cl, finalArgumentMapper.apply(url));
            } catch (IllegalAccessException | InvocationTargetException e) {
                LOGGER.error("Could not add {} to current classloader.", url, e);
            }
        };
    }

    private static Method getDeclaredMethod(Class<? extends ClassLoader> clazz, String method, Class... args)
        throws NoSuchMethodException {
        try {
            return clazz.getDeclaredMethod(method, args);
        } catch (NoSuchMethodException e) {
            try {
                if (ClassLoader.class.isAssignableFrom(clazz.getSuperclass())) {
                    return getDeclaredMethod((Class<? extends ClassLoader>) clazz.getSuperclass(), method, args);
                }
            } catch (NoSuchMethodException e2) {
                e2.addSuppressed(e);
                throw e2;
            }
            throw e;
        }
    }

    private static Stream<File> getFileStream(File resourceDir, File libDir) {
        Stream<File> toClassPath = Stream.of(resourceDir);
        if (libDir.isDirectory()) {
            File[] listFiles = libDir
                .listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".jar"));
            if (listFiles.length != 0) {
                toClassPath = Stream.concat(toClassPath, Stream.of(listFiles));
            }
        }
        return toClassPath;
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#getName()
     */
    @Override
    public String getName() {
        return "Setup of MCRConfigurationDir";
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#getPriority()
     */
    @Override
    public int getPriority() {
        return Integer.MAX_VALUE - 100;
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#startUp(jakarta.servlet.ServletContext)
     */
    @Override
    public void startUp(ServletContext servletContext) {
        MCRConfigurationDir.setServletContext(servletContext);
        loadExternalLibs();
        MCRConfigurationLoader configurationLoader = MCRConfigurationLoaderFactory.getConfigurationLoader();
        Map<String, String> properties = configurationLoader.load();
        final Map<String, String> deprecated = configurationLoader.loadDeprecated();
        MCRConfigurationBase.initialize(deprecated, properties, true);
        if (servletContext != null) {
            Log4jServletContainerInitializer log4jInitializer = new Log4jServletContainerInitializer();
            try {
                log4jInitializer.onStartup(null, servletContext);
            } catch (ServletException e) {
                LOGGER.error("Could not start Log4J2 context", e);
            }
        }
        String configFileKey = "log4j.configurationFile";
        URL log4j2ConfigURL = null;
        if (System.getProperty(configFileKey) == null) {
            log4j2ConfigURL = MCRResourceHelper.getResourceUrl("log4j2.xml");
        }
        LoggerContext logCtx;
        if (log4j2ConfigURL == null) {
            logCtx = (LoggerContext) LogManager.getContext(false);
        } else {
            logCtx = (LoggerContext) LogManager.getContext(null, false, URI.create(log4j2ConfigURL.toString()));
        }
        logCtx.reconfigure();
        LOGGER.info(() -> "Using Log4J2 configuration at: " +
            logCtx.getConfiguration().getConfigurationSource().getLocation());
        MCRSessionMgr.addSessionListener(new MCRSessionThreadContext());
    }

}
