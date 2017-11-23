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

import java.util.Date;

/**
 * Validates date values against a minimum date. 
 * Date values are specified by one or more SimpleDateFormat patterns separated by ";".
 * Example: &lt;xed:validate type="date" format="yyyy-MM" min="2017-01" ... /&gt;
 *  * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRMinDateValidator extends MCRDateValidator {

    private static final String ATTR_MIN = "min";

    private Date minDate;

    @Override
    public boolean hasRequiredAttributes() {
        return super.hasRequiredAttributes() && hasAttributeValue(ATTR_MIN);
    }

    @Override
    public void configure() {
        super.configure();
        String min = getAttributeValue(ATTR_MIN);
        this.minDate = converter.string2date(min);
    }

    @Override
    protected boolean isValid(String value) {
        Date date = converter.string2date(value);
        if (date == null) {
            return false;
        } else {
            return minDate.before(date) || minDate.equals(date);
        }
    }
}
