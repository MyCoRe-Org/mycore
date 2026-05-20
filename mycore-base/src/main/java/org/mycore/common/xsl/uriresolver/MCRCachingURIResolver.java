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

import java.util.function.Supplier;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRCache;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * {@link URIResolver} that resolves XML content from a URI and caches the result for re-use.
 */
@MCRConfigurationProxy(proxyClass = MCRCachingURIResolver.Factory.class)
public class MCRCachingURIResolver implements URIResolver {

    private final static Logger LOGGER = LogManager.getLogger();

    private final MCRCache<String, Element> cache;

    private final long maxAge;

    /**
     * Creates a new {@code MCRCachingURIResolver} with the given cache capacity and maximum entry age.
     *
     * @param cacheCapacity maximum number of entries the cache may hold
     * @param maxAge maximum age of a cache entry in milliseconds;
     *               entries older than this are resolved again on the next access
     */
    public MCRCachingURIResolver(int cacheCapacity, long maxAge) {
        this.cache = new MCRCache<>(cacheCapacity, MCRCachingURIResolver.class.getName());
        this.maxAge = maxAge;
    }

    /**
     * Resolves XML content from the given URI, returning a cached result if still valid.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:&lt;uri&gt;
     * </pre>
     * <p>Example request:
     * <pre>
     *   cache:mcrobject:mcr_document_00000001
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <root>
     *     ...
     *   </root>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link JDOMSource} wrapping the resolved or cached XML element
     */
    @Override
    public Source resolve(String href, String base) {
        String hrefToCache = href.substring(href.indexOf(':') + 1);
        LOGGER.debug(() -> "resolving: " + hrefToCache);

        long maxDateCached = System.currentTimeMillis() - maxAge;
        Element resolvedXML = cache.getIfUpToDate(hrefToCache, maxDateCached);

        if (resolvedXML == null) {
            LOGGER.debug(() -> hrefToCache + " not in cache, must resolve");
            resolvedXML = MCRURIResolver.obtainInstance().resolve(hrefToCache);
            cache.put(hrefToCache, resolvedXML);
        } else {
            LOGGER.debug(() -> hrefToCache + " already in cache");
        }

        return new JDOMSource(resolvedXML);
    }

    /**
     * Factory that creates {@link MCRCachingURIResolver} instances from MyCoRe configuration properties.
     */
    public static class Factory implements Supplier<MCRCachingURIResolver> {

        /**
         * Maximum number of entries the cache may hold.
         */
        @MCRProperty(name = "CacheCapacity")
        public String capacity;

        /**
         * Maximum age of a cache entry in milliseconds before it is considered stale.
         */
        @MCRProperty(name = "CacheMaxAge")
        public String maxAge;

        @Override
        public MCRCachingURIResolver get() {
            return new MCRCachingURIResolver(Integer.parseInt(capacity), Long.parseLong(maxAge));
        }

    }

}
