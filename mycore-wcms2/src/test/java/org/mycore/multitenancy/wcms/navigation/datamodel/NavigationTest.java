package org.mycore.multitenancy.wcms.navigation.datamodel;

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
import org.mycore.datamodel.navigation.InsertItem;
import org.mycore.datamodel.navigation.Item;
import org.mycore.datamodel.navigation.MenuItem;
import org.mycore.datamodel.navigation.Navigation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class NavigationTest {

    private Navigation navigation;

    @Before
    public void setup() {
        this.navigation = new Navigation();
        this.navigation.setTemplate("template_mysample");
        this.navigation.setDir("/content");
        this.navigation.setHistoryTitle("History Title");
        this.navigation.setHrefStartingPage("/content/below/index.xml");
        this.navigation.setMainTitle("Main Title");
        this.navigation.setParentPage("/docportal/index.xml");
        this.navigation.setParentTenant("docportal");
        this.navigation.getInclude().add("uri:a");
        this.navigation.getInclude().add("uri:b");

        MenuItem menu1 = new MenuItem();
        menu1.setId("main");
        MenuItem menu2 = new MenuItem();
        menu1.setId("below");
        InsertItem insert1 = new InsertItem();
        insert1.setURI("myuri:workflow");

        this.navigation.addMenu(menu1);
        this.navigation.addInsertItem(insert1);
        this.navigation.addMenu(menu2);
    }

    @Test
    public void toXML() throws Exception {
        JAXBContext jc = JAXBContext.newInstance(Navigation.class);
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
        assertEquals("/docportal/index.xml", navigationElement.getAttributeValue("parentPage"));
        assertEquals("docportal", navigationElement.getAttributeValue("parentTenant"));
        // test includes
        assertEquals(2, navigationElement.getChildren("include").size());
        // test children
        assertEquals(2, navigationElement.getChildren("menu").size());
        assertEquals(1, navigationElement.getChildren("insert").size());
    }

    @Test
    public void fromXML() throws Exception {
        JAXBContext jc = JAXBContext.newInstance(Navigation.class);
        Unmarshaller m = jc.createUnmarshaller();
        Object o = m.unmarshal(new File("src/test/resources/navigation/navigation.xml"));
        assertTrue(o instanceof Navigation);
        Navigation navigation = (Navigation)o;
        
        // test navigation
        assertEquals("template1", navigation.getTemplate());
        assertEquals("{tenantPath}/content/below/index.xml", navigation.getHrefStartingPage());
        assertEquals("/content", navigation.getDir());
        assertEquals("main title", navigation.getMainTitle());
        assertEquals("history title", navigation.getHistoryTitle());
        assertEquals("DocPortal", navigation.getParentTenant());
        assertEquals("/content/test.xml", navigation.getParentPage());
        assertEquals("test:sample", navigation.getInclude().get(0));
        // test menu
        MenuItem menu = (MenuItem)navigation.getChildren().get(0);
        assertEquals("main", menu.getId());
        assertEquals("/content/main", menu.getDir());
        assertEquals("Hauptmenü links", menu.getLabel("de"));
        assertEquals("Main menu left", menu.getLabel("en"));
        // test item
        Item searchItem = (Item)menu.getChildren().get(0);
        assertEquals("{tenantPath}/content/main/search.xml", searchItem.getHref());
        assertEquals(Item.Type.intern, searchItem.getType());
        assertEquals(Item.Target._self, searchItem.getTarget());
        assertEquals(Item.Style.normal, searchItem.getStyle());
        assertEquals(false, searchItem.isReplaceMenu());
        assertEquals(true, searchItem.isConstrainPopUp());
        assertEquals("Suche", searchItem.getLabel("de"));
        assertEquals("Retrieval", searchItem.getLabel("en"));
        assertEquals(2, searchItem.getChildren().size());
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
        assertEquals("/docportal/index.xml", navigationObject.get("parentPage").getAsString());
        assertEquals("docportal", navigationObject.get("parentTenant").getAsString());
        assertEquals(2, navigationObject.get("include").getAsJsonArray().size());
    }
}
