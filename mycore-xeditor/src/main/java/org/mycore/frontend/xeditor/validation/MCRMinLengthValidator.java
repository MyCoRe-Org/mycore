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

package org.mycore.frontend.xeditor.validation;

/**
 * Validates text values to have a given minimum length 
 * Example: &lt;xed:validate minLength="10" ... /&gt;
 *  
 * @author Frank Lützenkirchen
 */
public class MCRMinLengthValidator extends MCRValidator {

    private static final String ATTR_MIN_LENGTH = "minLength";

    private int minLength;

    @Override
    public boolean hasRequiredAttributes() {
        return hasAttributeValue(ATTR_MIN_LENGTH);
    }

    @Override
    public void configure() {
        minLength = Integer.parseInt(getAttributeValue(ATTR_MIN_LENGTH));
    }

    @Override
    protected boolean isValid(String value) {
        return minLength <= value.length();
    }
}
