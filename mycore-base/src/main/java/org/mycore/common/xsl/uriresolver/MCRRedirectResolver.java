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
import org.mycore.common.config.MCRConfiguration2;

/**
 * Redirect to different URIResolver that is defined via property. This resolver is meant to serve static content as
 * no variable substitution takes place Example: MCR.URIResolver.redirect.alias=webapp:path/to/alias.xml
 */
public class MCRRedirectResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String configsuffix = href.substring(href.indexOf(':') + 1);

        // get the parameters from mycore.properties
        String propertyName = "MCR.URIResolver.redirect." + configsuffix;
        String propValue = MCRConfiguration2.getStringOrThrow(propertyName);
        LOGGER.info("Redirect {} to {}", href, propValue);
        return MCRURIResolver.obtainInstance().resolve(propValue, base);
    }

}
