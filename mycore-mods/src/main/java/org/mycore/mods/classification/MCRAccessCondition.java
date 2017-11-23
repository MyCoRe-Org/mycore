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

package org.mycore.mods.classification;

import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;

/**
 * Translates <code>&lt;mods:accessCondition /&gt;</code> into mycore classifications
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRAccessCondition extends MCRAuthorityInfo {
    public String href;

    private static final MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    private static final Logger LOGGER = LogManager.getLogger(MCRAccessCondition.class);

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
            throw new MCRException(
                href + " is ambigous: " + categoryByURI.stream().map(MCRCategory::getId).collect(Collectors.toList()));
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
            LOGGER.warn("authorityURI:{}, valueURI:{}", authorityURI, href);
            return null;
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
        return classes.stream()
            .map(cat -> new MCRCategoryID(cat.getId().getRootID(), categId))
            .filter(DAO::exist)
            .findFirst()
            .orElse(null);
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
