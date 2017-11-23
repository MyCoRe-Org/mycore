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

package org.mycore.sword.application;

import java.util.List;

import org.mycore.datamodel.metadata.MCRObjectID;
import org.swordapp.server.SwordServerException;

/**
 * Should supply the mycore sword implementation with the right MyCoReIds. It also handles pagination.
 * The Standard impl. is {@link MCRSwordSolrObjectIDSupplier} and it should handle 99% of all use cases.
 */
public abstract class MCRSwordObjectIDSupplier {
    /**
     * @return how many objects a collection has
     * @throws SwordServerException if an error occurs while determining the result
     */
    public abstract long getCount() throws SwordServerException;

    /**
     * @param from first object id which should appear in the list
     * @param count count how many ids should appear in the list
     * @return a list of MyCoReObjectIDs
     * @throws SwordServerException if an error occurs while determining the result
     */
    public abstract List<MCRObjectID> get(int from, int count) throws SwordServerException;
}
