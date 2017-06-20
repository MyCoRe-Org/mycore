/*
* This file is part of *** M y C o R e ***
* See http://www.mycore.de/ for details.
*
* This program is free software; you can use it, redistribute it
* and / or modify it under the terms of the GNU General Public License
* (GPL) as published by the Free Software Foundation; either version 2
* of the License or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful, but
* WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program, in a file called gpl.txt or license.txt.
* If not, write to the Free Software Foundation Inc.,
* 59 Temple Place - Suite 330, Boston, MA 02111-1307 USA
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
