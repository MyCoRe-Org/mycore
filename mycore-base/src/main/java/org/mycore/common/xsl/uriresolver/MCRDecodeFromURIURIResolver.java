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

import org.jdom2.CDATA;
import org.jdom2.IllegalDataException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

/**
 * A {@link MCRDecodeFromURIURIResolver} is a {@link URIResolver} that accepts an URL-encoded
 * string, reverses what the built-in XSLT-function <code>encode-for-uri</code> does
 * and returns it as a <code>string</code>-element using {@link MCRURIResolverResponse#ofString(String)}.
 */
public class MCRDecodeFromURIURIResolver implements URIResolver {

    /**
     * Resolves the given URI by URL-decoding the provided value
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:&lt;url-encoded-value&gt;
     * </pre>
     * <p>Example request:
     * <pre>
     *   decodeFromUri:%23foobar
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <string>
     *     #foobar
     *   </string>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link Source} wrapping the URL-decoded value
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {

        String encodedValue = href.substring(href.indexOf(':') + 1);
        String string = decode(encodedValue);

        return MCRURIResolverResponse.ofString(string);

    }

    /**
     * Reverses what the built-in XSLT-function <code>encode-for-uri</code> does.
     * @param encodedValue a URL-encoded value
     * @return the URL-decoded value
     */
    public static String decode(String encodedValue) throws TransformerException {
        try {
            return new CDATA(URLDecoder.decode(encodedValue, StandardCharsets.UTF_8)).getText();
        } catch (IllegalDataException e) {
            throw new TransformerException("Non-printable decoded value: " + encodedValue, e);
        } catch (IllegalArgumentException e) {
            throw new TransformerException("Not an URL-encoded value: " + encodedValue, e);
        }
    }

}
