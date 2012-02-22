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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.xml.sax.SAXParseException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
class MCRContentIS extends MCRContent {

    MCRContentIS(final InputStream in, final String systemId) {
        super(false, systemId);
        this.in = in;
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.ifs2.MCRContent#asXML()
     */
    @Override
    public Document asXML() throws JDOMException, IOException, SAXParseException {
        return MCRXMLParserFactory.getNonValidatingParser().parseXML(this);
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.ifs2.MCRContent#getDocType()
     */
    @Override
    public String getDocType() {
        if (!(in instanceof ByteArrayInputStream)) {
            try {
                in = new ByteArrayInputStream(asByteArray());
            } catch (final IOException e) {
                throw new MCRException(e);
            }
        }
        String docType = MCRUtils.parseDocumentType(in);
        final int pos = docType.indexOf(':') + 1;
        if (pos > 0) {
            //filter namespace prefix
            docType = docType.substring(pos);
        }
        return docType;
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.ifs2.MCRContent#getInputStream()
     */
    @Override
    public InputStream getInputStream() throws IOException {
        return in;
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.ifs2.MCRContent#getSource()
     */
    @Override
    public Source getSource() {
        return new StreamSource(in, systemId);
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.ifs2.MCRContent#makeCopies(int)
     */
    @Override
    public MCRContent[] makeCopies(final int numCopies) throws IOException {
        final MCRContent[] copies = new MCRContent[numCopies];
        final byte[] bytes = asByteArray();
        for (int i = 0; i < numCopies; i++) {
            copies[i] = MCRContent.readFrom(bytes, systemId);
        }
        return copies;
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.ifs2.MCRContent#sendTo(java.io.OutputStream)
     */
    @Override
    public void sendTo(final OutputStream out) throws IOException {
        final InputStream input = getInputStream();
        MCRUtils.copyStream(input, out);
        input.close();
    }

}
