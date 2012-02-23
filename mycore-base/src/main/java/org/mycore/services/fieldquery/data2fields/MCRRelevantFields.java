/*
 * $Revision$ 
 * $Date$
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

package org.mycore.services.fieldquery.data2fields;

import java.util.ArrayList;
import java.util.List;

import org.mycore.common.MCRCache;
import org.mycore.services.fieldquery.MCRFieldDef;

public class MCRRelevantFields {

    private static MCRCache cache = new MCRCache(200, MCRRelevantFields.class.getName());

    public static MCRRelevantFields getFieldsFor(MCRFieldsSelector selector) {
        String key = selector.getKey();
        MCRRelevantFields fields = (MCRRelevantFields) (cache.get(key));

        if (fields == null) {
            fields = new MCRRelevantFields(selector);
            cache.put(key, fields);
        }
        return fields;
    }

    private String key;

    private List<MCRFieldDef> fields = new ArrayList<MCRFieldDef>();

    private MCRRelevantFields(MCRFieldsSelector selector) {
        StringBuilder sb = new StringBuilder();
        for (MCRFieldDef field : MCRFieldDef.getFieldDefs(selector.getIndex()))
            if (field.isUsedFor(selector)) {
                fields.add(field);
                sb.append(field.getName()).append("/");
            }
        key = sb.toString();
    }

    public String getKey() {
        return key;
    }

    public List<MCRFieldDef> getFields() {
        return fields;
    }
}
