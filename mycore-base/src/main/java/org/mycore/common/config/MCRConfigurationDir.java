/*
 * $Id$
 * $Revision: 5697 $ $Date: Dec 18, 2013 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.common.config;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

import javax.servlet.ServletContext;

/**
 * This helper class determines in which directory to look for addition configuration files.
 * 
 * The directory path is build this way:
 * <ol>
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
 *  <li>System property <code>MCR.NameOfProject</code></li>
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

    private static ServletContext SERVLET_CONTEXT = null;

    private static String APP_NAME = null;

    private static File getMyCoReDirectory() {
        //Windows Vista onwards:
        String localAppData = isWindows() ? System.getenv("LOCALAPPDATA") : null;
        //on every other platform
        String userDir = System.getProperty("user.home");
        String parentDir = localAppData != null ? localAppData : userDir;
        return new File(parentDir, getConfigBaseName());
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
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
        if (!System.getProperties().keySet().contains(DISABLE_CONFIG_DIR_PROPERTY)) {
            return new File(getMyCoReDirectory(), getPrefix() + getAppName());
        }
        return null;
    }

    /**
     * Returns a File object, if {@link #getConfigurationDirectory()} does not return <code>null</code> and directory exists.
     * @param relativePath relative path to file or directory with configuration directory as base.
     * @return null if configuration directory does not exist or is disabled.
     */
    public static File getConfigFile(String relativePath) {
        File configurationDirectory = getConfigurationDirectory();
        if (configurationDirectory == null || !configurationDirectory.isDirectory()) {
            return null;
        }
        return new File(configurationDirectory, relativePath);
    }

}
