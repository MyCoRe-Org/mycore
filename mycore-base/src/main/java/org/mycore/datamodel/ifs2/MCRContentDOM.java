/*
 * $Id$
 * $Revision: 5697 $ $Date: 22.02.2012 $
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

package org.mycore.datamodel.ifs2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jdom.JDOMException;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
class MCRContentDOM extends MCRContent {
    protected Document dom;

    MCRContentDOM(final Document dom) throws IOException {
        super(null);
        this.dom = dom;
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.ifs2.MCRContent#asXML()
     */
    @Override
    public org.jdom.Document asXML() throws JDOMException, IOException, SAXParseException {
        return new org.jdom.input.DOMBuilder().build(dom);
    }

    @Override
    public MCRContent ensureXML() {
        return this;
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.ifs2.MCRContent#getDocType()
     */
    @Override
    public String getDocType() {
        return dom.getDoctype() == null ? dom.getDocumentElement().getLocalName() : dom.getDoctype().getName();
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.ifs2.MCRContent#getInputStream()
     */
    @Override
    public InputStream getInputStream() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        sendTo(out);
        return new ByteArrayInputStream(out.toByteArray());
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.ifs2.MCRContent#getSource()
     */
    @Override
    public Source getSource() {
        DOMSource source = new DOMSource(dom);
        source.setSystemId(systemId);
        return source;
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.ifs2.MCRContent#makeCopies(int)
     */
    @Override
    public MCRContent[] makeCopies(final int numCopies) throws IOException {
        final MCRContent[] copies = new MCRContent[numCopies];
        for (int i = 0; i < numCopies; i++) {
            copies[i] = MCRContent.readFrom((Document) dom.cloneNode(true));
        }
        return copies;
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.ifs2.MCRContent#sendTo(java.io.OutputStream)
     */
    @Override
    public void sendTo(final OutputStream out) throws IOException {
        try {
            final Transformer xformer = TransformerFactory.newInstance().newTransformer();
            final Result result = new StreamResult(out);
            xformer.transform(getSource(), result);
        } catch (final Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            new IOException(e);
        }
    }

}
