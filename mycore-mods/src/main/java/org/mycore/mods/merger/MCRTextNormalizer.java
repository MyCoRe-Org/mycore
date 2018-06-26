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

/**
 * Normalizes text to be fault tolerant when matching for duplicates.
 * Accents, umlauts, case are normalized. Punctuation and non-alphabetic/non-digit characters are removed.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRTextNormalizer {

    public static String normalizeText(String text) {
        String normalizedText = text.toLowerCase(Locale.getDefault());
        normalizedText = new MCRHyphenNormalizer().normalize(normalizedText).replace("-", " ");
        //canonical decomposition, remove accents
        normalizedText = Normalizer.normalize(normalizedText, Form.NFD).replaceAll("\\p{M}", "");
        normalizedText = normalizedText.replace("ue", "u")
                                        .replace("oe", "o").replace("ae", "a")
                                        .replace("ß", "s").replace("ss", "s");
        //remove all non-alphabetic characters
        normalizedText = normalizedText.replaceAll("[^a-z0-9]\\s]", "");
        // remove all words with fewer than four characters
        // normalizedText = normalizedText.replaceAll("\\b.{1,3}\\b", " ").trim();
        normalizedText = normalizedText.replaceAll("\\p{Punct}", " ").trim(); // remove all punctuation
        normalizedText = normalizedText.replaceAll("\\s+", " "); // normalize whitespace
        return normalizedText;
    }
}
