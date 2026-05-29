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

package org.mycore.common.xsl;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.resource.MCRResourceHelper;
import org.mycore.resource.MCRResourcePath;

/**
 * Utility methods for resolving XSL stylesheet resource paths and URIs.
 */
public final class MCRXSLResourceHelper {

    /**
     * Prefix for resource URIs used by the URI resolver.
     */
    public static final String RESOURCE_PREFIX = "resource:";

    private MCRXSLResourceHelper() {
    }

    /**
     * Returns the configured XSL stylesheet folder.
     *
     * @return the XSL folder path as configured via
     *         {@code MCR.Layout.Transformer.Factory.XSLFolder}
     * @throws org.mycore.common.config.MCRConfigurationException
     *         if the property is not set
     */
    public static String getXSLFolder() {
        return MCRConfiguration2.getStringOrThrow("MCR.Layout.Transformer.Factory.XSLFolder");
    }

    /**
     * Returns the full resource URI for a given path,
     * relative to the configured XSL folder.
     *
     * @param path the filename or relative path within the XSL folder
     * @return the full resource URI
     */
    public static String getXSLResourceURI(String path) {
        return RESOURCE_PREFIX + getXSLFolder() + "/" + path;
    }

    /**
     * Returns the resource URI to the directory of the XSL file identified by {@code uri}.
     *
     * <p>If {@code uri} is {@code null}, the configured XSL folder
     * (see {@link #getXSLFolder()}) is returned as the default directory.
     *
     * <p>Otherwise, the parent directory of {@code uri} is resolved via
     * {@link MCRResourceHelper#getResourcePath(String)}.
     *
     * @param uri the resource URI of the current XSL file, or {@code null}
     *            if no base is available
     * @return the resource URI to the directory, or {@code null}
     *         if {@code uri} cannot be resolved to a resource path
     */
    public static String getXSLDirectory(String uri) {
        if (uri == null) {
            // the file was not included from another file, so we need to use the default resource directory
            return getXSLResourceURI("");
        } else {
            String resolvingBase = null;
            MCRResourcePath resourcePath = MCRResourceHelper.getResourcePath(uri);
            if (resourcePath != null) {
                String path = resourcePath.asRelativePath();
                resolvingBase = RESOURCE_PREFIX + path.substring(0, path.lastIndexOf('/') + 1);
            }

            return resolvingBase;
        }
    }

}
