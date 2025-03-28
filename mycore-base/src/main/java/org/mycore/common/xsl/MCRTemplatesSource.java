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

import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRClassTools;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLResource;
import org.xml.sax.SAXException;

/**
 * Represents an XSL file that will be used in XSL transformation and which is loaded
 * as a resource. The object provides helper methods to support caching of the compiled 
 * templates file.
 * 
 * @author Thomas Scheffler (yagee) 
 * @author Frank Lützenkirchen
 */
public class MCRTemplatesSource {

    private static final Logger LOGGER = LogManager.getLogger();

    /** The path to the XSL resource */
    private String resource;

    /**
     * @param resource the path to the XSL file, which will be loaded as a resource
     */
    public MCRTemplatesSource(String resource) {
        this.resource = resource;
    }

    /** Have to use SAX here to resolve entities */
    public SAXSource getSource() throws SAXException, ParserConfigurationException {
        try {
            return (SAXSource) MCRURIResolver.obtainInstance().resolve("resource:" + resource, null);
        } catch (TransformerException e) {
            throw new SAXException(e);
        }
    }

    /** Returns the path to the XSL file, for use as a caching key */
    public String getKey() {
        return resource;
    }

    @Override
    public String toString() {
        return resource;
    }

    public URL getURL() {
        try {
            return MCRXMLResource.getInstance().getURL(resource, MCRClassTools.getClassLoader());
        } catch (IOException e) {
            LOGGER.warn("Could not determine URL of resource {}", resource, e);
            return null;
        }
    }

    /** Returns the timestamp the XSL file was last modified on the filesystem. */
    public long getLastModified() {
        try {
            return MCRXMLResource.getInstance().getLastModified(resource, MCRClassTools.getClassLoader());
        } catch (IOException e) {
            LOGGER.warn("Could not determine last modified date of resource {}", resource);
            return -1;
        }
    }

    public MCRCache.ModifiedHandle getModifiedHandle(long checkPeriod) {
        return MCRXMLResource.getInstance().getModifiedHandle(resource, MCRClassTools.getClassLoader(),
            checkPeriod);
    }
}
