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

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRUtils;

/**
 * provides a cache for reading XML resources.
 * 
 * Cache size can be configured by property
 * <code>MCR.MCRXMLResouce.Cache.Size</code> which defaults to <code>100</code>.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRXMLResource {

    private static MCRCache resourceCache;

    private static MCRXMLResource instance;

    private static Logger LOGGER = Logger.getLogger(MCRXMLResource.class);

    private MCRXMLResource() {
        if (resourceCache == null) {
            resourceCache = new MCRCache(MCRConfiguration.instance().getInt("MCR.MCRXMLResouce.Cache.Size", 100), "XML resources");
        }
    }

    /**
     * @return singleton instance
     */
    public synchronized static MCRXMLResource instance() {
        if (instance == null)
            instance = new MCRXMLResource();
        return instance;
    }

    /**
     * Returns JDOM Document using ClassLoader of MCRXMLResource class
     * 
     * @param name
     *            resource name
     * @see MCRXMLResource#getResource(String, ClassLoader)
     */
    public Document getResource(String name) throws IOException, JDOMException {
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
     * Returns parsed XML resource as JDOM Document.
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
     * @throws JDOMException
     *             if resource cannot be parsed
     */
    public Document getResource(String name, ClassLoader classLoader) throws IOException, JDOMException {
        URLConnection con = getResourceURLConnection(name, classLoader);
        if (con == null)
            return null;
        LOGGER.debug(name + " last modified: " + con.getLastModified());
        CacheEntry entry = (CacheEntry) resourceCache.getIfUpToDate(name, con.getLastModified());
        if (entry != null && entry.resourceURL.equals(con.getURL())) {
            LOGGER.debug("Using cached resource " + name);
            return entry.doc;
        }
        entry = new CacheEntry();
        resourceCache.put(name, entry);
        entry.resourceURL = con.getURL();
        Document doc = getDocument(con);
        entry.doc = doc;
        return entry.doc;
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
        ByteArrayOutputStream baos;
        URLConnection con = getResourceURLConnection(name, classLoader);
        if (con == null)
            return null;
        baos = new ByteArrayOutputStream();
        InputStream in = new BufferedInputStream(con.getInputStream());
        try {
            MCRUtils.copyStream(in, baos);
            baos.close();
            return baos.toByteArray();
        } finally {
            in.close();
        }
    }

    private URLConnection getResourceURLConnection(String name, ClassLoader classLoader) throws IOException {
        LOGGER.debug("Reading xml from classpath resource " + name);
        URL url = classLoader.getResource(name);
        LOGGER.debug("Resource URL:" + url);
        if (url == null)
            return null;
        URLConnection con = url.openConnection();
        return con;
    }

    private Document getDocument(URLConnection con) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        builder.setValidation(false);
        builder.setEntityResolver(MCRURIResolver.instance());
        InputStream in = con.getInputStream();
        try {
            Document doc = builder.build(in);
            return doc;
        } finally {
            in.close();
        }
    }

    public long getLastModified(String name, ClassLoader classLoader) throws IOException {
        URLConnection con = getResourceURLConnection(name, classLoader);
        return con == null ? -1 : con.getLastModified();
    }

    public boolean exists(String name, ClassLoader classLoader) throws IOException {
        return getResourceURLConnection(name, classLoader) != null;
    }

    private static class CacheEntry {
        URL resourceURL;

        Document doc;
    }
}
