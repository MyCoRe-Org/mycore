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

package org.mycore.datamodel.classifications2;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Interface of the Data Access Object for Classifications.
 * 
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 * @since 2.0
 */
public interface MCRCategoryDAO {

    /**
     * Adds a category as child of another category.
     * When parentID is null a root category will be created.
     * 
     * @param parentID
     *            ID of the parent category
     * @param category
     *            Category (with children) to be added
     * @return the parent category
     */
    MCRCategory addCategory(MCRCategoryID parentID, MCRCategory category);

    /**
     * Adds a category as child of another category.
     * When parentID is null a root category will be created.
     * 
     * @param parentID
     *            ID of the parent category
     * @param category
     *            Category (with children) to be added
     * @param position
     *            insert position
     * @return the parent category
     */
    MCRCategory addCategory(MCRCategoryID parentID, MCRCategory category, int position);

    /**
     * Deletes a category with all child categories.
     * 
     * @param id
     *            ID of Category to be removed
     */
    void deleteCategory(MCRCategoryID id);

    /**
     * Tells if a given category exists.
     * 
     * @param id
     *            ID of Category
     * @return true if category is present
     */
    boolean exist(MCRCategoryID id);

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
    List<MCRCategory> getCategoriesByLabel(MCRCategoryID baseID, String lang, String text);

    /**
     * Retrieve all Categories tagged by a specific label in a specific lang.
     * 
     * @param lang
     *            language attribute of the label
     * @param text
     *            text of the label
     * @return a collection of MCRCategories with matching labels
     */
    List<MCRCategory> getCategoriesByLabel(String lang, String text);

    /**
     * Returns MCRCategory with this id and childLevel levels of subcategories.
     * 
     * @param id
     *            ID of category
     * @param childLevel
     *            how many levels of subcategories should be retrieved (-1 for
     *            infinitive)
     * @return MCRCategory with <code>id</code> or null if the category cannot be found
     */
    MCRCategory getCategory(MCRCategoryID id, int childLevel);

    /**
     * Returns the list of child categories for the specified category.
     * 
     * @param id
     *            ID of category
     * @return list of child category
     */
    List<MCRCategory> getChildren(MCRCategoryID id);

    /**
     * Returns the parent of the given category and its parent and so on. The
     * last element in the list is the root category (the classification)
     * 
     * @param id
     *            ID of Category
     * @return list of parents
     */
    List<MCRCategory> getParents(MCRCategoryID id);

    /**
     * Returns all category IDs that do not have a parent category.
     * 
     * @return list of category IDs
     */
    List<MCRCategoryID> getRootCategoryIDs();

    /**
     * Returns all categories that do not have a parent category.
     * 
     * @return list of category IDs
     */
    List<MCRCategory> getRootCategories();

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
    MCRCategory getRootCategory(MCRCategoryID baseID, int childLevel);

    /**
     * Tells if a given category contains subcategories.
     * 
     * @param id
     *            ID of Category
     * @return true if subcategories are present
     */
    boolean hasChildren(MCRCategoryID id);

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
    void moveCategory(MCRCategoryID id, MCRCategoryID newParentID);

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
    void moveCategory(MCRCategoryID id, MCRCategoryID newParentID, int index);

    /**
     * Removes a label from a Category.
     * 
     * @param id
     *            ID of the category
     * @param lang
     *            which language should be removed?
     * @return category where the label was removed
     */
    MCRCategory removeLabel(MCRCategoryID id, String lang);

    /**
     * Replaces a <code>MCRCategory</code> by a new version of the same
     * category.
     * 
     * This replacment includes all subcategories and labels. So former
     * subcategories and labels not present in <code>newCategory</code> will
     * be removed while new ones will be inserted.
     * 
     * If you can use the other methods defined by this interface as they ought
     * to be more optimized.
     * 
     * @param newCategory
     *            new version of MCRCategory
     * @throws IllegalArgumentException
     *             if old version of MCRCategory does not exist
     * @return collection of replaced categories
     */
    Collection<? extends MCRCategory> replaceCategory(MCRCategory newCategory)
        throws IllegalArgumentException;

    /**
     * Sets or updates a label from a Category.
     * 
     * @param id
     *            ID of the category
     * @param label
     *            to be set or updated
     * @return category where the label was set
     */
    MCRCategory setLabel(MCRCategoryID id, MCRLabel label);

    /**
     * Sets a new set of labels from a Category.
     * 
     * @param id
     *            ID of the category
     * @param labels
     *            to be set
     * @return category where the labels was set
     */
    MCRCategory setLabels(MCRCategoryID id, Set<MCRLabel> labels);

    /**
     * Sets or updates the URI from a Category.
     * 
     * @param id
     *            ID of the category
     * @param uri
     *            to be set or updated
     * @return category where the uri was set
     */
    MCRCategory setURI(MCRCategoryID id, URI uri);

    /**
     * allows to determine when the last change was made to the categories.
     * @return either the last change time or the init time of the DAO class
     */
    long getLastModified();

    /**
     * Gets the last modified timestamp for the given root id. If there is no timestamp at the moment -1 is returned.
     * 
     * @param root ID of root category
     * 
     * @return the last modified timestamp (if any) or -1
     */
    long getLastModified(String root);
}
