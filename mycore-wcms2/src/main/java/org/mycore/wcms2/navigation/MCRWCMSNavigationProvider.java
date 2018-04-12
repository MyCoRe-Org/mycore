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

package org.mycore.wcms2.navigation;

import org.mycore.wcms2.datamodel.MCRNavigation;

import com.google.gson.JsonObject;

/**
 * Connection between the Navigation Datamodel and the WCMS.
 *
 * @author Matthias Eichner
 */
public interface MCRWCMSNavigationProvider extends MCRWCMSJSONProvider<MCRNavigation, JsonObject> {

    String JSON_HIERARCHY = "hierarchy";

    String JSON_ITEMS = "items";

    String JSON_CHILDREN = "children";

    String JSON_WCMS_ID = "wcmsId";

    String JSON_WCMS_TYPE = "wcmsType";

    enum WCMSType {
        root, item, insert, menu, group
    }

    /**
     * <p>
     * Converts a <code>Navigation</code> object to a json one. The structure of the json is:
     * </p>
     * <pre>
     * {
     *   hierarchy: [
     *     {"wcmsId":0,"children":[
     *       {"wcmsId":1,"children":[
     *         {"wcmsId":2}
     *       ]}
     *     ]}
     *   }
     *   items: [
     *     {"wcmsId" : "0", "wcmsType" : "root", "mainTitle" : "My Main Title", "dir" : "/content" ...},
     *     {"wcmsId" : "1", "wcmsType" : "menu", "id" : "main", "labelMap":{"de":"Hauptmenü","en":"Main menu"} ... }
     *     {"wcmsId" : "2", "wcmsType" : "item", "type" : "intern", "style" : "bold" ...}
     *     ...
     *   ]
     * }
     * </pre>
     * @return the generated json
     */
    @Override
    JsonObject toJSON(MCRNavigation navigation);

    /**
     * Converts an WCMS JSON Object to an <code>Navigation</code> object.
     * 
     * TODO: json data structure
     */
    @Override
    MCRNavigation fromJSON(JsonObject jsonNavigation);

}
