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

package org.mycore.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MCRLanguageDetectorTest extends MCRTestCase {

    @Test
    public void testLanguageDetection() {
        assertEquals("de",
            MCRLanguageDetector.detectLanguage("Das Leben ist eher kurz als lang, und wir stehen alle mittenmang"));
        assertEquals("en",
            MCRLanguageDetector.detectLanguage("MyCoRe is the best repository software currently available"));
        assertEquals("fr",
            MCRLanguageDetector.detectLanguage("Tout vient à point à qui sait attendre"));
        assertEquals("de",
            MCRLanguageDetector.detectLanguage("Ein simples β macht noch keinen Griechen aus Dir."));
        assertEquals("el",
            MCRLanguageDetector.detectLanguage("Φοβοῦ τοὺς Δαναοὺς καὶ δῶρα φέροντας"));
    }
}
