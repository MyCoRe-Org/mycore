/*
 * 
 * $Revision$ $Date$
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

package org.mycore.datamodel.classifications;

import java.util.ArrayList;
import java.util.List;

import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This class is the manangement class for the SQL store of the classification
 * system of MyCoRe. They would only used by the MCRClassification.
 * 
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRClassificationManager {
    protected static MCRClassificationManager manager;

    /**
     * Make an instance of MCRClassificationManager.
     */
    public static MCRClassificationManager instance() {
        if (manager == null) {
            manager = new MCRClassificationManager();
        }

        return manager;
    }

    protected MCRCache categoryCache;

    protected MCRCache classificationCache;

    protected MCRCache jDomCache;

    protected MCRClassificationInterface store;

    private static final MCRConfiguration CONFIG = MCRConfiguration.instance();

    /**
     * Constructor for a new MCRClassificationManager.
     */
    protected MCRClassificationManager() {
        Object object = CONFIG.getInstanceOf("MCR.Persistence.Classification.Store.Class");
        store = (MCRClassificationInterface) object;

        int classifSize = CONFIG.getInt("MCR.Persistence.Classification.Store.CacheSize.Class", 30);
        int categSize = CONFIG.getInt("MCR.Persistence.Classification.Store.CacheSize.Categ", 500);
        classificationCache = new MCRCache(classifSize, "ClassificationManager Classifications");
        categoryCache = new MCRCache(categSize, "ClassificationManager Categories");
        jDomCache = new MCRCache(categSize, "ClassificationManager Classification Documents");

    }

    /**
     * This method create the classificaton in the store.
     * 
     * @param classification
     *            a classification as MCRClassificationItem
     */
    protected final void createClassificationItem(MCRClassificationItem classification) {
        if (store.classificationItemExists(classification.getId())) {
            throw new MCRPersistenceException("Classification already exists");
        }

        store.createClassificationItem(classification);
        classificationCache.put(classification.getId(), classification);
        CONFIG.systemModified();
    }

    /**
     * This method create the category in the store.
     * 
     * @param category
     *            a category as MCRCategoryItem
     */
    protected final void createCategoryItem(MCRCategoryItem category) {
        if (store.categoryItemExists(category.getClassID(), category.getId())) {
            throw new MCRPersistenceException("Category " + category.getId() + " already exists");
        }

        store.createCategoryItem(category);
        categoryCache.put(getCachingID(category), category);
        CONFIG.systemModified();
    }

    /**
     * This method create all categories they are stored in the list as
     * MCRCategoryItem in the store.
     * 
     * @param catlist
     *            a list of MCRCategoryItem
     */
    protected final void createCategoryItems(List catlist) {
        if ((catlist == null) || (catlist.size() == 0))
            return;
        for (int i = 0; i < catlist.size(); i++) {
            MCRCategoryItem cat = (MCRCategoryItem) catlist.get(i);
            createCategoryItem(cat);
            createCategoryItems(cat.getCategories());
        }
    }

    /**
     * This method return a MCRClassificationItem of a classification
     * 
     * @param ID
     *            the classification ID
     * @return the corresponding MCRClassificationItem
     */
    public final MCRClassificationItem retrieveClassificationItem(String ID) {
        MCRClassificationItem c = (MCRClassificationItem) (classificationCache.get(ID));
        if (c == null) {
            c = store.retrieveClassificationItem(ID);
            if (c != null) {
                classificationCache.put(ID, c);
                return c.clone();
            } else {
                return c;
            }
        }
        return c.clone();
    }

    /**
     * This method return a MCRCategoryItem of a category of a classification.
     * 
     * @param classifID
     *            the classification ID
     * @param categID
     *            the category ID
     * @return the corresponding MCRCategoryItem
     */
    public final MCRCategoryItem retrieveCategoryItem(String classifID, String categID) {
        String cachingID = classifID + "@@" + categID;
        MCRCategoryItem c = (MCRCategoryItem) (categoryCache.get(cachingID));
        if (c == null) {
            c = store.retrieveCategoryItem(classifID, categID);
            if (c != null) {
                categoryCache.put(cachingID, c);
                return c.clone();
            } else {
                return c;
            }
        }
        return c.clone();
    }

    /**
     * This method return a MCRCategoryItem of a classification with a given
     * label text.
     * 
     * @param classifID
     *            the classification ID
     * @param labeltext
     *            the text of the label
     * @return the corresponding MCRCategoryItem
     */
    public final MCRCategoryItem retrieveCategoryItemForLabelText(String classifID, String labeltext) {
        MCRCategoryItem c = store.retrieveCategoryItemForLabelText(classifID, labeltext);
        if (c == null) {
            return c;
        }
        return c.clone();
    }

    /**
     * This method return an array of MCRCategoryItem for a category of a
     * classification.
     * 
     * @param classifID
     *            the classification ID
     * @param parentID
     *            the category ID
     * @return
     */
    public final MCRCategoryItem[] retrieveChildren(String classifID, String parentID) {
        ArrayList retrieved = store.retrieveChildren(classifID, parentID);
        MCRCategoryItem[] children = new MCRCategoryItem[retrieved.size()];

        for (int i = 0; i < children.length; i++) {
            MCRCategoryItem cRetrieved = (MCRCategoryItem) (retrieved.get(i));
            String cachingID = getCachingID(cRetrieved);
            MCRCategoryItem cFromCache = (MCRCategoryItem) (categoryCache.get(cachingID));

            if (cFromCache != null) {
                children[i] = cFromCache;
            } else {
                children[i] = cRetrieved;
                categoryCache.put(cachingID, cRetrieved);
            }
        }

        return children;
    }

    /**
     * This method return the number of childs of a parent category for a
     * classification.
     * 
     * @param classifID
     *            the classification ID
     * @param parentID
     *            the category ID
     * @return the number of childs
     */
    public final int retrieveNumberOfChildren(String classifID, String parentID) {
        return store.retrieveNumberOfChildren(classifID, parentID);
    }

    /**
     * This method return a ID String of classification ID + '@@' + category ID.
     * 
     * @param category
     *            the MCRCategoryItem
     * @return an ID String
     */
    protected final String getCachingID(MCRCategoryItem category) {
        return category.getClassID() + "@@" + category.getId();
    }

    /**
     * This method return an array of IDs of all classifications they stored in
     * the system.
     * 
     * @return an array of IDs
     */
    protected final String[] getAllClassificationID() {
        return store.getAllClassificationID();
    }

    /**
     * This method return an array of all MCRClassificationItems they stored in
     * the system.
     * 
     * @return an array of all MCRClassificationItems
     */
    protected final MCRClassificationItem[] getAllClassification() {
        return store.getAllClassification();
    }

    /**
     * This method delete a classification from the store.
     * 
     * @param classifID
     *            the classification ID as MCRObjectID
     */
    protected final void deleteClassificationItem(MCRObjectID classifID) {
        deleteClassificationItem(classifID.getId());
    }

    /**
     * This method delete a classification from the store.
     * 
     * @param classifID
     *            the classification ID as String
     */
    protected final void deleteClassificationItem(String classifID) {
        classificationCache.remove(classifID);
        jDomCache.remove(classifID);
        store.deleteClassificationItem(classifID);
        CONFIG.systemModified();
    }

    /**
     * This method delete a category item from the store.
     * 
     * @param classifID
     *            the classification ID as MCRObjectID
     * @param categID
     *            the category ID as String
     */
    protected final void deleteCategoryItem(MCRObjectID classifID, String categID) {
        deleteCategoryItem(classifID.getId(), categID);
    }

    /**
     * This method delete a category item from the store.
     * 
     * @param classifID
     *            the classification ID as String
     * @param categID
     *            the category ID as String
     */
    protected final void deleteCategoryItem(String classifID, String categID) {
        categoryCache.remove(classifID + "@@" + categID);
        jDomCache.remove(classifID + "@@" + categID);
        store.deleteCategoryItem(classifID, categID);
        CONFIG.systemModified();
    }

}
