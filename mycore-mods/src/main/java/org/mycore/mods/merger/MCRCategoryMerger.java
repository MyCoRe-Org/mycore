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

package org.mycore.mods.merger;

import java.util.ArrayList;
import java.util.List;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.mods.classification.MCRClassMapper;

/**
 * Merges MODS elements that represent a classification category.
 * When those elements represent two categories A and B, 
 * and B is a child of A in the classification tree, 
 * B should win and be regarded the more detailed information, 
 * while A should be ignored.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRCategoryMerger extends MCRMerger {

    private static final MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    @Override
    public boolean isProbablySameAs(MCRMerger other) {
        if (!(other instanceof MCRCategoryMerger)) {
            return false;
        }

        MCRCategoryMerger cmOther = (MCRCategoryMerger) other;
        if (!MCRClassMapper.supportsClassification(this.element)
            || !MCRClassMapper.supportsClassification(cmOther.element)) {
            return false;
        }

        MCRCategoryID idThis = MCRClassMapper.getCategoryID(this.element);
        MCRCategoryID idOther = MCRClassMapper.getCategoryID(cmOther.element);
        if (idThis == null || idOther == null) {
            return false;
        }

        return idThis.equals(idOther) || oneIsDescendantOfTheOther(idThis, idOther);
    }

    static boolean oneIsDescendantOfTheOther(MCRCategoryID idThis, MCRCategoryID idOther) {
        List<MCRCategory> ancestorsAndSelfOfThis = getAncestorsAndSelf(idThis);
        List<MCRCategory> ancestorsAndSelfOfOther = getAncestorsAndSelf(idOther);

        return ancestorsAndSelfOfThis.containsAll(ancestorsAndSelfOfOther)
            || ancestorsAndSelfOfOther.containsAll(ancestorsAndSelfOfThis);
    }

    private static List<MCRCategory> getAncestorsAndSelf(MCRCategoryID categoryID) {
        List<MCRCategory> ancestorsAndSelf = new ArrayList<MCRCategory>();
        ancestorsAndSelf.addAll(DAO.getParents(categoryID));
        ancestorsAndSelf.remove(DAO.getRootCategory(categoryID, 0));
        ancestorsAndSelf.add(DAO.getCategory(categoryID, 0));
        return ancestorsAndSelf;
    }

    @Override
    public void mergeFrom(MCRMerger other) {
        MCRCategoryMerger cmo = (MCRCategoryMerger) other;

        MCRCategoryID idThis = MCRClassMapper.getCategoryID(this.element);
        MCRCategoryID idOther = MCRClassMapper.getCategoryID(cmo.element);

        if (idThis.equals(idOther)) {
            return;
        }

        if (getAncestorsAndSelf(idOther).containsAll(getAncestorsAndSelf(idThis))) {
            MCRClassMapper.assignCategory(this.element, idOther);
        }
    }
}
