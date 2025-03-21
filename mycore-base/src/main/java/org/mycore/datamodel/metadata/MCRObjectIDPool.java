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

package org.mycore.datamodel.metadata;

import org.mycore.common.MCRException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

/**
 * holds weak references to generated {@link MCRObjectID} instances.
 * @author Thomas Scheffler (yagee)
 *
 */
class MCRObjectIDPool {
    private static LoadingCache<String, MCRObjectID> objectIDCache = CacheBuilder
        .newBuilder()
        .weakValues()
        .build(new CacheLoader<>() {
            @Override
            public MCRObjectID load(String id) {
                return new MCRObjectID(id);
            }
        });

    static MCRObjectID getMCRObjectID(String id) {
        try {
            return objectIDCache.getUnchecked(id);
        } catch (UncheckedExecutionException ignoredIfCauseIsMCRException) {
            Throwable cause = ignoredIfCauseIsMCRException.getCause();
            if (cause instanceof MCRException mcre) {
                throw mcre;
            }
            throw ignoredIfCauseIsMCRException;
        }
    }

    static long getSize() {
        objectIDCache.cleanUp();
        //objectIDCache.size() may return more as actually present;
        return objectIDCache.asMap()
            .entrySet()
            .stream()
            .filter(e -> e.getKey() != null)
            .filter(e -> e.getValue() != null)
            .count();
    }

    static MCRObjectID getIfPresent(String id) {
        return objectIDCache.getIfPresent(id);
    }

}
