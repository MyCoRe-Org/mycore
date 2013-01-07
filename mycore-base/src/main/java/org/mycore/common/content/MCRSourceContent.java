/*
 * $Id$
 * $Revision: 5697 $ $Date: Jan 2, 2013 $
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

package org.mycore.common.content;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.transform.JDOMSource;
import org.w3c.dom.Node;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSourceContent extends MCRWrappedContent {
    private Source source;

    public MCRSourceContent(Source source) {
        this.source = source;
        MCRContent baseContent = null;
        if (source instanceof JDOMSource) {
            JDOMSource src = (JDOMSource) source;
            Document xml = src.getDocument();
            if (xml != null) {
                baseContent = new MCRJDOMContent(xml);
            } else {
                for (Object node : src.getNodes()) {
                    if (node instanceof Element) {
                        Element element = (Element) node;
                        Document doc = element.getDocument();
                        if (doc == null) {
                            baseContent = new MCRJDOMContent(element);
                        } else {
                            if (doc.getRootElement() == element) {
                                baseContent = new MCRJDOMContent(doc);
                            } else {
                                baseContent = new MCRJDOMContent((Element) element.clone());
                            }
                        }
                        break;
                    } else if (node instanceof Document) {
                        baseContent = new MCRJDOMContent((Document) node);
                        break;
                    }
                }
            }
        } else if (source instanceof SAXSource) {
            SAXSource src = (SAXSource) source;
            baseContent = new MCRSAXContent(src.getXMLReader(), src.getInputSource());
        } else if (source instanceof DOMSource) {
            Node node = ((DOMSource) source).getNode();
            baseContent = new MCRDOMContent(node.getOwnerDocument());
        } else if (source instanceof StreamSource) {
            InputStream inputStream = ((StreamSource) source).getInputStream();
            baseContent = new MCRStreamContent(inputStream);
        }
        if (baseContent != null) {
            baseContent.setSystemId(getSystemId());
        }
        this.setBaseContent(baseContent);
    }

    @Override
    public String getSystemId() {
        return source.getSystemId();
    }

    @Override
    public Source getSource() throws IOException {
        return source;
    }
}
