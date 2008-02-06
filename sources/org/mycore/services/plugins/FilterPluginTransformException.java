/*
 * 
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

package org.mycore.services.plugins;

import org.mycore.common.MCRException;

/**
 * Exception to be thrown if transformation to text stream failed
 * 
 * @author Thomas Scheffler (yagee)
 */
public class FilterPluginTransformException extends MCRException {
    /**
     * just uses super Constructor yet...
     * 
     * @param message
     * @see MCRException
     */
    public FilterPluginTransformException(String message) {
        super(message);

        // TODO Auto-generated constructor stub
    }

    /**
     * just uses super Constructor yet...
     * 
     * @param message
     * @param exception
     * @see MCRException
     */
    public FilterPluginTransformException(String message, Exception exception) {
        super(message, exception);

        // TODO Auto-generated constructor stub
    }
}
