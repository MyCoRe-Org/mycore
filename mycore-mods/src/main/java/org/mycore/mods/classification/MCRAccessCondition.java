/**
 * 
 */
package org.mycore.mods.classification;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

/**
 * Translates <code>&lt;mods:accessCondition /&gt;</code> into mycore classifications
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRAccessCondition extends MCRAuthorityInfo {
    public String href;

    private static final MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    private static final Logger LOGGER = Logger.getLogger(MCRAccessCondition.class);

    public MCRAccessCondition(String href) {
        this.href = href;
    }

    /* (non-Javadoc)
     * @see org.mycore.mods.classification.MCRAuthorityInfo#lookupCategoryID()
     */
    @Override
    protected MCRCategoryID lookupCategoryID() {
        Collection<MCRCategory> categoryByURI = MCRAuthorityWithURI.getCategoryByURI(href);
        if (categoryByURI.size() > 1) {
            throw new MCRException(href + " is ambigous: "
                + Collections2.transform(categoryByURI, new Function<MCRCategory, MCRCategoryID>() {

                    @Override
                    public MCRCategoryID apply(MCRCategory input) {
                        return input.getId();
                    }
                }));
        }
        if (!categoryByURI.isEmpty()) {
            return categoryByURI.iterator().next().getId();
        }
        //maybe href is in form {authorityURI}#{categId}
        String categId, authorityURI = null;
        try {
            authorityURI = href.substring(0, href.lastIndexOf("#"));
            categId = href.substring(authorityURI.length() + 1);
        } catch (RuntimeException e) {
            LOGGER.warn("authorityURI:" + authorityURI + ", valueURI:" + href);
            throw e;
        }
        int internalStylePos = authorityURI.indexOf(MCRAuthorityWithURI.CLASS_URI_PART);
        if (internalStylePos > 0) {
            String rootId = authorityURI.substring(internalStylePos + MCRAuthorityWithURI.CLASS_URI_PART.length());
            MCRCategoryID catId = new MCRCategoryID(rootId, categId);
            if (DAO.exist(catId)) {
                return catId;
            }
        }
        Collection<MCRCategory> classes = MCRAuthorityWithURI.getCategoryByURI(authorityURI);
        for (MCRCategory cat : classes) {
            MCRCategoryID catId = new MCRCategoryID(cat.getId().getRootID(), categId);
            if (DAO.exist(catId)) {
                return catId;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.mycore.mods.classification.MCRAuthorityInfo#setInElement(org.jdom2.Element)
     */
    @Override
    public void setInElement(Element modsElement) {
        modsElement.setAttribute("href", href, MCRConstants.XLINK_NAMESPACE);
    }

    /* (non-Javadoc)
     * @see org.mycore.mods.classification.MCRAuthorityInfo#setInElement(org.w3c.dom.Element)
     */
    @Override
    public void setInElement(org.w3c.dom.Element modsElement) {
        modsElement.setAttributeNS(MCRConstants.XLINK_NAMESPACE.getURI(), "href", href);
    }

    @Override
    public String toString() {
        return href;
    }

}
