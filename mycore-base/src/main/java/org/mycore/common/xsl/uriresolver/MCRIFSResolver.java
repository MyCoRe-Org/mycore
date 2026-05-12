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
import java.net.URISyntaxException;
import java.util.StringTokenizer;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.MCRPathXML;

public class MCRIFSResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Reads XML from a http or https URL.
     *
     * @param href
     *            the URL of the xml document
     * @return the root element of the xml document
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        LOGGER.debug("Reading xml from url {}", href);

        String path = href.substring(href.indexOf(':') + 1);

        int i = path.indexOf("?host");
        if (i > 0) {
            path = path.substring(0, i);
        }
        StringTokenizer st = new StringTokenizer(path, "/");

        String ownerID = st.nextToken();
        try {
            String aPath = MCRXMLFunctions.decodeURIPath(path.substring(ownerID.length() + 1));
            // TODO: make this more pretty
            if (ownerID.endsWith(":")) {
                ownerID = ownerID.substring(0, ownerID.length() - 1);
            }
            LOGGER.debug("Get {} path: {}", ownerID, aPath);
            return new JDOMSource(MCRPathXML.getDirectoryXML(MCRPath.getPath(ownerID, aPath)));
        } catch (IOException | URISyntaxException e) {
            throw new TransformerException(e);
        }
    }

}
