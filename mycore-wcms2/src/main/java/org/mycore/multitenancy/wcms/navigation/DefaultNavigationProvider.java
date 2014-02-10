package org.mycore.multitenancy.wcms.navigation;

import org.apache.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.datamodel.navigation.InsertItem;
import org.mycore.datamodel.navigation.Item;
import org.mycore.datamodel.navigation.ItemContainer;
import org.mycore.datamodel.navigation.MenuItem;
import org.mycore.datamodel.navigation.Navigation;
import org.mycore.datamodel.navigation.NavigationItem;
import org.mycore.frontend.MCRLayoutUtilities;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Default implementation of <code>NavigationProvider</code>.
 *
 * @author Matthias Eichner
 */
public class DefaultNavigationProvider implements NavigationProvider {
    private static final Logger LOGGER = Logger.getLogger(DefaultSectionProvider.class);

    private static Gson gson;

    static {
        gson = new Gson();
    }

    /*
     * (non-Javadoc)
     * @see org.mycore.multitenancy.wcms.navigation.NavigationProvider#toJSON(org.mycore.datamodel.navigation.Navigation)
     */
    @Override
    public JsonObject toJSON(Navigation navigation) {
        JsonObject returnObject = new JsonObject();
        JsonArray hierarchy = new JsonArray();
        JsonArray items = new JsonArray();

        returnObject.add(JSON_HIERARCHY, hierarchy);
        returnObject.add(JSON_ITEMS, items);
        create(navigation, hierarchy, items);

        return returnObject;
    }

    private void create(NavigationItem item, JsonArray hierarchy, JsonArray items) throws MCRException {
        JsonObject hierarchyObject = add(item, hierarchy, items);
        if(item instanceof ItemContainer) {
            JsonArray childHierarchyArray = new JsonArray();
            for(NavigationItem childItem : ((ItemContainer)item).getChildren()) {
                create(childItem, childHierarchyArray, items);
            }
            if(childHierarchyArray.size() > 0)
                hierarchyObject.add(JSON_CHILDREN, childHierarchyArray);
        }
    }

    private JsonObject add(NavigationItem item, JsonArray hierarchy, JsonArray items) {
        int id = items.size();
        JsonObject jsonItem = gson.toJsonTree(item).getAsJsonObject();
        jsonItem.addProperty(JSON_WCMS_ID, id);
        jsonItem.remove(JSON_CHILDREN);
        WCMSType type = null;
        String href = null;
        if(item instanceof Navigation) {
            type = WCMSType.root;
            href = ((Navigation) item).getHrefStartingPage();
        } else if(item instanceof MenuItem) {
            type = WCMSType.menu;
            href = ((MenuItem) item).getDir();
        } else if(item instanceof Item) {
            type = WCMSType.item;
            href = ((Item) item).getHref();
        } else if(item instanceof InsertItem) {
            type = WCMSType.insert;
        } else {
            LOGGER.warn("Unable to set type for item " + id);
        }
        jsonItem.addProperty(JSON_WCMS_TYPE, type.name());
        if (href != null){
            jsonItem.add("access", getAccess(href));
        }
        items.add(jsonItem);
        // create hierarchy for root
        JsonObject hierarchyObject = new JsonObject();
        hierarchyObject.addProperty(JSON_WCMS_ID, id);
        hierarchy.add(hierarchyObject);
        return hierarchyObject;
    }

    /*
     * (non-Javadoc)
     * @see org.mycore.multitenancy.wcms.navigation.NavigationProvider#fromJSON(com.google.gson.JsonObject)
     */
    @Override
    public Navigation fromJSON(JsonObject navigationJSON) {
        JsonArray items = navigationJSON.get(JSON_ITEMS).getAsJsonArray();
        JsonArray hierarchy = navigationJSON.get(JSON_HIERARCHY).getAsJsonArray();
        if(hierarchy.size() > 0) {
            JsonObject root = hierarchy.get(0).getAsJsonObject();
            NavigationItem navigation = createItem(root, items);
            if(navigation instanceof Navigation) {
                return (Navigation)navigation;
            }
        }
        return null;
    }

    private NavigationItem createItem(JsonObject hierarchyObject, JsonArray items) {
        if(!hierarchyObject.has(JSON_WCMS_ID)) {
            LOGGER.warn("While saving navigation.xml. Invalid object in hierarchy.");
            return null;
        }
        NavigationItem item = getNavigationItem(hierarchyObject.get(JSON_WCMS_ID).getAsString(), items);
        if(item == null) {
            LOGGER.warn("While saving navigation.xml. Item with id " +
                        hierarchyObject.get(JSON_WCMS_ID) + " is null!");
            return null;
        }

        JsonElement children = hierarchyObject.get(JSON_CHILDREN);
        if(children != null && children.isJsonArray() && item instanceof ItemContainer) {
            ItemContainer itemAsContainer = (ItemContainer)item;
            for(JsonElement child : children.getAsJsonArray()) {
                if(child.isJsonObject()) {
                    NavigationItem childItem = createItem(child.getAsJsonObject(), items);
                    if(childItem != null) {
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
    private NavigationItem getNavigationItem(String wcmsId, JsonArray items) {
        for(JsonElement e : items) {
            if(e.isJsonObject()) {
                JsonObject item = e.getAsJsonObject();
                if(item.has(JSON_WCMS_ID) && item.has(JSON_WCMS_TYPE) &&
                   wcmsId.equals(item.get(JSON_WCMS_ID).getAsString())) {
                    WCMSType wcmsType = WCMSType.valueOf(item.get(JSON_WCMS_TYPE).getAsString());
                    if(wcmsType.equals(WCMSType.root)) {
                        return gson.fromJson(item, Navigation.class);
                    } else if(wcmsType.equals(WCMSType.menu)) {
                        return gson.fromJson(item, MenuItem.class);
                    } else if(wcmsType.equals(WCMSType.item)) {
                        return gson.fromJson(item, Item.class);
                    } else if(wcmsType.equals(WCMSType.insert)) {
                        return gson.fromJson(item, InsertItem.class);
                    }
                }
            }
        }
        return null;
    }
    private JsonObject getAccess(String href){
        JsonObject accessObject = new JsonObject();
        if (MCRLayoutUtilities.hasRule("read", href)){
            JsonObject readObject = new JsonObject();
            accessObject.add("read", readObject);
            readObject.addProperty("ruleID", MCRLayoutUtilities.getRuleID("read", href));
            readObject.addProperty("ruleDes", MCRLayoutUtilities.getRuleDescr("read", href));
        }
        else{
            JsonObject readObject = new JsonObject();
            accessObject.add("read", readObject);
            readObject.addProperty("ruleID", "");
            readObject.addProperty("ruleDes", "");
        }
        if (MCRLayoutUtilities.hasRule("write", href)){
            JsonObject writeObject = new JsonObject();
            accessObject.add("write", writeObject);
            writeObject.addProperty("ruleID", MCRLayoutUtilities.getRuleID("write", href));
            writeObject.addProperty("ruleDes", MCRLayoutUtilities.getRuleDescr("write", href));  
        }
        else{
            JsonObject writeObject = new JsonObject();
            accessObject.add("write", writeObject);
            writeObject.addProperty("ruleID", "");
            writeObject.addProperty("ruleDes", ""); 
        }
        return accessObject;
    }
}
