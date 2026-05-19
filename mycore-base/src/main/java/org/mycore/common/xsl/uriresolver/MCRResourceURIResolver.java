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
 * {@link URIResolver} that reads XML resources from the application classpath.
 */
public class MCRResourceURIResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Resolves the given classpath resource and returns its content as a source.
     * <p>XSL stylesheets are parsed via SAX with entity resolution enabled.
     * All other resource types are delegated to {@link MCRURIResolver} using the resolved resource URL.
     * If the resource cannot be found on the classpath, {@code null} is returned.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{path/to/resource}
     * </pre>
     * <p>Example request:
     * <pre>
     *   resource:xsl/my-stylesheet.xsl
     *   resource:config/defaults.xml
     * </pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet, passed through to the delegated resolver
     *             for non-XSL resources
     * @return a {@link SAXSource} for XSL resources, the delegated {@link Source} for all others,
     *         or {@code null} if the resource is not found on the classpath
     * @throws TransformerException if the SAX parser cannot be created or the resource cannot be read
     */
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
