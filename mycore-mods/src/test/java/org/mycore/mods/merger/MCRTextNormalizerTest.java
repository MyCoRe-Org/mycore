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

package org.mycore.mods.merger;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

import static org.junit.Assert.assertEquals;

public class MCRTextNormalizerTest extends MCRTestCase {

    private static final char EN_DASH = '\u2013';

    private static final char COMBINING_DIAERESIS = '\u0308';

    private static final char LATIN_SMALL_LETTER_SHARP_S = '\u00DF';

    private static final char LATIN_CAPITAL_LETTER_SHARP_S = '\u1E9E';

    private static final char LATIN_SMALL_LIGATURE_FF = '\uFB00';

    private static final char LATIN_SMALL_LIGATURE_FFI = '\uFB03';

    private static final char LATIN_SMALL_LIGATURE_FFL = '\uFB04';

    private static final char LATIN_SMALL_LIGATURE_FI = '\uFB01';

    private static final char LATIN_SMALL_LIGATURE_FL = '\uFB02';

    private static final char LATIN_SMALL_LIGATURE_IJ = '\u0133';

    private static final char LATIN_CAPITAL_LIGATURE_IJ = '\u0132';

    private static final char LATIN_SMALL_LIGATURE_ST = '\uFB06';

    private static final char LATIN_SMALL_LIGATURE_LONG_ST = '\uFB05';

    @Test
    public void testNormalizeAlphabetical() {
        assertEquals("abc", MCRTextNormalizer.normalizeText("abc"));
    }

    @Test
    public void testNormalizeNumerical() {
        assertEquals("123", MCRTextNormalizer.normalizeText("123"));
    }

    @Test
    public void testNormalizeCase() {
        assertEquals("abc", MCRTextNormalizer.normalizeText("ABC"));
    }

    @Test
    public void testNormalizeHyphens() {
        assertEquals("a b c", MCRTextNormalizer.normalizeText("a" + EN_DASH + "b" + EN_DASH + "c"));
    }

    @Test
    public void testNormalizeUmlat() {
        assertEquals("a", MCRTextNormalizer.normalizeText("ä"));
        assertEquals("a", MCRTextNormalizer.normalizeText("a" + COMBINING_DIAERESIS));
        assertEquals("a", MCRTextNormalizer.normalizeText("Ä"));
        assertEquals("a", MCRTextNormalizer.normalizeText("A" + COMBINING_DIAERESIS));
        assertEquals("o", MCRTextNormalizer.normalizeText("ö"));
        assertEquals("o", MCRTextNormalizer.normalizeText("o" + COMBINING_DIAERESIS));
        assertEquals("o", MCRTextNormalizer.normalizeText("Ö"));
        assertEquals("o", MCRTextNormalizer.normalizeText("O" + COMBINING_DIAERESIS));
        assertEquals("u", MCRTextNormalizer.normalizeText("ü"));
        assertEquals("u", MCRTextNormalizer.normalizeText("u" + COMBINING_DIAERESIS));
        assertEquals("u", MCRTextNormalizer.normalizeText("Ü"));
        assertEquals("u", MCRTextNormalizer.normalizeText("U" + COMBINING_DIAERESIS));
    }

    @Test
    public void testNormalizeSharpS() {
        assertEquals("s", MCRTextNormalizer.normalizeText(Character.toString(LATIN_SMALL_LETTER_SHARP_S)));
        assertEquals("s", MCRTextNormalizer.normalizeText(Character.toString(LATIN_CAPITAL_LETTER_SHARP_S)));
    }

    @Test
    public void testNormalizeDoubleS() {
        assertEquals("s", MCRTextNormalizer.normalizeText("S"));
        assertEquals("s", MCRTextNormalizer.normalizeText("SS"));
    }

    @Test
    public void testNormalizeLigature() {
        assertEquals("ff", MCRTextNormalizer.normalizeText(Character.toString(LATIN_SMALL_LIGATURE_FF)));
        assertEquals("ffi", MCRTextNormalizer.normalizeText(Character.toString(LATIN_SMALL_LIGATURE_FFI)));
        assertEquals("ffl", MCRTextNormalizer.normalizeText(Character.toString(LATIN_SMALL_LIGATURE_FFL)));
        assertEquals("fi", MCRTextNormalizer.normalizeText(Character.toString(LATIN_SMALL_LIGATURE_FI)));
        assertEquals("fl", MCRTextNormalizer.normalizeText(Character.toString(LATIN_SMALL_LIGATURE_FL)));
        assertEquals("ij", MCRTextNormalizer.normalizeText(Character.toString(LATIN_SMALL_LIGATURE_IJ)));
        assertEquals("ij", MCRTextNormalizer.normalizeText(Character.toString(LATIN_CAPITAL_LIGATURE_IJ)));
        assertEquals("st", MCRTextNormalizer.normalizeText(Character.toString(LATIN_SMALL_LIGATURE_ST)));
        assertEquals("st", MCRTextNormalizer.normalizeText(Character.toString(LATIN_SMALL_LIGATURE_LONG_ST)));
    }

    @Test
    public void testNormalizePunctuation() {
        assertEquals("a a", MCRTextNormalizer.normalizeText("<{[(a~!@#$%a)]}>"));
    }

    @Test
    public void testNormalizeSpace() {
        assertEquals("a a", MCRTextNormalizer.normalizeText(" a   a "));
    }

}
