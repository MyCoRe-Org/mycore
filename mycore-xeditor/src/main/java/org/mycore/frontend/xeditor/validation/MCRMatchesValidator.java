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
 * Validates input to match a specified java.util.regex pattern.
 * Example: &lt;xed:validate xpath="//mods:identifier[@type='doi']" matches="10\.\d+.*" ... /&gt;
 *
 * @author Frank L\u00FCtzenkirchen 
 */
public class MCRMatchesValidator extends MCRValidator {

    private static final String ATTR_MATCHES = "matches";

    private String pattern;

    @Override
    public boolean hasRequiredAttributes() {
        return hasAttributeValue(ATTR_MATCHES);
    }

    @Override
    public void configure() {
        this.pattern = getAttributeValue(ATTR_MATCHES);
    }

    @Override
    protected boolean isValid(String value) {
        return value.matches(pattern);
    }
}
