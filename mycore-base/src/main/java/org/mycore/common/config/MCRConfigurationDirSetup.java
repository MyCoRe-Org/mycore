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

package org.mycore.common.config;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.web.Log4jServletContainerInitializer;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.events.MCRStartupHandler;
import org.mycore.common.events.MCRStartupHandler.AutoExecutable;
import org.mycore.common.log4j2.MCRSessionThreadContext;

/**
 * Called by {@link MCRStartupHandler} on start up to setup {@link MCRConfiguration}.
 * 
 * @author Thomas Scheffler (yagee)
 * @since 2013.12
 */
public class MCRConfigurationDirSetup implements AutoExecutable {

    private static final StatusLogger LOGGER = StatusLogger.getLogger();

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
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#startUp(javax.servlet.ServletContext)
     */
    @Override
    public void startUp(ServletContext servletContext) {
        MCRConfigurationDir.setServletContext(servletContext);
        loadExternalLibs();
        MCRConfigurationLoader configurationLoader = MCRConfigurationLoaderFactory.getConfigurationLoader();
        Map<String, String> properties = configurationLoader.load();
        MCRConfigurationBase.initialize(properties, true);
        if (servletContext != null) {
            Log4jServletContainerInitializer log4jInitializer = new Log4jServletContainerInitializer();
            try {
                log4jInitializer.onStartup(null, servletContext);
            } catch (ServletException e) {
                System.err.println("Could not start Log4J2 context");
            }
        }
        String configFileKey = "log4j.configurationFile";
        URL log4j2ConfigURL = null;
        if (System.getProperty(configFileKey) == null) {
            log4j2ConfigURL = MCRConfigurationDir.getConfigResource("log4j2.xml");
        }
        LoggerContext logCtx;
        if (log4j2ConfigURL == null) {
            logCtx = (LoggerContext) LogManager.getContext(false);
        } else {
            logCtx = (LoggerContext) LogManager.getContext(null, false, URI.create(log4j2ConfigURL.toString()));
        }
        logCtx.reconfigure();
        System.out.printf(Locale.ROOT, "Using Log4J2 configuration at: %s%n",
            logCtx.getConfiguration().getConfigurationSource().getLocation());
        MCRSessionMgr.addSessionListener(new MCRSessionThreadContext());
    }

    public static void loadExternalLibs() {
        File resourceDir = MCRConfigurationDir.getConfigFile("resources");
        if (resourceDir == null) {
            //no configuration dir exists
            return;
        }
        ClassLoader classLoader = MCRConfigurationDir.class.getClassLoader();
        if (!(classLoader instanceof URLClassLoader)) {
            System.err.println(classLoader.getClass() + " is unsupported for adding extending CLASSPATH at runtime.");
            return;
        }
        File libDir = MCRConfigurationDir.getConfigFile("lib");
        Set<URL> currentCPElements = Stream.of(((URLClassLoader) classLoader).getURLs()).collect(Collectors.toSet());
        Class<? extends ClassLoader> classLoaderClass = classLoader.getClass();
        try {
            Method addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addUrlMethod.setAccessible(true);
            getFileStream(resourceDir, libDir)
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
                .forEach(u -> {
                    System.out.println("Adding to CLASSPATH: " + u);
                    try {
                        addUrlMethod.invoke(classLoader, u);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        LOGGER.error("Could not add {} to current classloader.", u, e);
                    }
                });
        } catch (NoSuchMethodException | SecurityException e) {
            LogManager.getLogger(MCRConfigurationInputStream.class)
                .warn("{} does not support adding additional JARs at runtime", classLoaderClass, e);
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
}
