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

import java.util.List;

import org.mycore.common.xml.MCRXPathBuilder;
import org.mycore.common.xml.MCRXPathEvaluator;
import org.mycore.frontend.xeditor.MCRBinding;

/**
 * Validates using an XPath test expression.
 *   
 * Example: &lt;xed:validate xpath="//max" test="(string-length(.) = 0) or (number(.) &gt;= number(../min))" ... /&gt;
 *
 * @author Frank L\u00FCtzenkirchen 
 */
public class MCRXPathTestValidator extends MCRValidator {

    private static final String ATTR_TEST = "test";

    private String xPathExpression;

    @Override
    public boolean hasRequiredAttributes() {
        return hasAttributeValue(ATTR_TEST);
    }

    @Override
    public void configure() {
        this.xPathExpression = getAttributeValue(ATTR_TEST);
    }

    @Override
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
            MCRXPathEvaluator evaluator = nodeBinding.getXPathEvaluator();
            boolean result = evaluator.test(xPathExpression);
            nodeBinding.detach();

            results.mark(absPath, result, this);
            isValid = isValid && result;
        }
        return isValid;
    }
}
