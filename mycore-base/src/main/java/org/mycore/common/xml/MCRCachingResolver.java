/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.common.xml;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRCache;
import org.mycore.common.config.MCRConfiguration2;

/**
 * Resolves XML content from a given URI and caches it for re-use.
 * 
 * If the URI was already resolved within the last
 * MCR.URIResolver.CachingResolver.MaxAge milliseconds, the cached version is returned.
 * The default max age is one hour.
 * 
 * The cache capacity is configured via MCR.URIResolver.CachingResolver.Capacity
 * The default capacity is 100.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRCachingResolver implements URIResolver {

    private final static Logger LOGGER = LogManager.getLogger();

    private final static String CONFIG_PREFIX = "MCR.URIResolver.CachingResolver";

    private long maxAge;

    private MCRCache<String, Element> cache;

    public MCRCachingResolver() {
        int capacity = MCRConfiguration2.getInt(CONFIG_PREFIX + ".Capacity").orElseThrow();
        maxAge = MCRConfiguration2.getLong(CONFIG_PREFIX + ".MaxAge").orElseThrow();
        cache = new MCRCache<String, Element>(capacity, MCRCachingResolver.class.getName());
    }

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        href = href.substring(href.indexOf(":") + 1);
        LOGGER.debug("resolving " + href);

        long maxDateCached = System.currentTimeMillis() - maxAge;
        Element resolvedXML = cache.getIfUpToDate(href, maxDateCached);

        if (resolvedXML == null) {
            LOGGER.debug(href + " not in cache, must resolve");
            resolvedXML = MCRURIResolver.instance().resolve(href);
            cache.put(href, resolvedXML);
        } else {
            LOGGER.debug(href + " already in cache");
        }

        return new JDOMSource(resolvedXML);
    }
}
