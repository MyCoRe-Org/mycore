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
import org.mycore.common.xsl.MCRXSLResourceHelper;
import org.mycore.datamodel.metadata.MCRXMLConstants;

/**
 * {@link URIResolver} that returns an XSL stylesheet containing include configured directives.
 * <p>Includes can be configured either as a comma-separated list of XSL filenames:
 * <pre>
 *   MCR.URIResolver.xslIncludes.{configId}=first.xsl,second.xsl
 * </pre>
 * or via a class implementing {@link MCRXslIncludeHrefs}:
 * <pre>
 *   MCR.URIResolver.xslIncludes.class.{configId}=org.foo.XSLHrefs
 * </pre>
 */
public class MCRXSLIncludeResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Resolves the configured XSL includes for the given config ID and returns them as
     * an XSL stylesheet source.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{configId}
     *   &lt;scheme&gt;:class.{configId}
     * </pre>
     * <p>Example request:
     * <pre>
     *   xslInclude:components
     *   xslInclude:class.template
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
     *     <xsl:include href="resource:xsl/first.xsl"/>
     *     <xsl:include href="resource:xsl/second.xsl"/>
     *   </xsl:stylesheet>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link JDOMSource} wrapping an {@code <xsl:stylesheet>} element containing
     *         the configured {@code <xsl:include>} directives
     */
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

        for (String include : propValue) {
            // create a new include element
            Element includeElement = new Element("include", xslNamespace);
            includeElement.setAttribute("href",
                include.contains(":") ? include : MCRXSLResourceHelper.getXSLResourceURI(include));
            root.addContent(includeElement);
            LOGGER.debug("Resolved XSL include: {}", include);
        }
        return new JDOMSource(root);
    }

    public interface MCRXslIncludeHrefs {
        List<String> getHrefs();
    }

}
