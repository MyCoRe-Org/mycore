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
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRSourceContent;
import org.mycore.common.xml.MCRXMLParserFactory;

/**
 * {@link URIResolver} that delegates to another URI resolver and guarantees a non-{@code null}
 * result by returning an empty XML element on failure or empty resolution.
 */
public class MCRNotNullURIResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Resolves the target URI and returns its content, falling back to an empty XML result
     * instead of returning {@code null} or propagating exceptions.
     * <p>The resolved content is fully parsed into a JDOM document before being returned,
     * using a silent XML parser to suppress log noise. If the sub-URI is empty, the target
     * resolves to {@code null}, or any exception occurs, an empty result is returned.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{anyMCRUri}
     * </pre>
     * <p>Example request:
     * <pre>
     *   notnull:mcrobject:mcr_document_00000001
     * </pre>
     * <p>Example response on success: the resolved content of the target URI.
     * <p>Example response on failure:
     * <pre>{@code
     *   <null/>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet, passed through to the delegated resolver
     * @return a {@link JDOMSource} wrapping the resolved content, or an empty result if
     *         resolution fails or returns {@code null}
     */
    @Override
    public Source resolve(String href, String base) {
        String target = href.substring(href.indexOf(':') + 1);
        // fixes exceptions if suburi is empty like "mcrobject:"
        String subUri = target.substring(target.indexOf(':') + 1);
        if (subUri.isEmpty()) {
            return MCRURIResolverResponse.ofNull();
        }
        // end fix
        LOGGER.debug("Ensuring xml is not null: {}", target);
        try {
            Source result = MCRURIResolver.obtainInstance().resolve(target, base);
            if (result != null) {
                // perform actual construction of xml document, as in MCRURIResolver#resolve(String),
                // by performing the same actions as MCRSourceContent#asXml(),
                // but with a MCRXMLParser configured to be silent to suppress undesirable log messages
                MCRContent content = new MCRSourceContent(result).getBaseContent();
                Document document = MCRXMLParserFactory.getParser(false, true).parseXML(content);
                return new JDOMSource(document.getRootElement().detach());
            } else {
                LOGGER.debug("MCRNotNullResolver returning empty xml");
                return MCRURIResolverResponse.ofNull();
            }
        } catch (Exception ex) {
            LOGGER.info("MCRNotNullResolver caught exception: {}", ex::getLocalizedMessage);
            LOGGER.debug(ex::getLocalizedMessage, ex);
            LOGGER.debug("MCRNotNullResolver returning empty xml");
            return MCRURIResolverResponse.ofNull();
        }
    }

}
