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

package org.mycore.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.common.MCRLinkTableManager;

/**
 * This class provides helper functions for access cache.
 */
public class MCRAccessCacheHelper {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCRLinkTableManager LT_MANAGER = MCRLinkTableManager.instance();

    /**
     * removes all cached permission for the object and its derivates including descendants.
     * @param id the object id
     */
    public static void clearPermissionCache(String id) {
        clearPermissionCache(id, true);
    }

    /**
     * removes all cached permission for the object and its derivates.
     * @param id the object id
     * @param includeDescendants include descendants
     */
    public static void clearPermissionCache(String id, boolean includeDescendants) {
        LOGGER.info("Invalidate permission cache for obj {}", id);

        final ArrayList<String> idsToClear = new ArrayList<>();
        idsToClear.add(id);
        if (includeDescendants) {
            collectDescendants(idsToClear, id);
        } else {
            collectDerivates(idsToClear, id);
        }
        MCRAccessManager.invalidAllPermissionCachesById(idsToClear.toArray(new String[0]));
    }

    private static void collectDerivates(List<String> idsToClear, String parent) {
        idsToClear.addAll(LT_MANAGER.getDestinationOf(parent, MCRLinkTableManager.ENTRY_TYPE_DERIVATE));
    }

    private static void collectDescendants(List<String> idsToClear, String parent) {
        collectDerivates(idsToClear, parent);
        final Collection<String> children = LT_MANAGER.getSourceOf(parent, MCRLinkTableManager.ENTRY_TYPE_PARENT);
        children.forEach(child -> {
            idsToClear.add(child);
            collectDescendants(idsToClear, child);
        });
    }
}
