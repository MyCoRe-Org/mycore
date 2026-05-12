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

import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.xml.MCREntityResolver;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.resource.MCRResourceHelper;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Reads XML from the CLASSPATH of the application. the location of the file in the format resource:path/to/file
 */
public class MCRResourceResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String path = href.substring(href.indexOf(':') + 1);
        URL resource = MCRResourceHelper.getResourceUrl(path);
        if (resource != null) {
            //have to use SAX here to resolve entities
            if (path.endsWith(".xsl")) {
                XMLReader reader;
                try {
                    reader = MCRXMLParserFactory.getNonValidatingParser().getXMLReader();
                } catch (SAXException | ParserConfigurationException e) {
                    throw new TransformerException(e);
                }
                reader.setEntityResolver(MCREntityResolver.getInstance());
                InputSource input = new InputSource(resource.toString());
                SAXSource saxSource = new SAXSource(reader, input);
                LOGGER.debug("include stylesheet: {}", saxSource::getSystemId);
                return saxSource;
            } else {
                return MCRURIResolver.obtainInstance().resolve(resource.toString(), base);
            }
        }
        return null;
    }

}
