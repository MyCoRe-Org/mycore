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

package org.mycore.frontend;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPathExpressionException;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRException;

/**
 * {@link URIResolver} that provides layout utility functions such as read access checks
 * and personal navigation.
 */
public class MCRLayoutUtilsURIResolver implements URIResolver {

    /**
     * Resolves the given URI by invoking the specified layout utility function.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:readAccess:{webpageID}
     *   &lt;scheme&gt;:readAccess:{webpageID}:split:{blockerWebpageID}
     *   &lt;scheme&gt;:personalNavigation
     * </pre>
     * <p>Example request:
     * <pre>
     *   layoutUtils:readAccess:content/below/index.xml
     *   layoutUtils:readAccess:content/page.xml:split:content/blocker.xml
     *   layoutUtils:personalNavigation
     * </pre>
     * <p>Example response for {@code readAccess}:
     * <pre>{@code
     *   <true/>
     * }</pre>
     * <p>Example response for {@code personalNavigation}: the personal navigation DOM subtree.
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link Source} wrapping the function result
     * @throws TransformerException if no arguments are given, the function is unknown,
     *                              or the personal navigation cannot be loaded
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String[] args = href.split(":", 3);

        if (args.length < 2) {
            throw new TransformerException("No arguments given");
        }
        String function = args[1];

        if (function.equals("readAccess")) {
            String[] params = args[2].split(":split:");
            if (params.length == 1) {
                return new JDOMSource(new Element(String.valueOf(MCRLayoutUtilities.readAccess(params[0]))));
            } else if (params.length == 2) {
                return new JDOMSource(
                    new Element(String.valueOf(MCRLayoutUtilities.readAccess(params[0], params[1]))));
            }
        } else if (function.equals("personalNavigation")) {
            try {
                return new DOMSource(MCRLayoutUtilities.getPersonalNavigation());
            } catch (JDOMException | XPathExpressionException e) {
                throw new MCRException("Error while loading personal navigation!", e);
            }
        }
        throw new TransformerException("Unknown argument: " + args[2]);
    }

}
