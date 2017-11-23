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

package org.mycore.common.content.transformer;

import java.io.IOException;

import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRXMLContent;
import org.xml.sax.SAXException;

/**
 * Transforms xml content in pretty, UTF-8 encoded format.
 *  
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRToPrettyXML extends MCRContentTransformer {

    @Override
    public MCRContent transform(MCRContent source) throws IOException {
        MCRXMLContent content;
        try {
            content = (source instanceof MCRXMLContent ? (MCRXMLContent) source : new MCRJDOMContent(source.asXML()));
        } catch (JDOMException | SAXException e) {
            throw new IOException(e);
        }
        if (content != source) {
            content.setName(source.getName());
            content.setLastModified(source.lastModified());
        }
        content.setFormat(Format.getPrettyFormat().setEncoding(getEncoding()));
        return content;
    }

    @Override
    public String getEncoding() {
        return "UTF-8";
    }

    @Override
    protected String getDefaultExtension() {
        return "xml";
    }

    @Override
    public String getMimeType() throws Exception {
        return "text/xml";
    }
}
