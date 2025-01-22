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
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Locale;

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

    public static final String DISABLE_CONFIG_DIR_PROPERTY = "MCR.DisableConfigDir";

    public static final String CONFIGURATION_DIRECTORY_PROPERTY = "MCR.ConfigDir";

    private static ServletContext servletContext;

    private static volatile String appName;

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
        if (appName == null) {
            synchronized (MCRConfigurationDir.class) {
                if (appName == null) {
                    appName = buildAppName();
                }
            }
        }
        return appName;
    }

    private static String buildAppName() {
        String appName = System.getProperty("MCR.AppName");
        String nameOfProject = System.getProperty("MCR.NameOfProject");
        if (appName == null) {
            if (nameOfProject != null) {
                appName = nameOfProject;
            } else {
                appName = (buildAppNameFromContext() != null) ? (buildAppNameFromContext())
                    : (buildAppNameFromFileName(getSource(MCRConfigurationDir.class)));
            }
        }
        return appName;
    }

    private static String buildAppNameFromContext() {
        String servletAppName = servletContext.getInitParameter("appName");
        String contextPath = servletContext.getContextPath();
        String servletContextName = servletContext.getServletContextName();
        String updatedAppname;
        if (servletAppName != null && !servletAppName.isEmpty()) {
            updatedAppname = servletAppName;
        } else if (!contextPath.isEmpty()) {
            updatedAppname = contextPath.substring(1);//remove leading '/'
        } else if (servletContextName != null && !(servletContextName.isBlank() || servletContextName.contains("/"))) {
            updatedAppname = servletContextName.replaceAll("\\s", "");
        } else {
            updatedAppname = null;
        }
        return updatedAppname;
    }

    private static String buildAppNameFromFileName(String sourceFileName) {
        String appName = sourceFileName;
        if (appName != null) {
            int beginIndex = appName.lastIndexOf('.') - 1;
            if (beginIndex > 0) {
                appName = sourceFileName.substring(0, beginIndex);
            }
            appName = appName.replaceAll("-\\d.*", "");//strips version
        } else {
            appName = "default";
        }
        return appName;
    }

    private static String getPrefix() {
        String dataPrefix = System.getProperty("MCR.DataPrefix");
        return dataPrefix == null ? "" : dataPrefix + "-";
    }

    static void setServletContext(ServletContext servletContext) {
        MCRConfigurationDir.servletContext = servletContext;
        appName = null;
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

}
