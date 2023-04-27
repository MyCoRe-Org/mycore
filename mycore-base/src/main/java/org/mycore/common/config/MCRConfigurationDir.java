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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRDeveloperTools;
import org.mycore.common.MCRUtils;

import jakarta.servlet.ServletContext;

/**
 * This helper class determines in which directory to look for addition configuration files.
 * <p>
 * The configuration directory can be set with the system property or environment variable <code>MCR.ConfigDir</code>.
 * <p>
 * The directory path is build this way:
 * <ol>
 *  <li>System property <code>MCR.Home</code> defined
 *      <ol>
 *          <li><code>System.getProperty("MCR.Home")</code></li>
 *          <li><code>{prefix+'-'}{appName}</code></li>
 *      </ol>
 *  </li>
 *  <li>Windows:
 *      <ol>
 *          <li><code>%LOCALAPPDATA%</code></li>
 *          <li>MyCoRe</li>
 *          <li><code>{prefix+'-'}{appName}</code></li>
 *      </ol>
 *  </li>
 *  <li>other systems
 *      <ol>
 *          <li><code>$HOME</code></li>
 *          <li>.mycore</li>
 *          <li><code>{prefix+'-'}{appName}</code></li>
 *      </ol>
 *  </li>
 * </ol>
 * 
 * <code>{prefix}</code> can be defined by setting System property <code>MCR.DataPrefix</code>.
 * <code>{appName}</code> is always lowercase String determined using this
 * <ol>
 *  <li>System property <code>MCR.AppName</code></li>
 *  <li>System property <code>MCR.NameOfProject</code></li>
 *  <li>Servlet Context Init Parameter <code>appName</code>
 *  <li>Servlet Context Path (if not root context, {@link ServletContext#getContextPath()})</li>
 *  <li>Servlet Context Name ({@link ServletContext#getServletContextName()}) with space characters removed</li>
 *  <li>base name of jar including this class</li>
 *  <li>the String <code>"default"</code></li>
 * </ol>
 * 
 * @author Thomas Scheffler (yagee)
 * @see System#getProperties()
 * @since 2013.12
 */
public class MCRConfigurationDir {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String DISABLE_CONFIG_DIR_PROPERTY = "MCR.DisableConfigDir";

    public static final String CONFIGURATION_DIRECTORY_PROPERTY = "MCR.ConfigDir";

    private static ServletContext SERVLET_CONTEXT = null;

    private static String APP_NAME = null;

    private static File getMyCoReDirectory() {
        String mcrHome = System.getProperty("MCR.Home");
        if (mcrHome != null) {
            return new File(mcrHome);
        }
        //Windows Vista onwards:
        String localAppData = isWindows() ? System.getenv("LOCALAPPDATA") : null;
        //on every other platform
        String userDir = System.getProperty("user.home");
        String parentDir = localAppData != null ? localAppData : userDir;
        return new File(parentDir, getConfigBaseName());
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows");
    }

    private static String getConfigBaseName() {
        return isWindows() ? "MyCoRe" : ".mycore";
    }

    private static String getSource(Class<MCRConfigurationDir> clazz) {
        if (clazz == null) {
            return null;
        }
        ProtectionDomain protectionDomain = clazz.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        if (codeSource == null) {
            System.err.println("Cannot get CodeSource.");
            return null;
        }
        URL location = codeSource.getLocation();
        String fileName = location.getFile();
        File sourceFile = new File(fileName);
        return sourceFile.getName();
    }

    private static String getAppName() {
        if (APP_NAME == null) {
            APP_NAME = buildAppName();
        }
        return APP_NAME;
    }

    private static String buildAppName() {
        String appName = System.getProperty("MCR.AppName");
        if (appName != null) {
            return appName;
        }
        String nameOfProject = System.getProperty("MCR.NameOfProject");
        if (nameOfProject != null) {
            return nameOfProject;
        }
        if (SERVLET_CONTEXT != null) {
            String servletAppName = SERVLET_CONTEXT.getInitParameter("appName");
            if (servletAppName != null && !servletAppName.isEmpty()) {
                return servletAppName;
            }
            String contextPath = SERVLET_CONTEXT.getContextPath();
            if (!contextPath.isEmpty()) {
                return contextPath.substring(1);//remove leading '/'
            }
            String servletContextName = SERVLET_CONTEXT.getServletContextName();
            if (servletContextName != null
                && !(servletContextName.trim().isEmpty() || servletContextName.contains("/"))) {
                return servletContextName.replaceAll("\\s", "");
            }
        }
        String sourceFileName = getSource(MCRConfigurationDir.class);
        if (sourceFileName != null) {
            int beginIndex = sourceFileName.lastIndexOf('.') - 1;
            if (beginIndex > 0) {
                sourceFileName = sourceFileName.substring(0, beginIndex);
            }
            return sourceFileName.replaceAll("-\\d.*", "");//strips version 
        }
        return "default";
    }

