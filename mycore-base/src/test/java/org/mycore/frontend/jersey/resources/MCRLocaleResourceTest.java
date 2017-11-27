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
