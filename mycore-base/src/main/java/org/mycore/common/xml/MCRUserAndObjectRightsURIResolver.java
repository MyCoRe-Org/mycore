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

package org.mycore.common.xml;

import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * URI-Resolver, that checks if a MyCoRe object is
 * worldReadable or worldReadableComplete and certain user and role information
 * 
 * It is used as replacement for Xalan-Java functions in XSLT3 stylesheets.
 * 
 * 
 * It is registered as property:
 * MCR.URIResolver.ModuleResolver.userobjectrights=org.mycore.common.xml.MCRUserAndObjectRightsURIResolver
 *  
 * returns for boolean results
 * an XML element &lt;boolean&gt; with text 'true' or 'false'
 * 
 * or for user attributes 
 * an XML element &lt;userattribute name='{key}'&gt;{value}&lt;/userattribute&gt;
 * 
 * sample usage (usually in SOLR indexing templates):
 * 
 * &lt;field name="worldReadable"&gt;
 *     &lt;xsl:value-of select="document(concat('userobjectrights:isWorldReadable:',@ID))/boolean" /&gt;
 * &lt;/field&gt;
 * &lt;field name="worldReadableComplete"&gt;
 *     &lt;xsl:value-of select="document(concat('userobjectrights:isWorldReadableComplete:',@ID))/boolean" /&gt;
 * &lt;/field&gt;
 * 
 */
public class MCRUserAndObjectRightsURIResolver implements URIResolver {

    static final Logger LOGGER = LogManager.getLogger(MCRURIResolver.class);

    @Override
    public Source resolve(String href, String base) {
        String query = href.substring(href.indexOf(":") + 1);

        String key = query.substring(0, query.indexOf(":"));
        String value = query.substring(query.indexOf(":") + 1);

        try {
            Document doc = MCRDOMUtils.getDocumentBuilder().newDocument();
            Element result = doc.createElement("boolean");
            doc.appendChild(result);
            if (Objects.equals(key, "isWorldReadable")) {
                result.appendChild(doc.createTextNode(Boolean.toString(MCRXMLFunctions.isWorldReadable(value))));
                return new DOMSource(doc);
            }
            if (Objects.equals(key, "isWorldReadableComplete")) {
                result.appendChild(
                    doc.createTextNode(Boolean.toString(MCRXMLFunctions.isWorldReadableComplete(value))));
                return new DOMSource(doc);
            }

            if (Objects.equals(key, "isDisplayedEnabledDerivate")) {
                result.appendChild(
                    doc.createTextNode(Boolean.toString(MCRAccessManager.checkDerivateDisplayPermission(value))));
                return new DOMSource(doc);
            }

            if (Objects.equals(key, "isCurrentUserInRole")) {
                result.appendChild(
                    doc.createTextNode(
                        Boolean.toString(MCRSessionMgr.getCurrentSession().getUserInformation().isUserInRole(value))));
                return new DOMSource(doc);
            }
            if (Objects.equals(key, "isCurrentUserSuperUser")) {
                result.appendChild(
                    doc.createTextNode(
                        Boolean.toString(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID()
                            .equals(MCRSystemUserInformation.getSuperUserInstance().getUserID()))));
                return new DOMSource(doc);
            }

            if (Objects.equals(key, "isCurrentUserGuestUser")) {
                result.appendChild(
                    doc.createTextNode(
                        Boolean.toString(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID()
                            .equals(MCRSystemUserInformation.getGuestInstance().getUserID()))));
                return new DOMSource(doc);
            }

            if (Objects.equals(key, "getCurrentUserAttribute")) {
                doc = MCRDOMUtils.getDocumentBuilder().newDocument();
                Element attr = doc.createElement("userattribute");
                attr.setAttribute("name", key);
                doc.appendChild(attr);
                attr.appendChild(
                    doc.createTextNode(MCRSessionMgr.getCurrentSession().getUserInformation().getUserAttribute(value)));
                return new DOMSource(doc);
            }

            return new DOMSource(doc);
        } catch (ParserConfigurationException e) {
            LOGGER.error("Could not create DOM document", e);
        }
        return null;
    }
}
