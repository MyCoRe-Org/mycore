/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.datamodel.classifications2;

import java.util.Collection;
import java.util.List;

/**
 * Interface of the Data Access Object for Classifications.
 * 
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 * @since 2.0
 */
public interface MCRClassificationDAO extends MCRTransactionalDAO {

    /**
     * Adds a category as child of another category.
     * 
     * @param parentID
     *            ID of the parent category
     * @param category
     *            Category (with children) to be added
     */
    public abstract void addCategory(MCRCategoryID parentID, MCRCategory category);

    /**
     * Deletes a category with all child categories.
     * 
     * @param id
     *            ID of Category to be removed
     */
    public abstract void deleteCategory(MCRCategoryID id);

    /**
     * Retrieve all Categories tagged by a specific label in a specific lang.
     * 
     * @param baseID
     *            base Category which subtree is searched for the label.
     * @param lang
     *            language attribute of the label
     * @param text
     *            text of the label
     * @return a collection of MCRCategories with matching labels
     */
    public abstract Collection<MCRCategory> getCategoriesByLabel(MCRCategoryID baseID, String lang, String text);

    /**
     * Returns MCRCategory with this id and childLevel levels of subcategories.
     * 
     * @param id
     *            ID of category
     * @param childLevel
     *            how many levels of subcategories should be retrieved (-1 for
     *            invinitive)
     * @return MCRCategory with <code>id</code>
     */
    public abstract MCRCategory getCategory(MCRCategoryID id, int childLevel);

    /**
     * Returns the list of child categories for the specified category.
     * 
     * @param id
     *            ID of category
     * @return list of child category
     */
    public abstract List<MCRCategory> getChildren(MCRCategoryID id);

    /**
     * Returns the parent of the given category and its parent and so on. The
     * last element in the list is the root category (the classification)
     * 
     * @param id
     *            ID of Category
     * @return list of parents
     */
    public abstract List<MCRCategory> getParents(MCRCategoryID id);

    /**
     * Returns the root Category with ancestor axis of the specified category
     * and childLevel levels of subcategories.
     * 
     * You can say it is the combination of getParents(MCRCategoryID) and
     * getCategory(MCRCategoryID, int).
     * 
     * @param baseID
     *            Category with relative level set to "0".
     * @param childLevel
     *            amount of subcategory levels rooted at baseID category
     * @return the root Category (Classification)
     * @see #getParents(MCRCategoryID)
     * @see #getCategory(MCRCategoryID, int)
     */
    public abstract MCRCategory getRootCategory(MCRCategoryID baseID, int childLevel);

    /**
     * Tells if a given category contains subcategories.
     * 
     * @param id
     *            ID of Category
     * @return true if subcategories are present
     */
    public abstract boolean hasChildren(MCRCategoryID id);

    /**
     * Moves a Category from one subtree in a classification to a new parent.
     * 
     * All subcategories remain children of the moved category.
     * 
     * @param id
     *            ID of the Category which should be moved
     * @param newParentID
     *            ID of the new parent
     */
    public abstract void moveCategory(MCRCategoryID id, MCRCategoryID newParentID);

    /**
     * Moves a Category from one subtree in a classification to a new parent as
     * the <code>index</code>th child.
     * 
     * @param id
     *            ID of the Category which should be moved
     * @param newParentID
     *            ID of the new parent
     * @param index
     *            insert category at index in the list of children
     */
    public abstract void moveCategory(MCRCategoryID id, MCRCategoryID newParentID, int index);

    /**
     * Removes a label from a Category.
     * 
     * @param id
     *            ID of the category
     * @param lang
     *            which language should be removed?
     */
    public abstract void removeLabel(MCRCategoryID id, String lang);

    /**
     * Sets or updates a label from a Category.
     * 
     * @param id
     *            ID of the category
     * @param label
     *            to be set or updated
     */
    public abstract void setLabel(MCRCategoryID id, MCRLabel label);

}
