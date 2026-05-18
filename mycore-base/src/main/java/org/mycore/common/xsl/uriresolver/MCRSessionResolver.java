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
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRSessionMgr;

/**
 * {@link URIResolver} that reads a JDOM element stored in the current session.
 */
public class MCRSessionResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Resolves the given session key and returns the stored JDOM element as an XML source.
     * <p>The value stored under the key must be a JDOM {@link Element}; it is cloned before
     * being returned to prevent unintended modification of the session state.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{key}
     * </pre>
     * <p>Example request:
     * <pre>
     *   session:mySessionKey
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <myElement>...</myElement>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link JDOMSource} wrapping a clone of the element stored under the given key
     */
    @Override
    public Source resolve(String href, String base) {
        String key = href.substring(href.indexOf(':') + 1);
        LOGGER.debug("Reading xml from session using key {}", key);
        Element value = (Element) MCRSessionMgr.getCurrentSession().get(key);
        return new JDOMSource(value.clone());
    }

}
