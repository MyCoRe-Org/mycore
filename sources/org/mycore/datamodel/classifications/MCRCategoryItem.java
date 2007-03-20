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

import org.mycore.common.MCRUsageException;

/**
 * This class represents a category item of the MyCoRe classification model and
 * implements the abstract MCRClassificationObject class.
 * 
 * @author Frank Luetzenkirchen
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRCategoryItem extends MCRClassificationObject {
    protected String parentID;
    protected String classifID;
    protected String URL;
    protected int counter;

    /**
     * The constructor to fill this item.
     * 
     * @param parent
     *            the parent MCRClassificationObject
     * @param ID
     *            an identifier String
     */
    public MCRCategoryItem(String ID, MCRClassificationObject parent) {
        super(ID);
        if (parent == null) {
            throw new MCRUsageException("Parameter parent is null");
        }
        this.classifID = parent.getClassificationID();
        if (ID.equals(classifID)) {
            throw new MCRUsageException("A category ID can not be the same as its classification ID");
        }

        if (parent instanceof MCRCategoryItem) {
            this.parentID = parent.ID;
        }

        if (parent.childrenIDs != null) {
            parent.childrenIDs = null;
        }

        URL = "";
    }

    /**
     * The constructor to fill this item.
     * 
     * @param ID
     *            an identifier String
     * @param classifID
     *            the ID of the classification
     * @param parentID
     *            the ID of the parent
     */
    public MCRCategoryItem(String ID, String classifID, String parentID) {
        super(ID);
        this.classifID = classifID;
        this.parentID = parentID;
        URL = "";
        counter =0;
    }

    /**
     * The method call the MCRClassificationManager to create this instance.
     */
    public final void create() {
        manager().createCategoryItem(this);
    }

    /**
     * The method call the MCRClassificationManager to delete this instance.
     */
    public void delete() {
        MCRClassificationObject parent = getParent();

        if (parent == null) {
            parent = getClassificationItem();
        }

        parent.childrenIDs = null;
        super.delete();
        manager().deleteCategoryItem(classifID, ID);
    }

    /**
     * The methode return the classification ID.
     * 
     * @return the classification ID
     */
    public String getClassificationID() {
        return classifID;
    }

    public MCRClassificationItem getClassificationItem() {
        ensureNotDeleted();

        return MCRClassificationItem.getClassificationItem(classifID);
    }

    public MCRCategoryItem getParent() {
        ensureNotDeleted();

        if (parentID == null) {
            return null;
        }
        return getCategoryItem(classifID, parentID);
    }

    public String getParentID() {
        ensureNotDeleted();

        return parentID;
    }

    /**
     * The method return a MCRCategoryItem for the given Classification and
     * Category ID.
     * 
     * @param classifID
     *            the classification ID
     * @param categID
     *            the category ID
     * @return a MCRCategoryItem
     */
    public static MCRCategoryItem getCategoryItem(String classifID, String categID) {
        if ((classifID == null) || (classifID.trim().length() == 0)) {
            throw new MCRUsageException("Parameter classifID is null or empty");
        }
        if ((categID == null) || (categID.trim().length() == 0)) {
            throw new MCRUsageException("Parameter categID is null or empty");
        }
        return manager().retrieveCategoryItem(classifID, categID);
    }

    /**
     * The method return a MCRCategoryItem for the given Classification and
     * Category label text.
     * 
     * @param classifID
     *            the classification ID
     * @param labeltext
     *            the category label text
     * @return a MCRCategoryItem
     */
    public static MCRCategoryItem getCategoryItemForLabelText(String classifID, String labeltext) {
        if ((classifID == null) || (classifID.trim().length() == 0)) {
            throw new MCRUsageException("Parameter classifID is null or empty");
        }
        if ((labeltext == null) || (labeltext.trim().length() == 0)) {
            throw new MCRUsageException("Parameter labeltext is null or empty");
        }
        return manager().retrieveCategoryItemForLabelText(classifID, labeltext);
    }

    /**
     * The method returns the URL string.
     * 
     * @return the URL string
     */
    public final String getURL() {
        return URL;
    }

    /**
     * The method set the URL string.
     * 
     * @param url
     *            the URL string
     */
    public final void setURL(String url) {
        if (url == null) {
            URL = "";

            return;
        }

        URL = url;
    }

    /**
     * Put all data to a string
     */
    public final String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Classification: ").append(classifID).append("\n");
        sb.append("Parent ID:      ").append(parentID).append("\n");
        sb.append(super.toString());
        sb.append("URL             ").append(URL).append("\n");

        return sb.toString();
    }
}
