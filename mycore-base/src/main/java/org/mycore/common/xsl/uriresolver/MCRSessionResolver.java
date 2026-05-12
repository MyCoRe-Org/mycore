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

public class MCRSessionResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Reads XML from URIs of type session:key. The method MCRSession.get( key ) is called and must return a JDOM
     * element.
     *
     * @see org.mycore.common.MCRSession#get(Object)
     * @param href
     *            the URI in the format session:key
     * @return the root element of the xml document
     */
    @Override
    public Source resolve(String href, String base) {
        String key = href.substring(href.indexOf(':') + 1);
        LOGGER.debug("Reading xml from session using key {}", key);
        Element value = (Element) MCRSessionMgr.getCurrentSession().get(key);
        return new JDOMSource(value.clone());
    }

}
