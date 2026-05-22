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

package org.mycore.resource.uriresolver;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.mycore.common.xsl.uriresolver.MCRURIResolver;

/**
 * {@link URIResolver} that resolves a {@code file://} URI and returns its content as a stream source.
 */
public class MCRFileURIResolver implements URIResolver {

    /**
     * Resolves the given file URI and returns its content as a {@link StreamSource}.
     * <p>URI Syntax:
     * <pre>
     *   file://{path}
     * </pre>
     * <p>Example request:
     * <pre>
     *   file:///var/data/mycore/foo.xml
     * </pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet, passed through to the delegated resolver
     * @return a {@link StreamSource} reading from the resolved file path
     * @throws TransformerException if the URI scheme is not {@code file}, or the file cannot be read
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        URI hrefURI = MCRURIResolver.resolveURI(href, base);
        if (!hrefURI.getScheme().equals("file")) {
            throw new TransformerException("Unsupported file uri scheme: " + hrefURI.getScheme());
        }
        Path path = Paths.get(hrefURI);
        StreamSource source;
        try {
            source = new StreamSource(Files.newInputStream(path), hrefURI.toASCIIString());
            return source;
        } catch (IOException e) {
            throw new TransformerException(e);
        }
    }

}
