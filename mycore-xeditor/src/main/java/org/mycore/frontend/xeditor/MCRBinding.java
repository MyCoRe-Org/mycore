/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.frontend.xeditor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaxen.JaxenException;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Parent;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.common.xml.MCRNodeBuilder;
import org.mycore.common.xml.MCRXPathBuilder;
import org.mycore.common.xml.MCRXPathEvaluator;
import org.mycore.frontend.xeditor.tracker.MCRAddedAttribute;
import org.mycore.frontend.xeditor.tracker.MCRAddedElement;
import org.mycore.frontend.xeditor.tracker.MCRChangeData;
import org.mycore.frontend.xeditor.tracker.MCRChangeTracker;
import org.mycore.frontend.xeditor.tracker.MCRRemoveAttribute;
import org.mycore.frontend.xeditor.tracker.MCRRemoveElement;
import org.mycore.frontend.xeditor.tracker.MCRSetAttributeValue;
import org.mycore.frontend.xeditor.tracker.MCRSetElementText;

/**
 * @author Frank Lützenkirchen
 */
public class MCRBinding {

    private static final Logger LOGGER = LogManager.getLogger();

    protected String name;

    protected String xPath;

    protected List<Object> boundNodes = new ArrayList<>();

    protected MCRBinding parent;

    protected List<MCRBinding> children = new ArrayList<>();

    protected MCRChangeTracker tracker;

    private Map<String, Object> staticVariables;

    public MCRBinding(Document document) {
        this.boundNodes.add(document);
    }

    public MCRBinding(Document document, MCRChangeTracker tracker) {
        this(document);
        this.tracker = tracker;

    }

    private MCRBinding(MCRBinding parent) {
        this.parent = parent;
        parent.children.add(this);
    }

    public MCRBinding(String xPath, boolean buildIfNotExists, MCRBinding parent) throws JaxenException {
        this(parent);
        bind(xPath, buildIfNotExists, null);
    }

    public MCRBinding(String xPath, String initialValue, String name, MCRBinding parent)
        throws JaxenException {
        this(parent);
        this.name = (name != null) && !name.isEmpty() ? name : null;
        bind(xPath, true, initialValue);
    }

    private void bind(String xPath, boolean buildIfNotExists, String initialValue) throws JaxenException {
        this.xPath = xPath;

        Map<String, Object> variables = buildXPathVariables();

        XPathExpression<Object> xPathExpr = XPathFactory.instance().compile(xPath, Filters.fpassthrough(), variables,
            MCRConstants.getStandardNamespaces());

        boundNodes.addAll(xPathExpr.evaluate(parent.getBoundNodes()));

        for (Object boundNode : boundNodes) {
            if (!(boundNode instanceof Element || boundNode instanceof Attribute || boundNode instanceof Document)) {
                throw new TypeNotPresentException(
                    "XPath MUST only bind either element, attribute or document nodes: " + xPath, null);
            }
        }

        LOGGER.debug("Bind to {} selected {} node(s)", () -> xPath, boundNodes::size);

        if (boundNodes.isEmpty() && buildIfNotExists) {
            MCRNodeBuilder builder = new MCRNodeBuilder(variables);
            Object built = builder.buildNode(xPath, initialValue, (Parent) (parent.getBoundNode()));
            LOGGER.debug("Bind to {} generated node {}", () -> xPath, () -> MCRXPathBuilder.buildXPath(built));
            boundNodes.add(built);
            trackNodeCreated(builder.getFirstNodeBuilt());
        }
    }

    public MCRBinding(int pos, MCRBinding parent) {
        this(parent);
        boundNodes.add(parent.getBoundNodes().get(pos - 1));
        LOGGER.debug("Repeater bind to child [{}]", pos);
    }

    public String getXPath() {
        return xPath;
    }

    public List<Object> getBoundNodes() {
        return boundNodes;
    }

    public Object getBoundNode() {
        return boundNodes.getFirst();
    }

    public void removeBoundNode(int index) {
        Object node = boundNodes.remove(index);
        if (node instanceof Element element) {
            track(MCRRemoveElement.remove(element));
        } else {
            track(MCRRemoveAttribute.remove((Attribute) node));
        }
    }

    public Element cloneBoundElement(int index) {
        Element template = (Element) (boundNodes.get(index));
        Element newElement = template.clone();
        Element parent = template.getParentElement();
        int indexInParent = parent.indexOf(template) + 1;
        parent.addContent(indexInParent, newElement);
        boundNodes.add(index + 1, newElement);
        trackNodeCreated(newElement);
        return newElement;
    }

    private void trackNodeCreated(Object node) {
        if (node instanceof Element element) {
            MCRChangeTracker.removeChangeTracking(element);
            track(MCRAddedElement.added(element));
        } else {
            Attribute attribute = (Attribute) node;
            track(MCRAddedAttribute.added(attribute));
        }
    }

    public MCRBinding getParent() {
        return parent;
    }

    public void detach() {
        if (parent != null) {
            parent.children.remove(this);
            this.parent = null;
        }
    }

    public List<MCRBinding> getAncestorsAndSelf() {
        List<MCRBinding> ancestors = new ArrayList<>();
        MCRBinding current = this;
        do {
            ancestors.addFirst(current);
            current = current.getParent();
        } while (current != null);
        return ancestors;
    }

    public String getValue() {
        return getValue(getBoundNode());
    }

    public static String getValue(Object node) {
        if (node instanceof Element element) {
            return element.getTextTrim();
        } else {
            return ((Attribute) node).getValue();
        }
    }

    public boolean hasValue(String value) {
        return boundNodes.stream().map(MCRBinding::getValue).anyMatch(value::equals);
    }

    public void setValue(String value) {
        setValue(getBoundNode(), value);
    }

    public void setDefault(String value) {
        if (getValue().isEmpty()) {
            setValue(getBoundNode(), value);
        }
    }

    public void setValues(String value) {
        for (int i = 0; i < boundNodes.size(); i++) {
            setValue(i, value);
        }
    }

    public void setValue(int index, String value) {
        setValue(boundNodes.get(index), value);
    }

    private void setValue(Object node, String value) {
        if (value.equals(getValue(node))) {
            return;
        }
        if (node instanceof Attribute attribute) {
            track(MCRSetAttributeValue.setValue(attribute, value));
        } else {
            track(MCRSetElementText.setText((Element) node, value));
        }
    }

    public String getName() {
        return name;
    }

    public List<MCRBinding> getChildren() {
        return children;
    }

    private Map<String, Object> getVariables() {
        if (staticVariables != null) {
            return staticVariables;
        }
        if (parent != null) {
            return parent.getVariables();
        }
        return Collections.emptyMap();
    }

    public void setVariables(Map<String, Object> variables) {
        this.staticVariables = variables;
    }

    public Map<String, Object> buildXPathVariables() {
        Map<String, Object> variables = new HashMap<>(getVariables());

        for (MCRBinding ancestor : getAncestorsAndSelf()) {
            for (MCRBinding child : ancestor.getChildren()) {
                String childName = child.getName();
                if (childName != null) {
                    variables.put(childName, child.getBoundNodes());
                }
            }
        }
        return variables;
    }

    public String getAbsoluteXPath() {
        return MCRXPathBuilder.buildXPath(getBoundNode());
    }

    public MCRXPathEvaluator getXPathEvaluator() {
        return new MCRXPathEvaluator(buildXPathVariables(), getBoundNodes());
    }

    public void track(MCRChangeData change) {
        if (tracker != null) {
            tracker.track(change);
        } else if (parent != null) {
            parent.track(change);
        }
    }
}
