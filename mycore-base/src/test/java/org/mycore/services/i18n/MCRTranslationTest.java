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

package org.mycore.services.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Languages", string = "de,en,fr,pl")
})
public class MCRTranslationTest {

    /**
     * use with care: only required for Junit tests if properties changes.
     *
     * <pre>
     * MCR.Metadata.Languages=…
     * </pre>
     */
    public static void reInit() {
        MCRTranslation.reInit();
    }

    @BeforeEach
    public void cleanUp() {
        reInit();
    }

    @Test
    public void translate() {
        // default locale should be 'de'
        assertEquals("Hallo Welt", MCRTranslation.translate("junit.hello"));
        // fall back to 'de'
        assertEquals("Hallo Welt", MCRTranslation.translateToLocale("junit.hello", Locale.FRENCH));
    }

    /*
     * Test method for 'org.mycore.services.i18n.MCRTranslation.getStringArray(String)'
     */
    @Test
    public void getStringArray() {
        assertEquals(0, MCRTranslation.getStringArray(null).length);
        assertEquals(1, MCRTranslation.getStringArray("test").length);
        assertEquals(2, MCRTranslation.getStringArray("string1;string2").length);
        assertEquals("string1", MCRTranslation.getStringArray("string1;string2")[0]);
        assertEquals(2, MCRTranslation.getStringArray("string1\\;;string2").length);
        assertEquals("string1;", MCRTranslation.getStringArray("string1\\;;string2")[0]);
        assertEquals("string1\\", MCRTranslation.getStringArray("string1\\\\;string2")[0]);
    }

    @Test
    public void getAvailableLanguages() {
        Set<String> availableLanguages = MCRTranslation.getAvailableLanguages();
        assertEquals(4, availableLanguages.size(), "Expected 4 languages, but got: " + availableLanguages);
    }

    @Test
    public void getDeprecatedMessageKeys() {
        assertEquals("MyCoRe ID", MCRTranslation.translateToLocale("oldLabel", Locale.ENGLISH),
            "Depreacted I18N keys do not work");
    }

}
