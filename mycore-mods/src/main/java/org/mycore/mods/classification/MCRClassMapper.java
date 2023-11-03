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

import java.util.Set;

import org.mycore.common.MCRCache;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.w3c.dom.Element;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRClassMapper {

    private static final String TYPE_OF_RESOURCE = "typeOfResource";

    private static final String ACCESS_CONDITION = "accessCondition";

    //as of MODS 3.7 with addition of accessCondition
    private static final Set<String> SUPPORTED = Set.of("accessCondition", "area", "cartographics", "city",
        "citySection", "classification", "continent", "country", "county", "descriptionStandard",
        "extraTerrestrialArea", "form", "frequency", "genre", "geographic", "geographicCode", "hierarchicalGeographic",
        "island", "languageTerm", "name", "occupation", "physicalLocation", "placeTerm", "publisher",
        "recordContentSource", "region", "roleTerm", "scriptTerm", "state", "subject", "targetAudience", "temporal",
        "territory", "titleInfo", "topic", "typeOfResource");

    private static final String NS_MODS_URI = MCRConstants.MODS_NAMESPACE.getURI();

    private static final MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    private static final MCRCache<String, String> AUTH_CACHE = new MCRCache<>(100, "MCRCategory authority");

    private static final MCRCache<String, String> AUTH_URI_CACHE = new MCRCache<>(100, "MCRCategory authorityURI");

    private static final MCRCache<MCRAuthKey, MCRAuthorityInfo> AUTHORITY_INFO_CACHE = new MCRCache<>(1000,
        "MCRAuthorityInfo cache");

    private MCRClassMapper() {
    }

    public static boolean supportsClassification(org.jdom2.Element modsElement) {
        return modsElement.getNamespaceURI().equals(NS_MODS_URI) && SUPPORTED.contains(modsElement.getName());
    }

    public static boolean supportsClassification(Element modsElement) {
        return modsElement.getNamespaceURI().equals(NS_MODS_URI) && SUPPORTED.contains(modsElement.getLocalName());
    }

    public static MCRCategoryID getCategoryID(org.jdom2.Element modsElement) {
        if (supportsClassification(modsElement)) {
            MCRAuthorityInfo authInfo;
            if (modsElement.getAttributeValue("authorityURI") != null) {
                //authorityURI
                String authorityURI = modsElement.getAttributeValue("authorityURI");
                String valueURI = modsElement.getAttributeValue("valueURI");
                authInfo = new MCRAuthorityWithURI(authorityURI, valueURI);
            } else if (modsElement.getAttributeValue("authority") != null) {
                //authority
                authInfo = MCRAuthorityAndCode.getAuthorityInfo(modsElement);
            } else if (modsElement.getName().equals(ACCESS_CONDITION)) {
                //accessDefinition
                String href = modsElement.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE, "");
                authInfo = new MCRAccessCondition(href);
            } else if (modsElement.getName().equals(TYPE_OF_RESOURCE)) {
                //typeOfResource
                String code = modsElement.getTextTrim();
                authInfo = new MCRTypeOfResource(code);
            } else {
                return null;
            }
            if (authInfo == null) {
                return null;
            }
            return authInfo.getCategoryID();
        }
        return null;
    }

    public static MCRCategoryID getCategoryID(Element modsElement) {
        if (supportsClassification(modsElement)) {
            MCRAuthorityInfo authInfo;
            if (!modsElement.getAttribute("authorityURI").isEmpty()) {
                //authorityURI
                String authorityURI = modsElement.getAttribute("authorityURI");
                String valueURI = modsElement.getAttribute("valueURI");
                authInfo = new MCRAuthorityWithURI(authorityURI, valueURI);
            } else if (!modsElement.getAttribute("authority").isEmpty()) {
                //authority
                authInfo = MCRAuthorityAndCode.getAuthorityInfo(modsElement);
            } else if (modsElement.getLocalName().equals(ACCESS_CONDITION)) {
                //accessDefinition
                String href = modsElement.getAttributeNS(MCRConstants.XLINK_NAMESPACE.getURI(), "href");
                authInfo = new MCRAccessCondition(href);
            } else if (modsElement.getLocalName().equals(TYPE_OF_RESOURCE)) {
                //typeOfResource
                String code = modsElement.getTextContent().trim();
                authInfo = new MCRTypeOfResource(code);
            } else {
                return null;
            }
            if (authInfo == null) {
                return null;
            }
            return authInfo.getCategoryID();
        }
        return null;
    }

    public static void assignCategory(org.jdom2.Element modsElement, MCRCategoryID categID) {
        MCRAuthorityInfo authInfo = modsElement.getNamespace().equals(MCRConstants.MODS_NAMESPACE)
            ? getAuthInfo(categID, modsElement.getName())
            : null;
        if (authInfo == null) {
            throw new MCRException(modsElement.getQualifiedName() + " could not be assigned to category " + categID);
        }
        authInfo.setInElement(modsElement);
    }

    public static void assignCategory(Element modsElement, MCRCategoryID categID) {
        MCRAuthorityInfo authInfo = modsElement.getNamespaceURI().equals(MCRConstants.MODS_NAMESPACE.getURI())
            ? getAuthInfo(categID, modsElement.getLocalName())
            : null;
        if (authInfo == null) {
            throw new MCRException(modsElement.getTagName() + " could not be assigned to category " + categID);
        }
        authInfo.setInElement(modsElement);
    }

    private static MCRAuthorityInfo getAuthInfo(MCRCategoryID categID, String elementLocalName) {
        MCRAuthKey authKey = new MCRAuthKey(elementLocalName, categID);
        long classLastModified = Math.max(0, DAO.getLastModified(categID.getRootID()));
        MCRAuthorityInfo authInfo = AUTHORITY_INFO_CACHE.getIfUpToDate(authKey, classLastModified);
        if (authInfo == null) {
            if (elementLocalName.equals(TYPE_OF_RESOURCE)
                && categID.getRootID().equals(MCRTypeOfResource.TYPE_OF_RESOURCE)) {
                authInfo = new MCRTypeOfResource(categID.getId().replace('_', ' '));
            } else if (elementLocalName.equals(ACCESS_CONDITION)) {
                String authURI = getAuthorityURI(categID.getRootID());
                String valueURI = MCRAuthorityWithURI.getValueURI(DAO.getCategory(categID, 0), authURI);
                authInfo = new MCRAccessCondition(valueURI);
            } else if (SUPPORTED.contains(elementLocalName)) {
                String authority = getAuthority(categID.getRootID());
                if (authority != null) {
                    authInfo = new MCRAuthorityAndCode(authority, categID.getId());
                } else {
                    String authURI = getAuthorityURI(categID.getRootID());
                    String valueURI = MCRAuthorityWithURI.getValueURI(DAO.getCategory(categID, 0), authURI);
                    authInfo = new MCRAuthorityWithURI(authURI, valueURI);
                }
            }
            if (authInfo == null) {
                authInfo = new MCRNullAuthInfo();
            }
            AUTHORITY_INFO_CACHE.put(authKey, authInfo, classLastModified);
        }
        return authInfo instanceof MCRNullAuthInfo ? null : authInfo;
    }

    private static String getAuthority(String rootID) {
        long classLastModified = Math.max(0, DAO.getLastModified(rootID));
        String auth = AUTH_CACHE.getIfUpToDate(rootID, classLastModified);
        if (auth == null) {
            MCRCategory rootCategory = DAO.getRootCategory(MCRCategoryID.rootID(rootID), 0);
            auth = rootCategory.getLabel("x-auth").map(MCRLabel::getText).orElse("");
            AUTH_CACHE.put(rootID, auth, classLastModified);
        }
        return auth.isEmpty() ? null : auth;
    }

    private static String getAuthorityURI(String rootID) {
        long classLastModified = Math.max(0, DAO.getLastModified(rootID));
        String authURI = AUTH_URI_CACHE.getIfUpToDate(rootID, classLastModified);
        if (authURI == null) {
            MCRCategory rootCategory = DAO.getRootCategory(MCRCategoryID.rootID(rootID), 0);
            authURI = MCRAuthorityWithURI.getAuthorityURI(rootCategory);
            AUTH_URI_CACHE.put(rootID, authURI, classLastModified);
        }
        return authURI;
    }

    private static class MCRAuthKey {
        String localName;

        MCRCategoryID categID;

        MCRAuthKey(String localName, MCRCategoryID categID) {
            this.localName = localName;
            this.categID = categID;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((localName == null) ? 0 : localName.hashCode());
            result = prime * result + ((categID == null) ? 0 : categID.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            MCRAuthKey other = (MCRAuthKey) obj;
            if (localName == null) {
                if (other.localName != null) {
                    return false;
                }
            } else if (!localName.equals(other.localName)) {
                return false;
            }
            if (categID == null) {
                return other.categID == null;
            } else {
                return categID.equals(other.categID);
            }
        }

    }

    private static class MCRNullAuthInfo extends MCRAuthorityInfo {

        @Override
        protected MCRCategoryID lookupCategoryID() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setInElement(org.jdom2.Element modsElement) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setInElement(Element modsElement) {
            throw new UnsupportedOperationException();
        }

    }

}
