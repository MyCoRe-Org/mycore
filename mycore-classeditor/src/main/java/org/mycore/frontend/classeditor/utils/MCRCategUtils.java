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

package org.mycore.frontend.classeditor.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.frontend.classeditor.json.MCRJSONCategory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;

public class MCRCategUtils {

    public static MCRCategory newCategory(MCRCategoryID id, SortedSet<MCRLabel> labels, MCRCategoryID mcrCategoryID) {
        MCRJSONCategory category = new MCRJSONCategory();
        category.setId(id);
        category.setLabels(labels);
        category.setParentID(mcrCategoryID);
        return category;
    }

    public static String maskCategID(MCRCategoryID categoryID) {
        String rootID = categoryID.getRootID();
        String id = categoryID.getId();
        return rootID + "." + (id == null ? "" : id);
    }

    public static Map<MCRCategoryID, String> getCategoryIDMap(String json) {
        Map<MCRCategoryID, String> categories = new HashMap<>();
        JsonStreamParser jsonStreamParser = new JsonStreamParser(json);
        if (jsonStreamParser.hasNext()) {
            JsonArray saveObjArray = jsonStreamParser.next().getAsJsonArray();
            for (JsonElement jsonElement : saveObjArray) {
                //jsonObject.item.id.rootid
                JsonObject root = jsonElement.getAsJsonObject();
                String rootId = root.getAsJsonObject("item").getAsJsonObject("id").getAsJsonPrimitive("rootid")
                    .getAsString();

                String state = root.getAsJsonPrimitive("state").getAsString();
                JsonElement parentIdJSON = root.get("parentId");
                if (parentIdJSON != null && parentIdJSON.isJsonPrimitive()
                    && "_placeboid_".equals(parentIdJSON.getAsString())) {
                    state = "new";
                }

                categories.put(new MCRCategoryID(rootId), state);
            }
        } else {
            return null;
        }
        return categories;
    }

}
