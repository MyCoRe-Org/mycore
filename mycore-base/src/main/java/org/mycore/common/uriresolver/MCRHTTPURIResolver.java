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

import java.io.IOException;
import java.net.URI;
import java.util.function.Supplier;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.mycore.common.MCRHTTPClient;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.xsl.uriresolver.MCRURIResolver;

/**
 * {@link URIResolver} that fetches XML content from a remote HTTP or HTTPS endpoint.
 *
 * @see MCRHTTPClient
 */
@MCRConfigurationProxy(proxyClass = MCRHTTPURIResolver.Factory.class)
public class MCRHTTPURIResolver implements URIResolver {

    private final MCRHTTPClient client;

    /**
     * Creates a new {@code MCRHTTPURIResolver} that uses the given HTTP client for all requests.
     *
     * @param client the HTTP client to use; must not be {@code null}
     */
    public MCRHTTPURIResolver(MCRHTTPClient client) {
        this.client = client;
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

    /**
     * Factory that creates {@link MCRHTTPURIResolver} instances from MyCoRe configuration properties.
     */
    public static class Factory implements Supplier<MCRHTTPURIResolver> {

        /**
         * The HTTP client implementation used to perform GET requests.
         */
        @MCRInstance(name = "Client", valueClass = MCRHTTPClient.class)
        public MCRHTTPClient client;

        @Override
        public MCRHTTPURIResolver get() {
            return new MCRHTTPURIResolver(client);
        }

    }
}
