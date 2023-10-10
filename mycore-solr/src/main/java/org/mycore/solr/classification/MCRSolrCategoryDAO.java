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

package org.mycore.solr.classification;

import java.net.URI;
import java.util.Collection;
import java.util.SortedSet;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImpl;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;

/**
 * Extends the default category dao with solr support. Every create/write/delete operation
 * on a classification/category results in a solr reindex additionally.
 * 
 * @author Matthias Eichner
 */
public class MCRSolrCategoryDAO extends MCRCategoryDAOImpl {

    @Override
    public MCRCategory setURI(MCRCategoryID id, URI uri) {
        MCRCategory category = super.setURI(id, uri);
        MCRSolrClassificationUtil.reindex(category);
        return category;
    }

    @Override
    public MCRCategory setLabel(MCRCategoryID id, MCRLabel label) {
        MCRCategory category = super.setLabel(id, label);
        MCRSolrClassificationUtil.reindex(category);
        return category;
    }

    @Override
    public MCRCategory setLabels(MCRCategoryID id, SortedSet<MCRLabel> labels) {
        MCRCategory category = super.setLabels(id, labels);
        MCRSolrClassificationUtil.reindex(category);
        return category;
    }

    @Override
    public MCRCategory removeLabel(MCRCategoryID id, String lang) {
        MCRCategory category = super.removeLabel(id, lang);
        MCRSolrClassificationUtil.reindex(category);
        return category;
    }

    @Override
    public MCRCategory addCategory(MCRCategoryID parentID, MCRCategory category, int position) {
        MCRCategory parent = super.addCategory(parentID, category, position);
        MCRSolrClassificationUtil.reindex(category, parent);
        return parent;
    }

    @Override
    public void deleteCategory(MCRCategoryID id) {
        MCRCategory category = MCRCategoryDAOFactory.getInstance().getCategory(id, 0);
        MCRCategory parent = category.getParent();
        super.deleteCategory(id);
        MCRSolrClassificationUtil.solrDelete(id, parent);
    }

    @Override
    public void moveCategory(MCRCategoryID id, MCRCategoryID newParentID, int index) {
        super.moveCategory(id, newParentID, index);
        MCRSolrClassificationUtil.solrMove(id, newParentID);
    }

    @Override
    public Collection<MCRCategoryImpl> replaceCategory(MCRCategory newCategory) throws IllegalArgumentException {
        Collection<MCRCategoryImpl> replacedCategories = super.replaceCategory(newCategory);
        // remove all old categories
        MCRSolrClassificationUtil.solrDelete(newCategory.getId(), newCategory.getParent());
        // reindex all new
        MCRSolrClassificationUtil.reindex(replacedCategories.toArray(MCRCategory[]::new));
        return replacedCategories;
    }

}
