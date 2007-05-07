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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * This calss implmets all common parts of a classifcation or category item.
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public abstract class MCRClassificationObject implements Cloneable {

    // logger
    static Logger LOGGER = Logger.getLogger(MCRClassificationObject.class);

    /** The langth of the languages * */
    public static final int MAX_CLASSIFICATION_LANG = 8;

    /** The length of the text * */
    public static final int MAX_CLASSIFICATION_TEXT = 254;

    /** The length of the description * */
    public static final int MAX_CLASSIFICATION_DESCRIPTION = 254;

    /** The length of the URL * */
    public static final int MAX_CATEGORY_URL = 254;

    String id;

    List<MCRLabel> labels;

    List<MCRCategoryItem> catgegories;

    /**
     * This method return a list of all category items.
     * 
     * @return a list of all category items
     */
    public List<MCRCategoryItem> getCategories() {
        if (catgegories == null) {
            catgegories = new ArrayList<MCRCategoryItem>();
        }
        return catgegories;
    }

    /**
     * This method set the categoy entry with a list of category items.
     * 
     * @param catgegories
     */
    public void setCatgegories(List<MCRCategoryItem> catgegories) {
        this.catgegories = catgegories;
    }

    /**
     * This method return the ID of this object.
     * 
     * @return the ID as String.
     */
    public String getId() {
        return id;
    }

    /**
     * This method set the ID from a String
     * 
     * @param id
     *            the ID as String
     */
    public void setId(String id) {
        this.id = id;
    }

    public void addLabel(MCRLabel label) {
        if (labels == null)
            labels = new ArrayList<MCRLabel>();
        labels.add(label);
    }

    public List<MCRLabel> getLabels() {
        if (labels == null) {
            labels = new ArrayList<MCRLabel>();
        }
        return labels;
    }

    public void setLabels(List<MCRLabel> labels) {
        this.labels = labels;
    }

    /**
     * This method return the MCRLabel object for a given language
     * 
     * @param lang
     *            the language as ISO String
     * @return the MCRLabel instance
     */
    public MCRLabel retrieveLabel(String lang) {
        if (labels.size() == 0) {
            return null;
        }
        for (MCRLabel lbl : labels) {
            if (lbl.getLang().equals(lang)) {
                return lbl;
            }
        }
        return (MCRLabel) labels.get(0);
    }

    public boolean equals(Object arg0) {
        if (!(arg0 instanceof MCRClassificationObject)) {
            return false;
        }
        MCRClassificationObject o = (MCRClassificationObject) arg0;
        return o.getId().equals(getId());
    }

    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public MCRClassificationObject clone() {
        MCRClassificationObject clone = null;
        try {
            clone = (MCRClassificationObject) super.clone();
        } catch (CloneNotSupportedException ce) {
            // Can not happen
        }

        // The clone has a reference to this object's category and label list,
        // so
        // owerwrite with null so it get initialized on next access;
        clone.catgegories = null;
        List<MCRCategoryItem> clonedCatList = clone.getCategories();
        clone.labels = null;
        List<MCRLabel> clonedLabelList = clone.getLabels();
        for (MCRCategoryItem category : catgegories) {
            clonedCatList.add(category.clone());
        }
        for (MCRLabel label : labels) {
            clonedLabelList.add(label.clone());
        }
        return clone;
    }

}
