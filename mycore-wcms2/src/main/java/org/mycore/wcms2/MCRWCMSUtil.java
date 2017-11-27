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

package org.mycore.wcms2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.util.MCRServletContentHelper;
import org.mycore.wcms2.datamodel.MCRNavigation;

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
     */
    public static void save(MCRNavigation navigation, OutputStream out) throws JAXBException, IOException,
        JDOMException {
        JAXBContext jc = JAXBContext.newInstance(MCRNavigation.class);
        Marshaller m = jc.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        ByteArrayOutputStream bout = new ByteArrayOutputStream(MCRServletContentHelper.DEFAULT_BUFFER_SIZE);
        m.marshal(navigation, bout);
        byte[] xml = bout.toByteArray();
        if (saveInOldFormat()) {
            xml = convertToOldFormat(xml);
        }
        out.write(xml);
    }

    /**
     * Converts the navigation.xml to the old format.
     */
    private static byte[] convertToOldFormat(byte[] xml) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(new ByteArrayInputStream(xml));
        Element rootElement = doc.getRootElement();
        rootElement.setAttribute("href", rootElement.getName());
        List<org.jdom2.Element> children = rootElement.getChildren();
        for (Element menu : children) {
            String id = menu.getAttributeValue("id");
            menu.setName(id);
            menu.setAttribute("href", id);
            menu.removeAttribute("id");
        }
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        ByteArrayOutputStream bout = new ByteArrayOutputStream(xml.length);
        out.output(doc, bout);
        return bout.toByteArray();
    }

    private static boolean saveInOldFormat() {
        return MCRConfiguration.instance().getBoolean("MCR.NavigationFile.SaveInOldFormat");
    }

}
