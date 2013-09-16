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

import org.jaxen.JaxenException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Parent;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRBinding {

    private final static Logger LOGGER = Logger.getLogger(MCRBinding.class);

    private XPathExpression<Object> xPath;

    private List<Object> boundNodes = new ArrayList<Object>();

    private String name;

    private List<MCRBinding> children = new ArrayList<MCRBinding>();

    private MCRBinding parent;

    public MCRBinding(Document document) throws JDOMException {
        this.xPath = XPathFactory.instance().compile("/", Filters.fpassthrough(), null, MCRUsedNamespaces.getNamespaces());
        this.boundNodes.add(document);
    }

    public MCRBinding(String xPathExpression, MCRBinding parent) throws JDOMException, JaxenException {
        this(xPathExpression, null, null, parent);
    }

    public MCRBinding(String xPathExpression, String defaultValue, String name, MCRBinding parent) throws JDOMException, JaxenException {
        if (!((name == null) || name.isEmpty()))
            this.name = name;

        this.parent = parent;
        parent.children.add(this);
        Map<String, Object> variables = buildXPathVariables();

        this.xPath = XPathFactory.instance().compile(xPathExpression, Filters.fpassthrough(), variables, MCRUsedNamespaces.getNamespaces());

        boundNodes.addAll(xPath.evaluate(parent.getBoundNodes()));

        LOGGER.debug("Bind to " + xPathExpression + " selected " + boundNodes.size() + " node(s)");

        if (boundNodes.isEmpty()) {
            Object built = MCRNodeBuilder.build(xPathExpression, defaultValue, variables, (Parent) (parent.getBoundNode()));
            LOGGER.debug("Bind to " + xPathExpression + " generated node " + MCRXPathBuilder.buildXPath(built));
            boundNodes.add(built);
        }
    }

    public MCRBinding(int pos, MCRBinding parent) {
        this.parent = parent;
        parent.children.add(this);
        
        String xPathExpression = parent.getRelativeXPath() + "[" + pos + "]";
        Map<String, Object> variables = buildXPathVariables();
        this.xPath = XPathFactory.instance().compile(xPathExpression, Filters.fpassthrough(), variables, MCRUsedNamespaces.getNamespaces());
        
        boundNodes.add(parent.getBoundNodes().get(pos - 1));
        LOGGER.debug("Repeater bind to " + xPathExpression + " selected " + boundNodes.size() + " node(s)");
    }

    public List<Object> getBoundNodes() {
        return boundNodes;
    }

    public Object getBoundNode() {
        return boundNodes.get(0);
    }

    public Element cloneBoundElement(int index) {
        Element template = (Element) (boundNodes.get(index));
        Element newElement = template.clone();
        Element parent = template.getParentElement();
        int indexInParent = parent.indexOf(template) + 1;
        parent.addContent(indexInParent, newElement);
        boundNodes.add(index + 1, newElement);
        return newElement;
    }

    public void detachBoundNodes() {
        while (!boundNodes.isEmpty())
            detachBoundNode();
    }

    public void detachBoundNode() {
        Object node = getBoundNode();
        if (node instanceof Attribute)
            ((Attribute) node).detach();
        else
            ((Element) node).detach();
        boundNodes.remove(node);
    }

    public MCRBinding getParent() {
        return parent;
    }

    public List<MCRBinding> getAncestorsAndSelf() {
        List<MCRBinding> ancestors = new ArrayList<MCRBinding>();
        MCRBinding current = this;
        do {
            ancestors.add(0, current);
            current = current.getParent();
        } while (current != null);
        return ancestors;
    }

    private String getValue(Object node) {
        if (node instanceof Element)
            return ((Element) node).getTextTrim();
        else
            return ((Attribute) node).getValue();
    }

    public String getValue() {
        return getValue(getBoundNode());
    }

    public boolean hasValue(String value) {
        for (Object node : boundNodes)
            if (value.equals(getValue(node)))
                return true;

        return false;
    }

    public void setValue(String value) {
        setValue(getBoundNode(), value);
    }

    public void setValue(int index, String value) {
        setValue(getBoundNodes().get(index), value);
    }

    private void setValue(Object node, String value) {
        if (node instanceof Element)
            ((Element) node).setText(value);
        else if (node instanceof Attribute)
            ((Attribute) node).setValue(value);
    }

    public String getName() {
        return name;
    }

    public List<MCRBinding> getChildren() {
        return children;
    }

    public Map<String, Object> buildXPathVariables() {
        Map<String, Object> variables = new HashMap<String, Object>();
        for (MCRBinding ancestor : getAncestorsAndSelf()) {
            for (MCRBinding child : ancestor.getChildren()) {
                String childName = child.getName();
                if (childName != null)
                    variables.put(childName, child.getBoundNodes());
            }
        }
        return variables;
    }

    public String getRelativeXPath() {
        return xPath.getExpression();
    }

    public String getAbsoluteXPath() {
        return MCRXPathBuilder.buildXPath(getBoundNode());
    }
}
