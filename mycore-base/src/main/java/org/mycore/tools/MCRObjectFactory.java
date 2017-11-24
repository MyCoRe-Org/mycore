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

package org.mycore.tools;

import java.util.Date;

import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * @author shermann
 */
public class MCRObjectFactory {

    /**
     * Creates a {@link Document} suitable for
     * {@link MCRObject#MCRObject(Document)}. The metadata element of this
     * mycore object is empty. The create and modify date are set to
     * "right now".
     */
    public static Document getSampleObject(MCRObjectID id) {
        Document xml = new Document();
        Element root = createRootElement(id);
        xml.setRootElement(root);
        root.addContent(createStructureElement());
        root.addContent(createMetadataElement(id));
        root.addContent(createServiceElement());
        return xml;
    }

    /***/
    private static Element createServiceElement() {
        Element service = new Element("service");
        Element servDates = new Element("servdates");
        service.addContent(servDates);
        Element createDate = new Element("servdate");
        createDate.setAttribute("type", "createdate");
        createDate.setAttribute("inherited", "0");

        Element modifyDate = new Element("servdate");
        modifyDate.setAttribute("type", "modifydate");
        modifyDate.setAttribute("inherited", "0");

        servDates.addContent(createDate);
        servDates.addContent(modifyDate);

        String text = getDateString(new Date());
        createDate.setText(text);
        modifyDate.setText(text);

        return service;
    }

    private static String getDateString(Date date) {
        MCRISO8601Date isoDate = new MCRISO8601Date();
        isoDate.setDate(date);
        return isoDate.getISOString();
    }

    /***/
    private static Element createMetadataElement(MCRObjectID id) {
        return "derivate".equals(id.getTypeId()) ? new Element("derivate") : new Element("metadata");
    }

    /**
     * @param id
     */
    private static Element createRootElement(MCRObjectID id) {
        String rootTag = "derivate".equals(id.getTypeId()) ? "mycorederivate" : "mycoreobject";
        Element root = new Element(rootTag);
        root.setAttribute("ID", id.toString());
        root.setAttribute("label", id.toString());
        root.setAttribute("noNamespaceSchemaLocation", getXSD(id), MCRConstants.XSI_NAMESPACE);
        root.setAttribute("version", "2.0");
        return root;
    }

    /**
     * Creates the structure element.
     */
    private static Element createStructureElement() {
        return new Element("structure");
    }

    /**
     * @param id
     * @return the name of the xsd schema depending on the given object id
     */
    private static String getXSD(MCRObjectID id) {
        return "datamodel-" + id.getTypeId();
    }
}
