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
package org.mycore.datamodel.classifications2.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;

/**
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * @since 2.0
 */
public class MCRCategoryDAOImpl implements MCRCategoryDAO {

    /**
     * calculates left and right value throug the subtree rooted at
     * <code>co</code>.
     * 
     * @param co
     *            root node of subtree
     * @param leftStart
     *            co.left will be set to this value
     * @param levelStart co.getLevel() will return this value
     * @return co.right
     */
    protected static int calculateLeftRightAndLevel(MCRCategoryImpl co, int leftStart, int levelStart) {
        int curValue = leftStart;
        final int nextLevel = levelStart + 1;
        co.setLeft(leftStart);
        co.setLevel(levelStart);
        for (MCRCategory child : co.getChildren()) {
            curValue = calculateLeftRightAndLevel((MCRCategoryImpl) child, ++curValue, nextLevel);
        }
        co.setRight(++curValue);
        return curValue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.classifications2.MCRClassificationService#hasChildren(org.mycore.datamodel.classifications2.MCRCategoryID)
     */
    public boolean hasChildren(MCRCategoryID cid) {
        // TODO: implement hasChildren
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.classifications2.MCRClassificationService#getChildren(org.mycore.datamodel.classifications2.MCRCategoryID)
     */
    public List<MCRCategory> getChildren(MCRCategoryID cid) {
        // TODO: implement getChildren
        return new ArrayList<MCRCategory>();
    }

    public void addCategory(MCRCategoryID parentID, MCRCategory category) {
        // TODO Auto-generated method stub

    }

    public void beginTransaction() {
        // TODO Auto-generated method stub

    }

    public void commitTransaction() {
        // TODO Auto-generated method stub

    }

    public void deleteCategory(MCRCategoryID id) {
        // TODO Auto-generated method stub

    }

    public Collection<MCRCategory> getCategoriesByLabel(MCRCategoryID baseID, String lang, String text) {
        // TODO Auto-generated method stub
        return null;
    }

    public MCRCategory getCategory(MCRCategoryID id, int childLevel) {
        // TODO Auto-generated method stub
        return null;
    }

    public List<MCRCategory> getParents(MCRCategoryID id) {
        // TODO Auto-generated method stub
        return null;
    }

    public MCRCategory getRootCategory(MCRCategoryID baseID, int childLevel) {
        // TODO Auto-generated method stub
        return null;
    }

    public void moveCategory(MCRCategoryID id, MCRCategoryID newParentID) {
        // TODO Auto-generated method stub

    }

    public void moveCategory(MCRCategoryID id, MCRCategoryID newParentID, int index) {
        // TODO Auto-generated method stub

    }

    public void removeLabel(MCRCategoryID id, String lang) {
        // TODO Auto-generated method stub

    }

    public void rollBackTransaction() {
        // TODO Auto-generated method stub

    }

    public void setLabel(MCRCategoryID id, MCRLabel label) {
        // TODO Auto-generated method stub

    }

}
