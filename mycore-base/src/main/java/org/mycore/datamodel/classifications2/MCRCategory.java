package org.mycore.datamodel.classifications2;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * @since 2.0
 */
public interface MCRCategory {

    /**
     * @return true if this is a root category
     */
    boolean isClassification();

    /**
     * @return true if this is not a root category
     * @see #isClassification()
     */
    boolean isCategory();

    /**
     * Tells if this category has subcategories.
     * 
     * @return true if this category has subcategories
     * @see #getChildren()
     */
    boolean hasChildren();

    /**
     * Returns a list of subcategories.
     * 
     * Implementors must never return <code>null</code> if no children are
     * present. As this method may need a new call the underlaying persistence
     * layer use hasChildren() if you just want to know if subcategories are
     * present. Changes to the list may not affect the underlaying persistence
     * layer.
     * 
     * @return subcategories
     * @see #hasChildren()
     */
    List<MCRCategory> getChildren();

    /**
     * @return the id
     */
    MCRCategoryID getId();

    /**
     * @param id
     *            the id to set
     */
    void setId(MCRCategoryID id);

    /**
     * @return the labels
     */
    Set<MCRLabel> getLabels();

    /**
     * @return the label in the current language (if available), default language (if available), any language in
     * MCR.Metadata.Languages(if available), any other language that does not start with x- or any other language
     */
    Optional<MCRLabel> getCurrentLabel();

    /**
     * @return the label in the specified language (if available) or null
     */
    Optional<MCRLabel> getLabel(String lang);

    /**
     * Returns the hierarchie level of this category.
     * 
     * @return 0 if this is the root category
     */
    int getLevel();

    /**
     * Returns root category (the classification).
     * 
     * @return the root category
     */
    MCRCategory getRoot();

    /**
     * Returns the parent element
     * 
     * @return the categories parent or null if isClassification()==true or
     *         category currently not attached
     */
    MCRCategory getParent();

    /**
     * Returns the URI associated with this category.
     * 
     * @return the URI
     */
    URI getURI();

    /**
     * @param uri
     *            the URI to set
     */
    void setURI(URI uri);

}
