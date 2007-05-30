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

import java.util.ArrayList;
import java.util.List;

public class MCRClassificationServiceImpl implements MCRClassificationService {

    /**
     * calculates left and right value throug the subtree rooted at <code>co</code>.
     * @param co root node of subtree
     * @param startValue co.left will be set to this value
     * @return co.right
     */
    protected static int calculateLeftRight(MCRClassificationObject co, int startValue) {
        int curValue = startValue;
        co.setLeft(startValue);
        for (MCRClassificationObject child : co.getChildren()) {
            curValue = calculateLeftRight(child, ++curValue);
        }
        co.setRight(++curValue);
        return curValue;
    }

    /**
     * calculates level attribute of <code>co</code> and all of its children.
     * @param co root node of subtree
     * @param startValue co.level will set to this value
     * @return max level value in subtree
     */
    protected static int calculateLevel(MCRClassificationObject co, int startValue) {
        co.setLevel(startValue);
        int maxLevel = startValue;
        final int nextLevel = startValue + 1;
        for (MCRClassificationObject child : co.getChildren()) {
            int curValue = calculateLevel(child, nextLevel);
            if (curValue > maxLevel) {
                maxLevel = curValue;
            }
        }
        return maxLevel;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.classifications2.MCRClassificationService#hasChildren(org.mycore.datamodel.classifications2.MCRClassificationID)
     */
    public boolean hasChildren(MCRClassificationID cid) {
        // TODO: implement hasChildren
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.classifications2.MCRClassificationService#getChildren(org.mycore.datamodel.classifications2.MCRClassificationID)
     */
    public List<MCRClassificationObject> getChildren(MCRClassificationID cid) {
        // TODO: implement getChildren
        return new ArrayList<MCRClassificationObject>();
    }

}
