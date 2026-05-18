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
 * {@link URIResolver} that redirects to another URI defined via a configuration property.
 * <p>The target URI is looked up from {@code MCR.URIResolver.redirect.{alias}} and passed
 * to {@link MCRURIResolver} for resolution. No variable substitution takes place, making
 * this resolver suitable for static content only.
 */
public class MCRRedirectResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Resolves the configured target URI for the given alias and returns its content.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{alias}
     * </pre>
     * <p>The alias is appended to {@code MCR.URIResolver.redirect.} to look up the target URI.
     * <p>Example configuration:
     * <pre>
     *   MCR.URIResolver.redirect.myAlias=webapp:path/to/file.xml
     * </pre>
     * <p>Example request:
     * <pre>
     *   redirect:myAlias
     * </pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet, passed through to the delegated resolver
     * @return the {@link Source} returned by resolving the configured target URI
     * @throws TransformerException if the target URI cannot be resolved
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String configSuffix = href.substring(href.indexOf(':') + 1);

        // get the parameters from mycore.properties
        String propertyName = "MCR.URIResolver.redirect." + configSuffix;
        String propValue = MCRConfiguration2.getStringOrThrow(propertyName);
        LOGGER.info("Redirect {} to {}", href, propValue);
        return MCRURIResolver.obtainInstance().resolve(propValue, base);
    }

}
