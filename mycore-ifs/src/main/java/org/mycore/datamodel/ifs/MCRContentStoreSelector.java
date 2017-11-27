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

package org.mycore.datamodel.ifs;

import org.mycore.common.MCRException;

/**
 * Decides which MCRContentStore implementation should be used to store the
 * content of a given file. The system configuration sets the
 * MCRContentStoreSelector implementation that is used to make this decision by
 * the property "MCR.IFS.ContentStoreSelector.Class". MyCoRe provides a simple
 * implementation of this interface in the class MCRSimpleContentStoreSelector
 * that decides based on the file content type.
 * 
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public interface MCRContentStoreSelector {
    /**
     * Returns the ID of the file content store to be used to store the content
     * of the given file. The store selector can make the decision based on the
     * properties of the given file or based on system configuration.
     */
    String selectStore(MCRFile file) throws MCRException;

    String getDefaultStore();

    /**
     * Returns the IDs of all ContentStores available to MyCoRe
     * 
     * @return Array of StoreIDs
     */
    String[] getAvailableStoreIDs();
}
