package org.mycore.wcms2;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.wcms2.navigation.MCRWCMSDefaultSectionProvider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class JSONSectionProviderTest {

    @Test
    public void toJSON() throws Exception {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("MCR.WCMS2.mycoreTagList", "");
        MCRConfiguration.instance().initialize(properties, true);
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(new File("src/test/resources/navigation/content.xml"));
        MCRWCMSDefaultSectionProvider prov = new MCRWCMSDefaultSectionProvider();
        JsonArray sectionArray = prov.toJSON(doc.getRootElement());

        assertEquals(2, sectionArray.size());
        // test section one
        JsonObject section1 = (JsonObject) sectionArray.get(0);
        assertEquals("Title one", section1.getAsJsonPrimitive("title").getAsString());
        assertEquals("de", section1.getAsJsonPrimitive("lang").getAsString());
        assertEquals("<div><p>Content one</p><br /></div>", section1.getAsJsonPrimitive("data").getAsString());
        // test section two
        JsonObject section2 = (JsonObject) sectionArray.get(1);
        assertEquals("Title two", section2.getAsJsonPrimitive("title").getAsString());
        assertEquals("en", section2.getAsJsonPrimitive("lang").getAsString());
        assertEquals("Content two", section2.getAsJsonPrimitive("data").getAsString());
    }

}
