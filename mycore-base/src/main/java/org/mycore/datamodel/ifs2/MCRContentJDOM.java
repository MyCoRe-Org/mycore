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

import javax.xml.transform.Source;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMSource;
import org.mycore.common.MCRConfiguration;
import org.xml.sax.SAXParseException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
class MCRContentJDOM extends MCRContent {

    protected Document jdom;

    /**
     * The format in which XML content is written. By default, this is 
     * pretty format using indentation and line breaks, UTF-8 encoded.
     * This can be changed to raw format without formatting by setting
     * MCR.IFS2.PrettyXML=false.
     */
    protected static Format xmlFormat;

    static {
        final boolean prettyXML = MCRConfiguration.instance().getBoolean("MCR.IFS2.PrettyXML", true);
        xmlFormat = prettyXML ? Format.getPrettyFormat().setIndent("  ") : Format.getRawFormat();
        xmlFormat.setEncoding("UTF-8");
    }

    MCRContentJDOM(final Document jdom) throws IOException {
        super(null);
        this.jdom = jdom;
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.ifs2.MCRContent#asXML()
     */
    @Override
    public Document asXML() throws JDOMException, IOException, SAXParseException {
        return (Document) jdom.clone();
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
        return jdom.getDocType() == null ? jdom.getRootElement().getName() : jdom.getDocType().getElementName();
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.ifs2.MCRContent#getInputStream()
     */
    @Override
    public InputStream getInputStream() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final XMLOutputter xout = new XMLOutputter(xmlFormat);
        xout.output(jdom, out);
        out.close();
        return new ByteArrayInputStream(out.toByteArray());
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.ifs2.MCRContent#getSource()
     */
    @Override
    public Source getSource() {
        JDOMSource source = new JDOMSource(jdom);
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
            copies[i] = MCRContent.readFrom((Document) jdom.clone());
        }
        return copies;
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.ifs2.MCRContent#sendTo(java.io.OutputStream)
     */
    @Override
    public void sendTo(final OutputStream out) throws IOException {
        final XMLOutputter xout = new XMLOutputter(xmlFormat);
        xout.output(jdom, out);
    }
}
