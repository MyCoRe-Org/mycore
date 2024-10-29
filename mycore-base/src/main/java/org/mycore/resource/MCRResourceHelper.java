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

package org.mycore.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.mycore.common.MCRException;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.hint.MCRHintsBuilder;
import org.mycore.resource.hint.MCRResourceHintKeys;

/**
 * A utility class that simplifies the transition from 2023.05 and earlier versions of MyCoRe to the new
 * resource lookup strategy introduced in MCR-2881. Methods in this class use <code>null</code>-values
 * to communicate the absence of resource pointed to by {@link MCRResourcePath}, rather than using
 * {@link java.util.Optional} to accomplish that, as used in the API of {@link MCRResourceResolver}
 */
public final class MCRResourceHelper {

    private MCRResourceHelper() {
    }

    /**
     * Shorthand for {@link MCRResourceResolver#resolveResource(String)} returning <code>null</code>
     * instead of an empty {@link java.util.Optional}.
     */
    public static URL getResourceUrl(String path) {
        return MCRResourceResolver.instance().resolveResource(path).orElse(null);
    }

    /**
     * Shorthand for {@link MCRResourceResolver#resolveResource(String, MCRHints)}, using the given class loader,
     * returning <code>null</code> instead of an empty {@link java.util.Optional}.
     */
    public static URL getResourceUrl(String path, ClassLoader classLoader) {
        return MCRResourceResolver.instance().resolveResource(path, modifyHints(classLoader)).orElse(null);
    }

    /**
     * Shorthand for {@link MCRResourceResolver#resolveResource(String)} returning <code>null</code>
     * instead of an empty {@link java.util.Optional}, opening an {@link InputStream} on the resource, if present.
     */
    public static InputStream getResourceAsStream(String path) {
        return asStream(getResourceUrl(path));
    }

    /**
     * Shorthand for {@link MCRResourceResolver#resolveResource(String, MCRHints)}, using the given class loader,
     * returning <code>null</code> instead of an empty {@link java.util.Optional}, opening an {@link InputStream}
     * on the resource, if present.
     */
    public static InputStream getResourceAsStream(String path, ClassLoader classLoader) {
        return asStream(getResourceUrl(path, classLoader));
    }

    /**
     * Shorthand for {@link MCRResourceResolver#resolveWebResource(String)} returning <code>null</code>
     * instead of an empty {@link java.util.Optional}.
     */
    public static URL getWebResourceUrl(String path) {
        return MCRResourceResolver.instance().resolveWebResource(path).orElse(null);
    }

    /**
     * Shorthand for {@link MCRResourceResolver#resolveWebResource(String, MCRHints)}, using the given class loader,
     * returning <code>null</code> instead of an empty {@link java.util.Optional}.
     */
    public static URL getWebResourceUrl(String path, ClassLoader classLoader) {
        return MCRResourceResolver.instance().resolveWebResource(path, modifyHints(classLoader)).orElse(null);
    }

    /**
     * Shorthand for {@link MCRResourceResolver#resolveWebResource(String)} returning <code>null</code>
     * instead of an empty {@link java.util.Optional}, opening an {@link InputStream} on the resource, if present.
     */
    public static InputStream getWebResourceAsStream(String path) {
        return asStream(getWebResourceUrl(path));
    }

    /**
     * Shorthand for {@link MCRResourceResolver#resolveWebResource(String, MCRHints)}, using the given class loader,
     * returning <code>null</code> instead of an empty {@link java.util.Optional}, opening an {@link InputStream}
     * on the resource, if present.
     */
    public static InputStream getWebResourceAsStream(String path, ClassLoader classLoader) {
        return asStream(getWebResourceUrl(path, classLoader));
    }

    /**
     * Shorthand for {@link MCRResourceResolver#reverse(URL)} returning <code>null</code>
     * instead of an empty {@link java.util.Optional} and taking a String instead of an {@link URL} the .
     */
    public static MCRResourcePath getResourcePath(String resourceUrl) {
        try {
            return MCRResourceResolver.instance().reverse(new URI(resourceUrl).toURL()).orElse(null);
        } catch (URISyntaxException | MalformedURLException e) {
            throw new MCRException("Unable to convert string tu URL", e);
        }
    }

    private static MCRHints modifyHints(ClassLoader classLoader) {
        MCRHintsBuilder builder = MCRResourceResolver.instance().defaultHints().builder();
        builder.add(MCRResourceHintKeys.CLASS_LOADER, classLoader);
        return builder.build();
    }

    private static InputStream asStream(URL resourceUrl) {
        try {
            return resourceUrl == null ? null : resourceUrl.openStream();
        } catch (IOException e) {
            throw new MCRException(e);
        }
    }


}
