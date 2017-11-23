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
 * Validates date values specified by one or more SimpleDateFormat patterns separated by ";".
 * Example: &lt;xed:validate type="date" format="yyyy-MM-dd;dd.MM.yyyy" ... /&gt;
 *  * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRDateValidator extends MCRValidator {

    private static final String TYPE_DATE = "date";

    private static final String ATTR_TYPE = "type";

    private static final String ATTR_FORMAT = "format";

    /** The converter used to convert strings to dates */
    protected MCRDateConverter converter;

    @Override
    public boolean hasRequiredAttributes() {
        return TYPE_DATE.equals(getAttributeValue(ATTR_TYPE)) && hasAttributeValue(ATTR_FORMAT);
    }

    @Override
    public void configure() {
        String format = getAttributeValue(ATTR_FORMAT);
        converter = new MCRDateConverter(format);
    }

    @Override
    protected boolean isValid(String value) {
        return (converter.string2date(value) != null);
    }
}
