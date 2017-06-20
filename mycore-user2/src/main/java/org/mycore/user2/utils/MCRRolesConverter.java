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
        Collection<String> roles = new HashSet<String>();

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
