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

package org.mycore.urn.services;

import java.util.List;
import java.util.Vector;

import org.mycore.common.MCRException;

/**
 * @author shermann
 * 
 */
@Deprecated
public class MCRURNProvider extends MCRAbstractURNProvider {

    /* (non-Javadoc)
     * @see fsu.archiv.mycore.urn.IURNProvider#generateURN()
     */
    public MCRURN generateURN() throws MCRException {
        String urn = MCRURNManager.buildURN("Default");
        return MCRURN.parse(urn);
    }

    /* (non-Javadoc)
     * @see fsu.archiv.mycore.urn.IURNProvider#generateURN(int)
     */
    public MCRURN[] generateURN(int amount) {
        List<MCRURN> list = new Vector<>(amount);
        while (amount != 0) {
            list.add(generateURN());
            amount--;
        }
        return list.toArray(new MCRURN[list.size()]);
    }
}
