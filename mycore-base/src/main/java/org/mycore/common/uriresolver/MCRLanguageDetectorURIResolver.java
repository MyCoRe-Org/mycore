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

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.mycore.common.MCRLanguageDetector;
import org.mycore.common.xsl.uriresolver.MCRURIResolverResponse;

/**
 * {@link URIResolver} that detects the language of a given text and returns the result as XML.
 */
public class MCRLanguageDetectorURIResolver implements URIResolver {

    /**
     * Detects the language of the given text and returns it as an XML source.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{method}:{text}
     * </pre>
     * <ul>
     *   <li>{@code full} - detects language by Unicode script first, falls back to word and ending heuristics</li>
     *   <li>{@code character} - detects language by Unicode script only, returns empty result for Latin scripts</li>
     * </ul>
     * <p>The {@code {text}} value must be URL-encoded.
     * <p>Example request:
     * <pre>
     *   detectLanguage:full:This%20is%20an%20english%20sentence
     *   detectLanguage:character:%D9%87%D8%B0%D8%A7%20%D9%86%D8%B5%20%D8%B9%D8%B1%D8%A8%D9%8A
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <string>en</string>
     * }</pre>
     * If the language cannot be detected, an empty {@code <string/>} element is returned.
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link Source} wrapping a {@code <string>} element containing the detected language code
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String[] split = href.split(":", 3);

        if (split.length != 3) {
            throw new TransformerException("Invalid URI syntax, expected: detectLanguage:{method}:{text}");
        }

        String method = split[1];
        String text = URLDecoder.decode(split[2], StandardCharsets.UTF_8);

        String language = switch (method) {
            case "character" -> MCRLanguageDetector.detectLanguageByCharacter(text);
            case "full" -> MCRLanguageDetector.detectLanguage(text);
            default -> throw new TransformerException("Invalid method: " + method + ", expected: full or character");
        };

        return MCRURIResolverResponse.ofString(language != null ? language : "");
    }
}
