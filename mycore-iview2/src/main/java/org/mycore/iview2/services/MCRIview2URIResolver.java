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

package org.mycore.iview2.services;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;

/**
 * {@link URIResolver} that provides IView2 tile and file support checks as XML.
 */
public class MCRIview2URIResolver implements URIResolver {

    /**
     * Resolves the given IView2 query and returns the result as an XML source.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{function}:{argument}
     * </pre>
     * <p>Supported functions:
     * <ul>
     *   <li>{@code isCompletelyTiled} – whether the given derivate is completely tiled</li>
     *   <li>{@code isFileSupported} – whether the given file type is supported by IView2</li>
     * </ul>
     * <p>Example request:
     * <pre>
     *   iview:isCompletelyTiled:mcr_derivate_00000001
     *   iview:isFileSupported:image.tif
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <true/>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link JDOMSource} wrapping an element named {@code true} or {@code false}
     * @throws TransformerException if the URI does not contain exactly three segments or
     *                              the function name is unknown
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String[] params = href.split(":");

        if (params.length != 3) {
            throw new TransformerException("Invalid href: " + href);
        }

        switch (params[1]) {
            case "isCompletelyTiled" -> {
                boolean completelyTiled = MCRIView2Tools.isCompletelyTiled(params[2]);
                return new JDOMSource(new Element(String.valueOf(completelyTiled)));
            }
            case "isFileSupported" -> {
                final boolean supported = MCRIView2Tools.isFileSupported(params[2]);
                return new JDOMSource(new Element(String.valueOf(supported)));
            }
            default -> throw new TransformerException("Invalid href: " + href);
        }
    }
}
