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

package org.mycore.mods.classification;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.xsl.uriresolver.MCRURIResolver;

/**
 * {@link URIResolver} that resolves a MODS authority or URI reference to a classification
 * in parent style.
 */
public class MCRModsClassificationURIResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    private static Optional<MCRAuthorityInfo> getAuthorityInfo(String href) {
        final URI uri = URI.create(href);
        //keep any encoded '/' inside a path segment
        final String[] decodedPathSegments = Stream.of(uri.getRawPath().substring(1).split("/"))
            .map(s -> "/" + s) //mask as a path for parsing
            .map(URI::create) //parse
            .map(URI::getPath) //decode
            .map(p -> p.substring(1)) //strip '/' again
            .toArray(String[]::new);
        MCRAuthorityInfo authInfo = null;
        switch (decodedPathSegments[0]) {
            case "uri" -> {
                if (decodedPathSegments.length == 3) {
                    authInfo = new MCRAuthorityWithURI(decodedPathSegments[1], decodedPathSegments[2]);
                }
            }
            case "authority" -> {
                if (decodedPathSegments.length == 3) {
                    authInfo = new MCRAuthorityAndCode(decodedPathSegments[1], decodedPathSegments[2]);
                }
            }
            case "accessCondition" -> {
                if (decodedPathSegments.length == 2) {
                    authInfo = new MCRAccessCondition(decodedPathSegments[1]);
                }
            }
            case "typeOfResource" -> {
                if (decodedPathSegments.length == 2) {
                    authInfo = new MCRTypeOfResource(decodedPathSegments[1]);
                }
            }
            default -> LOGGER.warn("Unrecognized path segment {}", decodedPathSegments[0]);
        }
        LOGGER.debug("authinfo {}", authInfo);
        return Optional.ofNullable(authInfo);
    }

    /**
     * Resolves the given MODS authority reference to a classification and returns it as an XML source.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:/{sourcePath}/{subPath}
     * </pre>
     * <p>Supported source paths:
     * <ul>
     *   <li>{@code uri} – subPath is {@code {authorityURI}/{valueURI}} (URI-encoded)</li>
     *   <li>{@code authority} – subPath is {@code {authority}/{text()}} (URI-encoded)</li>
     *   <li>{@code accessCondition} – subPath is {@code {xlink:href}} (URI-encoded)</li>
     *   <li>{@code typeOfResource} – subPath is {@code {text()}} (URI-encoded)</li>
     * </ul>
     * <p>Example request:
     * <pre>
     *   modsclass:/authority/marcrelator/aut
     *   modsclass:/uri/http%3A%2F%2Fwww.example.org%2Fclassifications/http%3A%2F%2Fwww.example.org%2Fpub-type%23Sound
     *   modsclass:/accessCondition/http%3A%2F%2Fwww.mycore.org%2Fclassifications%2Fmir_licenses%23cc_by-sa_4.0
     *   modsclass:/typeOfResource/sound%20recording
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <categories>
     *     <category ID="marcrelator:aut">
     *       ...
     *     </category>
     *   </categories>
     * }</pre>
     * <p>Example response if no classification is found:
     * <pre>{@code
     *   <empty/>
     * }</pre>
     *
     * @param href the URI to resolve; parsed as a path with source type and URI-encoded arguments
     * @param base the base URI of the calling stylesheet, passed through to the delegated resolver
     * @return a {@link Source} wrapping the classification result, or an {@code <empty/>} element
     *         if no matching category is found
     * @throws TransformerException if the delegated resolution fails
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        final Optional<String> categoryURI = getAuthorityInfo(href)
            .map(MCRAuthorityInfo::getCategoryID)
            .map(category -> String.format(Locale.ROOT, "classification:metadata:0:parents:%s:%s", category.getRootID(),
                category.getId()));
        if (categoryURI.isPresent()) {
            LOGGER.debug("{} -> {}", () -> href, categoryURI::get);
            return MCRURIResolver.obtainInstance().resolve(categoryURI.get(), base);
        }
        LOGGER.debug("no category found for {}", href);
        return new JDOMSource(new Element("empty"));
    }

}
