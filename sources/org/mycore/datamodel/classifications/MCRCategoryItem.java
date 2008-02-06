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

import java.io.Serializable;

/**
 * This method implemets a special category item of a MyCore classification.
 * 
 * @author Thomas Scheffler (yagee)
 * @author jens Kupferschmidt
 * 
 * @version $Revision$ $Date$
 */
public class MCRCategoryItem extends MCRClassificationObject implements Serializable {

    private static final long serialVersionUID = -6270311269042057342L;

    private int numberOfObjects;

    private String classID;

    private String parentID;

    private MCRLink link;

    /**
     * This method return the link entry.
     * 
     * @return the link entry as MCRLink
     */
    public MCRLink getLink() {
        return link;
    }

    /**
     * This method set the link entry.
     * 
     * @param link
     *            the link entry
     */
    public void setLink(MCRLink link) {
        this.link = link;
    }

    /**
     * This method return the classification ID.
     * 
     * @return the classification ID
     */
    public String getClassID() {
        return classID;
    }

    /**
     * This method set the classification ID.
     * 
     * @param classID
     *            the classification ID
     */
    public void setClassID(String classID) {
        this.classID = classID;
    }

    /**
     * This method return the parent category ID.
     * 
     * @return the parent category ID
     */
    public String getParentID() {
        return parentID;
    }

    /**
     * This method set the parent category ID.
     * 
     * @param parentID
     *            the parent category ID
     */
    public void setParentID(String parentID) {
        this.parentID = parentID;
    }

    /**
     * This method return the number of object.
     * 
     * @return the number of objects
     */
    public int getNumberOfObjects() {
        return numberOfObjects;
    }

    /**
     * This method set the number of objects.
     * 
     * @param numberOfObjects
     *            the number of objects
     */
    public void setNumberOfObjects(int numberOfObjects) {
        this.numberOfObjects = numberOfObjects;
    }

    /**
     * This method clone this class.
     * 
     * @return a clone of this class
     */
    public MCRCategoryItem clone() {
        MCRCategoryItem clone = (MCRCategoryItem) super.clone();
        if (link != null) {
            clone.link = link.clone();
        }
        return clone;
    }
    
    public static MCRCategoryItem retrieveCategoryItem(String classID, String categID){
        return CM.retrieveCategoryItem(classID, categID);
    }

}
