/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.frontend.jersey.filter.MCRSessionHookFilter;
import org.mycore.services.i18n.MCRTranslationTest;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import jakarta.ws.rs.core.MediaType;

public class MCRLocaleResourceTest extends MCRTestCase {

    private MCRJerseyTestFeature jersey;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        jersey = new MCRJerseyTestFeature();
        jersey.setUp(Set.of(
            MCRSessionHookFilter.class,
            MCRLocaleResource.class));
        MCRTranslationTest.reInit();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        try {
            jersey.tearDown();
        } finally {
            super.tearDown();
        }
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> map = super.getTestProperties();
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

    @Test
    public void translate() {
        String hello = jersey.target("locale/translate/junit.hello").request().get(String.class);
        assertEquals("Hallo Welt", hello);
    }

    @Test
    public void translateToLocale() {
        String hello = jersey.target("locale/translate/en/junit.hello").request(MediaType.TEXT_PLAIN).get(String.class);
        assertEquals("Hello World", hello);
    }

}
