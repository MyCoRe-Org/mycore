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

package org.mycore.mods.classification;

import java.net.URISyntaxException;
import java.text.MessageFormat;

import javax.xml.parsers.DocumentBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRDOMUtils;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Thomas Scheffler (yagee)
 * @author Frank L\u00FCtzenkirchen
 */
public final class MCRMODSClassificationSupport {

    private static final Logger LOGGER = LogManager.getLogger(MCRMODSClassificationSupport.class);

    private MCRMODSClassificationSupport() {
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
            final Element returns = document.createElementNS(source.getNamespaceURI(), source.getLocalName());
            MCRClassMapper.assignCategory(returns, categoryID);
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
            MCRCategoryID category = MCRClassMapper.getCategoryID(source);
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
        MCRCategoryID category = MCRClassMapper.getCategoryID(source);
        if (category == null) {
            return "";
        }

        String id;
        try {
            id = MCRXMLFunctions.encodeURIPath(category.getID());
        } catch (URISyntaxException e) {
            /* This should be impossible! */
            throw new MCRException(e);
        }

        return MessageFormat.format("classification:metadata:0:children:{0}:{1}", category.getRootID(), id);
    }

    public static String getClassCategParentLink(final NodeList sources) {
        if (sources.getLength() == 0) {
            LOGGER.warn("Cannot get first element of node list 'sources'.");
            return "";
        }
        final Element source = (Element) sources.item(0);
        MCRCategoryID category = MCRClassMapper.getCategoryID(source);
        if (category == null) {
            return "";
        }

        String id;
        try {
            id = MCRXMLFunctions.encodeURIPath(category.getID());
        } catch (URISyntaxException e) {
            /* This should be impossible! */
            throw new MCRException(e);
        }

        return MessageFormat
            .format("classification:metadata:0:parents:{0}:{1}", category.getRootID(), id);
    }

    static String getText(final Element element) {
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
