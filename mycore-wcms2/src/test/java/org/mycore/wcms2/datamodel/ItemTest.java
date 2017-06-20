package org.mycore.wcms2.datamodel;

import static org.junit.Assert.assertEquals;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMResult;
import org.junit.Before;
import org.junit.Test;
import org.mycore.wcms2.datamodel.MCRNavigationItem.Target;
import org.mycore.wcms2.datamodel.MCRNavigationItem.Type;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ItemTest {

    private MCRNavigationItem item;

    @Before
    public void setup() {
        this.item = new MCRNavigationItem();
        this.item.setHref("/content/main/search.xml");
        this.item.setStyle("bold");
        this.item.setTarget(Target._self);
        this.item.setType(Type.intern);
        this.item.setTemplate("template_mysample");
        this.item.setConstrainPopUp(true);
        this.item.setReplaceMenu(false);

        this.item.setI18n("item.test.key");
        this.item.addLabel("de", "Deutschland");
        this.item.addLabel("en", "England");
    }

    @Test
    public void toXML() throws Exception {
        JAXBContext jc = JAXBContext.newInstance(MCRNavigationItem.class);
        Marshaller m = jc.createMarshaller();
        JDOMResult JDOMResult = new JDOMResult();
        m.marshal(this.item, JDOMResult);
        Element itemElement = JDOMResult.getDocument().getRootElement();

        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(itemElement, System.out);

        assertEquals("template_mysample", itemElement.getAttributeValue("template"));
        assertEquals("bold", itemElement.getAttributeValue("style"));
        assertEquals("_self", itemElement.getAttributeValue("target"));
        assertEquals("intern", itemElement.getAttributeValue("type"));
        assertEquals("true", itemElement.getAttributeValue("constrainPopUp"));
        assertEquals("false", itemElement.getAttributeValue("replaceMenu"));
        assertEquals("item.test.key", itemElement.getAttributeValue("i18nKey"));

        Element label1 = (Element) itemElement.getChildren().get(0);
        Element label2 = (Element) itemElement.getChildren().get(1);

        assertEquals("Deutschland", label1.getValue());
        assertEquals("England", label2.getValue());
    }

    @Test
    public void toJSON() throws Exception {
        Gson gson = new Gson();
        JsonElement jsonElement = gson.toJsonTree(this.item);
        JsonObject navigationObject = jsonElement.getAsJsonObject();

        assertEquals("template_mysample", navigationObject.get("template").getAsString());
        assertEquals("bold", navigationObject.get("style").getAsString());
        assertEquals("_self", navigationObject.get("target").getAsString());
        assertEquals("intern", navigationObject.get("type").getAsString());
        assertEquals(true, navigationObject.get("constrainPopUp").getAsBoolean());
        assertEquals(false, navigationObject.get("replaceMenu").getAsBoolean());
    }
}
