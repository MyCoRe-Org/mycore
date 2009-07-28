/**
 * 
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.backend.hibernate.types;

import java.io.ByteArrayOutputStream;
import java.sql.Blob;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.lob.BlobImpl;
import org.hibernate.type.BlobType;

import org.mycore.common.MCRException;

public class MCRXMLBlobType extends BlobType {

    private static final long serialVersionUID = -5081036473140936467L;

    private static final DocumentFactory DOC_FACTORY = new DocumentFactory();

    @Override
    public Object fromXMLNode(Node xml, Mapping factory) {
        if (xml.getNodeType() != Node.ELEMENT_NODE) {
            throw new UnsupportedOperationException("Blobs can only be read from elements: " + xml.getNodeTypeName());
        }
        Element e = (Element) xml;
        OutputFormat format = OutputFormat.createCompactFormat();
        format.setTrimText(false);
        format.setNewLineAfterDeclaration(true);
        format.setSuppressDeclaration(false);
        format.setEncoding("UTF-8");
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            XMLWriter xmlWriter = new XMLWriter(bout, format);
            xmlWriter.write(e.content());
        } catch (Exception e1) {
            throw new MCRException("Cannot write xml elements to Blob.", e1);
        }
        BlobImpl blob = new BlobImpl(bout.toByteArray());
        return blob;
    }

    @Override
    public void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory) {
        Blob blob = (Blob) value;
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            throw new UnsupportedOperationException("Blobs can only be written to elements.");
        }
        Element e;
        SAXReader xmlReader = new SAXReader(DOC_FACTORY);
        xmlReader.setValidation(false);
        try {
            e = (Element) node;
            e.appendContent(xmlReader.read(blob.getBinaryStream()));
        } catch (Exception e1) {
            throw new MCRException("Cannot build xml elements from Blob.", e1);
        }
    }

}
