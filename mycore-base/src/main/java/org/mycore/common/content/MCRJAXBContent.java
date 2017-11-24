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

package org.mycore.common.content;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Source;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.transform.JDOMResult;
import org.xml.sax.SAXParseException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRJAXBContent<T> extends MCRXMLContent {

    JAXBContext ctx;

    T jaxbObject;

    public MCRJAXBContent(JAXBContext ctx, T jaxbObject) {
        super();
        this.ctx = ctx;
        this.jaxbObject = jaxbObject;
        Class<?> clazz = jaxbObject.getClass();
        if (!clazz.isAnnotationPresent(XmlRootElement.class)) {
            throw new IllegalArgumentException("Class " + clazz.getName() + " is not a JAXB annotated.");
        }
        this.docType = getRootTag(jaxbObject);
        setName(jaxbObject.getClass().getSimpleName() + "-" + jaxbObject + ".xml");
        setSystemId(jaxbObject.toString());
    }

    private String getRootTag(T jaxbObject) {
        return jaxbObject.getClass().getAnnotation(XmlRootElement.class).name();
    }

    private Marshaller getMarshaller() throws JAXBException {
        Marshaller marshaller = ctx.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_ENCODING, getSafeEncoding());
        return marshaller;
    }

    public T getObject() {
        return jaxbObject;
    }

    /* (non-Javadoc)
     * @see org.mycore.common.content.MCRContent#getSource()
     */
    @Override
    public Source getSource() throws IOException {
        try {
            Marshaller marshaller = getMarshaller();
            return new JAXBSource(marshaller, jaxbObject);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.mycore.common.content.MCRContent#sendTo(java.io.OutputStream)
     */
    @Override
    public void sendTo(OutputStream out) throws IOException {
        try {
            Marshaller marshaller = getMarshaller();
            marshaller.marshal(jaxbObject, out);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.mycore.common.content.MCRContent#sendTo(java.io.File)
     */
    @Override
    public void sendTo(File target) throws IOException {
        try {
            Marshaller marshaller = getMarshaller();
            marshaller.marshal(jaxbObject, target);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.mycore.common.content.MCRContent#asXML()
     */
    @Override
    public Document asXML() throws JDOMException, IOException, SAXParseException {
        JDOMResult result = new JDOMResult();
        try {
            Marshaller marshaller = getMarshaller();
            marshaller.marshal(jaxbObject, result);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
        return result.getDocument();
    }
}
