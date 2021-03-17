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

package org.mycore.solr.common.xml;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.xml.MCRDOMUtils;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLFunctions;
import org.w3c.dom.Document;

/**
 * URI-Resolver, that checks if a MyCoRe object is
 * worldReadable or worldReadableComplete
 * 
 * It is registered as property:
 * MCR.URIResolver.ModuleResolver.solrwr=org.mycore.solr.common.xml.MCRSolrWorldReadableURIResolver
 *  
 * returns an XML text node: 'true' or 'false'
 * 
 * sample usage (usually in SOLR indexing templates):
 * 
 * &lt;field name="worldReadable"&gt;
 *     &lt;xsl:value-of select="document(concat('solrwr:isWorldReadable:',@ID))/text()" /&gt;
 * &lt;/field&gt;
 * &lt;field name="worldReadableComplete"&gt;
 *     &lt;xsl:value-of select="document(concat('solrwr:isWorldReadableComplete:',@ID))/text()" /&gt;
 * &lt;/field&gt;
 * 
 */
public class MCRSolrWorldReadableURIResolver implements URIResolver {

    static final Logger LOGGER = LogManager.getLogger(MCRURIResolver.class);

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String query = href.substring(href.indexOf(":") + 1);

        String key = query.substring(0, href.indexOf(":"));
        String value = query.substring(href.indexOf(":") + 1);

        try {
            Document doc = MCRDOMUtils.getDocumentBuilder().newDocument();
            if ("isWorldReadable".equals(key)) {
                return new DOMSource(doc.createTextNode(Boolean.toString(MCRXMLFunctions.isWorldReadable(value))));
            }
            if ("isWorldReadableComplete".equals(key)) {
                return new DOMSource(
                    doc.createTextNode(Boolean.toString(MCRXMLFunctions.isWorldReadableComplete(value))));
            }
            if ("isCurrentUserInRole".equals(key)) {
                return new DOMSource(
                    doc.createTextNode(
                        Boolean.toString(MCRSessionMgr.getCurrentSession().getUserInformation().isUserInRole(value))));
            }
            if ("isCurrentUserSuperUser".equals(key)) {
                return new DOMSource(
                    doc.createTextNode(
                        Boolean.toString(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID()
                            .equals(MCRSystemUserInformation.getSuperUserInstance().getUserID()))));
            }

            if ("isCurrentUserGuestUser".equals(key)) {
                return new DOMSource(
                    doc.createTextNode(
                        Boolean.toString(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID()
                            .equals(MCRSystemUserInformation.getGuestInstance().getUserID()))));
            }

            if ("getCurrentUserAttribute".equals(key)) {
                return new DOMSource(
                    doc.createTextNode(MCRSessionMgr.getCurrentSession().getUserInformation().getUserAttribute(value)));
            }
            
            if ("isDisplayedEnabledDerivate".equals(key)) {
                return new DOMSource(
                    doc.createTextNode(Boolean.toString(MCRAccessManager.checkDerivateDisplayPermission(value))));
            }
            
            return new DOMSource(doc.createTextNode(""));

        } catch (ParserConfigurationException e) {
            LOGGER.error("Could not create DOM document", e);
        }
        return null;
    }
}
