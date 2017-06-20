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
    private static final Logger LOGGER = LogManager.getLogger(MCRWCMSDefaultSectionProvider.class);

    private static Gson gson;

    static {
        gson = new Gson();
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
        if (item instanceof MCRNavigationItemContainer) {
            JsonArray childHierarchyArray = new JsonArray();
            for (MCRNavigationBaseItem childItem : ((MCRNavigationItemContainer) item).getChildren()) {
                create(childItem, childHierarchyArray, items);
            }
            if (childHierarchyArray.size() > 0)
                hierarchyObject.add(JSON_CHILDREN, childHierarchyArray);
        }
    }

    private JsonObject add(MCRNavigationBaseItem item, JsonArray hierarchy, JsonArray items) {
        int id = items.size();
        JsonObject jsonItem = gson.toJsonTree(item).getAsJsonObject();
        jsonItem.addProperty(JSON_WCMS_ID, id);
        jsonItem.remove(JSON_CHILDREN);
        WCMSType type = null;
        String href = null;
        if (item instanceof MCRNavigation) {
            type = WCMSType.root;
            href = ((MCRNavigation) item).getHrefStartingPage();
        } else if (item instanceof MCRNavigationMenuItem) {
            type = WCMSType.menu;
            href = ((MCRNavigationMenuItem) item).getDir();
        } else if (item instanceof MCRNavigationItem) {
            type = WCMSType.item;
            href = ((MCRNavigationItem) item).getHref();
        } else if (item instanceof MCRNavigationInsertItem) {
            type = WCMSType.insert;
        } else if (item instanceof MCRNavigationGroup) {
            type = WCMSType.group;
        } else {
            LOGGER.warn("Unable to set type for item " + id);
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
        if (hierarchy.size() > 0) {
            JsonObject root = hierarchy.get(0).getAsJsonObject();
            MCRNavigationBaseItem navigation = createItem(root, items);
            if (navigation instanceof MCRNavigation) {
                return (MCRNavigation) navigation;
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
            LOGGER.warn("While saving navigation.xml. Item with id " + hierarchyObject.get(JSON_WCMS_ID) + " is null!");
            return null;
        }

        JsonElement children = hierarchyObject.get(JSON_CHILDREN);
        if (children != null && children.isJsonArray() && item instanceof MCRNavigationItemContainer) {
            MCRNavigationItemContainer itemAsContainer = (MCRNavigationItemContainer) item;
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
                    if (wcmsType.equals(WCMSType.root)) {
                        return gson.fromJson(item, MCRNavigation.class);
                    } else if (wcmsType.equals(WCMSType.menu)) {
                        return gson.fromJson(item, MCRNavigationMenuItem.class);
                    } else if (wcmsType.equals(WCMSType.item)) {
                        return gson.fromJson(item, MCRNavigationItem.class);
                    } else if (wcmsType.equals(WCMSType.insert)) {
                        return gson.fromJson(item, MCRNavigationInsertItem.class);
                    } else if (wcmsType.equals(WCMSType.group)) {
                        return gson.fromJson(item, MCRNavigationGroup.class);
                    }
                }
            }
        }
        return null;
    }

    private JsonObject getAccess(String href) {
        JsonObject accessObject = new JsonObject();
        if (MCRLayoutUtilities.hasRule("read", href)) {
            JsonObject readObject = new JsonObject();
            accessObject.add("read", readObject);
            readObject.addProperty("ruleID", MCRLayoutUtilities.getRuleID("read", href));
            readObject.addProperty("ruleDes", MCRLayoutUtilities.getRuleDescr("read", href));
        }
        if (MCRLayoutUtilities.hasRule("write", href)) {
            JsonObject writeObject = new JsonObject();
            accessObject.add("write", writeObject);
            writeObject.addProperty("ruleID", MCRLayoutUtilities.getRuleID("write", href));
            writeObject.addProperty("ruleDes", MCRLayoutUtilities.getRuleDescr("write", href));
        }
        return accessObject;
    }
}
