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

public class MCRHTTPResolver implements URIResolver {

    private static final String HTTP_CLIENT_CLASS = "MCR.HTTPClient.Class";

    private final MCRHTTPClient client;

    public MCRHTTPResolver() {
        this.client = MCRConfiguration2.getInstanceOfOrThrow(MCRHTTPClient.class, HTTP_CLIENT_CLASS);
    }

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
