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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.w3c.dom.Element;

/**
 * Authority information that is represented by authority ID and code value. Such authority info comes from a
 * standardized vocabulary registered at the Library of Congress.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
class MCRAuthorityAndCode extends MCRAuthorityInfo {
    private static final MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    private static final Logger LOGGER = LogManager.getLogger(MCRAuthorityAndCode.class);

    /**
     * Inspects the attributes in the given MODS XML element and returns the AuthorityInfo given there.
     */
    public static MCRAuthorityAndCode getAuthorityInfo(org.jdom2.Element modsElement) {
        String authority = modsElement.getAttributeValue("authority");
        String type = modsElement.getAttributeValue("type");
        String code = modsElement.getTextTrim();
        return getAuthorityInfo(authority, type, code);
    }

    /**
     * Inspects the attributes in the given MODS XML element and returns the AuthorityInfo given there.
     */
    public static MCRAuthorityAndCode getAuthorityInfo(Element modsElement) {
        String authority = modsElement.getAttribute("authority");
        String type = modsElement.getAttribute("type");
        String code = MCRMODSClassificationSupport.getText(modsElement).trim();
        return getAuthorityInfo(authority, type, code);
    }

    /**
     * Builds the authority info from the given values, does some checks on the values.
     * 
     * @return the authority info, or null if the values are illegal or unsupported.
     */
    private static MCRAuthorityAndCode getAuthorityInfo(String authority, String type, String code) {
        if (authority == null)
            return null;

        if ("text".equals(type)) {
            LOGGER.warn("Type 'text' is currently unsupported when resolving a classification category");
            return null;
        }
        return new MCRAuthorityAndCode(authority, code);
    }

    /**
     * xml:lang value of category or classification <label> for MODS @authority.
     */
    public static final String LABEL_LANG_AUTHORITY = "x-auth";

    /**
     * Returns the authority code for the given classification
     */
    protected static String getAuthority(MCRCategory classification) {
        return getLabel(classification, LABEL_LANG_AUTHORITY, null);
    }

    /** The authority code */
    private String authority;

    /** The value code */
    private String code;

    public MCRAuthorityAndCode(String authority, String code) {
        this.authority = authority;
        this.code = code;
    }

    @Override
    public String toString() {
        return authority + "#" + code;
    }

    @Override
    protected MCRCategoryID lookupCategoryID() {
        return DAO
            .getCategoriesByLabel(LABEL_LANG_AUTHORITY, authority)
            .stream()
            .findFirst()
            .map(c -> new MCRCategoryID(c.getId().getRootID(), code))
            .orElse(null);
    }

    @Override
    public void setInElement(org.jdom2.Element element) {
        element.setAttribute("authority", authority);
        element.setText(code);
    }

    @Override
    public void setInElement(Element element) {
        element.setAttribute("authority", authority);
        element.setTextContent(code);
    }
}
