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

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

/**
 * Normalizes text to be fault-tolerant when matching for duplicates.
 * Accents, umlauts, case are normalized. Punctuation and non-alphabetic/non-digit characters are removed.
 *
 * @author Frank Lützenkirchen
 */
public class MCRTextNormalizer {

    private static final char LATIN_SMALL_LETTER_SHARP_S = '\u00DF';

    private static final char LATIN_SMALL_LIGATURE_FF = '\uFB00';

    private static final char LATIN_SMALL_LIGATURE_FFI = '\uFB03';

    private static final char LATIN_SMALL_LIGATURE_FFL = '\uFB04';

    private static final char LATIN_SMALL_LIGATURE_FI = '\uFB01';

    private static final char LATIN_SMALL_LIGATURE_FL = '\uFB02';

    private static final char LATIN_SMALL_LIGATURE_IJ = '\u0133';

    private static final char LATIN_SMALL_LIGATURE_ST = '\uFB06';

    private static final char LATIN_SMALL_LIGATURE_LONG_ST = '\uFB05';

    /**
     * Normalizes text to be fault-tolerant when matching for duplicates.
     * Accents, umlauts, case are normalized. Punctuation and non-alphabetic/non-digit characters are removed.
     **/
    public String normalize(String text) {
        return normalizeText(text);
    }

    /**
     * Normalizes text to be fault-tolerant when matching for duplicates.
     * Accents, umlauts, case are normalized. Punctuation and non-alphabetic/non-digit characters are removed.
     **/
    public static String normalizeText(String text) {

        String normalizedText = text;

        // make lowercase
        normalizedText = normalizedText.toLowerCase(Locale.getDefault());

        // replace all hyphen-like characters with a single space
        normalizedText = MCRHyphenNormalizer.normalizeHyphen(normalizedText, ' ');

        // canonical decomposition
        normalizedText = Normalizer.normalize(normalizedText, Form.NFD);

        // strip accents
        normalizedText = StringUtils.stripAccents(normalizedText);

        // replace sharp s and double s with normal s
        normalizedText = normalizedText.replace(Character.toString(LATIN_SMALL_LETTER_SHARP_S), "s");
        normalizedText = normalizedText.replace("ss", "s");

        // decompose ligatures
        normalizedText = normalizedText.replace(Character.toString(LATIN_SMALL_LIGATURE_FF), "ff");
        normalizedText = normalizedText.replace(Character.toString(LATIN_SMALL_LIGATURE_FFI), "ffi");
        normalizedText = normalizedText.replace(Character.toString(LATIN_SMALL_LIGATURE_FFL), "ffl");
        normalizedText = normalizedText.replace(Character.toString(LATIN_SMALL_LIGATURE_FI), "fi");
        normalizedText = normalizedText.replace(Character.toString(LATIN_SMALL_LIGATURE_FL), "fl");
        normalizedText = normalizedText.replace(Character.toString(LATIN_SMALL_LIGATURE_IJ), "ij");
        normalizedText = normalizedText.replace(Character.toString(LATIN_SMALL_LIGATURE_ST), "st");
        normalizedText = normalizedText.replace(Character.toString(LATIN_SMALL_LIGATURE_LONG_ST), "st");

        // remove all non-alphabetic/non-digit characters
        normalizedText = normalizedText.replaceAll("[^\\p{Alpha}\\p{Digit}\\p{Space}]", " ");

        // replace all sequences of space-like characters with a single space
        normalizedText = normalizedText.replaceAll("\\p{Space}+", " ");

        // remove leading and trailing spaces
        normalizedText = normalizedText.trim();

        return normalizedText;
    }

}
