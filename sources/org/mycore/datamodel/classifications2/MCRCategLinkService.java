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

import java.util.Collection;
import java.util.Map;

/**
 * 
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 * @since 2.0
 */
public interface MCRCategLinkService extends MCRTransactionalDAO {

    /**
     * Counts links to a collection of categories.
     * 
     * @param categIDs
     *            Collection of MCRCategoryID which links should be
     *            counted
     * @return a Map with MCRCategoryID as key and the number of links as
     *         value
     */
    public abstract Map<MCRCategoryID, Number> countLinks(Collection<MCRCategoryID> categIDs);

    /**
     * Counts links to a collection of categories.
     * 
     * @param categIDs
     *            Collection of MCRCategoryID which links should be
     *            counted
     * @param type
     *            restrict links that refer to object of this type
     * @return a Map with MCRCategoryID as key and the number of links as
     *         value
     */
    public abstract Map<MCRCategoryID, Number> countLinksForType(Collection<MCRCategoryID> categIDs, String type);

    /**
     * Delete all links that refer to the given Object ID.
     * 
     * @param id
     *            an Object ID
     * @see #deleteLinks(Collection)
     */
    public abstract void deleteLink(String id);

    /**
     * Delete all links that refer to the given collection of Object IDs.
     * 
     * @param ids
     *            a collection of Object IDs
     * @see #deleteLink(String)
     */
    public abstract void deleteLinks(Collection<String> ids);

    /**
     * Returns a list of linked Object IDs.
     * 
     * @param id
     *            ID of the category
     * @return Collection of Object IDs
     */
    public abstract Collection<String> getLinksFromCategory(MCRCategoryID id);

    /**
     * Returns a list of linked Object IDs restricted by the specified type.
     * 
     * @param id
     *            ID of the category
     * @param type
     *            restrict links that refer to object of this type
     * @return Collection of Object IDs
     */
    public abstract Collection<String> getLinksFromCategoryForType(MCRCategoryID id, String type);

    /**
     * Returns a list of linked categories.
     * 
     * @param id
     *            Object ID of a linked Object
     * @return list of MCRCategoryID of linked categories
     */
    public abstract Collection<MCRCategoryID> getLinksFromObject(String id);

    /**
     * Add links between categories and Objects.
     * 
     * Implementors must assure that ancestor (parent) axis categories are
     * implicit linked by this method.
     * 
     * @param map
     *            containing the single links
     * @see #countLinks(Collection)
     * @see #countLinksForType(Collection, String)
     */
    public abstract void setLinks(Map<MCRObjectReference, MCRCategoryID> map);

}
