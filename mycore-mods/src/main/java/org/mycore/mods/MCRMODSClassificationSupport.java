/*
 * $Id$
 * $Revision: 5697 $ $Date: 07.04.2011 $
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

import org.apache.log4j.Logger;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConstants;
import org.mycore.common.xml.MCRDOMUtils;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Thomas Scheffler (yagee)
 * @author Frank L\u00FCtzenkirchen
 */
public final class MCRMODSClassificationSupport {

    private static final MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    private static final Logger LOGGER = Logger.getLogger(MCRMODSClassificationSupport.class);

    private MCRMODSClassificationSupport() {
    }

    /**
     * Inspects the authority information in the given MODS XML element and returns a category ID which matches.
     * 
     * @param modsElement
     *            MODS element
     * @return {@link MCRCategoryID} instance or null if no such category exists
     */
    public static MCRCategoryID getCategoryID(Element modsElement) {
        MCRAuthorityInfo mCRAuthorityInfo = MCRAuthorityInfo.getAuthorityInfo(modsElement);
        return mCRAuthorityInfo == null ? null : mCRAuthorityInfo.getCategoryID();
    }

    /**
     * Inspects the authority information in the given MODS XML element and returns a category ID which matches.
     * 
     * @param modsElement
     *            MODS element
     * @return {@link MCRCategoryID} instance or null if no such category exists
     */
    public static MCRCategoryID getCategoryID(org.jdom2.Element modsElement) {
        MCRAuthorityInfo mCRAuthorityInfo = MCRAuthorityInfo.getAuthorityInfo(modsElement);
        return mCRAuthorityInfo == null ? null : mCRAuthorityInfo.getCategoryID();
    }

    /**
     * For a category ID, looks up the authority information for that category and sets the attributes in the given MODS
     * element so that it represents that category.
     * 
     * @param categoryID
     *            the ID of the category that is set
     * @param inElement
     *            the element in which authority/authorityURI/valueURI should be set.
     */
    public static void setAuthorityInfo(MCRCategoryID categoryID, org.jdom2.Element inElement) {
        MCRAuthorityInfo.getAuthorityInfo(categoryID).setInElement(inElement);
    }

    /**
     * For a category ID, looks up the authority information for that category and returns the attributes in the given
     * MODS element so that it represents that category. This is used as a Xalan extension.
     */
    public static NodeList getClassNodes(final NodeList sources) {
        if (sources.getLength() == 0) {
            LOGGER.warn("Cannot get first element of node list 'sources'.");
            return null;
        }
        DocumentBuilder documentBuilder = MCRDOMUtils.getDocumentBuilderUnchecked();
        try {
            Document document = documentBuilder.newDocument();
            final Element source = (Element) sources.item(0);
            final String categId = source.getAttributeNS(MCRConstants.MCR_NAMESPACE.getURI(), "categId");
            final MCRCategoryID categoryID = MCRCategoryID.fromString(categId);
            final MCRAuthorityInfo mCRAuthorityInfo = MCRAuthorityInfo.getAuthorityInfo(categoryID);
            final Element returns = document.createElement("returns");
            mCRAuthorityInfo.setInElement(returns);
            return returns.getChildNodes();
        } catch (Throwable e) {
            LOGGER.warn("Error in Xalan Extension", e);
            return null;
        } finally {
            MCRDOMUtils.releaseDocumentBuilder(documentBuilder);
        }
    }

    public static NodeList getMCRClassNodes(final NodeList sources) {
        if (sources.getLength() == 0) {
            LOGGER.warn("Cannot get first element of node list 'sources'.");
            return null;
        }
        DocumentBuilder documentBuilder = MCRDOMUtils.getDocumentBuilderUnchecked();
        try {
            final Document document = documentBuilder.newDocument();
            final Element source = (Element) sources.item(0);
            MCRCategoryID category = getCategoryID(source);
            if (category == null) {
                return null;
            }
            final Element returns = document.createElement("returns");
            returns.setAttributeNS(MCRConstants.MCR_NAMESPACE.getURI(), "mcr:categId", category.toString());
            return returns.getChildNodes();
        } catch (Throwable e) {
            LOGGER.warn("Error in Xalan Extension", e);
            return null;
        } finally {
            MCRDOMUtils.releaseDocumentBuilder(documentBuilder);
        }
    }

