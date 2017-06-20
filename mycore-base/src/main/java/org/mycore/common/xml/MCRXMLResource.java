/**
 * $RCSfile: MCRXMLResource.java,v $
 * $Revision: 1.0 $ $Date: 01.07.2008 06:12:32 $
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.common.xml;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.common.MCRCache;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationDir;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRURLContent;

/**
 * provides a cache for reading XML resources.
 * 
 * Cache size can be configured by property
 * <code>MCR.MCRXMLResouce.Cache.Size</code> which defaults to <code>100</code>.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRXMLResource {

    private volatile static MCRCache<String, CacheEntry> resourceCache;

    private static MCRXMLResource instance = new MCRXMLResource();

    private static Logger LOGGER = LogManager.getLogger(MCRXMLResource.class);

    private MCRXMLResource() {
        resourceCache = new MCRCache<String, CacheEntry>(MCRConfiguration.instance().getInt(
            "MCR.MCRXMLResource.Cache.Size", 100), "XML resources");
    }

    /**
     * @return singleton instance
     */
    public static MCRXMLResource instance() {
        return instance;
    }

    public URL getURL(String name) throws IOException {
        return getURL(name, MCRXMLResource.class.getClassLoader());
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
    public MCRContent getResource(String name) throws IOException, JDOMException {
        return getResource(name, this.getClass().getClassLoader());
    }

    /**
     * returns xml as byte array using ClassLoader of MCRXMLResource class
     * 
     * @param name
     *            resource name
     * @see MCRXMLResource#getRawResource(String, ClassLoader)
     */
    public byte[] getRawResource(String name) throws IOException {
        return getRawResource(name, this.getClass().getClassLoader());
    }

    /**
     * Returns MCRContent of resource.
     * 
     * A cache is used to avoid reparsing if the source of the resource did not
     * change.
     * 
     * @param name
     *            the resource name
     * @param classLoader
     *            a ClassLoader that should be used to locate the resource
     * @return a parsed Document of the resource or <code>null</code> if the
     *         resource is not found
     * @throws IOException
     *             if resource cannot be loaded
     */
    public MCRContent getResource(String name, ClassLoader classLoader) throws IOException {
        ResourceModifiedHandle modifiedHandle = getModifiedHandle(name, classLoader, 10000);
        CacheEntry entry = resourceCache.getIfUpToDate(name, modifiedHandle);
        URL resolvedURL = modifiedHandle.getURL();
        if (entry != null && (resolvedURL == null || entry.resourceURL.equals(resolvedURL))) {
            LOGGER.debug("Using cached resource " + name);
            return entry.content;
        }
        if (resolvedURL == null) {
            LOGGER.warn("Could not resolve resource: " + name);
            return null;
        }
        entry = new CacheEntry();
        resourceCache.put(name, entry);
        entry.resourceURL = resolvedURL;
        entry.content = getDocument(entry.resourceURL);
        return entry.content;
    }

    public ResourceModifiedHandle getModifiedHandle(String name, ClassLoader classLoader, long checkPeriod) {
        ResourceModifiedHandle modifiedHandle = new ResourceModifiedHandle(name, classLoader, checkPeriod);
        return modifiedHandle;
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
            InputStream in = new BufferedInputStream(con.getInputStream())) {
            IOUtils.copy(in, baos);
            return baos.toByteArray();
        } finally {
            closeURLConnection(con);
        }
    }

    private static URLConnection getResourceURLConnection(String name, ClassLoader classLoader) throws IOException {
        LOGGER.debug("Reading xml from classpath resource " + name);
        URL url = MCRConfigurationDir.getConfigResource(name, classLoader);
        LOGGER.debug("Resource URL:" + url);
        if (url == null) {
            return null;
        }
        URLConnection con = url.openConnection();
        return con;
    }

    private static MCRContent getDocument(URL url) {
        MCRContent content = new MCRURLContent(url);
        return content;
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

    private static class CacheEntry {
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
            return this.resolvedURL == null ? MCRConfigurationDir.getConfigResource(name, classLoader)
                : this.resolvedURL;
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
                LOGGER.debug(name + " last modified: " + lastModified);
                return lastModified;
            } finally {
                closeURLConnection(con);
            }
        }

    }

    private static void closeURLConnection(URLConnection con) throws IOException {
        if (con == null) {
            return;
        }
        con.getInputStream().close();
    }
}
