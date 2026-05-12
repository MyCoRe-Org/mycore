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

package org.mycore.common.xsl.uriresolver;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRXMLConstants;

/**
 * <p>
 * Includes xsl files which are set in the mycore.properties file.
 * </p>
 * Example: MCR.URIResolver.xslIncludes.components=iview.xsl,wcms.xsl
 * <p>
 * Or retrieve the include hrefs from a class implementing
 * {@link org.mycore.common.xsl.uriresolver.MCRXslIncludeResolver.MCRXslIncludeHrefs}.
 * The class. part have to be set, everything after * class. Can be freely chosen.
 * </p>
 * Example: MCR.URIResolver.xslIncludes.class.template=org.foo.XSLHrefs
 * <p>
 * Returns a xsl file with the includes as href.
 */
public class MCRXslIncludeResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public Source resolve(String href, String base) {
        String includePart = href.substring(href.indexOf(':') + 1);
        Namespace xslNamespace = Namespace.getNamespace("xsl", "http://www.w3.org/1999/XSL/Transform");

        Element root = new Element("stylesheet", xslNamespace);
        root.setAttribute(MCRXMLConstants.VERSION, "1.0");

        // get the parameters from mycore.properties
        String propertyName = "MCR.URIResolver.xslIncludes." + includePart;
        List<String> propValue;
        if (includePart.startsWith("class.")) {
            MCRXslIncludeHrefs incHrefClass = MCRConfiguration2.getInstanceOfOrThrow(
                MCRXslIncludeHrefs.class, propertyName);
            propValue = incHrefClass.getHrefs();
        } else {
            propValue = MCRConfiguration2.getString(propertyName)
                .map(MCRConfiguration2::splitValue)
                .map(s -> s.collect(Collectors.toList()))
                .orElseGet(Collections::emptyList);
        }

        final String xslFolder = MCRConfiguration2.getStringOrThrow(MCRURIResolver.PROPERTY_XSL_FOLDER);
        for (String include : propValue) {
            // create a new include element
            Element includeElement = new Element("include", xslNamespace);
            includeElement.setAttribute("href",
                include.contains(":") ? include : MCRURIResolver.RESOURCE_PREFIX + xslFolder + "/" + include);
            root.addContent(includeElement);
            LOGGER.debug("Resolved XSL include: {}", include);
        }
        return new JDOMSource(root);
    }

    public interface MCRXslIncludeHrefs {
        List<String> getHrefs();
    }

}
