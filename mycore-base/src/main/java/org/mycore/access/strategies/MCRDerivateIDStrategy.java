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

package org.mycore.access.strategies;

import java.util.Collection;

import org.mycore.datamodel.common.MCRLinkTableManager;

/**
 * @author Silvio Hermann
 */
public class MCRDerivateIDStrategy implements MCRAccessCheckStrategy {

    @Override
    public boolean checkPermission(String id, String permission) {
        if (!id.contains("_derivate_")) {
            return new MCRObjectIDStrategy().checkPermission(id, permission);
        }
        final Collection<String> l = MCRLinkTableManager.instance().getSourceOf(id, "derivate");
        if (l != null && !l.isEmpty()) {
            return checkPermission(l.iterator().next(), permission);
        }
        return false;
    }

}
