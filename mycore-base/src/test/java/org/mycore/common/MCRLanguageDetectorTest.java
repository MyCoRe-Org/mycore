/*
 * 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.common;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
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
