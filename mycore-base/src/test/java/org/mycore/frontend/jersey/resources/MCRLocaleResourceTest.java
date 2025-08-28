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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.frontend.jersey.filter.MCRSessionHookFilter;
import org.mycore.services.i18n.MCRTranslationTest;
import org.mycore.test.MyCoReTest;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import jakarta.ws.rs.core.MediaType;

@MyCoReTest
@MCRTestConfiguration(
    properties = {
        @MCRTestProperty(key = "MCR.Metadata.Languages", string = "de,en,it")
    })
public class MCRLocaleResourceTest {

    private MCRJerseyTestFeature jersey;

    @BeforeEach
    public void setUp() throws Exception {
        jersey = new MCRJerseyTestFeature();
        jersey.setUp(Set.of(
            MCRSessionHookFilter.class,
            MCRLocaleResource.class));
        MCRTranslationTest.reInit();
    }

    @AfterEach
    public void tearDown() throws Exception {
        jersey.tearDown();
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
