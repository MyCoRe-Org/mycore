/*
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.common.fo;

import org.mycore.common.config.MCRConfiguration;

/**
 * Returns the XSL-FO formatter instance configured via property, for example
 * 
 * MCR.LayoutService.FoFormatter.class=org.mycore.common.fo.MCRFoFormatterFOP
 * 
 * (which is the default using Apache FOP)
 * 
 * @see MCRFoFormatterInterface
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRFoFactory {

    /** The configuration property */
    private final static String property = "MCR.LayoutService.FoFormatter.class";

    /** The singleton */
    private static MCRFoFormatterInterface formatter;

    /** Returns the XSL-FO formatter instance configured */
    public static synchronized MCRFoFormatterInterface getFoFormatter() {
        if (formatter == null) {
            formatter = MCRConfiguration.instance().getInstanceOf(property, MCRFoFormatterFOP.class.getName());
        }
        return formatter;
    }
}
