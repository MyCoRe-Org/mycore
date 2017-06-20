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
