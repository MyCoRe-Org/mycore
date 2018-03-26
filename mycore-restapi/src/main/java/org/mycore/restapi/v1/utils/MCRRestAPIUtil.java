/* 
 * $Revision: 34285 $ $Date: 2016-01-07 14:05:50 +0100 (Do, 07 Jan 2016) $
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 */
package org.mycore.restapi.v1.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.mycore.common.config.MCRConfiguration2;

/**
 * This class contains some generic utility functions for the REST API
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRRestAPIUtil {
    public static String getWWWAuthenticateHeader(String s,
        Map<String, String> attributes) {
        String realm = MCRConfiguration2.getString("MCR.NameOfProject")
            .map(p -> p + ": ")
            .orElse("")
            + "Rest-API V1";
        LinkedHashMap<String, String> attrMap = new LinkedHashMap<>();
        attrMap.put("realm", realm);
        Optional.ofNullable(attributes).ifPresent(attrMap::putAll);
        StringBuilder b = new StringBuilder();
        attrMap.entrySet().stream()
            .forEach(e -> appendFieldValue(b, e.getKey(), e.getValue()));
        b.insert(0, " ");
        return Optional.ofNullable(s).orElse("Basic") + b.toString();
    }

    private static void appendField(StringBuilder b, String field) {
        if (b.length() > 0) {
            b.append(", ");
        }
        b.append(field);
    }

    private static void appendValue(StringBuilder b, String value) {
        for (char c : value.toCharArray()) {
            if ((c < 0x20) || (c == 0x22) || (c == 0x5c) || (c > 0x7e)) {
                b.append(' ');
            } else {
                b.append(c);
            }
        }
    }

    private static void appendFieldValue(StringBuilder b, String field, String value) {
        appendField(b, field);
        if (value != null && !value.isEmpty()) {
            b.append("=\"");
            appendValue(b, value);
            b.append("\"");
        }
    }

}
