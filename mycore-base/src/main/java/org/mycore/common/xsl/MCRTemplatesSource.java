/*
 * $Revision$ $Date$
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

package org.mycore.common.xsl;

import java.io.IOException;
import java.net.URL;

import javax.xml.transform.sax.SAXSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRCache;
import org.mycore.common.config.MCRConfigurationDir;
import org.mycore.common.xml.MCREntityResolver;
import org.mycore.common.xml.MCRXMLResource;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Represents an XSL file that will be used in XSL transformation and which is loaded
 * as a resource. The object provides helper methods to support caching of the compiled 
 * templates file.
 * 
 * @author Thomas Scheffler (yagee) 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRTemplatesSource {

    private final static Logger LOGGER = LogManager.getLogger(MCRTemplatesSource.class);

    /** The path to the XSL resource */
    private String resource;

    /**
     * @param resource the path to the XSL file, which will be loaded as a resource
     */
    public MCRTemplatesSource(String resource) {
        this.resource = resource;
    }

    /** Have to use SAX here to resolve entities */
    public SAXSource getSource() throws SAXException {
        XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.setEntityResolver(MCREntityResolver.instance());
        URL resourceURL = MCRConfigurationDir.getConfigResource(resource);
        if (resourceURL == null) {
            throw new SAXException("Could not find resource: " + resource);
        }
        InputSource input = new InputSource(resourceURL.toString());
        return new SAXSource(reader, input);
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
            return MCRXMLResource.instance().getURL(resource, MCRXSLTransformerFactory.class.getClassLoader());
        } catch (IOException e) {
            LOGGER.warn("Could not determine URL of resource " + resource, e);
            return null;
        }
    }

    /** Returns the timestamp the XSL file was last modified on the filesystem. */
    public long getLastModified() {
        try {
            return MCRXMLResource.instance().getLastModified(resource, MCRXSLTransformerFactory.class.getClassLoader());
        } catch (IOException e) {
            LOGGER.warn("Could not determine last modified date of resource " + resource);
            return -1;
        }
    }

    public MCRCache.ModifiedHandle getModifiedHandle(long checkPeriod) {
        return MCRXMLResource.instance().getModifiedHandle(resource, MCRXSLTransformerFactory.class.getClassLoader(),
            checkPeriod);
    }
}
