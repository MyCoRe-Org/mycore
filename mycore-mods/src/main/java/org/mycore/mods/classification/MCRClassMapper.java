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

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.xerces.impl.xs.XMLSchemaLoader;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSNamedMap;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCREntityResolver;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.mods.MCRMODSCommands;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRClassMapper {

    private static final String TYPE_OF_RESOURCE = "typeOfResource";

    private static final String ACCESS_CONDITION = "accessCondition";

    private static final Set<String> authorityElements, authorityURIElements, supported;

    private static final String NS_MODS_URI = MCRConstants.MODS_NAMESPACE.getURI();

    private static final MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    private static final MCRCache<String, String> authCache = new MCRCache<>(100, "MCRCategory authority");

    private static final MCRCache<String, String> authURICache = new MCRCache<>(100, "MCRCategory authorityURI");

    private static final MCRCache<MCRAuthKey, MCRAuthorityInfo> authInfoCache = new MCRCache<>(1000,
        "MCRAuthorityInfo cache");

    static {
        //check which MODS elements support authority or authorityURI
        XMLSchemaLoader loader = new XMLSchemaLoader();
        loader.setEntityResolver(MCREntityResolver.instance());
        InputSource resolveEntity;
        try {
            //use catalog to resolve MODS schema
            resolveEntity = MCREntityResolver.instance().resolveEntity(null, MCRMODSCommands.MODS_V3_XSD_URI);
        } catch (SAXException | IOException e) {
            resolveEntity = new InputSource(MCRMODSCommands.MODS_V3_XSD_URI);
        }
        XSModel model = loader.loadURI(resolveEntity.getSystemId());
        XSNamedMap map = model.getComponents(XSConstants.ELEMENT_DECLARATION);
        HashSet<String> authority = new HashSet<>();
        HashSet<String> authorityURI = new HashSet<>();
        for (int j = 0; j < map.getLength(); j++) {
            XSElementDeclaration o = (XSElementDeclaration) map.item(j);
            if (o.getTypeDefinition() instanceof XSComplexTypeDefinition) {
                //only complex type may contain attributes
                XSComplexTypeDefinition typeDef = (XSComplexTypeDefinition) o.getTypeDefinition();
                @SuppressWarnings("unchecked")
                List<XSAttributeUse> attributeUses = typeDef.getAttributeUses();
                for (XSAttributeUse attrUse : attributeUses) {
                    XSAttributeDeclaration attrDeclaration = attrUse.getAttrDeclaration();
                    switch (attrDeclaration.getName()) {
                        case "authority":
                            authority.add(o.getName());
                            break;
                        case "authorityURI":
                            authorityURI.add(o.getName());
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        authorityElements = Collections.unmodifiableSet(authority);
        authorityURIElements = Collections.unmodifiableSet(authorityURI);
        HashSet<String> merged = new HashSet<>(authorityURI);
        merged.addAll(authority);
        merged.add(ACCESS_CONDITION);
        merged.add(TYPE_OF_RESOURCE);
        supported = Collections.unmodifiableSet(merged);
    }

    private MCRClassMapper() {
    }

    public static boolean supportsClassification(org.jdom2.Element modsElement) {
        return modsElement.getNamespaceURI().equals(NS_MODS_URI) && supported.contains(modsElement.getName());
    }

    public static boolean supportsClassification(Element modsElement) {
        return modsElement.getNamespaceURI().equals(NS_MODS_URI) && supported.contains(modsElement.getLocalName());
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
                String authority = modsElement.getAttributeValue("authority");
                String code = modsElement.getTextTrim();
                authInfo = new MCRAuthorityAndCode(authority, code);
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
                String authority = modsElement.getAttribute("authority");
                String code = modsElement.getTextContent().trim();
                authInfo = new MCRAuthorityAndCode(authority, code);
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
        MCRAuthorityInfo authInfo = authInfoCache.getIfUpToDate(authKey, classLastModified);
        if (authInfo == null) {
            if (elementLocalName.equals(TYPE_OF_RESOURCE)
                && categID.getRootID().equals(MCRTypeOfResource.TYPE_OF_RESOURCE)) {
                authInfo = new MCRTypeOfResource(categID.getID().replace('_', ' '));
            }
            if (authInfo == null) {
                boolean supportCode = authorityElements.contains(elementLocalName);
                boolean supportURI = authorityURIElements.contains(elementLocalName);
                if (supportCode) {
                    String authority = getAuthority(categID.getRootID());
                    if (authority != null) {
                        authInfo = new MCRAuthorityAndCode(authority, categID.getID());
                    }
                }
                if (authInfo == null && supportURI) {
                    String authURI = getAuthorityURI(categID.getRootID());
                    String valueURI = MCRAuthorityWithURI.getValueURI(DAO.getCategory(categID, 0), authURI);
                    authInfo = new MCRAuthorityWithURI(authURI, valueURI);
                }
            }
            if (authInfo == null && elementLocalName.equals(ACCESS_CONDITION)) {
                String authURI = getAuthorityURI(categID.getRootID());
                String valueURI = MCRAuthorityWithURI.getValueURI(DAO.getCategory(categID, 0), authURI);
                authInfo = new MCRAccessCondition(valueURI);
            }
            if (authInfo == null) {
                authInfo = new MCRNullAuthInfo();
            }
            authInfoCache.put(authKey, authInfo, classLastModified);
        }
        return authInfo instanceof MCRNullAuthInfo ? null : authInfo;
    }

    private static String getAuthority(String rootID) {
        long classLastModified = Math.max(0, DAO.getLastModified(rootID));
        String auth = authCache.getIfUpToDate(rootID, classLastModified);
        if (auth == null) {
            MCRCategory rootCategory = DAO.getRootCategory(MCRCategoryID.rootID(rootID), 0);
            auth = rootCategory.getLabel("x-auth").map(MCRLabel::getText).orElse("");
            authCache.put(rootID, auth, classLastModified);
        }
        return auth.isEmpty() ? null : auth;
    }

    private static String getAuthorityURI(String rootID) {
        long classLastModified = Math.max(0, DAO.getLastModified(rootID));
        String authURI = authURICache.getIfUpToDate(rootID, classLastModified);
        if (authURI == null) {
            MCRCategory rootCategory = DAO.getRootCategory(MCRCategoryID.rootID(rootID), 0);
            authURI = MCRAuthorityWithURI.getAuthorityURI(rootCategory);
            authURICache.put(rootID, authURI, classLastModified);
        }
        return authURI;
    }

    private static class MCRAuthKey {
        String localName;

        MCRCategoryID categID;

        public MCRAuthKey(String localName, MCRCategoryID categID) {
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
                if (other.categID != null) {
                    return false;
                }
            } else if (!categID.equals(other.categID)) {
                return false;
            }
            return true;
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
