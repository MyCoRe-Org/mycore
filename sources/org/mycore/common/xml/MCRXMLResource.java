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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;

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
     * returns document using ClassLoader of MCRXMLResource class
     * 
     * @param name
     *            resource name
     * @see MCRXMLResource#getResource(String, ClassLoader)
     */
    public Document getResource(String name) throws IOException, JDOMException {
        return getResource(name, this.getClass().getClassLoader());
    }

    /**
     * returns parsed XML resource as Document.
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
        LOGGER.debug("Reading xml from classpath resource " + name);
        URL url = classLoader.getResource(name);
        LOGGER.debug("Resource URL:" + url);
        if (url == null)
            return null;
        URLConnection con = url.openConnection();
        LOGGER.debug(name + " last modified: " + con.getLastModified());
        CacheEntry entry = (CacheEntry) resourceCache.getIfUpToDate(name, con.getLastModified());
        if (entry != null && entry.resourceURL.equals(url)) {
            LOGGER.debug("Using cached resource " + name);
            return entry.doc;
        }
        entry = new CacheEntry();
        resourceCache.put(name, entry);
        entry.resourceURL = url;
        Document doc = getDocument(con);
        entry.doc = doc;
        return entry.doc;
    }

    private Document getDocument(URLConnection con) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        builder.setValidation(false);
        builder.setEntityResolver(MCRURIResolver.instance());
        Document doc = builder.build(con.getInputStream());
        return doc;
    }

    private static class CacheEntry {
        URL resourceURL;

        Document doc;
    }

}
