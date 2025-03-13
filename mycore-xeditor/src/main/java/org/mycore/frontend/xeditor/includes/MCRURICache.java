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
package org.mycore.frontend.xeditor.includes;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRCache;
import org.mycore.common.config.MCRConfiguration2;

final class MCRURICache {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CACHE_LABEL = "XEditor Includes Cache";

    private static final String PROP_CACHE_CAPACITY = "MCR.XEditor.URICache.Capacity";

    private static final String PROP_CACHE_INCREMENT = "MCR.XEditor.URICache.AutoIncrement";

    private static final String PROP_CACHE_PATTERNS = "MCR.XEditor.URICache.Pattern";

    private static final MCRURICache SHARED_INSTANCE = new MCRURICache();

    private List<Pattern> patternsOfToBeCachedURIs = new ArrayList<>();

    private MCRCache<String, Element> cache;

    private int cacheNumToAutoIncrement;

    public static MCRURICache obtainInstance() {
        return SHARED_INSTANCE;
    }

    private MCRURICache() {
        int capacity = MCRConfiguration2.getInt(PROP_CACHE_CAPACITY).get();
        cache = new MCRCache<>(capacity, CACHE_LABEL);

        this.cacheNumToAutoIncrement = MCRConfiguration2.getInt(PROP_CACHE_INCREMENT).get();

        MCRConfiguration2.getSubPropertiesMap(PROP_CACHE_PATTERNS).values().forEach(
            pattern -> patternsOfToBeCachedURIs.add(Pattern.compile(pattern)));
    }

    synchronized Element get(String uri) {
        Element cached = cache.get(uri);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("URI {} was cached? {} (hit rate {})", uri, (cached != null), cache.getHitRate());
        }
        return cached;
    }

    synchronized boolean offer(String uri, Element resolved) {
        boolean shouldBeCached = shouldBeCached(uri);
        if (shouldBeCached) {
            incrementCapacityIfRequired();
            LOGGER.debug(() -> "caching resolved URI " + uri);
            cache.put(uri, resolved);
        }
        return shouldBeCached;
    }

    private void incrementCapacityIfRequired() {
        if ((cacheNumToAutoIncrement > 0) && cache.isFull()) {
            cache.setCapacity(cache.getCapacity() + cacheNumToAutoIncrement);
        }
    }

    private boolean shouldBeCached(String uri) {
        return patternsOfToBeCachedURIs.stream().anyMatch(p -> p.matcher(uri).matches());
    }
}
