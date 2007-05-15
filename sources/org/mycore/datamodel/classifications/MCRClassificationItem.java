/*
 * $RCSfile$
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRClassificationItem extends MCRClassificationObject implements Serializable {

    private static final long serialVersionUID = 147519495882309725L;

    private boolean counterEnabled;

    /**
     * This method return a boolean flag if counter is enabled.
     * 
     * @return true if counter is enabled, else false
     */
    public boolean isCounterEnabled() {
        return counterEnabled;
    }

    /**
     * This method set the counter enable flag.
     * 
     * @param counterEnabled
     *            the flag as true or false value
     */
    public void setCounterEnabled(boolean counterEnabled) {
        this.counterEnabled = counterEnabled;
    }

    /**
     * This method return a clone of this class.
     */
    public MCRClassificationItem clone() {
        return (MCRClassificationItem) super.clone();
    }

    /**
     * This method insert a path of categories into the class. The source is an
     * other MCRClassificationItem. The category is selected by name. The copy
     * of the category is a clone.
     * 
     * @param oldClass
     *            a source MCRClassificationItem
     * @param categname
     *            the name of the category
     */
    public final void copyCategory(MCRClassificationItem oldClass, String categname) {
        // get a list of categories as path to the category named categname
        ArrayList<MCRCategoryItem> path = getPath(oldClass, categname);
        if (LOGGER.isDebugEnabled()) {
            for (int i = 0; i < path.size(); i++) {
                MCRClassificationObject obj = path.get(i);
                LOGGER.debug("Add non removeable category item " + obj.getId());
            }
        }
        // add the path list of category clones to this classification
        addPathOfCategories(this, path);
    }

    private final ArrayList<MCRCategoryItem> getPath(MCRClassificationObject obj, String categname) {
        // get all categories of this item
        List<MCRCategoryItem> categories = obj.getCategories();
        // initialize return
        ArrayList<MCRCategoryItem> retlist = new ArrayList<MCRCategoryItem>();
        for (int i = 0; i < categories.size(); i++) {
            MCRCategoryItem item = (MCRCategoryItem) categories.get(i);
            // find category
            if (item.getId().equals(categname)) {
                MCRCategoryItem newitem = new MCRCategoryItem();
                newitem.setId(item.getId());
                newitem.setClassID(item.getClassID());
                if (item.getLabels() != null) {
                    List labellist = item.getLabels();
                    for (int j = 0; j < labellist.size(); j++) {
                        MCRLabel label = (MCRLabel) labellist.get(j);
                        newitem.addLabel(label.clone());
                    }
                }
                if (item.getLink() != null) {
                    newitem.setLink(item.getLink().clone());
                }
                retlist.add(newitem);
                return retlist;
            }
            // search for category
            ArrayList<MCRCategoryItem> path = getPath(item, categname);
            if (path.size() > 0) {
                MCRCategoryItem newitem = new MCRCategoryItem();
                newitem.setId(item.getId());
                newitem.setClassID(item.getClassID());
                if (item.getLabels() != null) {
                    List labellist = item.getLabels();
                    for (int j = 0; j < labellist.size(); j++) {
                        MCRLabel label = (MCRLabel) labellist.get(j);
                        newitem.addLabel(label.clone());
                    }
                }
                if (item.getLink() != null) {
                    newitem.setLink(item.getLink().clone());
                }
                path.add(newitem);
                return path;
            }
        }
        // return a null array if nothing found
        return retlist;
    }

    private final void addPathOfCategories(MCRClassificationObject obj, ArrayList<MCRCategoryItem> path) {
        // return is no more categories in the list.
        if (path.size() == 0)
            return;
        // check if it is the same ID, no cahnges
        MCRCategoryItem newitem = (MCRCategoryItem) path.get(path.size() - 1);
        List<MCRCategoryItem> categories = obj.getCategories();
        for (int i = 0; i < categories.size(); i++) {
            MCRCategoryItem item = (MCRCategoryItem) categories.get(i);
            if (newitem.getId().equals(item.getId())) {
                path.remove(path.size() - 1);
                addPathOfCategories(item, path);
                return;
            }
        }
        // item does not exists, add
        if (!obj.getId().equals("_class_")) {
            newitem.setParentID(obj.getId());
        }
        obj.getCategories().add(newitem);
        path.remove(path.size() - 1);
        addPathOfCategories(newitem, path);
    }
}
