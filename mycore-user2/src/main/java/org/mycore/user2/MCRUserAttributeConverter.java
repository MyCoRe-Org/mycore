/*
 * $Id$ 
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
package org.mycore.user2;

import java.util.Map;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public interface MCRUserAttributeConverter<ValueType, BoundType> {

    /**
     * Convert a given value to the specified type.
     * 
     * @param value the value of type <code>&lt;ValueType&gt;</code>, to convert
     * @param separator the value separator or <code>null</code>
     * @param valueMapping the value mapping or <code>null</code>
     * @return the converted value of type <code>&lt;BoundType&gt;</code>
     * @throws Exception if there's an error during the conversion.
     */
    public abstract BoundType convert(ValueType value, String separator, Map<String, String> valueMapping)
        throws Exception;
}
