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
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRPathContent;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * {@link URIResolver} that resolves a file within a derivate and returns its content as a source.
 */
public class MCRDerivateFileResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Resolves the given derivate path and returns the file content as a source.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{derivateId}/{filePath}
     * </pre>
     * <p>Example request:
     * <pre>
     *   mcrfile:mcr_derivate_00000001/path/to/file.xml
     * </pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link Source} with the content of the resolved file
     * @throws TransformerException if no valid derivate path is provided or the file cannot be read
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        LOGGER.debug("Reading xml from MCRFile {}", href);
        MCRPath file = null;
        String id = href.substring(href.indexOf(':') + 1);
        if (id.contains("/")) {
            // assume that is a derivate with path
            try {
                MCRObjectID derivateID = MCRObjectID.getInstance(id.substring(0, id.indexOf('/')));
                String path = id.substring(id.indexOf('/'));
                file = MCRPath.getPath(derivateID.toString(), path);
            } catch (MCRException exc) {
                // just check if the id is valid, don't care about the exception
            }
        }
        if (file == null) {
            throw new TransformerException("mcrfile: Resolver needs a path: " + href);
        }
        try {
            return new MCRPathContent(file).getSource();
        } catch (Exception e) {
            throw new TransformerException(e);
        }
    }

}