    public static String getClassCategLink(final NodeList sources) {
        if (sources.getLength() == 0) {
            LOGGER.warn("Cannot get first element of node list 'sources'.");
            return "";
        }
        final Element source = (Element) sources.item(0);
        MCRCategoryID category = getCategoryID(source);
        if (category == null) {
            return "";
        }
        return MessageFormat.format("classification:metadata:0:children:{0}:{1}", category.getRootID(),
            category.getID());
    }

    public static String getClassCategParentLink(final NodeList sources) {
        if (sources.getLength() == 0) {
            LOGGER.warn("Cannot get first element of node list 'sources'.");
            return "";
        }
        final Element source = (Element) sources.item(0);
        MCRCategoryID category = getCategoryID(source);
        if (category == null) {
            return "";
        }
        return MessageFormat
            .format("classification:metadata:0:parents:{0}:{1}", category.getRootID(), category.getID());
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

    /**
     * MCRAuthorityInfo holds a combination of either authority ID and value code, or authorityURI and valueURI. In
     * MODS, this combination typically represents a value from a normed vocabulary like a classification. The
     * AuthorityInfo can be mapped to a MCRCategory in MyCoRe.
     * 
     * @see http://www.loc.gov/standards/mods/userguide/classification.html
     * @author Frank L\u00FCtzenkirchen
     */
    private static abstract class MCRAuthorityInfo {

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
         * Builds the authority information that represents the category with the given ID, by looking up x-auth and
         * x-uri labels set in the classification and category.
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
         * Returns the label value of the given type ("language"), or the given default if that label does not exist in
         * the category.
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

    /**
     * Authority information that is represented by authority ID and code value. Such authority info comes from a
     * standardized vocabulary registered at the Library of Congress.
     * 
     * @author Frank L\u00FCtzenkirchen
     */
    private static class MCRAuthorityAndCode extends MCRAuthorityInfo {

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
            String code = getText(modsElement).trim();
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
            MCRCategory category = getCategoryByAuthority(authority);
            return category == null ? null : new MCRCategoryID(category.getId().getRootID(), code);
        }

        /**
         * Returns the classification associated with the given authority.
         * 
         * @param authority
         *            a valid MODS authority like "lcc"
         * @return a MCRCategory that should be a root category, or null if no such category exists.
         */
        private MCRCategory getCategoryByAuthority(final String authority) {
            List<MCRCategory> categoriesByLabel = DAO.getCategoriesByLabel(LABEL_LANG_AUTHORITY, authority);
            return categoriesByLabel.isEmpty() ? null : categoriesByLabel.get(0);
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

    /**
     * Authority information that is represented by authorityURI and valueURI. Such authority info comes from a
     * vocabulary that is not registered at the Library of Congress, but maintained by an external authority like the
     * MyCoRe application.
     * 
     * @author Frank L\u00FCtzenkirchen
     */
    private static class MCRAuthorityWithURI extends MCRAuthorityInfo {

        /** The attribute holding the value URI in XML */
        private static final String ATTRIBUTE_VALUE_URI = "valueURI";

        /** The attribute holding the authority URI in XML */
        private static final String ATTRIBUTE_AUTHORITY_URI = "authorityURI";

        /**
         * Inspects the attributes in the given MODS XML element and returns the AuthorityInfo given there.
         */
        public static MCRAuthorityWithURI getAuthorityInfo(org.jdom2.Element element) {
            String authorityURI = element.getAttributeValue(ATTRIBUTE_AUTHORITY_URI);
            String valueURI = element.getAttributeValue(ATTRIBUTE_VALUE_URI);
            return getAuthorityInfo(authorityURI, valueURI);
        }

        /**
         * Inspects the attributes in the given MODS XML element and returns the AuthorityInfo given there.
         */
        public static MCRAuthorityWithURI getAuthorityInfo(Element element) {
            String authorityURI = element.getAttribute(ATTRIBUTE_AUTHORITY_URI);
            String valueURI = element.getAttribute(ATTRIBUTE_VALUE_URI);
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
         * xml:lang value of category or classification <label> for MODS @authorityURI or @valueURI.
         */
        private static final String LABEL_LANG_URI = "x-uri";

        private static final String CLASS_URI_PART = "classifications/";

        /**
         * Returns the authority URI for the given classification.
         */
        protected static String getAuthorityURI(MCRCategory classification) {
            String defaultURI = MCRFrontendUtil.getBaseURL() + CLASS_URI_PART + classification.getId().getRootID();
            return getLabel(classification, LABEL_LANG_URI, defaultURI);
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
        public String toString() {
            return authorityURI + "#" + valueURI;
        }

        @Override
        protected MCRCategoryID lookupCategoryID() {
            for (MCRCategory category : getCategoryByURI(valueURI)) {
                if (authorityURI.equals(category.getRoot().getLabel(LABEL_LANG_URI).getText())) {
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
         * Returns the categories which have the given URI as label.
         */
        private static Collection<MCRCategory> getCategoryByURI(final String uri) {
            return DAO.getCategoriesByLabel(LABEL_LANG_URI, uri);
        }

        @Override
        public void setInElement(org.jdom2.Element element) {
            element.setAttribute(ATTRIBUTE_AUTHORITY_URI, authorityURI);
            element.setAttribute(ATTRIBUTE_VALUE_URI, valueURI);
        }

        @Override
        public void setInElement(Element element) {
            element.setAttribute(ATTRIBUTE_AUTHORITY_URI, authorityURI);
            element.setAttribute(ATTRIBUTE_VALUE_URI, valueURI);
        }
    }

    /**
     * Authority information that is a static mapping for mods:typeOfResource. This element is always mapped to a
     * classification with the ID typeOfResource.
     * 
     * @author Frank L\u00FCtzenkirchen
     */
    private static class MCRTypeOfResource extends MCRAuthorityInfo {

        /**
         * The name of the MODS element typeOfResource, which is same as the classification ID used to map the codes to
         * categories.
         */
        public final static String TYPE_OF_RESOURCE = "typeOfResource";

        /**
         * The mods:typeOfResource code, which is same as the category ID
         */
        private String code;

        public MCRTypeOfResource(String code) {
            this.code = code;
        }

        /**
         * If the given element is mods:typeOfResource, returns the MCRTypeOfResource mapping.
         */
        public static MCRTypeOfResource getAuthorityInfo(org.jdom2.Element modsElement) {
            if (modsElement == null) {
                return null;
            }
            String name = modsElement.getName();
            String code = modsElement.getTextTrim();
            return getTypeOfResource(name, code);
        }

        /**
         * If the given element is mods:typeOfResource, returns the MCRTypeOfResource mapping.
         */
        public static MCRTypeOfResource getAuthorityInfo(Element modsElement) {
            if (modsElement == null) {
                return null;
            }
            String name = modsElement.getLocalName();
            String code = getText(modsElement).trim();
            return getTypeOfResource(name, code);
        }

        /**
         * If the given element name is typeOfResource, returns the MCRTypeOfResource mapping.
         */
        private static MCRTypeOfResource getTypeOfResource(String name, String code) {
            return (name.equals(TYPE_OF_RESOURCE) && isClassificationPresent()) ? new MCRTypeOfResource(code) : null;
        }

        @Override
        public String toString() {
            return TYPE_OF_RESOURCE + "#" + code;
        }

        @Override
        protected MCRCategoryID lookupCategoryID() {
            return new MCRCategoryID(TYPE_OF_RESOURCE, code.replace(" ", "_")); // Category IDs can not contain spaces
        }

        @Override
        public void setInElement(org.jdom2.Element element) {
            element.setText(code);

        }

        @Override
        public void setInElement(Element element) {
            element.setTextContent(code);
        }

        public static boolean isClassificationPresent() {
            return MCRCategoryDAOFactory.getInstance().exist(MCRCategoryID.rootID(TYPE_OF_RESOURCE));
        }
    }
}
