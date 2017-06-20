package org.mycore.frontend.classeditor.json;

import java.util.ArrayList;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;

public class MCRJSONCategoriesSaveList {
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

    ArrayList<CategorySaveElement> updateList = new ArrayList<MCRJSONCategoriesSaveList.CategorySaveElement>();

    ArrayList<CategorySaveElement> deleteList = new ArrayList<MCRJSONCategoriesSaveList.CategorySaveElement>();

    public void add(MCRCategory categ, MCRCategoryID parentID, int index, String status) throws Exception {
        if ("updated".equals(status)) {
            updateList.add(new CategorySaveElement(categ, parentID, index));
        } else if ("deleted".equals(status)) {
            deleteList.add(new CategorySaveElement(categ, parentID, index));
        } else {
            throw new Exception("Unknown status.");
        }
    }

}
