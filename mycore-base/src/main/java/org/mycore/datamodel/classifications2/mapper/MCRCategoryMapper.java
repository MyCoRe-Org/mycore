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

import java.util.List;

import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;

/**
 * @author Frank Lützenkirchen
 */
public class MCRCategoryMapper extends MCRCategoryMapperBase {

    private static final MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    protected String getMappingRule(MCRCategoryID categoryID) {
        MCRCategory category = DAO.getCategory(categoryID, 0);
        //"x-mapper" was used in previous versions of mycore
        MCRLabel label = category.getLabel("x-mapping").orElse(category.getLabel("x-mapper")
            .orElseThrow(() -> new MCRException("Category " + category + " does not hav a label for 'x-mapping'.")));
        return label.getText();
    }

    protected void addParentsToList(MCRCategoryID childID, List<MCRCategoryID> list) {
        for (MCRCategory parent : DAO.getParents(childID)) {
            if (parent.isCategory())
                list.add(parent.getId());
        }
    }
}
