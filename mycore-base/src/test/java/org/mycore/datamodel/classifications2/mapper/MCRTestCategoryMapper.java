/*
 * $Revision$ 
 * $Date$
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

    private Map<MCRCategoryID, MCRCategoryID> parents = new HashMap<MCRCategoryID, MCRCategoryID>();

    private Map<MCRCategoryID, String> mappingRules = new HashMap<MCRCategoryID, String>();

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
