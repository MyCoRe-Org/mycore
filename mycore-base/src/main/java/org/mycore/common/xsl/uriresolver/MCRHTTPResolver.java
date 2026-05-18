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

import java.io.IOException;
import java.net.URI;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.mycore.common.MCRHTTPClient;
import org.mycore.common.config.MCRConfiguration2;

/**
 * {@link URIResolver} that fetches XML content from a remote HTTP or HTTPS endpoint.
 * <p>The HTTP client used for requests is configured via {@code MCR.HTTPClient.Class}.
 */
public class MCRHTTPResolver implements URIResolver {

    private static final String HTTP_CLIENT_CLASS = "MCR.HTTPClient.Class";

    private final MCRHTTPClient client;

    public MCRHTTPResolver() {
        this.client = MCRConfiguration2.getInstanceOfOrThrow(MCRHTTPClient.class, HTTP_CLIENT_CLASS);
    }

    /**
     * Resolves the given URI by performing an HTTP GET request and returning the response as a source.
     * <p>URI Syntax:
     * <pre>
     *   http://{host}/{path}
     *   https://{host}/{path}
     * </pre>
     * <p>Example request:
     * <pre>
     *   https://example.org/api/document/1
     * </pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI used to resolve relative URIs
     * @return a {@link Source} wrapping the HTTP response body
     * @throws TransformerException if the request fails or the response cannot be read
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        URI hrefURI = MCRURIResolver.resolveURI(href, base);
        try {
            final Source source = client.get(hrefURI).getSource();
            source.setSystemId(hrefURI.toASCIIString());
            return source;
        } catch (IOException e) {
            throw new TransformerException(e);
        }
    }

}
