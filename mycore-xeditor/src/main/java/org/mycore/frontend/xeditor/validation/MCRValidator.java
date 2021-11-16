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

package org.mycore.frontend.xeditor.validation;

import org.jaxen.JaxenException;
import org.jdom2.JDOMException;
import org.mycore.common.xml.MCRXPathBuilder;
import org.mycore.common.xml.MCRXPathEvaluator;
import org.mycore.frontend.xeditor.MCRBinding;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.List;

public abstract class MCRValidator {

    private static final String ATTR_RELEVANT_IF = "relevant-if";

    private Node ruleElement;

    protected String xPath;
    protected String relevantIfXPath;

    public void init(String baseXPath, Node ruleElement) {
        Node relativeXPath = ruleElement.getAttributes().getNamedItem("xpath");
        this.xPath = relativeXPath != null ? relativeXPath.getNodeValue() : baseXPath;
        this.ruleElement = ruleElement;

        if (hasRequiredAttributes()) {
            relevantIfXPath = getAttributeValue(ATTR_RELEVANT_IF);
            configure();
        }
    }

    public abstract boolean hasRequiredAttributes();

    /** If validator uses properties to configure its behavior, override this */
    public void configure() {
        // empty
    }

    public Node getRuleElement() {
        return ruleElement;
    }

    public String getAttributeValue(String name) {
        NamedNodeMap attributes = ruleElement.getAttributes();
        Node attribute = attributes.getNamedItem(name);
        return attribute == null ? null : attribute.getNodeValue();
    }

    public boolean hasAttributeValue(String name) {
        return (getAttributeValue(name) != null);
    }

    public boolean validate(MCRValidationResults results, MCRBinding root) throws JaxenException, JDOMException {
        MCRBinding binding = new MCRBinding(xPath, false, root);
        boolean isValid = validateBinding(results, binding);
        binding.detach();
        return isValid;
    }

    public boolean validateBinding(MCRValidationResults results, MCRBinding binding) {
        boolean isValid = true; // all nodes must validate

        List<Object> boundNodes = binding.getBoundNodes();
        for (int i = 0; i < boundNodes.size(); i++) {
            Object node = boundNodes.get(i);

            String absPath = MCRXPathBuilder.buildXPath(node);
            if (results.hasError(absPath)) {
                continue;
            }

            MCRBinding nodeBinding = new MCRBinding(i + 1, binding);
            if (!isRelevant(nodeBinding)) {
                continue;
            }

            String value = MCRBinding.getValue(node);
            if (value.isEmpty()) {
                continue;
            }

            boolean result = isValid(value);
            results.mark(absPath, result, this);
            isValid = isValid && result;
        }
        return isValid;
    }

    protected boolean isRelevant(MCRBinding binding) {
        if (null == relevantIfXPath) {
            return true;
        } else {
            MCRXPathEvaluator evaluator = binding.getXPathEvaluator();
            boolean isRelevant = evaluator.test(relevantIfXPath);
            binding.detach();
            return isRelevant;
        }
    }

    protected boolean isValid(String value) {
        return true;
    }
}
