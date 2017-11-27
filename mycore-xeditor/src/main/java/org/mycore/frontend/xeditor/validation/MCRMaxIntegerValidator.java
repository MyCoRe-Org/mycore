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

/**
 * Validates values to be integer numbers not bigger than a given maximum value.
 * Example: &lt;xed:validate type="integer" max="200" ... /&gt;
 *  * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRMaxIntegerValidator extends MCRIntegerValidator {

    private static final String ATTR_MAX = "max";

    private int max;

    @Override
    public boolean hasRequiredAttributes() {
        return super.hasRequiredAttributes() && hasAttributeValue(ATTR_MAX);
    }

    @Override
    public void configure() {
        max = Integer.parseInt(getAttributeValue(ATTR_MAX));
    }

    @Override
    protected boolean isValid(String value) {
        if (!super.isValid(value)) {
            return false;
        } else {
            return Integer.parseInt(value) <= max;
        }
    }
}
