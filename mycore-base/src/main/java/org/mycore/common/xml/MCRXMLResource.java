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

package org.mycore.common.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRClassTools;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRURLContent;
import org.mycore.resource.MCRResourceHelper;

/**
 * Provides a cache for reading XML resources.
 * <p>
 * Cache size can be configured by property
 * <code>MCR.MCRXMLResource.Cache.Size</code> which defaults to <code>100</code>.
 *
 * @author Thomas Scheffler (yagee)
 */
public final class MCRXMLResource {

    private static final MCRCache<String, CacheEntry> RESOURCE_CACHE = new MCRCache<>(
        MCRConfiguration2.getInt("MCR.MCRXMLResource.Cache.Size").orElse(100),
        "XML resources");

    private static final Logger LOGGER = LogManager.getLogger();

    private MCRXMLResource() {
    }

    /**
     * @return singleton instance
     */
    public static MCRXMLResource getInstance() {
        return LazyInstanceHolder.SINGLETON_INSTANCE;
    }

    private static URLConnection getResourceURLConnection(String name, ClassLoader classLoader) throws IOException {
        URL url = MCRResourceHelper.getResourceUrl(name, classLoader);
        if (url == null) {
            return null;
        }
        return url.openConnection();
    }

    private static MCRContent getDocument(URL url) {
        return new MCRURLContent(url);
    }

    private static void closeURLConnection(URLConnection con) throws IOException {
        if (con == null) {
            return;
        }
        con.getInputStream().close();
    }

    public URL getURL(String name, ClassLoader classLoader) throws IOException {
        URLConnection con = getResourceURLConnection(name, classLoader);
        if (con == null) {
            return null;
        }
        try {
            return con.getURL();
        } finally {
            closeURLConnection(con);
        }
    }

    /**
     * Returns MCRContent using ClassLoader of MCRXMLResource class
     *
     * @param name
     *            resource name
     * @see MCRXMLResource#getResource(String, ClassLoader)
     */
    public MCRContent getResource(String name) throws IOException {
        return getResource(name, MCRClassTools.getClassLoader());
    }

    /**
     * returns xml as byte array using ClassLoader of MCRXMLResource class
     *
     * @param name
     *            resource name
     * @see MCRXMLResource#getRawResource(String, ClassLoader)
     */
    public byte[] getRawResource(String name) throws IOException {
        return getRawResource(name, MCRClassTools.getClassLoader());
    }

    /**
     * Returns MCRContent of resource.
     * <p>
     * A cache is used to avoid reparsing if the source of the resource did not
     * change.
     *
     * @param name
     *            the resource name
     * @return a parsed Document of the resource or <code>null</code> if the
     *         resource is not found
     * @throws IOException
     *             if resource cannot be loaded
     */
    public MCRContent getResource(String name, ClassLoader classLoader) throws IOException {
        ResourceModifiedHandle modifiedHandle = getModifiedHandle(name, classLoader, 10_000);
        CacheEntry entry = RESOURCE_CACHE.getIfUpToDate(name, modifiedHandle);
        URL resolvedURL = modifiedHandle.getURL();
        if (entry != null && (resolvedURL == null || entry.resourceURL.equals(resolvedURL))) {
            LOGGER.debug("Using cached resource {}", name);
            return entry.content;
        }
        if (resolvedURL == null) {
            LOGGER.warn("Could not resolve resource: {}", name);
            return null;
        }
        entry = new CacheEntry();
        RESOURCE_CACHE.put(name, entry);
        entry.resourceURL = resolvedURL;
        entry.content = getDocument(entry.resourceURL);
        return entry.content;
    }

    public ResourceModifiedHandle getModifiedHandle(String name, ClassLoader classLoader, long checkPeriod) {
        return new ResourceModifiedHandle(name, classLoader, checkPeriod);
    }

    /**
     * Returns raw XML resource as byte array. Note that no cache will be used.
     *
     * @param name
     *            the resource name
     * @param classLoader
     *            a ClassLoader that should be used to locate the resource
     * @return unparsed xml of the resource or <code>null</code> if the
     *         resource is not found
     * @throws IOException
     *             if resource cannot be loaded
     */
    public byte[] getRawResource(String name, ClassLoader classLoader) throws IOException {
        URLConnection con = getResourceURLConnection(name, classLoader);
        if (con == null) {
            return null;
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(64 * 1024);
            InputStream in = con.getInputStream()) {
            in.transferTo(baos);
            return baos.toByteArray();
        } finally {
            closeURLConnection(con);
        }
    }

    public long getLastModified(String name, ClassLoader classLoader) throws IOException {
        URLConnection con = getResourceURLConnection(name, classLoader);
        try {
            return con == null ? -1 : con.getLastModified();
        } finally {
            closeURLConnection(con);
        }
    }

    public boolean exists(String name, ClassLoader classLoader) throws IOException {
        final URLConnection resourceURLConnection = getResourceURLConnection(name, classLoader);
        try {
            return resourceURLConnection != null;
        } finally {
            closeURLConnection(resourceURLConnection);
        }
    }

    private static final class CacheEntry {
        URL resourceURL;

        MCRContent content;
    }

    public static class ResourceModifiedHandle implements MCRCache.ModifiedHandle {
        private long checkPeriod;

        private String name;

        private ClassLoader classLoader;

        private URL resolvedURL;

        public ResourceModifiedHandle(String name, ClassLoader classLoader, long checkPeriod) {
            this.name = name;
            this.classLoader = classLoader;
            this.checkPeriod = checkPeriod;
        }

        public URL getURL() {
            return this.resolvedURL == null ? MCRResourceHelper.getResourceUrl(name, classLoader) : this.resolvedURL;
        }

        @Override
        public long getCheckPeriod() {
            return checkPeriod;
        }

        @Override
        public long getLastModified() throws IOException {
            URLConnection con = getResourceURLConnection(name, classLoader);
            if (con == null) {
                return -1;
            }
            try {
                long lastModified = con.getLastModified();
                resolvedURL = con.getURL();
                LOGGER.debug("{} last modified: {}", name, lastModified);
                return lastModified;
            } finally {
                closeURLConnection(con);
            }
        }

    }

    private static final class LazyInstanceHolder {
        public static final MCRXMLResource SINGLETON_INSTANCE = new MCRXMLResource();
    }

}
