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

import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRConstants;
import org.mycore.common.xsl.uriresolver.MCRURIResolverHelper;

/**
 * {@link URIResolver} that dynamically builds an XML element tree from URI query parameters.
 */
public class MCRBuildXMLURIResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Resolves the given URI and constructs an XML element tree from its query parameters.
     * <p>Each query parameter is interpreted as an XPath-like path/value pair and mapped
     * onto the resulting XML tree. Intermediate elements are created as needed. Attribute
     * nodes are created when a path segment starts with {@code @}. Namespace prefixes are
     * resolved via {@link MCRConstants}; unknown prefixes default to no namespace.
     * <p>An optional {@code _rootName_} parameter sets the name (and namespace) of the
     * root element. If omitted, {@code <root>} is used. If no {@code _rootName_} is given
     * and more than one top-level child would result, only the first top-level child
     * element is returned and a warning is logged.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:[_rootName_=&lt;element&gt;&amp;]&lt;xpath&gt;=&lt;value&gt;[&amp;...]
     * </pre>
     * <p>Example request:
     * <pre>
     *   buildxml:_rootName_=mods:mods&amp;mods:title=My+Document&amp;mods:date/@type=issued
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <mods:mods xmlns:mods="http://www.loc.gov/mods/v3">
     *     <mods:title>My Document</mods:title>
     *     <mods:date type="issued"/>
     *   </mods:mods>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link JDOMSource} wrapping the constructed root element
     */
    @Override
    public Source resolve(String href, String base) {
        String key = href.substring(href.indexOf(':') + 1);
        LOGGER.debug("Building xml from {}", key);

        Map<String, String> params = MCRURIResolverHelper.parseQueryParameters(key);

        Element defaultRoot = new Element("root");
        Element root = defaultRoot;
        String rootName = params.get("_rootName_");
        if (rootName != null) {
            root = new Element(getLocalName(rootName), getNamespace(rootName));
            params.remove("_rootName_");
        }

        for (Map.Entry<String, String> entry : params.entrySet()) {
            constructElement(root, entry.getKey(), entry.getValue());
        }
        if (root.equals(defaultRoot) && root.getChildren().size() > 1) {
            LOGGER.warn("More than 1 root node defined, returning first");
            return new JDOMSource(root.getChildren().getFirst().detach());
        }
        return new JDOMSource(root);
    }

    private static void constructElement(Element current, String xpath, String value) {
        StringTokenizer st = new StringTokenizer(xpath, "/");
        Element currentToken = current;
        String name = null;
        while (st.hasMoreTokens()) {
            name = st.nextToken();
            if (name.startsWith("@")) {
                break;
            }
            String localName = getLocalName(name);
            Namespace namespace = getNamespace(name);

            Element child = currentToken.getChild(localName, namespace);
            if (child == null) {
                child = new Element(localName, namespace);
                currentToken.addContent(child);
            }
            currentToken = child;
        }
        if (name.startsWith("@")) {
            name = name.substring(1);
            String localName = getLocalName(name);
            Namespace namespace = getNamespace(name);
            currentToken.setAttribute(localName, value, namespace);
        } else {
            currentToken.setText(value);
        }
    }

    private static Namespace getNamespace(String name) {
        if (!name.contains(":")) {
            return Namespace.NO_NAMESPACE;
        }
        String prefix = name.split(":")[0];
        Namespace ns = MCRConstants.getStandardNamespace(prefix);
        return ns == null ? Namespace.NO_NAMESPACE : ns;
    }

    private static String getLocalName(String name) {
        if (!name.contains(":")) {
            return name;
        } else {
            return name.split(":")[1];
        }
    }

}
