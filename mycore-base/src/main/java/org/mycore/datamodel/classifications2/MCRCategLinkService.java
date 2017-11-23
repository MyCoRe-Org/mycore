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
    Map<MCRCategoryID, Boolean> hasLinks(MCRCategory category);

    /**
     * Checks if the category with the given id is liked with an object
     * 
     * @return
     *      true if is linked otherwise false
     */
    boolean hasLink(MCRCategory classif);

    /**
     * Counts links to a collection of categories.
     * 
     * @param category
     *            a subtree rooted at a MCRCategory for which links should be counted
     * @param childrenOnly
     *            if only direct children of category should be queried (query may be more optimized)
     * @return a Map with MCRCategoryID as key and the number of links as value
     */
    Map<MCRCategoryID, Number> countLinks(MCRCategory category, boolean childrenOnly);

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
    Map<MCRCategoryID, Number> countLinksForType(MCRCategory category, String type, boolean childrenOnly);

    /**
     * Delete all links that refer to the given {@link MCRCategLinkReference}.
     * 
     * @param id
     *            an Object ID
     * @see #deleteLinks(Collection)
     */
    void deleteLink(MCRCategLinkReference id);

    /**
     * Delete all links that refer to the given collection of category links.
     * 
     * @param ids
     *            a collection of {@link MCRCategLinkReference}
     * @see #deleteLink(MCRCategLinkReference)
     */
    void deleteLinks(Collection<MCRCategLinkReference> ids);

    /**
     * Returns a list of linked Object IDs.
     * 
     * @param id
     *            ID of the category
     * @return Collection of Object IDs, empty Collection when no links exist
     */
    Collection<String> getLinksFromCategory(MCRCategoryID id);

    /**
     * Checks if a given reference is in a specific category.
     * 
     * @param reference
     *            reference, e.g. to a MCRObject
     * @return true if the reference is in the category
     */
    boolean isInCategory(MCRCategLinkReference reference, MCRCategoryID id);

    /**
     * Returns a list of linked Object IDs restricted by the specified type.
     * 
     * @param id
     *            ID of the category
     * @param type
     *            restrict links that refer to object of this type
     * @return Collection of Object IDs
     */
    Collection<String> getLinksFromCategoryForType(MCRCategoryID id, String type);

    /**
     * Returns a list of linked categories.
     * 
     * @param reference
     *            reference, e.g. to a MCRObject
     * @return list of MCRCategoryID of linked categories
     */
    Collection<MCRCategoryID> getLinksFromReference(MCRCategLinkReference reference);

    /**
     * Return a collection of all category link references for the given type
     */
    Collection<MCRCategLinkReference> getReferences(String type);

    /**
     * Return a collection of all link types.
     * 
     */
    Collection<String> getTypes();

    /**
     * Returns a collection of all links for the given type.
     * 
     */
    Collection<MCRCategoryLink> getLinks(String type);

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
    void setLinks(MCRCategLinkReference objectReference, Collection<MCRCategoryID> categories);

}
