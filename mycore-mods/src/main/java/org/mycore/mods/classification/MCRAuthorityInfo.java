package org.mycore.mods.classification;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRCache;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.w3c.dom.Element;

/**
 * MCRAuthorityInfo holds a combination of either authority ID and value code, or authorityURI and valueURI. In MODS,
 * this combination typically represents a value from a normed vocabulary like a classification. The AuthorityInfo can
 * be mapped to a MCRCategory in MyCoRe.
 * 
 * @see <a href="http://www.loc.gov/standards/mods/userguide/classification.html">MODS classification guidelines</a>
 * @author Frank L\u00FCtzenkirchen
 */
abstract class MCRAuthorityInfo {

    private static Logger LOGGER = LogManager.getLogger(MCRAuthorityInfo.class);

    private static final MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    /**
     * Returns the label value of the given type ("language"), or the given default if that label does not exist in the
     * category.
     */
    protected static String getLabel(MCRCategory category, String labelType, String defaultLabel) {
        return category.getLabel(labelType).map(MCRLabel::getText).orElse(defaultLabel);
    }

    /**
     * A cache that maps authority information to the category ID that is represented by that info.
     */
    private final static MCRCache<String, Object> categoryIDbyAuthorityInfo = new MCRCache<String, Object>(1000,
        "Category ID by authority info");

    /**
     * Used in the cache to indicate the case when no category ID maps to the given authority info
     */
    private final static String NULL = "null";

    /**
     * Returns the category ID that is represented by this authority information.
     * 
     * @return the category ID that maps this authority information, or null if no matching category exists.
     */
    public MCRCategoryID getCategoryID() {
        String key = toString();
        LOGGER.debug("get categoryID for " + key);

        Object categoryID = categoryIDbyAuthorityInfo.getIfUpToDate(key, DAO.getLastModified());
        if (categoryID == null) {
            LOGGER.debug("lookup categoryID for " + key);
            categoryID = lookupCategoryID();
            if (categoryID == null)
                categoryID = NULL; // Indicate that no matching category found, null can not be cached directly
            categoryIDbyAuthorityInfo.put(key, categoryID);
        }
        return categoryID instanceof MCRCategoryID ? (MCRCategoryID) categoryID : null;
    }

    /**
     * Looks up the category ID for this authority information in the classification database.
     */
    protected abstract MCRCategoryID lookupCategoryID();

    /**
     * Sets this authority information in the given MODS XML element by setting authority/authorityURI/valueURI
     * attributes and/or value code as text.
     */
    public abstract void setInElement(org.jdom2.Element modsElement);

    /**
     * Sets this authority information in the given MODS XML element by setting authority/authorityURI/valueURI
     * attributes and/or value code as text.
     */
    public abstract void setInElement(Element modsElement);

}
