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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.frontend.jersey.filter.MCRSessionHookFilter;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

public class MCRLocaleResourceTest extends MCRTestCase {

    private MCRJerseyTestFeature jersey;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jersey = new MCRJerseyTestFeature();
        jersey.setUp(Set.of(
            MCRSessionHookFilter.class,
            MCRLocaleResource.class));
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        jersey.tearDown();
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> map = new HashMap<>();
        map.put("MCR.Metadata.Languages", "de,en,it");
        return map;
    }

    @Test
    public void language() {
        final String language = jersey.target("locale/language").request().acceptLanguage(Locale.ITALY)
            .get(String.class);
        assertEquals("it", language);
    }

    @Test
    public void languages() {
        final String languagesString = jersey.target("locale/languages").request().get(String.class);
        JsonArray languages = JsonParser.parseString(languagesString).getAsJsonArray();
        assertEquals(3, languages.size());
    }

}
