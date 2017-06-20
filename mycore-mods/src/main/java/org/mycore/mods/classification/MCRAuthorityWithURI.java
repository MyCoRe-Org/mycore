package org.mycore.mods.classification;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.frontend.MCRFrontendUtil;
import org.w3c.dom.Element;

/**
 * Authority information that is represented by authorityURI and valueURI. Such authority info comes from a vocabulary
 * that is not registered at the Library of Congress, but maintained by an external authority like the MyCoRe
 * application.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
class MCRAuthorityWithURI extends MCRAuthorityInfo {
    /** The attribute holding the authority URI in XML */
    private static final String ATTRIBUTE_AUTHORITY_URI = "authorityURI";

    /** The attribute holding the value URI in XML */
    private static final String ATTRIBUTE_VALUE_URI = "valueURI";

    static final String CLASS_URI_PART = "classifications/";

    private static final MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    /**
     * xml:lang value of category or classification <label> for MODS @authorityURI or @valueURI.
     */
    private static final String LABEL_LANG_URI = "x-uri";

    private static final Logger LOGGER = LogManager.getLogger(MCRMODSClassificationSupport.class);

    /**
     * Inspects the attributes in the given MODS XML element and returns the AuthorityInfo given there.
     */
    public static MCRAuthorityWithURI getAuthorityInfo(Element element) {
        String authorityURI = element.getAttribute(ATTRIBUTE_AUTHORITY_URI);
        String valueURI = element.getAttribute(ATTRIBUTE_VALUE_URI);
        return getAuthorityInfo(authorityURI, valueURI);
    }

    /**
     * Inspects the attributes in the given MODS XML element and returns the AuthorityInfo given there.
     */
    public static MCRAuthorityWithURI getAuthorityInfo(org.jdom2.Element element) {
        String authorityURI = element.getAttributeValue(ATTRIBUTE_AUTHORITY_URI);
        String valueURI = element.getAttributeValue(ATTRIBUTE_VALUE_URI);
        return getAuthorityInfo(authorityURI, valueURI);
    }

    /**
     * Builds the authority info from the given values, does some checks on the values.
     * 
     * @return the authority info, or null if the values are illegal or unsupported.
     */
    private static MCRAuthorityWithURI getAuthorityInfo(String authorityURI, String valueURI) {
        if (authorityURI == null || authorityURI.isEmpty())
            return null;

        if (valueURI == null || valueURI.length() == 0) {
            LOGGER.warn("Did find attribute authorityURI='" + authorityURI + "', but no valueURI");
            return null;
        }
        return new MCRAuthorityWithURI(authorityURI, valueURI);
    }

    /**
     * Returns the authority URI for the given classification.
     */
    protected static String getAuthorityURI(MCRCategory classification) {
        String defaultURI = MCRFrontendUtil.getBaseURL() + CLASS_URI_PART + classification.getId().getRootID();
        return getLabel(classification, LABEL_LANG_URI, defaultURI);
    }

    /**
     * Returns the categories which have the given URI as label.
     */
    static Collection<MCRCategory> getCategoryByURI(final String uri) {
        return DAO.getCategoriesByLabel(LABEL_LANG_URI, uri);
    }

    /**
     * Returns the value URI for the given category and authority URI
     */
    protected static String getValueURI(MCRCategory category, String authorityURI) {
        String defaultURI = authorityURI + "#" + category.getId().getID();
        return getLabel(category, LABEL_LANG_URI, defaultURI);
    }

    /**
     * The authority URI
     */
    private String authorityURI;

    /**
     * The value URI
     */
    private String valueURI;

    public MCRAuthorityWithURI(String authorityURI, String valueURI) {
        this.authorityURI = authorityURI;
        this.valueURI = valueURI;
    }

    @Override
    protected MCRCategoryID lookupCategoryID() {
        for (MCRCategory category : getCategoryByURI(valueURI)) {
            if (authorityURI.equals(category.getRoot().getLabel(LABEL_LANG_URI).get().getText())) {
                return category.getId();
            }
        }
        //maybe valueUri is in form {authorityURI}#{categId}
        if (valueURI.startsWith(authorityURI) && authorityURI.length() < valueURI.length()) {
            String categId;
            try {
                categId = valueURI.substring(authorityURI.length() + 1);
            } catch (RuntimeException e) {
                LOGGER.warn("authorityURI:" + authorityURI + ", valueURI:" + valueURI);
                throw e;
            }
            int internalStylePos = authorityURI.indexOf(CLASS_URI_PART);
            if (internalStylePos > 0) {
                String rootId = authorityURI.substring(internalStylePos + CLASS_URI_PART.length());
                MCRCategoryID catId = new MCRCategoryID(rootId, categId);
                if (DAO.exist(catId)) {
                    return catId;
                }
            }
            return getCategoryByURI(authorityURI).stream()
                .map(cat -> new MCRCategoryID(cat.getId().getRootID(), categId))
                .filter(DAO::exist)
                .findFirst()
                .orElse(null);
        }
        return null;
    }

    @Override
    public void setInElement(Element element) {
        element.setAttribute(ATTRIBUTE_AUTHORITY_URI, authorityURI);
        element.setAttribute(ATTRIBUTE_VALUE_URI, valueURI);
    }

    @Override
    public void setInElement(org.jdom2.Element element) {
        element.setAttribute(ATTRIBUTE_AUTHORITY_URI, authorityURI);
        element.setAttribute(ATTRIBUTE_VALUE_URI, valueURI);
    }

    @Override
    public String toString() {
        return authorityURI + "#" + valueURI;
    }
}
