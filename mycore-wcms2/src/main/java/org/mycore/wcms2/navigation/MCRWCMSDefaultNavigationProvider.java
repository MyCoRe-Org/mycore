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

package org.mycore.wcms2.navigation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.frontend.MCRLayoutUtilities;
import org.mycore.wcms2.datamodel.MCRNavigation;
import org.mycore.wcms2.datamodel.MCRNavigationBaseItem;
import org.mycore.wcms2.datamodel.MCRNavigationGroup;
import org.mycore.wcms2.datamodel.MCRNavigationInsertItem;
import org.mycore.wcms2.datamodel.MCRNavigationItem;
import org.mycore.wcms2.datamodel.MCRNavigationItemContainer;
import org.mycore.wcms2.datamodel.MCRNavigationMenuItem;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Default implementation of <code>NavigationProvider</code>.
 *
 * @author Matthias Eichner
 */
public class MCRWCMSDefaultNavigationProvider implements MCRWCMSNavigationProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Gson GSON;

    private static final String PERMISSION_READ = "read";

    private static final String PERMISSION_WRITE = "write";

    static {
        GSON = new Gson();
    }

    @Override
    public JsonObject toJSON(MCRNavigation navigation) {
        JsonObject returnObject = new JsonObject();
        JsonArray hierarchy = new JsonArray();
        JsonArray items = new JsonArray();

        returnObject.add(JSON_HIERARCHY, hierarchy);
        returnObject.add(JSON_ITEMS, items);
        create(navigation, hierarchy, items);

        return returnObject;
    }

    private void create(MCRNavigationBaseItem item, JsonArray hierarchy, JsonArray items) throws MCRException {
        JsonObject hierarchyObject = add(item, hierarchy, items);
        if (item instanceof MCRNavigationItemContainer navigationItemContainer) {
            JsonArray childHierarchyArray = new JsonArray();
            for (MCRNavigationBaseItem childItem : navigationItemContainer.getChildren()) {
                create(childItem, childHierarchyArray, items);
            }
            if (!childHierarchyArray.isEmpty()) {
                hierarchyObject.add(JSON_CHILDREN, childHierarchyArray);
            }
        }
    }

    private JsonObject add(MCRNavigationBaseItem item, JsonArray hierarchy, JsonArray items) {
        int id = items.size();
        JsonObject jsonItem = GSON.toJsonTree(item).getAsJsonObject();
        jsonItem.addProperty(JSON_WCMS_ID, id);
        jsonItem.remove(JSON_CHILDREN);
        WCMSType type = null;
        String href = null;
        switch (item) {
            case MCRNavigation navigation -> {
                type = WCMSType.ROOT;
                href = navigation.getHrefStartingPage();
            }
            case MCRNavigationMenuItem menuItem -> {
                type = WCMSType.MENU;
                href = menuItem.getDir();
            }
            case MCRNavigationItem navigationItem -> {
                type = WCMSType.ITEM;
                href = navigationItem.getHref();
            }
            case MCRNavigationInsertItem mcrNavigationInsertItem -> {
                type = WCMSType.INSERT;
            }
            case MCRNavigationGroup mcrNavigationGroup -> {
                type = WCMSType.GROUP;
            }
            case null, default -> LOGGER.warn("Unable to set type for item {}", id);
        }
        jsonItem.addProperty(JSON_WCMS_TYPE, type.name());
        if (href != null) {
            jsonItem.add("access", getAccess(href));
        }
        items.add(jsonItem);
        // create hierarchy for root
        JsonObject hierarchyObject = new JsonObject();
        hierarchyObject.addProperty(JSON_WCMS_ID, id);
        hierarchy.add(hierarchyObject);
        return hierarchyObject;
    }

    @Override
    public MCRNavigation fromJSON(JsonObject navigationJSON) {
        JsonArray items = navigationJSON.get(JSON_ITEMS).getAsJsonArray();
        JsonArray hierarchy = navigationJSON.get(JSON_HIERARCHY).getAsJsonArray();
        if (!hierarchy.isEmpty()) {
            JsonObject root = hierarchy.get(0).getAsJsonObject();
            MCRNavigationBaseItem item = createItem(root, items);
            if (item instanceof MCRNavigation navigation) {
                return navigation;
            }
        }
        return null;
    }

    private MCRNavigationBaseItem createItem(JsonObject hierarchyObject, JsonArray items) {
        if (!hierarchyObject.has(JSON_WCMS_ID)) {
            LOGGER.warn("While saving navigation.xml. Invalid object in hierarchy.");
            return null;
        }
        MCRNavigationBaseItem item = getNavigationItem(hierarchyObject.get(JSON_WCMS_ID).getAsString(), items);
        if (item == null) {
            LOGGER.warn("While saving navigation.xml. Item with id {} is null!",
                () -> hierarchyObject.get(JSON_WCMS_ID));
            return null;
        }

        JsonElement children = hierarchyObject.get(JSON_CHILDREN);
        if (children != null && children.isJsonArray() && item instanceof MCRNavigationItemContainer itemAsContainer) {
            for (JsonElement child : children.getAsJsonArray()) {
                if (child.isJsonObject()) {
                    MCRNavigationBaseItem childItem = createItem(child.getAsJsonObject(), items);
                    if (childItem != null) {
                        itemAsContainer.getChildren().add(childItem);
                    }
                }
            }
        }
        return item;
    }

    /**
     * Internal method to get an <code>NavigationItem</code> from
     * the json array.
     *
     * @param wcmsId id of the item
     * @param items list of items
     * @return instance of <code>NavigationItem</code> or null
     */
    private MCRNavigationBaseItem getNavigationItem(String wcmsId, JsonArray items) {
        for (JsonElement e : items) {
            if (e.isJsonObject()) {
                JsonObject item = e.getAsJsonObject();
                if (item.has(JSON_WCMS_ID) && item.has(JSON_WCMS_TYPE)
                    && wcmsId.equals(item.get(JSON_WCMS_ID).getAsString())) {
                    WCMSType wcmsType = WCMSType.valueOf(item.get(JSON_WCMS_TYPE).getAsString());
                    if (wcmsType.equals(WCMSType.ROOT)) {
                        return GSON.fromJson(item, MCRNavigation.class);
                    } else if (wcmsType.equals(WCMSType.MENU)) {
                        return GSON.fromJson(item, MCRNavigationMenuItem.class);
                    } else if (wcmsType.equals(WCMSType.ITEM)) {
                        return GSON.fromJson(item, MCRNavigationItem.class);
                    } else if (wcmsType.equals(WCMSType.INSERT)) {
                        return GSON.fromJson(item, MCRNavigationInsertItem.class);
                    } else if (wcmsType.equals(WCMSType.GROUP)) {
                        return GSON.fromJson(item, MCRNavigationGroup.class);
                    }
                }
            }
        }
        return null;
    }

    private JsonObject getAccess(String href) {
        JsonObject accessObject = new JsonObject();
        if (MCRLayoutUtilities.hasRule(PERMISSION_READ, href)) {
            JsonObject readObject = new JsonObject();
            accessObject.add("read", readObject);
            readObject.addProperty("ruleID", MCRLayoutUtilities.getRuleID(PERMISSION_READ, href));
            readObject.addProperty("ruleDes", MCRLayoutUtilities.getRuleDescr(PERMISSION_READ, href));
        }
        if (MCRLayoutUtilities.hasRule(PERMISSION_WRITE, href)) {
            JsonObject writeObject = new JsonObject();
            accessObject.add("write", writeObject);
            writeObject.addProperty("ruleID", MCRLayoutUtilities.getRuleID(PERMISSION_WRITE, href));
            writeObject.addProperty("ruleDes", MCRLayoutUtilities.getRuleDescr(PERMISSION_WRITE, href));
        }
        return accessObject;
    }

}
