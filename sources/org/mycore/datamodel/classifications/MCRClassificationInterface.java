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

import java.util.Vector;

/**
 * This is an interface to store classifications an categories in the
 * MyCoRe project.
 *
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public interface MCRClassificationInterface
  {

  /**
   * The method create a new MCRClassificationItem in the datastore.
   *
   * @param classification an instance of a MCRClassificationItem
   **/
  public void createClassificationItem(MCRClassificationItem classification);  

  /**
   * The method update a MCRClassificationItem in the datastore.
   *
   * @param classification an instance of a MCRClassificationItem
   **/
  public void updateClassificationItem(MCRClassificationItem classification);  
  
  /**
   * The method remove a MCRClassificationItem from the datastore.
   *
   * @param classifID the ID of the MCRClassificationItem
   **/
  public void deleteClassificationItem(String classifID);

  /**
   * The method return a MCRClassificationItem from the datastore.
   *
   * @param classifID the ID of the MCRClassificationItem
   **/
  public MCRClassificationItem retrieveClassificationItem(String classifID);

  /**
   * The method return if the MCRClassificationItem is in the datastore.
   *
   * @param classifID the ID of the MCRClassificationItem
   * @return true if the MCRClassificationItem was found, else false
   **/
  public boolean classificationItemExists(String classifID);

  /**
   * The method create a new MCRCategoryItem in the datastore.
   *
   * @param category an instance of a MCRCategoryItem
   **/
  public void createCategoryItem(MCRCategoryItem category);
    
  /**
   * The method update a MCRCategoryItem in the datastore.
   *
   * @param category an instance of a MCRCategoryItem
   **/
  public void updateCategoryItem(MCRCategoryItem category);

  /**
   * The method remove a MCRCategoryItem from the datastore.
   *
   * @param classifID the ID of the MCRClassificationItem
   * @param categID the ID of the MCRCategoryItem
   **/
  public void deleteCategoryItem(String classifID, String categID);

  /**
   * The method return a MCRCategoryItem from the datastore.
   *
   * @param classifID the ID of the MCRClassificationItem
   * @param categID the ID of the MCRCategoryItem
   **/
  public MCRCategoryItem retrieveCategoryItem(String classifID, String categID);
  
  /**
   * The method return if the MCRCategoryItem is in the datastore.
   *
   * @param classifID the ID of the MCRClassificationItem
   * @param categID the ID of the MCRCategoryItem
   * @return true if the MCRCategoryItem was found, else false
   **/
  public boolean categoryItemExists(String classifID, String categID);

  /**
   * The method return an ArrayList of MCRCategoryItems from the datastore.
   *
   * @param classifID the ID of the MCRClassificationItem
   * @param categID the ID of the MCRCategoryItem
   * @return a list of MCRCategoryItem children
   **/
  public Vector retrieveChildren(String classifID, String parentID);
  
  /**
   * The method return the number of MCRCategoryItems from the datastore.
   *
   * @param classifID the ID of the MCRClassificationItem
   * @param categID the ID of the MCRCategoryItem
   * @return the number of MCRCategoryItem children
   **/
  public int retrieveNumberOfChildren(String classifID, String parentID);
  
}

