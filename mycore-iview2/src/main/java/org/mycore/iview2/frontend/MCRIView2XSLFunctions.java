/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.iview2.frontend;

import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import org.mycore.common.MCRXlink;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRMetaEnrichedLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRIView2XSLFunctions {

    private static final String METS_NAMESPACE_URI = "http://www.loc.gov/METS/";

    private static final MCRIView2XSLFunctionsAdapter ADAPTER = MCRIView2XSLFunctionsAdapter.obtainInstance();

    public static boolean hasMETSFile(String derivateID) {
        return ADAPTER.hasMETSFile(derivateID);
    }

    public static String getSupportedMainFile(String derivateID) {
        return ADAPTER.getSupportedMainFile(derivateID);
    }

    public static String getOptions(String derivateID, String extensions) {
        return ADAPTER.getOptions(derivateID, extensions);
    }

    /**
     * Get the full path of the main file of the first derivate.
     * 
     * @return the mainfile of the first derivate related to the given mcrid or
     *         null if there are no derivates related to the given mcrid
     */
    public static String getSupportedMainFileByOwner(String mcrID) {
        MCRObjectID objectID;
        try {
            objectID = MCRObjectID.getInstance(mcrID);
        } catch (Exception e) {
            return null;
        }

        MCRObject obj = MCRMetadataManager.retrieveMCRObject(objectID);
        List<MCRMetaEnrichedLinkID> derivates = obj.getStructure().getDerivates();
        if (!derivates.isEmpty()) {
            return derivates.getFirst() + "/" + ADAPTER.getSupportedMainFile(derivates.getFirst().toString());
        }
        return null;
    }

    public static String getThumbnailURL(String derivate, String imagePath) {
        String[] baseURLs = MCRConfiguration2.getStringOrThrow("MCR.Module-iview2.BaseURL").split(",");
        int index = imagePath.hashCode() % baseURLs.length;
        StringBuilder baseURL = new StringBuilder(baseURLs[index]);
        baseURL.append('/').append(derivate);
        if (imagePath.charAt(0) != '/') {
            baseURL.append('/');
        }
        int dotPos = imagePath.lastIndexOf('.');
        String imgPath = imagePath;
        if (dotPos > 0) {
            imgPath = imagePath.substring(0, dotPos);
        }
        baseURL.append(imgPath).append(".iview2/0/0/0.jpg");
        return baseURL.toString();
    }

    public static int getOrder(Node metsDiv) {
        Document document = metsDiv.getOwnerDocument();
        if (metsDiv.getLocalName().equals("smLink")) {
            return getSmLinkOrder(document, metsDiv);
        }
        Node structLink = document.getElementsByTagNameNS(METS_NAMESPACE_URI, "structLink").item(0);
        return getLowestOrder(document, metsDiv, structLink);
    }

    private static int getLowestOrder(Document document, Node metsDiv, Node structLink) {
        int order = Integer.MAX_VALUE;
        NodeList childDivs = metsDiv.getChildNodes();
        for (int i = 0; i < childDivs.getLength(); i++) {
            Node childDiv = childDivs.item(i);
            if (childDiv.getNodeType() != Node.ELEMENT_NODE || !childDiv.getLocalName().equals("div")) {
                continue;
            }
            order = Math.min(order, getLowestOrder(document, childDiv, structLink));
        }
        String logId = metsDiv.getAttributes().getNamedItem("ID").getNodeValue();
        NodeList smLinks = structLink.getChildNodes();
        for (int i = 0; i < smLinks.getLength(); i++) {
            Node smLink = smLinks.item(i);
            Node xlinkFrom = smLink.getAttributes().getNamedItemNS(XLINK_NAMESPACE.getURI(), MCRXlink.FROM);
            if (xlinkFrom == null || !xlinkFrom.getNodeValue().equals(logId)) {
                continue;
            }
            int smLinkOrder = getSmLinkOrder(document, smLink);
            order = Math.min(order, smLinkOrder);
        }
        return order;
    }

    private static int getSmLinkOrder(Document document, Node smLink) {
        Node xlinkTo = smLink.getAttributes().getNamedItemNS(XLINK_NAMESPACE.getURI(), MCRXlink.TO);
        Node physDiv = getElementById(document.getDocumentElement(), xlinkTo.getNodeValue());
        String orderValue = physDiv.getAttributes().getNamedItem("ORDER").getNodeValue();
        return Integer.parseInt(orderValue);
    }

    private static Node getElementById(Node base, String id) {
        NamedNodeMap attributes = base.getAttributes();
        if (IntStream.range(0, attributes.getLength())
            .mapToObj(i -> (Attr) attributes.item(i))
            .anyMatch(attr -> attr.getLocalName().equalsIgnoreCase("id") && attr.getNodeValue().equals(id))) {
            return base;
        }
        NodeList childNodes = base.getChildNodes();
        return IntStream.range(0, childNodes.getLength())
            .mapToObj(childNodes::item)
            .filter(child -> child.getNodeType() == Node.ELEMENT_NODE)
            .map(child -> getElementById(child, id))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }
}
