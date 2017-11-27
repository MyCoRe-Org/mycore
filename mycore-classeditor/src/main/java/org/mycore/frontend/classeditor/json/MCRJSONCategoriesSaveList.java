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

package org.mycore.frontend.classeditor.json;

import java.util.ArrayList;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;

public class MCRJSONCategoriesSaveList {
    ArrayList<CategorySaveElement> updateList = new ArrayList<>();

    ArrayList<CategorySaveElement> deleteList = new ArrayList<>();

    public void add(MCRCategory categ, MCRCategoryID parentID, int index, String status) throws Exception {
        if ("updated".equals(status)) {
            updateList.add(new CategorySaveElement(categ, parentID, index));
        } else if ("deleted".equals(status)) {
            deleteList.add(new CategorySaveElement(categ, parentID, index));
        } else {
            throw new Exception("Unknown status.");
        }
    }

    private class CategorySaveElement {
        private MCRCategory categ;
        private MCRCategoryID parentID;
        private int index;

        public CategorySaveElement(MCRCategory categ, MCRCategoryID parentID, int index) {
            this.categ = categ;
            this.parentID = parentID;
            this.index = index;
        }
    }

}
