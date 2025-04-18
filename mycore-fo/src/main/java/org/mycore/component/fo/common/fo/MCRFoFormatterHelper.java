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

package org.mycore.component.fo.common.fo;

import org.mycore.common.config.MCRConfiguration2;

/**
 * Returns the XSL-FO formatter instance configured via PROPERTY, for example
 * 
 * MCR.LayoutService.FoFormatter.class=org.mycore.common.fo.MCRFoFormatterFOP
 * 
 * (which is the default using Apache FOP)
 * 
 * @see MCRFoFormatterInterface
 * 
 * @author Frank Lützenkirchen
 */
public class MCRFoFormatterHelper {

    /** The configuration PROPERTY */
    private static final String PROPERTY = "MCR.LayoutService.FoFormatter.class";

    /** The singleton */
    private static MCRFoFormatterInterface formatter;

    /** @return the XSL-FO formatter instance configured */
    public static synchronized MCRFoFormatterInterface getFoFormatter() {
        if (formatter == null) {
            formatter = MCRConfiguration2.getInstanceOfOrThrow(MCRFoFormatterInterface.class, PROPERTY);
        }
        return formatter;
    }
}
