package org.mycore.frontend.classeditor.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.frontend.classeditor.json.MCRJSONCategory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;

public class MCRCategUtils {

    public static MCRCategory newCategory(MCRCategoryID id, Set<MCRLabel> labels, MCRCategoryID mcrCategoryID) {
        MCRJSONCategory category = new MCRJSONCategory();
        category.setId(id);
        category.setLabels(labels);
        category.setParentID(mcrCategoryID);
        return category;
    }

    public static String maskCategID(MCRCategoryID categoryID) {
        String rootID = categoryID.getRootID();
        String id = categoryID.getID();
        return rootID + "." + (id == null ? "" : id);
    }

    /**
     * Parses JSON String and extracts all category ids.
     * @param json
     * @return may return null if JSON Parser finds no JSON object or primitives.
     */
    public static Set<MCRCategoryID> getCategoryIDs(String json) {
        HashSet<MCRCategoryID> categories = new HashSet<>();
        JsonStreamParser jsonStreamParser = new JsonStreamParser(json);
        if (jsonStreamParser.hasNext()) {
            JsonArray saveObjArray = jsonStreamParser.next().getAsJsonArray();
            for (JsonElement jsonElement : saveObjArray) {
                //jsonObject.item.id.rootid
                JsonObject root = jsonElement.getAsJsonObject();
                String rootId = root.getAsJsonObject("item").getAsJsonObject("id").getAsJsonPrimitive("rootid")
                    .getAsString();
                categories.add(MCRCategoryID.rootID(rootId));
            }
        } else {
            return null;
        }
        return categories;
    }

    public static HashMap<MCRCategoryID, String> getCategoryIDMap(String json) {
        HashMap<MCRCategoryID, String> categories = new HashMap<>();
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
                if(parentIdJSON != null && parentIdJSON.isJsonPrimitive() && parentIdJSON.getAsString().equals("_placeboid_")){
                    state = "new";
                }

                categories.put(MCRCategoryID.rootID(rootId), state);
            }
        } else {
            return null;
        }
        return categories;
    }

    /**
     * Parses JSON String and extracts all category ids and returns a list of effected root category ids.
     * @param json
     * @return may return null if JSON Parser finds no JSON object or primitives.
     */
    public static Set<MCRCategoryID> getRootCategoryIDs(String json) {
        Set<MCRCategoryID> categories = getCategoryIDs(json);
        if (categories == null) {
            return null;
        }
        HashSet<MCRCategoryID> rootCategories = new HashSet<>();
        for (MCRCategoryID category : categories) {
            rootCategories.add(MCRCategoryID.rootID(category.getRootID()));
        }
        return rootCategories;
    }
    
}
