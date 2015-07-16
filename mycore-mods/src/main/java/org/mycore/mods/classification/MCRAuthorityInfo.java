package org.mycore.mods.classification;

import org.apache.log4j.Logger;
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
 * @see http://www.loc.gov/standards/mods/userguide/classification.html
 * @author Frank L\u00FCtzenkirchen
 */
abstract class MCRAuthorityInfo {

    private static Logger LOGGER = Logger.getLogger(MCRAuthorityInfo.class);

    private static final MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    /**
     * Inspects the attributes in the given MODS XML element and returns the AuthorityInfo given there.
     */
    public static MCRAuthorityInfo getAuthorityInfo(org.jdom2.Element modsElement) {
        MCRAuthorityInfo authorityInfo = MCRTypeOfResource.getAuthorityInfo(modsElement);
        if (authorityInfo == null)
            authorityInfo = MCRAuthorityWithURI.getAuthorityInfo(modsElement);
        if (authorityInfo == null)
            authorityInfo = MCRAuthorityAndCode.getAuthorityInfo(modsElement);
        return authorityInfo;
    }

    /**
     * Inspects the attributes in the given MODS XML element and returns the AuthorityInfo given there.
     */
    public static MCRAuthorityInfo getAuthorityInfo(Element modsElement) {
        MCRAuthorityInfo authorityInfo = MCRTypeOfResource.getAuthorityInfo(modsElement);
        if (authorityInfo == null)
            authorityInfo = MCRAuthorityWithURI.getAuthorityInfo(modsElement);
        if (authorityInfo == null)
            authorityInfo = MCRAuthorityAndCode.getAuthorityInfo(modsElement);
        return authorityInfo;
    }

    /** A cache that maps category ID to authority information */
    public final static MCRCache<String, MCRAuthorityInfo> authorityInfoByCategoryID = new MCRCache<String, MCRAuthorityInfo>(
        1000, "Authority info by category ID");

    /**
     * Returns the authority information that represents the category with the given ID.
     */
    public static MCRAuthorityInfo getAuthorityInfo(MCRCategoryID categoryID) {
        String key = categoryID.toString();
        LOGGER.debug("get authority info for " + key);

        MCRAuthorityInfo authorityInfo = authorityInfoByCategoryID.getIfUpToDate(key, DAO.getLastModified());

        if (authorityInfo == null) {
            authorityInfo = buildAuthorityInfo(categoryID);
            authorityInfoByCategoryID.put(key, authorityInfo);
        }
        return authorityInfo;
    }

    /**
     * Builds the authority information that represents the category with the given ID, by looking up x-auth and x-uri
     * labels set in the classification and category.
     */
    private static MCRAuthorityInfo buildAuthorityInfo(MCRCategoryID categoryID) {
        LOGGER.debug("build authority info for " + categoryID.toString());

        MCRCategory category = DAO.getCategory(categoryID, 0);
        MCRCategory classification = category.getRoot();

        if (classification.getId().getRootID().equals(MCRTypeOfResource.TYPE_OF_RESOURCE)) {
            return new MCRTypeOfResource(categoryID.getID().replace('_', ' ')); // Category IDs can not contain spaces
        }

        String authority = MCRAuthorityAndCode.getAuthority(classification);
        if (authority != null) {
            return new MCRAuthorityAndCode(authority, categoryID.getID());
        } else {
            String authorityURI = MCRAuthorityWithURI.getAuthorityURI(classification);
            String valueURI = MCRAuthorityWithURI.getValueURI(category, authorityURI);
            return new MCRAuthorityWithURI(authorityURI, valueURI);
        }
    }

    /**
     * Returns the label value of the given type ("language"), or the given default if that label does not exist in the
     * category.
     */
    protected static String getLabel(MCRCategory category, String labelType, String defaultLabel) {
        MCRLabel label = category.getLabel(labelType);
        return label == null ? defaultLabel : label.getText();
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
     * Looks up the category ID for this authority information in the classification database.
     */
    protected abstract MCRCategoryID lookupCategoryID();

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
