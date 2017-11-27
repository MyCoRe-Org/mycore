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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.sax.SAXHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSAXContent extends MCRXMLContent {

    private XMLReader xmlReader;

    private InputSource inputSource;

    public MCRSAXContent(XMLReader xmlReader, InputSource inputSource) {
        super();
        this.xmlReader = xmlReader;
        this.inputSource = inputSource;
        setSystemId(inputSource.getSystemId());
    }

    /* (non-Javadoc)
     * @see org.mycore.common.content.MCRContent#getInputStream()
     */
    @Override
    public InputStream getInputStream() throws IOException {
        return inputSource.getByteStream();
    }

    @Override
    public Source getSource() throws IOException {
        return new SAXSource(this.xmlReader, this.inputSource);
    }

    @Override
    public MCRContent ensureXML() {
        return this;
    }

    @Override
    public Document asXML() throws JDOMException, IOException, SAXException {
        SAXHandler jdomContentHandler = new SAXHandler();
        xmlReader.setContentHandler(jdomContentHandler);
        xmlReader.parse(inputSource);
        return jdomContentHandler.getDocument();
    }

    @Override
    public void setEncoding(String encoding) throws UnsupportedEncodingException {
        //defined by inputSource
    }

    @Override
    public String getEncoding() {
        return inputSource.getEncoding();
    }

}
