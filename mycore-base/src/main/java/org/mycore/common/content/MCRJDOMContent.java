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

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMSource;

/**
 * Reads MCRContent from a JDOM XML document.
 * 
 * @author Frank L\u00FCtzenkichen
 */
public class MCRJDOMContent extends MCRXMLContent {

    private Document jdom;

    /**
     * @param jdom the JDOM XML document to read from 
     */
    public MCRJDOMContent(Document jdom) {
        super();
        this.jdom = jdom;
        super.docType = jdom.getDocType() == null ? jdom.getRootElement().getName()
            : jdom.getDocType()
                .getElementName();
    }

    /**
     * Alternative constructor for newly created root elements
     * that do not have a Document parent yet, which is a very 
     * common use case.
     * 
     * @param jdom the JDOM XML root element to read from 
     */
    public MCRJDOMContent(Element jdom) {
        this(new Document(jdom));
    }

    @Override
    public Source getSource() {
        JDOMSource source = new JDOMSource(jdom);
        source.setSystemId(systemId);
        return source;
    }

    @Override
    public void sendTo(OutputStream out) throws IOException {
        if (jdom == null) {
            throw new IOException("JDOM document is null and cannot be written to OutputStream");
        }
        new XMLOutputter(format).output(jdom, out);
    }

    @Override
    public Document asXML() {
        return jdom;
    }
}
