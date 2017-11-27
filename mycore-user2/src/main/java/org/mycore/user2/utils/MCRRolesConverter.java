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
package org.mycore.user2.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.mycore.user2.MCRUserAttributeConverter;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class MCRRolesConverter implements MCRUserAttributeConverter<String, Collection<String>> {

    @Override
    public Collection<String> convert(String value, String separator, Map<String, String> valueMapping)
        throws Exception {
        Collection<String> roles = new HashSet<>();

        if (value != null) {
            for (final String v : value.split(separator)) {
                final String role = v.contains("@") ? v.substring(0, v.indexOf("@")) : v;
                if (valueMapping != null) {
                    final String[] mapping = valueMapping.containsKey(role) ? valueMapping.get(role).split(",") : null;
                    if (mapping != null)
                        roles.addAll(Arrays.asList(mapping));
                }
            }
        }

        return roles;
    }

}
