package org.mycore.wcms2;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.wcms2.datamodel.MCRNavigation;
import org.xml.sax.SAXException;

public abstract class MCRWCMSUtil {

    public static MCRNavigation load(org.w3c.dom.Document doc) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(MCRNavigation.class);
        Unmarshaller m = jc.createUnmarshaller();
        Object navigation = m.unmarshal(doc);
        if (navigation instanceof MCRNavigation) {
            return (MCRNavigation) navigation;
        }
        return null;
    }

    public static MCRNavigation load(File navigationFile) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(MCRNavigation.class);
        Unmarshaller m = jc.createUnmarshaller();
        Object navigation = m.unmarshal(navigationFile);
        if (navigation instanceof MCRNavigation) {
            return (MCRNavigation) navigation;
        }
        return null;
    }

    /**
     * Save navigation.xml with JAXB.
     * If MCR.navigationFile.SaveInOldFormat is true the navigation is stored in the old format.
     * @throws TransformerException 
     * @throws IOException 
     * @throws SAXException 
     * @throws ParserConfigurationException 
     */
    public static void save(MCRNavigation navigation, OutputStream out) throws JAXBException, IOException,
        JDOMException {
        StringWriter stringWriter = new StringWriter();
        JAXBContext jc = JAXBContext.newInstance(MCRNavigation.class);
        Marshaller m = jc.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(navigation, stringWriter);
        String xmlAsString = stringWriter.toString();
        byte[] xml = xmlAsString.getBytes(StandardCharsets.UTF_8);
        if (saveInOldFormat()) {
            xml = convertToOldFormat(xml);
        }
        out.write(xml);
    }

    /**
     * Converts the navigation.xml to the old format.
     * 
     * @param outputStream
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws TransformerException
     */
    private static byte[] convertToOldFormat(byte[] xml) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(new ByteArrayInputStream(xml));
        Element rootElement = doc.getRootElement();
        rootElement.setAttribute("href", rootElement.getName());
        List<org.jdom2.Element> children = rootElement.getChildren();
        for (int i = 0; i < children.size(); i++) {
            org.jdom2.Element menu = children.get(i);
            String id = menu.getAttributeValue("id");
            menu.setName(id);
            menu.setAttribute("href", id);
            menu.removeAttribute("id");
        }
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        StringWriter stringWriter = new StringWriter();
        out.output(doc, stringWriter);
        return stringWriter.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static boolean saveInOldFormat() {
        return MCRConfiguration.instance().getBoolean("MCR.NavigationFile.SaveInOldFormat");
    }

}
