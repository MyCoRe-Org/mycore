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

package org.mycore.common.uriresolver;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jdom2.Verifier;
import org.mycore.common.xsl.uriresolver.MCRURIResolverHelper;
import org.mycore.common.xsl.uriresolver.MCRURIResolverResponse;

/**
 * A {@link MCRDecodeFromURIURIResolver} is a {@link URIResolver} that accepts an URL-encoded
 * string, reverses what the built-in XSLT-function <code>encode-for-uri</code> does
 * and returns it as a <code>string</code>-element using {@link MCRURIResolverResponse#ofString(String)}.
 * <p>
 * Optionally, it also decodes <code>+</code> characters into spaces, so it can be used for form-encoded strings.
 */
public final class MCRDecodeFromURIURIResolver implements URIResolver {

    /**
     * Resolves the given URI by URL-decoding the provided value
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:&lt;url-encoded-value&gt;[?supportFormEncoding=true]
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

        String encodedValueAndParameters = href.substring(href.indexOf(':') + 1);
        String[] parts = encodedValueAndParameters.split(Pattern.quote("?"), 2);

        String encodedValue = parts[0];
        String query = parts.length > 1 ? parts[1] : "";

        Map<String, String> queryParameters = MCRURIResolverHelper.parseQueryParameters(query);
        String supportFormEncoding = queryParameters.getOrDefault("supportFormEncoding", "false");

        String string = decode(encodedValue, Boolean.parseBoolean(supportFormEncoding));

        return MCRURIResolverResponse.ofString(string);

    }

    /**
     * Reverses what the built-in XSLT-function <code>encode-for-uri</code> does.
     * <p>
     * Optionally, it also decodes <code>+</code> characters into spaces, so it can be used for form-encoded strings.
     * @param encodedValue a URL-encoded value
     * @param supportFormEncoding whether to support form-decoding
     * @return the URL-decoded value
     */
    public static String decode(String encodedValue, boolean supportFormEncoding) throws TransformerException {
        String type = supportFormEncoding ? "form" : "URL";
        String originalEncodedValue = encodedValue;
        try {
            if (!supportFormEncoding) {
                encodedValue = encodedValue.replace("+", "%2B");
            }
            String decodedValue = URLDecoder.decode(encodedValue, StandardCharsets.UTF_8);
            String reason = Verifier.checkCharacterData(decodedValue);
            if (reason != null) {
                throw new TransformerException(
                    "Non-printable " + type + "-decoded value (" + reason + "): " + originalEncodedValue);
            }
            return decodedValue;
        } catch (IllegalArgumentException e) {
            throw new TransformerException("Invalid " + type + "-encoded value: " + originalEncodedValue, e);
        }
    }

}
