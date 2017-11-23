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
 * Validates input to be a decimal number and not less than a given minimum value. 
 * The number format is specified by a given locale ID.
 * Example: &lt;xed:validate type="decimal" locale="en" min="1,000.00"... /&gt;
 *  * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRMinDecimalValidator extends MCRDecimalValidator {

    private static final String ATTR_MIN = "min";

    private double min;

    @Override
    public boolean hasRequiredAttributes() {
        return super.hasRequiredAttributes() && hasAttributeValue(ATTR_MIN);
    }

    @Override
    public void configure() {
        super.configure();
        min = converter.string2double(getAttributeValue(ATTR_MIN));
    }

    @Override
    protected boolean isValid(String value) {
        Double d = converter.string2double(value);
        if (d == null) {
            return false;
        } else {
            return min <= d;
        }
    }
}
