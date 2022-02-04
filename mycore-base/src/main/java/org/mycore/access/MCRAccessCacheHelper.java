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

public class MCRAccessCacheHelper {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * removes all cached permissions for the object and all direct children and all direct derivates
     * @param obj the object
     */
    public static void clearPermissionCache(String id) {
        LOGGER.info("Invalidate permission cache for obj {}", id);

        final ArrayList<String> idsToClear = new ArrayList<>();
        idsToClear.add(id);
        collectDescendants(idsToClear, id);
        MCRAccessManager.invalidAllPermissionCachesById(idsToClear.toArray(new String[0]));
    }

    private static void collectDescendants(List<String> descendantList, String parent) {
        // get derivates
        final MCRLinkTableManager ltManager = MCRLinkTableManager.instance();
        descendantList.addAll(ltManager.getDestinationOf(parent, MCRLinkTableManager.ENTRY_TYPE_DERIVATE));

        // get children
        final Collection<String> children = ltManager.getSourceOf(parent, MCRLinkTableManager.ENTRY_TYPE_PARENT);
        children.forEach(child -> {
            descendantList.add(child);
            collectDescendants(descendantList, child);
        });
    }
}
