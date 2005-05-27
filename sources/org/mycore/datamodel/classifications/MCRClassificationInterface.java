/**
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
 *
 **/

package org.mycore.datamodel.classifications;

import java.util.ArrayList;

/**
 * This is an interface to store classifications an categories in the MyCoRe
 * project.
 * 
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public interface MCRClassificationInterface {

    /**
     * The method create a new MCRClassificationItem in the datastore.
     * 
     * @param classification
     *            an instance of a MCRClassificationItem
     */
    public void createClassificationItem(MCRClassificationItem classification);

    /**
     * The method remove a MCRClassificationItem from the datastore.
     * 
     * @param ID
     *            the ID of the MCRClassificationItem
     */
    public void deleteClassificationItem(String ID);

    /**
     * The method return a MCRClassificationItem from the datastore.
     * 
     * @param ID
     *            the ID of the MCRClassificationItem
     */
    public MCRClassificationItem retrieveClassificationItem(String ID);

    /**
     * The method return if the MCRClassificationItem is in the datastore.
     * 
     * @param ID
     *            the ID of the MCRClassificationItem
     * @return true if the MCRClassificationItem was found, else false
     */
    public boolean classificationItemExists(String ID);

    /**
     * The method create a new MCRCategoryItem in the datastore.
     * 
     * @param category
     *            an instance of a MCRCategoryItem
     */
    public void createCategoryItem(MCRCategoryItem category);

    /**
     * The method remove a MCRCategoryItem from the datastore.
     * 
     * @param CLID
     *            the ID of the MCRClassificationItem
     * @param ID
     *            the ID of the MCRCategoryItem
     */
    public void deleteCategoryItem(String CLID, String ID);

    /**
     * The method return a MCRCategoryItem from the datastore.
     * 
     * @param CLID
     *            the ID of the MCRClassificationItem
     * @param ID
     *            the ID of the MCRCategoryItem
     */
    public MCRCategoryItem retrieveCategoryItem(String CLID, String ID);

    /**
     * The method return a MCRCategoryItem from the datastore.
     * 
     * @param CLID
     *            the ID of the MCRClassificationItem
     * @param labeltext
     *            the label text of the MCRCategoryItem
     */
    public MCRCategoryItem retrieveCategoryItemForLabelText(String CLID,
            String labeltext);

    /**
     * The method return if the MCRCategoryItem is in the datastore.
     * 
     * @param CLID
     *            the ID of the MCRClassificationItem
     * @param ID
     *            the ID of the MCRCategoryItem
     * @return true if the MCRCategoryItem was found, else false
     */
    public boolean categoryItemExists(String CLID, String ID);

    /**
     * The method return an ArrayList of MCRCategoryItems from the datastore.
     * 
     * @param CLID
     *            the ID of the MCRClassificationItem
     * @param PID
     *            the parent ID of the MCRCategoryItem
     * @return a list of MCRCategoryItem children
     */
    public ArrayList retrieveChildren(String CLID, String PID);

    /**
     * The method return the number of MCRCategoryItems from the datastore.
     * 
     * @param CLID
     *            the ID of the MCRClassificationItem
     * @param PID
     *            the parent ID of the MCRCategoryItem
     * @return the number of MCRCategoryItem children
     */
    public int retrieveNumberOfChildren(String CLID, String PID);

    /**
     * The method returns all availiable classification ID's they are loaded.
     * 
     * @return a list of classification ID's as String array
     */
    public String[] getAllClassificationID();

}