    private static String getPrefix() {
        String dataPrefix = System.getProperty("MCR.DataPrefix");
        return dataPrefix == null ? "" : dataPrefix + "-";
    }

    static void setServletContext(ServletContext servletContext) {
        SERVLET_CONTEXT = servletContext;
        APP_NAME = null;
    }

    /**
     * Returns the configuration directory for this MyCoRe instance.
     * @return null if System property {@value #DISABLE_CONFIG_DIR_PROPERTY} is set.
     */
    public static File getConfigurationDirectory() {
        if (System.getProperties().containsKey(CONFIGURATION_DIRECTORY_PROPERTY)) {
            return new File(System.getProperties().getProperty(CONFIGURATION_DIRECTORY_PROPERTY));
        }
        if (System.getenv().containsKey(CONFIGURATION_DIRECTORY_PROPERTY)) {
            return new File(System.getenv(CONFIGURATION_DIRECTORY_PROPERTY));
        }
        if (!System.getProperties().containsKey(DISABLE_CONFIG_DIR_PROPERTY)) {
            return new File(getMyCoReDirectory(), getPrefix() + getAppName());
        }
        return null;
    }

    /**
     * Returns a File object, if {@link #getConfigurationDirectory()} does not return <code>null</code>
     * and directory exists.
     * @param relativePath relative path to file or directory with configuration directory as base.
     * @return null if configuration directory does not exist or is disabled.
     */
    public static File getConfigFile(String relativePath) {
        File configurationDirectory = getConfigurationDirectory();
        if (configurationDirectory == null || !configurationDirectory.isDirectory()) {
            return null;
        }

        return MCRUtils.safeResolve(configurationDirectory.toPath(), relativePath).toFile();
    }

    /**
     * Returns URL of a config resource.
     * Same as {@link #getConfigResource(String, ClassLoader)} with second argument <code>null</code>.
     * @param relativePath as defined in {@link #getConfigFile(String)}
     */
    public static URL getConfigResource(String relativePath) {
        return getConfigResource(relativePath, null);
    }

    /**
     * Returns URL of a config resource.
     * If {@link #getConfigFile(String)} returns an existing file for "resources"+{relativePath}, its URL is returned.
     * In any other case this method returns the same as {@link ClassLoader#getResource(String)} 
     * @param relativePath as defined in {@link #getConfigFile(String)}
     * @param classLoader a classLoader to resolve the resource (see above), null defaults to this class' class loader
     */
    public static URL getConfigResource(String relativePath, ClassLoader classLoader) {
        if (MCRDeveloperTools.overrideActive()) {
            final Optional<Path> overriddenFilePath = MCRDeveloperTools.getOverriddenFilePath(relativePath, false);
            if (overriddenFilePath.isPresent()) {
                try {
                    return overriddenFilePath.get().toUri().toURL();
                } catch (MalformedURLException e) {
                    // Ignore
                }
            }
        }

        File resolvedFile = getConfigFile("resources/" + relativePath);
        if (resolvedFile != null && resolvedFile.exists()) {
            try {
                return resolvedFile.toURI().toURL();
            } catch (MalformedURLException e) {
                LOGGER.warn("Exception while returning URL for file: {}", resolvedFile, e);
            }
        }
        return getClassPathResource(relativePath, classLoader == null ? MCRClassTools.getClassLoader() : classLoader);
    }

    private static URL getClassPathResource(String relativePath, ClassLoader classLoader) {

        List<URL> possibleResourceUrls = getPossibleResources(relativePath, classLoader);
        if (possibleResourceUrls.isEmpty()) {
            return null;
        } else if (possibleResourceUrls.size() == 1) {
            return possibleResourceUrls.get(0);
        }

        //configuration directory always wins
        File configDir = getConfigurationDirectory();
        if (configDir != null) {
            String configDirUrl = configDir.toURI().toString();
            List<URL> configDirResourceUrls = possibleResourceUrls
                .stream()
                .filter(url -> url.toString().contains(configDirUrl))
                .collect(Collectors.toList());
            if (!configDirResourceUrls.isEmpty()) {
                return getHighestPriorityResource(configDirResourceUrls);
            }
        }

        // local files should generally win
        URL localResourceUrl = possibleResourceUrls
            .stream()
            .filter(url -> url.toString().startsWith("file:"))
            .findFirst()
            .orElse(null);
        if (localResourceUrl != null) {
            return localResourceUrl;
        }

        return getHighestPriorityResource(possibleResourceUrls);

    }

