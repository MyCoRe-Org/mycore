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

package org.mycore.solr.index.file.tika;

import org.mycore.common.MCRCache;
import org.mycore.common.config.MCRConfiguration2;

import java.util.Optional;

import static org.mycore.solr.MCRSolrConstants.SOLR_CONFIG_PREFIX;

/**
 * Manager for mappers, mapping Tika extracted metadata to Solr fields.
 */
public final class MCRTikaManager {

    private static final MCRTikaManager SINGLETON_INSTANCE = new MCRTikaManager();

    private final MCRTikaMapper defaultMapper = MCRConfiguration2.getInstanceOfOrThrow(MCRTikaMapper.class,
        SOLR_CONFIG_PREFIX + "Tika.Mapper.Default.Class");

    private final MCRCache<String, MCRTikaMapper> mapperCache = new MCRCache<>(100,
        "MCRTikaManager instance cache");

    private MCRTikaManager() {
    }

    public static MCRTikaManager getInstance() {
        return SINGLETON_INSTANCE;
    }

    /**
     * Returns the default Tika mapper. This mapper is used if no mapper is defined for a key.
     *
     * @return The default Tika mapper
     */
    public MCRTikaMapper getDefaultMapper() {
        return defaultMapper;
    }

    /**
     * Returns the Tika mapper for the given key. If no mapper is defined for the key, the default mapper is returned.
     *
     * @param key The key for which the mapper should be returned
     * @return The Tika mapper for the given key
     */
    public Optional<MCRTikaMapper> getMapper(String key) {
        MCRTikaMapper instance = mapperCache.get(key);
        if (instance == null) {
            synchronized (mapperCache) {
                instance = mapperCache.get(key);
                if (instance == null) {
                    instance = MCRConfiguration2.getInstanceOf(MCRTikaMapper.class,
                        SOLR_CONFIG_PREFIX + "Tika.Mapper." + key + ".Class").orElse(null);
                    if (instance != null) {
                        mapperCache.put(key, instance);
                    }
                }
            }
        }
        return Optional.ofNullable(instance);
    }

}
