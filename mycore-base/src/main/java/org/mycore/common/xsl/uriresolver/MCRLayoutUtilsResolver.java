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

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPathExpressionException;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRException;
import org.mycore.frontend.MCRLayoutUtilities;

/**
 * Resolver for MCRLayoutUtils. The following types are supported: readAccess:$webpageID,
 * readAccess:$webpageID:split:$blockerWebpageID
 * returns
 * <code>
 *     &lt;true /&gt;
 * </code>
 * or
 * <code>
 *     &lt;false /&gt;
 * </code>
 */
public class MCRLayoutUtilsResolver implements URIResolver {

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
