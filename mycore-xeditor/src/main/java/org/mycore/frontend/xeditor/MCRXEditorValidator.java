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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom2.JDOMException;
import org.mycore.frontend.editor.validation.MCRValidator;
import org.mycore.frontend.editor.validation.MCRValidatorBuilder;
import org.mycore.frontend.editor.validation.value.MCRRequiredValidator;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXEditorValidator {

    private static final String XED_VALIDATION_FAILED = "xed-validation-failed";

    private List<MCRValidationRule> validationRules = new ArrayList<MCRValidationRule>();

    private Set<String> xPathsOfInvalidFields = new HashSet<String>();

    private MCREditorSession session;

    public MCRXEditorValidator(MCREditorSession session) {
        this.session = session;
    }

    public void addValidationRule(String xPath, NodeIterator attributes) {
        MCRValidator validator = MCRValidatorBuilder.buildPredefinedCombinedValidator();
        for (Node node; (node = attributes.nextNode()) != null;)
            validator.setProperty(node.getNodeName(), node.getNodeValue());

        if ("true".equals(validator.getProperty("required"))) {
            MCRValidator vRequired = new MCRRequiredValidator();
            vRequired.setProperty("required", "true");
            validationRules.add(new MCRValidationRule(xPath, vRequired));
        }

        validationRules.add(new MCRValidationRule(xPath, validator));
    }

    public void removeValidationRules() {
        validationRules.clear();
    }

    public boolean isValid() throws JDOMException, JaxenException {
        MCRBinding root = session.getRootBinding();
        for (MCRValidationRule rule : validationRules) {
            String xPath = rule.getXPath();

            if (failed(xPath))
                continue;

            MCRBinding binding = new MCRBinding(xPath, root);
            String value = binding.getValue();
            binding.detach();

            if (!rule.isValid(value))
                xPathsOfInvalidFields.add(xPath);
        }

        boolean isValid = xPathsOfInvalidFields.isEmpty();
        session.getVariables().put(XED_VALIDATION_FAILED, String.valueOf(!isValid));
        return isValid;
    }

    public boolean failed(String xPath) {
        return xPathsOfInvalidFields.contains(xPath);
    }

    public void forgetInvalidFields() {
        xPathsOfInvalidFields.clear();
        session.getVariables().remove(XED_VALIDATION_FAILED);
    }
}

class MCRValidationRule {

    private String xPath;

    private MCRValidator validator;

    MCRValidationRule(String xPath, MCRValidator validator) {
        this.xPath = xPath;
        this.validator = validator;
    }

    public String getXPath() {
        return xPath;
    }

    public boolean isValid(String value) {
        return validator.isValid(value);
    }
}
