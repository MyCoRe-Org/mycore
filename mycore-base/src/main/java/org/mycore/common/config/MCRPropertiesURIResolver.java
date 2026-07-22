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

package org.mycore.common.config;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;

/**
 * {@link URIResolver} that returns configuration properties as an XML source.
 */
public class MCRPropertiesURIResolver implements URIResolver {

    /**
     * Resolves one or all properties matching the given key or key prefix.
     * <p>If a key prefix ending with {@code *} is given, all matching properties are returned
     * wrapped in a {@code <properties>} document. If no matches exist, an empty
     * {@code <properties>} element is returned.
     * If a plain key is given, a single {@code <entry>} element is returned. If the key does
     * not exist, the element has no text content.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{key}
     *   &lt;scheme&gt;:{key-prefix}*
     * </pre>
     * <p>Example request:
     * <pre>
     *   property:MCR.Metadata.Type.document
     *   property:MCR.Metadata.*
     * </pre>
     * <p>Example response for a key prefix:
     * <pre>{@code
     *   <!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
     *   <properties>
     *     <entry key="MCR.Metadata.Type.document">document</entry>
     *     <entry key="MCR.Metadata.Type.person">person</entry>
     *   </properties>
     * }</pre>
     * <p>Example response for a single key:
     * <pre>{@code
     *   <entry key="MCR.Metadata.Type.document">document</entry>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link JDOMSource} wrapping either a {@code <properties>} document or
     *         a single {@code <entry>} element
     */
    @Override
    public Source resolve(String href, String base) {
        String target = href.substring(href.indexOf(':') + 1);

        if (target.endsWith("*")) {
            return resolveKeyPrefix(target.substring(0, target.length() - 1));
        } else {
            return resolveKey(target);
        }
    }

    private JDOMSource resolveKeyPrefix(String keyPrefix) {
        final Element propertiesElement = new Element("properties");

        MCRConfiguration2.getSubPropertiesMap(keyPrefix).forEach((key, value) -> {
            final Element entryElement = new Element("entry");
            entryElement.setAttribute("key", keyPrefix + key);
            entryElement.setText(value);
            propertiesElement.addContent(entryElement);
        });

        return new JDOMSource(asPropertiesDocument(propertiesElement));
    }

    private Document asPropertiesDocument(Element propertiesElement) {
        final Document document = new Document();
        document.setDocType(getPropertiesDocType());
        document.setContent(propertiesElement);
        return document;
    }

    private DocType getPropertiesDocType() {
        return new DocType("properties", "SYSTEM", "http://java.sun.com/dtd/properties.dtd");
    }

    private JDOMSource resolveKey(String key) {
        final Element entryElement = new Element("entry");

        entryElement.setAttribute("key", key);
        String value = MCRConfiguration2.getRawPropertiesMap().get(key);
        if (value != null) {
            entryElement.setText(value);
        }

        return new JDOMSource(entryElement);
    }

}
