/**
 * 
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.services.i18n;

import static org.junit.Assert.assertEquals;

import java.util.Locale;
import java.util.Set;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRTranslationTest extends MCRTestCase {

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
        System.out.println(availableLanguages);
        assertEquals(5, availableLanguages.size());
    }

    @Test
    public void getDeprecatedMessageKeys() {
        assertEquals("Depreacted I18N keys do not work", "MyCoRe ID", MCRTranslation.translate("oldLabel", Locale.ENGLISH));
    }

}
