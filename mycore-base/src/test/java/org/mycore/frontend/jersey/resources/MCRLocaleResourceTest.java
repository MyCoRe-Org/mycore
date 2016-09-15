package org.mycore.frontend.jersey.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

public class MCRLocaleResourceTest extends MCRJerseyTest {

    @BeforeClass
    public static void register() {
        JERSEY_CLASSES.add(MCRLocaleResource.class);
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> map = new HashMap<>();
        map.put("MCR.Metadata.Languages", "de,en,ru");
        return map;
    }

    @Test
    public void language() {
        final String language = target("locale/language").request().get(String.class);
        assertEquals("de", language);
    }

    @Test
    public void languages() {
        final String languagesString = target("locale/languages").request().get(String.class);
        JsonArray languages = new JsonParser().parse(languagesString).getAsJsonArray();
        assertTrue(languages.size() == 3);
    }

}
