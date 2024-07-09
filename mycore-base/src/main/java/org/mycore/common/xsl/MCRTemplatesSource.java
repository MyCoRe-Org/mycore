/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRSourceContent;
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

    private static final Logger LOGGER = LogManager.getLogger(MCRTemplatesSource.class);

    private static final List<Namespace> MANAGED_NAMESPACES;

    static {
        List<Namespace> namespaces = new ArrayList<Namespace>();
        MCRConfiguration2.getSubPropertiesMap("MCR.Layout.NameSpaceManager.")
                .forEach((prefix, uri) -> {
                    namespaces.add(Namespace.getNamespace(prefix, uri));
                });
        MANAGED_NAMESPACES = namespaces;
    }

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
            MCRURIResolver instance = MCRURIResolver.instance();
            SAXSource resolved = (SAXSource) instance.resolve("resource:" + resource, null);
            if(MANAGED_NAMESPACES.isEmpty()) {
                return resolved;
            }

            Document build = new MCRSourceContent(resolved).asXML();
            Element root = build.getRootElement();

            MCRUtils.moveNamespacesUp(root);

            Attribute attribute = root.getAttribute("exclude-result-prefixes");
            Set<String> presentExcludedPrefixes = new HashSet<>();
            if(attribute != null && attribute.getValue() != null) {
                presentExcludedPrefixes.addAll(Stream.of(attribute.getValue().split(" ")).toList());
            }

            for (Namespace managedNamespace : MANAGED_NAMESPACES) {
                if( !root.getNamespacesIntroduced().contains(managedNamespace)) {
                    root.addNamespaceDeclaration(managedNamespace);
                }
                presentExcludedPrefixes.add(managedNamespace.getPrefix());
            }

            root.setAttribute("exclude-result-prefixes", String.join(" ", presentExcludedPrefixes));
            return new JDOMSource(build);
        } catch (TransformerException | JDOMException | IOException e) {
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
            return MCRXMLResource.instance().getURL(resource, MCRClassTools.getClassLoader());
        } catch (IOException e) {
            LOGGER.warn("Could not determine URL of resource {}", resource, e);
            return null;
        }
    }

    /** Returns the timestamp the XSL file was last modified on the filesystem. */
    public long getLastModified() {
        try {
            return MCRXMLResource.instance().getLastModified(resource, MCRClassTools.getClassLoader());
        } catch (IOException e) {
            LOGGER.warn("Could not determine last modified date of resource {}", resource);
            return -1;
        }
    }

    public MCRCache.ModifiedHandle getModifiedHandle(long checkPeriod) {
        return MCRXMLResource.instance().getModifiedHandle(resource, MCRClassTools.getClassLoader(),
            checkPeriod);
    }
}
