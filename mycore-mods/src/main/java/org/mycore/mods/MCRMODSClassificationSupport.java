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

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.frontend.servlets.MCRServlet;
import org.w3c.dom.Document;
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
    public static final String LABEL_LANG_AUTHORITY = "x-auth";

    private static final MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    private static final Logger LOGGER = Logger.getLogger(MCRMODSClassificationSupport.class);

    private static final DocumentBuilder DOC_BUILDER;
    static {
        DocumentBuilder documentBuilder = null;
        try {
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            LOGGER.error("Could not instantiate DocumentBuilder. Not all functions will be available.", e);
        }
        DOC_BUILDER = documentBuilder;
    }

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
     * @return a MCRCategoryID that should not be a root category or null if no such category exists.
     */
    public static MCRCategoryID getCategoryIDByValueURI(final String authorityURI, final String valueURI) {
        if (authorityURI.length() == 0 || valueURI.length() == 0) {
            return null;
        }
        final Collection<MCRCategory> categoryByURI = getCategoryByURI(valueURI);
        for (MCRCategory category : categoryByURI) {
            if (authorityURI.equals(category.getRoot().getLabel(LABEL_LANG_URI))) {
                return category.getId();
            }
        }
        //maybe valueUri is in form {authorityURI}#{categId}
        if (valueURI.startsWith(authorityURI)) {
            String categId;
            try {
                categId = valueURI.substring(authorityURI.length() + 1);
            } catch (RuntimeException e) {
                LOGGER.warn("authorityURI:" + authorityURI + ", valueURI:" + valueURI);
                throw e;
            }
            Collection<MCRCategory> classes = getCategoryByURI(authorityURI);
            for (MCRCategory cat : classes) {
                MCRCategoryID catId = new MCRCategoryID(cat.getId().getRootID(), categId);
                if (DAO.exist(catId)) {
                    return catId;
                }
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
        if (authorityURI != null && authorityURI.length() > 0) {
            final String valueURI = element.getAttribute("valueURI");
            if (valueURI == null || valueURI.length() == 0) {
                LOGGER.warn("Did find attribute authorityURI='" + authorityURI + "', but no valueURI");
                return null;
            }
            return getCategoryIDByValueURI(authorityURI, valueURI);
        }
        //test by authority
        final String authority = element.getAttribute("authority");
        if (authority != null && authority.length() > 0) {
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
            return getCategoryIDByValueURI(authorityURI, valueURI);
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

    public static NodeList getClassNodes(final NodeList sources) {
        try {
            final Element source = (Element) sources.item(0);
            final String classId = source.getAttributeNS(MCRConstants.MCR_NAMESPACE.getURI(), "classId");
            final String categId = source.getAttributeNS(MCRConstants.MCR_NAMESPACE.getURI(), "categId");
            final MCRCategoryID rootID = MCRCategoryID.rootID(classId);
            final MCRCategory cl = DAO.getRootCategory(rootID, 0);
            final Document document = DOC_BUILDER.newDocument();
            final Element returns = document.createElement("returns");
            final MCRLabel authLabel = cl.getLabel(LABEL_LANG_AUTHORITY);
            final String authority = authLabel == null ? null : authLabel.getText();
            if (authority != null) {
                returns.setAttribute("authority", authority);
                returns.setTextContent(categId);
            } else {
                final MCRLabel uriLabel = cl.getLabel(LABEL_LANG_URI);
                String authorityURI = uriLabel == null ? null : uriLabel.getText();
                if (authorityURI == null) {
                    authorityURI = MCRServlet.getBaseURL() + "classifications/" + classId;
                }
                returns.setAttribute("authorityURI", authorityURI);
                final MCRCategory category = DAO.getCategory(new MCRCategoryID(rootID.getRootID(), categId), 0);
                final MCRLabel categUriLabel = category.getLabel(LABEL_LANG_URI);
                String valueURI = categUriLabel == null ? null : categUriLabel.getText();
                if (valueURI == null) {
                    valueURI = authorityURI + "#" + categId;
                }
                returns.setAttribute("valueURI", valueURI);
            }
            return returns.getChildNodes();
        } catch (Throwable e) {
            LOGGER.warn("Error in Xalan Extension", e);
            return null;
        }
    }

    public static NodeList getMCRClassNodes(final NodeList sources) {
        try {
            final Element source = (Element) sources.item(0);
            MCRCategoryID category = getCategoryID(source);
            if (category == null) {
                return null;
            }
            final Document document = DOC_BUILDER.newDocument();
            final Element returns = document.createElement("returns");
            returns.setAttributeNS(MCRConstants.MCR_NAMESPACE.getURI(), "mcr:classId", category.getRootID());
            returns.setAttributeNS(MCRConstants.MCR_NAMESPACE.getURI(), "mcr:categId", category.getID());
            return returns.getChildNodes();
        } catch (Throwable e) {
            LOGGER.warn("Error in Xalan Extension", e);
            return null;
        }
    }

    public static String getClassCategLink(final NodeList sources) {
        final Element source = (Element) sources.item(0);
        MCRCategoryID category = getCategoryID(source);
        if (category == null) {
            return "";
        }
        return MessageFormat.format("classification:metadata:0:children:{0}:{1}", category.getRootID(), category.getID());
    }

    public static String getClassCategParentLink(final NodeList sources) {
        final Element source = (Element) sources.item(0);
        MCRCategoryID category = getCategoryID(source);
        if (category == null) {
            return "";
        }
        return MessageFormat.format("classification:metadata:0:parents:{0}:{1}", category.getRootID(), category.getID());
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
