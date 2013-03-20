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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.transform.TransformerException;

import org.jdom2.Attribute;
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

    private Set<String> displayedBindings = new HashSet<String>();

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

    public Document getEditedXML() {
        return editedXML;
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
        rememberDisplayedValues();
        return currentBinding.getValue();
    }

    public boolean hasValue(String value) {
        rememberDisplayedValues();
        return currentBinding.hasValue(value);
    }

    private void rememberDisplayedValues() {
        for (Object boundNode : currentBinding.getBoundNodes())
            displayedBindings.add(MCRXPathBuilder.buildXPath(boundNode));
    }

    public void setSubmittedValues(Map<String, String[]> submittedValues) throws JDOMException, ParseException {
        Set<String> submittedXPaths = submittedValues.keySet();
        MCRBinding root = new MCRBinding(editedXML);

        for (String xPath : submittedXPaths) {
            if (xPath.startsWith("/")) {
                String[] values = submittedValues.get(xPath);

                MCRBinding binding = new MCRBinding(xPath, root);
                List<Object> boundNodes = binding.getBoundNodes();
                while (boundNodes.size() < values.length)
                    addNewBoundNode(boundNodes);

                for (int i = 0; i < boundNodes.size(); i++) {
                    if ((values.length < i) && (values[i] != null) && (!values[i].trim().isEmpty()))
                        setValue(boundNodes.get(i), values[i].trim());
                }
            }
        }

        removeDeletedNodes();
    }

    private void removeDeletedNodes() throws JDOMException, ParseException {
        MCRBinding root = new MCRBinding(editedXML);
        List<Element> elementsToRemove = new ArrayList<Element>();

        for (String xPath : displayedBindings) {
            for (Object boundNode : new MCRBinding(xPath, root).getBoundNodes())
                if (boundNode instanceof Attribute)
                    ((Attribute) boundNode).detach();
                else
                    elementsToRemove.add((Element) boundNode);
        }
        for (Element elementToRemove : elementsToRemove)
            elementToRemove.detach();
    }

    private void addNewBoundNode(List<Object> boundNodes) {
        Element lastBoundElement = (Element) (boundNodes.get(boundNodes.size() - 1));
        Element newElement = lastBoundElement.clone();
        Element parent = lastBoundElement.getParentElement();
        int indexInParent = parent.indexOf(lastBoundElement) + 1;
        parent.addContent(indexInParent + 1, newElement);
        boundNodes.add(newElement);
        displayedBindings.add(MCRXPathBuilder.buildXPath(newElement));
    }

    private void setValue(Object node, String value) {
        if (node instanceof Element)
            ((Element) node).setText(value);
        else if (node instanceof Attribute)
            ((Attribute) node).setValue(value);

        String xPath = MCRXPathBuilder.buildXPath(node);
        displayedBindings.remove(xPath);
    }
}
