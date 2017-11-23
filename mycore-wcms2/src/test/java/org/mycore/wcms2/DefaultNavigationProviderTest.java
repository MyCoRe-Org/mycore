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

package org.mycore.wcms2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mycore.wcms2.navigation.MCRWCMSDefaultNavigationProvider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * TODO: fix this test
 * 
 * @author michel
 *
 */
public class DefaultNavigationProviderTest {

    @SuppressWarnings("unused")
    private MCRWCMSDefaultNavigationProvider navProv;

    @Test
    public void toJSON() throws Exception {
        MCRWCMSUtil.load(new File("src/test/resources/navigation/navigation.xml"));
        this.navProv = new MCRWCMSDefaultNavigationProvider();
        //        JsonObject jsonNavigation = this.navProv.toJSON(navigation);

        // test hierarchy
        //        JsonArray hierarchy = jsonNavigation.get("hierarchy").getAsJsonArray();
        //        JsonObject navRootItem = hierarchy.get(0).getAsJsonObject();
        //        JsonArray rootChildren = navRootItem.get("children").getAsJsonArray();
        //        assertEquals(2, rootChildren.size());
        //        JsonObject navMenuItem = rootChildren.get(0).getAsJsonObject();
        //        JsonArray menuChildren = navMenuItem.get("children").getAsJsonArray();
        //        assertEquals(2, menuChildren.size());

        // test items
        //        JsonArray items = jsonNavigation.get("items").getAsJsonArray();
        //        // test root
        //        JsonObject rootItem = getByWCMSType("root", items).get(0);
        //        assertNotNull(rootItem);
        //        assertEquals("{tenantPath}/content/below/index.xml", rootItem.getAsJsonPrimitive("hrefStartingPage").getAsString());
        //        assertEquals("/content", rootItem.getAsJsonPrimitive("dir").getAsString());
        //        assertEquals("main title", rootItem.getAsJsonPrimitive("mainTitle").getAsString());
        //        assertEquals("history title", rootItem.getAsJsonPrimitive("historyTitle").getAsString());
        //        assertEquals("template1", rootItem.getAsJsonPrimitive("template").getAsString());
        //        assertEquals("DocPortal", rootItem.getAsJsonPrimitive("parentTenant").getAsString());
        //        assertEquals("/content/test.xml", rootItem.getAsJsonPrimitive("parentPage").getAsString());
        //        JsonArray includeList = rootItem.get("include").getAsJsonArray();
        //        assertEquals(1, includeList.size());
        //        assertEquals("test:sample", includeList.get(0).getAsJsonPrimitive().getAsString());
        //        // test menu
        //        JsonObject menuItem = getByWCMSType("menu", items).get(0);
        //        assertNotNull(menuItem);
        //        assertEquals("main", menuItem.getAsJsonPrimitive("id").getAsString());
        //        assertEquals("/content/main", menuItem.getAsJsonPrimitive("dir").getAsString());
        //        JsonObject label = menuItem.get("labelMap").getAsJsonObject();
        //        assertEquals("Hauptmenü links", label.getAsJsonPrimitive("de").getAsString());
        //        assertEquals("Main menu left", label.getAsJsonPrimitive("en").getAsString());
        //        // test includes
        //        for(JsonObject includeItem : getByWCMSType("include", items)) {
        //            String ref = includeItem.getAsJsonPrimitive("ref").getAsString();
        //            String uri = includeItem.getAsJsonPrimitive("uri").getAsString();
        //            if(ref != null)
        //                assertEquals("search", ref);
        //            if(uri != null)
        //                assertEquals("test:insertUri", uri);
        //        }
        // test item
        //        JsonObject item = getByWebpageId("/editor_form_search-expert.xml", items);
        //        assertNotNull(item);
        //        assertEquals("extern", item.getAsJsonPrimitive("type").getAsString());
        //        assertEquals("_self", item.getAsJsonPrimitive("target").getAsString());
        //        assertEquals(false, item.getAsJsonPrimitive("replaceMenu").getAsBoolean());
        //        assertEquals(true, item.getAsJsonPrimitive("constrainPopUp").getAsBoolean());
        //        assertEquals("template2", item.getAsJsonPrimitive("template").getAsString());
        //        assertEquals("bold", item.getAsJsonPrimitive("style").getAsString());
        //        JsonObject label2 = item.get("labelMap").getAsJsonObject();
        //        assertEquals("Expertensuche", label2.getAsJsonPrimitive("de").getAsString());
        //        assertEquals("Expert Search", label2.getAsJsonPrimitive("en").getAsString());
    }

    @SuppressWarnings("unused")
    private List<JsonObject> getByWCMSType(String type, JsonArray items) {
        List<JsonObject> itemList = new ArrayList<>();
        for (JsonElement e : items) {
            JsonObject item = (JsonObject) e;
            if (item.getAsJsonPrimitive("wcmsType").getAsString().equals(type))
                itemList.add(item);
        }
        return itemList;
    }

    @SuppressWarnings("unused")
    private JsonObject getByWebpageId(String id, JsonArray items) {
        for (JsonElement e : items) {
            JsonObject item = (JsonObject) e;
            if (item.getAsJsonPrimitive("href") != null && item.getAsJsonPrimitive("href").getAsString().equals(id))
                return item;
        }
        return null;
    }

    @Test
    public void fromJSON() throws Exception {
        // TODO

    }
}
