/*
 * $Id$
 * $Revision: 5697 $ $Date: 07.09.2011 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.mods;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public final class MCRMODSClassificationSupport {
    /**
     * xml:lang value of category or classification <label> for MODS @authorityURI or @valueURI.
     */
    public static final String LABEL_LANG_URI = "x-uri";

    /**
     * xml:lang value of category or classification <label> for MODS @authority.
     */
    public static final String LABEL_LANG_AUTHORITY = "x-authority";

    private static final MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    private static final Logger LOGGER = Logger.getLogger(MCRMODSClassificationSupport.class);

    private MCRMODSClassificationSupport() {
    };

    /**
     * returns a collection of categories which are label with a specific URI.
     * @param uri any valid URI
     * @return a collection of {@link MCRCategory}.
     */
    public static Collection<MCRCategory> getCategoryByURI(final String uri) {
        return DAO.getCategoriesByLabel(LABEL_LANG_URI, uri);
    }

    /**
     * returns a classification associated with the given authority.
     * @param authority a valid mods authority like "lcc"
     * @return a MCRCategory that should be a root category or null if no such category exists.
     */
    public static MCRCategory getCategoryByAuthority(final String authority) {
        final List<MCRCategory> categoriesByLabel = DAO.getCategoriesByLabel(LABEL_LANG_AUTHORITY, authority);
        if (categoriesByLabel.isEmpty()) {
            return null;
        }
        return categoriesByLabel.get(0);
    }

    /**
     * returns a category which matches to the given authorityURI and valueURI.
     * @param authorityURI any valid URI
     * @param valueURI any valid URI
     * @return a MCRCategory that should not be a root category or null if no such category exists.
     */
    public static MCRCategory getCategoryByValueURI(final String authorityURI, final String valueURI) {
        final Collection<MCRCategory> categoryByURI = getCategoryByURI(valueURI);
        for (MCRCategory category : categoryByURI) {
            if (authorityURI.equals(category.getRoot().getLabel(LABEL_LANG_URI))) {
                return category;
            }
        }
        return null;
    }

    /**
     * returns category ID which matches authority and code value of a MODS element.
     * @param authority any valid authority attribute value
     * @param code text node of the mods element where @type='code'
     * @return {@link MCRCategoryID} instance or null if no such category exists
     */
    public static MCRCategoryID getCategoryIDByCode(final String authority, final String code) {
        final MCRCategory categoryByAuthority = getCategoryByAuthority(authority);
        if (categoryByAuthority == null) {
            return null;
        }
        return new MCRCategoryID(categoryByAuthority.getId().getRootID(), code);
    }

    /**
     * returns a category ID which matches to the given MODS element node.
     * @param element mods element
     * @return {@link MCRCategoryID} instance or null if no such category exists
     */
    public static MCRCategoryID getCategoryID(final Element element) {
        //test by authority URI
        final String authorityURI = element.getAttribute("authorityURI");
        if (authorityURI != null) {
            final String valueURI = element.getAttribute("valueURI");
            if (valueURI == null) {
                LOGGER.warn("Did find attribute authorityURI='" + authorityURI + "', but no valueURI");
                return null;
            }
            final MCRCategory category = getCategoryByValueURI(authorityURI, valueURI);
            return category == null ? null : category.getId();
        }
        //test by authority
        final String authority = element.getAttribute("authority");
        if (authority != null) {
            final String type = element.getAttribute("type");
            if ("text".equals(type)) {
                LOGGER.warn("Type 'text' is currently unsupported when resolving a classification category");
                return null;
            }
            final String code = getText(element).trim();
            return getCategoryIDByCode(authority, code);
        }
        return null;
    }

    /**
     * returns a category ID which matches to the given MODS element node.
     * @param element mods element
     * @return {@link MCRCategoryID} instance or null if no such category exists
     */
    public static MCRCategoryID getCategoryID(final org.jdom.Element element) {
        //test by authority URI
        final String authorityURI = element.getAttributeValue("authorityURI");
        if (authorityURI != null) {
            final String valueURI = element.getAttributeValue("valueURI");
            if (valueURI == null) {
                LOGGER.warn("Did find attribute authorityURI='" + authorityURI + "', but no valueURI");
                return null;
            }
            final MCRCategory category = getCategoryByValueURI(authorityURI, valueURI);
            return category == null ? null : category.getId();
        }
        //test by authority
        final String authority = element.getAttributeValue("authority");
        if (authority != null) {
            final String type = element.getAttributeValue("type");
            if ("text".equals(type)) {
                LOGGER.warn("Type 'text' is currently unsupported when resolving a classification category");
                return null;
            }
            final String code = element.getTextTrim();
            return getCategoryIDByCode(authority, code);
        }
        return null;
    }

    private static String getText(final Element element) {
        final StringBuilder sb = new StringBuilder();
        final NodeList nodeList = element.getChildNodes();
        final int length = nodeList.getLength();
        for (int i = 0; i < length; i++) {
            final Node node = nodeList.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) {
                sb.append(node.getNodeValue());
            }
        }
        return sb.toString();
    }
}
