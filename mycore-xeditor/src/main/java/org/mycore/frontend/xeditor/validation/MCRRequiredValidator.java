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

import org.mycore.frontend.xeditor.MCRBinding;

/**
 * Validates input to be required. XML is valid if at least one value exists at the given XPath.
 * Example: &lt;xed:validate required="true" ... /&gt;
 *
 * @author Frank L\u00FCtzenkirchen 
 */
public class MCRRequiredValidator extends MCRValidator {

    private static final String VALUE_TRUE = "true";

    private static final String ATTR_REQUIRED = "required";

    @Override
    public boolean hasRequiredAttributes() {
        return VALUE_TRUE.equals(getAttributeValue(ATTR_REQUIRED));
    }

    @Override
    public boolean validateBinding(MCRValidationResults results, MCRBinding binding) {
        if (binding.getBoundNodes().size() == 0) {
            String msg = "Condition for " + this.xPath + " can not be validated, no such XML source node";
            throw new RuntimeException(msg);
        }

        String absPath = binding.getAbsoluteXPath();
        if (results.hasError(absPath)) {
            return true;
        }

        boolean isValid = false;

        // at least one value must exist
        for (Object node : binding.getBoundNodes()) {
            if (!MCRBinding.getValue(node).isEmpty()) {
                isValid = true;
            }
        }

        results.mark(absPath, isValid, this);
        return isValid;
    }
}
