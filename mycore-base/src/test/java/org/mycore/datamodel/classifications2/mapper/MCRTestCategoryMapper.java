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

package org.mycore.datamodel.classifications2.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mycore.datamodel.classifications2.MCRCategoryID;

/**
 * @author Frank Lützenkirchen
 */
public class MCRTestCategoryMapper extends MCRCategoryMapperBase {

    private Map<MCRCategoryID, MCRCategoryID> parents = new HashMap<>();

    private Map<MCRCategoryID, String> mappingRules = new HashMap<>();

    public void setParent(MCRCategoryID childID, MCRCategoryID parentID) {
        parents.put(childID, parentID);
    }

    @Override
    protected void addParentsToList(MCRCategoryID childID, List<MCRCategoryID> list) {
        MCRCategoryID parent = parents.get(childID);
        if (parent != null) {
            list.add(parent);
            addParentsToList(parent, list);
        }
    }

    public void addMappingRule(MCRCategoryID categoryID, String mappingRule) {
        mappingRules.put(categoryID, mappingRule);
    }

    @Override
    protected String getMappingRule(MCRCategoryID categoryID) {
        return mappingRules.get(categoryID);
    }

    @Test
    public void doNothing() throws Exception {
        // remove me if needed, just prevent test errors java.lang.Exception: No runnable methods
    }
}
