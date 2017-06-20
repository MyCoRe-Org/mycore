/**
 * 
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
public interface MCRCategLinkService {

    /**
     * Checks if a categories id refered by objects.
     * 
     * @param category
     *            a subtree rooted at a MCRCategory for which links should be counted
     * @return true if the classification is used
     */
    public abstract Map<MCRCategoryID, Boolean> hasLinks(MCRCategory category);

    /**
     * Checks if the category with the given id is liked with an object
     * 
     * @return
     *      true if is linked otherwise false
     */
    public abstract boolean hasLink(MCRCategory classif);

    /**
     * Counts links to a collection of categories.
     * 
     * @param category
     *            a subtree rooted at a MCRCategory for which links should be counted
     * @param childrenOnly
     *            if only direct children of category should be queried (query may be more optimized)
     * @return a Map with MCRCategoryID as key and the number of links as value
     */
    public abstract Map<MCRCategoryID, Number> countLinks(MCRCategory category, boolean childrenOnly);

    /**
     * Counts links to a collection of categories.
     * 
     * @param category
     *            a subtree rooted at a MCRCategory for which links should be counted
     * @param type
     *            restrict links that refer to object of this type
     * @param childrenOnly
     *            if only direct children of category should be queried (query may be more optimized)
     * @return a Map with MCRCategoryID as key and the number of links as value
     */
    public abstract Map<MCRCategoryID, Number> countLinksForType(MCRCategory category, String type,
        boolean childrenOnly);

    /**
     * Delete all links that refer to the given {@link MCRCategLinkReference}.
     * 
     * @param id
     *            an Object ID
     * @see #deleteLinks(Collection)
     */
    public abstract void deleteLink(MCRCategLinkReference id);

    /**
     * Delete all links that refer to the given collection of category links.
     * 
     * @param ids
     *            a collection of {@link MCRCategLinkReference}
     * @see #deleteLink(MCRCategLinkReference)
     */
    public abstract void deleteLinks(Collection<MCRCategLinkReference> ids);

    /**
     * Returns a list of linked Object IDs.
     * 
     * @param id
     *            ID of the category
     * @return Collection of Object IDs, empty Collection when no links exist
     */
    public abstract Collection<String> getLinksFromCategory(MCRCategoryID id);

    /**
     * Checks if a given reference is in a specific category.
     * 
     * @param reference
     *            reference, e.g. to a MCRObject
     * @return true if the reference is in the category
     */
    public abstract boolean isInCategory(MCRCategLinkReference reference, MCRCategoryID id);

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
     * @param reference
     *            reference, e.g. to a MCRObject
     * @return list of MCRCategoryID of linked categories
     */
    public abstract Collection<MCRCategoryID> getLinksFromReference(MCRCategLinkReference reference);

    /**
     * Return a collection of all category link references for the given type
     */
    public abstract Collection<MCRCategLinkReference> getReferences(String type);

    /**
     * Return a collection of all link types.
     * 
     */
    public abstract Collection<String> getTypes();

    /**
     * Returns a collection of all links for the given type.
     * 
     */
    public abstract Collection<MCRCategoryLink> getLinks(String type);

    /**
     * Add links between categories and Objects.
     * 
     * Implementors must assure that ancestor (parent) axis categories are
     * implicit linked by this method.
     * 
     * @param objectReference
     *            reference to a Object
     * @param categories
     *            a collection of categoryIDs to be linked to
     * @see #countLinks(MCRCategory, boolean)
     * @see #countLinksForType(MCRCategory, String, boolean)
     */
    public abstract void setLinks(MCRCategLinkReference objectReference, Collection<MCRCategoryID> categories);

}
