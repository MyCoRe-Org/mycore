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

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mycore.access.MCRAccessException;

public class MCRDefaultChildrenOrderStrategy implements MCRChildrenOrderStrategy {

    @Override
    public List<MCRObjectID> getChildrenOrder(MCRObject parent) {
        List<MCRObjectID> childrenOrder = parent.getStructure().getChildrenOrder();
        Map<MCRObjectID, Integer> childrenOrderMap = new HashMap<>();

        for (int i = 0; i < childrenOrder.size(); i++) {
            childrenOrderMap.put(childrenOrder.get(i), i);
        }

        return MCRMetadataManager.getChildren(parent.getId())
            .stream()
            .sorted(Comparator.comparing((MCRObjectID o) -> childrenOrderMap.getOrDefault(o, Integer.MAX_VALUE))
                .thenComparing(MCRObjectID::compareTo))
            .toList();
    }

    @Override
    public void setChildrenOrder(MCRObject parent, List<MCRObjectID> newOrder) throws MCRAccessException {
        parent.getStructure().setChildrenOrder(newOrder);
        MCRMetadataManager.update(parent);
    }
}
