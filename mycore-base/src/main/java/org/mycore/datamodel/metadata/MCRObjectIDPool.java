/*
 * 
 * $Revision: 18725 $ $Date: 2010-09-21 11:21:23 +0200 (Di, 21 Sep 2010) $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
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
        .build(new CacheLoader<String, MCRObjectID>() {
            @Override
            public MCRObjectID load(String id) throws Exception {
                return new MCRObjectID(id);
            }
        });

    static MCRObjectID getMCRObjectID(String id) {
        try {
            return objectIDCache.getUnchecked(id);
        } catch (UncheckedExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof MCRException) {
                throw (MCRException) cause;
            }
            throw e;
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
