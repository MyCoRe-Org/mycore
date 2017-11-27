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

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.jdom2.JDOMException;
import org.jdom2.input.DOMBuilder;
import org.jdom2.output.XMLOutputter;
import org.w3c.dom.Document;

/**
 * Reads MCRContent from a W3C DOM XML document.
 * 
 * @author Frank L\u00FCtzenkichen
 */
public class MCRDOMContent extends MCRXMLContent {

    private Document dom;

    /**
     * @param dom the W3C DOM XML document to read from 
     */
    public MCRDOMContent(Document dom) {
        super();
        this.dom = dom;
        super.docType = dom.getDoctype() == null ? dom.getDocumentElement().getLocalName() : dom.getDoctype().getName();
    }

    @Override
    public Source getSource() {
        DOMSource source = new DOMSource(dom);
        source.setSystemId(systemId);
        return source;
    }

    @Override
    public void sendTo(OutputStream out) throws IOException {
        org.jdom2.Document jdom;
        try {
            jdom = asXML();
        } catch (JDOMException ex) {
            throw new IOException(ex);
        }
        new XMLOutputter(format).output(jdom, out);
    }

    @Override
    public org.jdom2.Document asXML() throws JDOMException {
        return new DOMBuilder().build(dom);
    }
}
