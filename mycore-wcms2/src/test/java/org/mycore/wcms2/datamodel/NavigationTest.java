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

package org.mycore.wcms2.datamodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMResult;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class NavigationTest {

    private MCRNavigation navigation;

    @Before
    public void setup() {
        this.navigation = new MCRNavigation();
        this.navigation.setTemplate("template_mysample");
        this.navigation.setDir("/content");
        this.navigation.setHistoryTitle("History Title");
        this.navigation.setHrefStartingPage("/content/below/index.xml");
        this.navigation.setMainTitle("Main Title");

        MCRNavigationMenuItem menu1 = new MCRNavigationMenuItem();
        menu1.setId("main");
        MCRNavigationMenuItem menu2 = new MCRNavigationMenuItem();
        menu1.setId("below");
        MCRNavigationInsertItem insert1 = new MCRNavigationInsertItem();
        insert1.setURI("myuri:workflow");

        this.navigation.addMenu(menu1);
        this.navigation.addInsertItem(insert1);
        this.navigation.addMenu(menu2);
    }

    @Test
    public void toXML() throws Exception {
        JAXBContext jc = JAXBContext.newInstance(MCRNavigation.class);
        Marshaller m = jc.createMarshaller();
        JDOMResult JDOMResult = new JDOMResult();
        m.marshal(this.navigation, JDOMResult);
        Element navigationElement = JDOMResult.getDocument().getRootElement();

        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(navigationElement, System.out);

        // test attributes
        assertEquals("template_mysample", navigationElement.getAttributeValue("template"));
        assertEquals("/content", navigationElement.getAttributeValue("dir"));
        assertEquals("History Title", navigationElement.getAttributeValue("historyTitle"));
        assertEquals("/content/below/index.xml", navigationElement.getAttributeValue("hrefStartingPage"));
        assertEquals("Main Title", navigationElement.getAttributeValue("mainTitle"));
        // test children
        assertEquals(2, navigationElement.getChildren("menu").size());
        assertEquals(1, navigationElement.getChildren("insert").size());
    }

    @Test
    public void fromXML() throws Exception {
        JAXBContext jc = JAXBContext.newInstance(MCRNavigation.class);
        Unmarshaller m = jc.createUnmarshaller();
        Object o = m.unmarshal(new File("src/test/resources/navigation/navigation.xml"));
        assertTrue(o instanceof MCRNavigation);
        MCRNavigation navigation = (MCRNavigation) o;

        // test navigation
        assertEquals("template1", navigation.getTemplate());
        assertEquals("{tenantPath}/content/below/index.xml", navigation.getHrefStartingPage());
        assertEquals("/content", navigation.getDir());
        assertEquals("main title", navigation.getMainTitle());
        assertEquals("history title", navigation.getHistoryTitle());
        // test menu
        MCRNavigationMenuItem menu = (MCRNavigationMenuItem) navigation.getChildren().get(0);
        assertEquals("main", menu.getId());
        assertEquals("/content/main", menu.getDir());
        assertEquals("Hauptmenü links", menu.getLabel("de"));
        assertEquals("Main menu left", menu.getLabel("en"));
        // test item
        MCRNavigationItem searchItem = (MCRNavigationItem) menu.getChildren().get(0);
        assertEquals("{tenantPath}/content/main/search.xml", searchItem.getHref());
        assertEquals(MCRNavigationItem.Type.intern, searchItem.getType());
        assertEquals(MCRNavigationItem.Target._self, searchItem.getTarget());
        assertEquals("normal", searchItem.getStyle());
        assertEquals(false, searchItem.isReplaceMenu());
        assertEquals(true, searchItem.isConstrainPopUp());
        assertEquals("Suche", searchItem.getLabel("de"));
        assertEquals("Retrieval", searchItem.getLabel("en"));
        assertEquals(2, searchItem.getChildren().size());
        // test group
        MCRNavigationGroup group = (MCRNavigationGroup) menu.getChildren().get(2);
        assertEquals("foo", group.getId());
        assertEquals("Foo-Gruppe", group.getLabel("de"));
        MCRNavigationItem foo1 = (MCRNavigationItem) group.getChildren().get(0);
        assertEquals("{tenantPath}/content/main/foo1.xml", foo1.getHref());
        assertEquals("Foo1", foo1.getLabel("de"));
    }

    @Test
    public void toJSON() throws Exception {
        Gson gson = new Gson();
        JsonElement jsonElement = gson.toJsonTree(this.navigation);
        JsonObject navigationObject = jsonElement.getAsJsonObject();

        assertEquals("template_mysample", navigationObject.get("template").getAsString());
        assertEquals("/content", navigationObject.get("dir").getAsString());
        assertEquals("History Title", navigationObject.get("historyTitle").getAsString());
        assertEquals("/content/below/index.xml", navigationObject.get("hrefStartingPage").getAsString());
        assertEquals("Main Title", navigationObject.get("mainTitle").getAsString());
    }
}
