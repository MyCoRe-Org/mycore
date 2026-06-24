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

package org.mycore.mods;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jdom2.transform.JDOMSource;

/**
 * {@link URIResolver} that converts textual page information into
 * a {@code mods:extent[@unit='pages']} element and returns it as XML.
 */
public class MCRMODSExtentURIResolver implements URIResolver {

    /**
     * Converts the given textual page information into a MODS extent element.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{pages}
     * </pre>
     * <p>The {@code {pages}} value must be URL-encoded
     * <p>Example requests:
     * <pre>
     *   modsExtent:pp.%203-4
     *   modsExtent:S.%203845%20-%2053
     *   modsExtent:123%20pages
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <mods:extent unit="pages">
     *     <mods:start>3845</mods:start>
     *     <mods:end>3853</mods:end>
     *   </mods:extent>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link JDOMSource} wrapping the {@code mods:extent} element
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String[] split = href.split(":", 2);
        if (split.length != 2) {
            throw new TransformerException("Invalid URI syntax, expected: <schema>:{pages}");
        }
        try {
            String pages = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
            return new JDOMSource(MCRMODSPagesHelper.buildExtentPages(pages));
        } catch (IllegalArgumentException e) {
            throw new TransformerException("Failed to decode pages: " + split[1], e);
        }
    }

}
