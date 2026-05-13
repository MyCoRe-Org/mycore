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

/**
 * Builds XML trees from a compact string representation.
 * <p>
 * Multiple XPath-like expressions can be separated using {@code &}.
 * <p>
 * Example request:
 * <pre>
 * buildxml:_rootName_=mycoreobject&amp;metadata/parents/parent/@href='FooBar_Document_4711'
 * </pre>
 * <p>
 * Example response:
 * <pre>{@code
 * <mycoreobject>
 *   <metadata>
 *     <parents>
 *       <parent href="FooBar_Document_4711" />
 *     </parents>
 *   </metadata>
 * </mycoreobject>
 * }</pre>
 */
public class MCRBuildXMLResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Builds a simple xml node tree on basis of name value pair
     */
    @Override
    public Source resolve(String href, String base) {
        String key = href.substring(href.indexOf(':') + 1);
        LOGGER.debug("Building xml from {}", key);

        Map<String, String> params = MCRURIResolverHelper.parseQueryParameters(href);

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
