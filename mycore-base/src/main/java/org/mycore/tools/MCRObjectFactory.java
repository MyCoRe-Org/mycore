/**
 * 
 */
package org.mycore.tools;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * @author shermann
 */
public class MCRObjectFactory {

    /***/
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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String toReturn = sdf.format(date);

        return toReturn;
    }

    /***/
    private static Element createMetadataElement(MCRObjectID id) {
        return id.toString().indexOf("_derivate_") != -1 ? new Element("derivate") : new Element("metadata");
    }

    /**
     * @param id
     */
    private static Element createRootElement(MCRObjectID id) {
        String rootTag = (id.toString().indexOf("_derivate_") != -1) ? "mycorederivate" : "mycoreobject";
        Element root = new Element(rootTag);
        root.setAttribute("ID", id.toString());
        root.setAttribute("label", id.toString());
        root.setAttribute("noNamespaceSchemaLocation", getXSD(id), MCRUtils.XSI);
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