    private static List<URL> getPossibleResources(String relativePath, ClassLoader classLoader) {
        List<URL> urls = new LinkedList<>();
        try {
            Enumeration<URL> resourceUrls = classLoader.getResources(relativePath);
            while (resourceUrls.hasMoreElements()) {
                urls.add(resourceUrls.nextElement());
            }
        } catch (IOException e) {
            LOGGER.error("Error while retrieving resource: {}", relativePath, e);
        }
        return urls;
    }

    private static URL getHighestPriorityResource(List<URL> resourceUrls) {
        List<URL> highestModulePriorityResources = getHighestModulePriorityResources(resourceUrls);
        if (!highestModulePriorityResources.isEmpty()) {
            return highestModulePriorityResources.get(0);
        }
        Optional<URL> highestLibraryPriorityResource = getHighestLibraryPriorityResource(resourceUrls);
        if (highestLibraryPriorityResource.isPresent()) {
            return highestLibraryPriorityResource.get();
        }
        return resourceUrls.get(0);
    }

    /**
     * Out of a list of possible resource URLs, this method returns the sub-list of resource URLs belonging
     * to the set of components with the highest module priority, out of the components that contain any of the
     * resource URLs.
     * <p>
     * Example:<br>
     * component A with priority 90 doesn't contain any of the resource URLs<br>
     * component B with priority 80 contains resource URL 3<br>
     * component C with priority 80 contains resource URL 2<br>
     * component D with priority 70 contains resource URL 1<br>
     * component E with priority 70 doesn't contain any of the resource URLs<br>
     * In this case, resource URLs 3 and 2, belonging to the components with priority 80, would be returned.
     * <p>
     * To facilitate this, the ordered list of components is iterated. For each component, the list of resource
     * URIs is searched for a resource URI belonging to that component. As soon as the first component with a matching
     * resource URI is found, the priority of that component is saved. From this pont on, only components with the same
     * priority are considered. As soon as a component with a lower priority is encountered, further processing is
     * aborted.
     * <p>
     * If no component contains any of the resource URIs, an empty list is returned.
     *
     * @param resourceUrls list of possible resource URLs
     * @return list of resource URLs belonging to components with the highest module priority
     */
    private static List<URL> getHighestModulePriorityResources(List<URL> resourceUrls) {
        int highestPriority = -1;
        List<URL> unmatchedResourceUrls = new ArrayList<>(resourceUrls);
        List<URL> highestPriorityModuleResourceUrls = new LinkedList<>();
        for (MCRComponent component : componentsByModulePriority()) {
            if (highestPriority != -1 && component.getPriority() != highestPriority) {
                break;
            }
            String componentJarFileUri = component.getJarFile().toURI().toString();
            for (URL resourceUrl : unmatchedResourceUrls) {
                if (resourceUrl.toString().contains(componentJarFileUri)) {
                    highestPriorityModuleResourceUrls.add(resourceUrl);
                    unmatchedResourceUrls.remove(resourceUrl);
                    highestPriority = component.getPriority();
                    break;
                }
            }
        }
        return highestPriorityModuleResourceUrls;
    }

    private static List<MCRComponent> componentsByModulePriority() {
        List<MCRComponent> components = new ArrayList<>(MCRRuntimeComponentDetector.getAllComponents());
        Collections.reverse(components);
        return components;
    }

    /**
     * Out of a list of possible resource URLs, this method returns the resource URL belonging
     * to the library with the highest servlet context priority.
     * <p>
     * To facilitate this, the ordered list of libraries, if available, is iterated. For each library, the list of
     * resource URIs is searched for a resource URI belonging to that library. As soon as the first library with a
     * matching resource URI is found, that resource URI is returned.
     * <p>
     * If no library contains any of the resource URIs, empty is returned.
     *
     * @param resourceUrls list of possible resource URLs
     * @return resource URLs belonging to library with the highest servlet context priority
     */
    private static Optional<URL> getHighestLibraryPriorityResource(List<URL> resourceUrls) {
        for (String library : librariesByServletContextOrder()) {
            for (URL resourceUrl : resourceUrls) {
                if (resourceUrl.toString().contains(library)) {
                    return Optional.of(resourceUrl);
                }
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private static List<String> librariesByServletContextOrder() {
        if (SERVLET_CONTEXT != null) {
            return (List<String>) SERVLET_CONTEXT.getAttribute(ServletContext.ORDERED_LIBS);
        } else {
            return Collections.emptyList();
        }
    }


}
