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
package org.mycore.user2;

import java.util.Map;

/**
 * @author René Adler (eagle)
 *
 */
public interface MCRUserAttributeConverter<V, B> {

    /**
     * Convert a given value to the specified type.
     *
     * @param value the value of type <code>&lt;V&gt;</code>, to convert
     * @param separator the value separator or <code>null</code>
     * @param valueMapping the value mapping or <code>null</code>
     * @return the converted value of type <code>&lt;B&gt;</code>
     */
    B convert(V value, String separator, Map<String, String> valueMapping);
}
