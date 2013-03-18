/*
 * $Revision$ 
 * $Date$
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

package org.mycore.frontend.xeditor;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.transform.TransformerException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRSourceContent;
import org.xml.sax.SAXException;

public class MCREditorSession {

    private static AtomicInteger idGenerator = new AtomicInteger(0);

    private String id;

    private Document editedXML;

    private MCRBinding currentBinding;

    private Map<String, String[]> parameters;

    public MCREditorSession(Map<String, String[]> parameters) {
        this.id = String.valueOf(idGenerator.incrementAndGet());
        this.parameters = parameters;
    }

    public String getID() {
        return id;
    }

    public void readSourceXML(String uri) throws JDOMException, IOException, SAXException, TransformerException {
        uri = replaceParameters(uri);
        if ((!uri.contains("{")) && (editedXML == null))
            setEditedXML(MCRSourceContent.getInstance(uri).asXML());
    }

    public void setRootElement(String rootElementName) throws JDOMException {
        if (editedXML == null)
            setEditedXML(new Document(new Element(rootElementName)));
    }

    private void setEditedXML(Document xml) throws JDOMException {
        editedXML = xml;
        currentBinding = new MCRBinding(editedXML);
    }

    private String replaceParameters(String uri) {
        for (String name : parameters.keySet()) {
            String token = "{" + name + "}";
            String value = parameters.get(name)[0];
            uri = uri.replace(token, value);
        }
        return uri;
    }

    public void bind(String xPath, String name) throws JDOMException, ParseException {
        currentBinding = new MCRBinding(xPath, name, currentBinding);
    }

    public void unbind() {
        currentBinding = currentBinding.getParent();
    }

    public String getAbsoluteXPath() {
        return currentBinding.getAbsoluteXPath();
    }

    public String getValue() {
        return currentBinding.getValue();
    }

    public boolean hasValue(String value) {
        return currentBinding.hasValue(value);
    }
}
